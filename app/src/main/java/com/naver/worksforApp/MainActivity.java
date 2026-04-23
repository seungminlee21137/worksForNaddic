package com.naver.worksforApp;

import android.app.AlertDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.webkit.CookieManager;
import android.webkit.JavascriptInterface;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.model.GlideUrl;
import com.bumptech.glide.load.model.LazyHeaders;
import com.bumptech.glide.request.target.CustomTarget;
import com.bumptech.glide.request.transition.Transition;
import com.naver.worksforApp.databinding.ActivityMainBinding;

public class MainActivity extends AppCompatActivity {

    private AppBarConfiguration appBarConfiguration;
    private ActivityMainBinding binding;
    private WebView mWebView;

    private String targetURL = "https://auth.worksmobile.com/login/login?accessUrl=https%3A%2F%2Fhome.worksmobile.com%2F";

    private static final String PREF_NAME = "LoginPrefs";
    private static final String KEY_ID = "USER_ID";
    private static final String KEY_PW = "USER_PW";
    private static final String KEY_EMAIL = "email";

    private long backPressedTime = 0;
    private int backPressCount = 0;
    private Toast toast;

    private volatile String tempId = "";
    private volatile String tempPw = "";
    private volatile String tempEmail = "";

    // =================================================================
    // 1. Javascript Interface
    // =================================================================
    public class WebAppInterface {
        Context mContext;

        WebAppInterface(Context c) {
            mContext = c;
        }

        @JavascriptInterface
        public void tempSaveCredentials(String id, String pw) {
            if (id != null && !id.isEmpty()) {
                tempId = id;
                tempEmail = id;
            }
            if (pw != null && !pw.isEmpty()) {
                tempPw = pw;
            }
        }

        @JavascriptInterface
        public void updateProfile(String name, String imgUrl) {
            new Handler(Looper.getMainLooper()).post(() -> {
                if (getSupportActionBar() != null) {
                    getSupportActionBar().setTitle(" 안녕하세요, " + name + "님");
                    String fullImageUrl = imgUrl.startsWith("/") ? "https://home.worksmobile.com" + imgUrl : imgUrl;
                    loadProfileImageToActionBar(fullImageUrl);
                }
            });
        }

        // (디버그용 알림창 기능 유지)
        @JavascriptInterface
        public void showDebugDialog(String id, String pw) {
            new Handler(Looper.getMainLooper()).post(() -> {
                new AlertDialog.Builder(mContext)
                        .setTitle("데이터 캡처 확인")
                        .setMessage("ID: " + id + "\nPW: " + pw + "\n\n이 값이 맞나요?")
                        .setPositiveButton("확인", null)
                        .show();
            });
        }
    }

    // =================================================================
    // 2. Lifecycle & Initialization
    // =================================================================
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        ViewCompat.setOnApplyWindowInsetsListener(binding.getRoot(), (v, windowInsets) -> {
            Insets systemBars = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return windowInsets;
        });

        setSupportActionBar(binding.toolbar);

        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment_content_main);
        appBarConfiguration = new AppBarConfiguration.Builder(navController.getGraph()).build();
        NavigationUI.setupActionBarWithNavController(this, navController, appBarConfiguration);

        initWebView(); // 중복되던 웹뷰 셋팅을 하나의 메서드로 통합
        mWebView.loadUrl(targetURL);
    }

    private void initWebView() {
        mWebView = findViewById(R.id.webView);
        mWebView.addJavascriptInterface(new WebAppInterface(this), "Android");

        WebSettings mWebSettings = mWebView.getSettings();
        mWebSettings.setJavaScriptEnabled(true);
        mWebSettings.setUserAgentString("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/123.0.0.0 Safari/537.36");
        mWebSettings.setUseWideViewPort(true);
        mWebSettings.setLoadWithOverviewMode(true);
        mWebSettings.setSupportZoom(false);
        mWebSettings.setBuiltInZoomControls(false);
        mWebSettings.setDisplayZoomControls(false);
        mWebSettings.setLayoutAlgorithm(WebSettings.LayoutAlgorithm.TEXT_AUTOSIZING);
        mWebSettings.setDomStorageEnabled(true);
        mWebSettings.setDatabaseEnabled(true);
        mWebSettings.setCacheMode(WebSettings.LOAD_DEFAULT);
        mWebSettings.setAllowContentAccess(true);
        mWebSettings.setSaveFormData(true);

        CookieManager cookieManager = CookieManager.getInstance();
        cookieManager.setAcceptCookie(true);
        cookieManager.setAcceptThirdPartyCookies(mWebView, true);

        mWebView.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);

                if (url.contains("auth.worksmobile.com")) {
                    handleLoginPage(view);
                } else if (url.contains("home.worksmobile.com") || url.contains("v.worksmobile.com")) {
                    handleHomePage(view);
                } else if (url.contains("workplace.worksmobile.com/my-space/")) {
                    handleMySpacePage(view);
                }

                applyGlobalStylesAndFixes(view);
            }
        });
    }

    // 1. 로그인 페이지 처리 (타이핑 이벤트 제거, 버튼 클릭/엔터 키만 감지하여 임시 보관)
    // 1. 로그인 페이지 처리
    private void handleLoginPage(WebView view) {
        SharedPreferences prefs = getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        String savedEmail = prefs.getString(KEY_EMAIL, "");
        String savedPw = prefs.getString(KEY_PW, "");

        if (!savedEmail.isEmpty() && !savedPw.isEmpty()) {
            // [자동 로그인 진행 로직은 기존과 동일]
            String autoLoginScript = "javascript:(function() {" +
                    "   var pwField = document.querySelector('#user_pwd');" +
                    "   if (!pwField) {" +
                    "       var idField = document.querySelector('#user_id');" +
                    "       var startBtn = document.querySelector('#loginStart');" +
                    "       if(idField && startBtn) {" +
                    "           idField.value = '" + savedEmail + "';" +
                    "           setTimeout(function() { startBtn.click(); }, 500);" +
                    "       }" +
                    "   } else {" +
                    "       var loginBtn = document.querySelector('#loginBtn') || document.querySelector('.btn_submit');" +
                    "       if(pwField && loginBtn) {" +
                    "           pwField.value = '" + savedPw + "';" +
                    "           setTimeout(function() { loginBtn.click(); }, 500);" +
                    "       }" +
                    "   }" +
                    "})();";
            view.loadUrl(autoLoginScript);
        } else {
            // [정보 캡처] 화면의 도메인을 직접 긁어와서 합칩니다 (하드코딩 X)
            String captureScript = "javascript:(function() {" +
                    "   var idField = document.querySelector('#user_id');" +
                    "   var pwField = document.querySelector('#user_pwd');" +
                    "   var domainField = document.querySelector('#mail_domain');" + // 👈 화면의 도메인 태그 찾기
                    "   var update = function() {" +
                    "       var idVal = idField ? idField.value : '';" +
                    "       /* 입력된 아이디에 '@'가 없고, 화면에 도메인 텍스트가 존재하면 동적으로 결합 */" +
                    "       if(idVal && idVal.indexOf('@') === -1 && domainField) {" +
                    "           idVal += domainField.innerText.trim();" +
                    "       }" +
                    "       window.Android.tempSaveCredentials(idVal, pwField ? pwField.value : '');" +
                    "   };" +
                    "   var btns = document.querySelectorAll('#loginStart, #loginBtn, .btn_submit');" +
                    "   btns.forEach(function(btn) { btn.addEventListener('click', update); });" +
                    "   document.addEventListener('keydown', function(e) { if(e.key === 'Enter') update(); });" +
                    "})();";
            view.loadUrl(captureScript);
        }
    }

    // 2. 홈 페이지 처리 ("home.worksmobile.com" 진입 시점에 최종 저장)
    private void handleHomePage(WebView view) {
        // [핵심] 홈 화면에 무사히 도착했으므로, 임시로 들고 있던 정보를 기기에 완전히 저장합니다.
        saveCredentialsToPrefs();

        CookieManager.getInstance().flush();

        String profileScript = "javascript:(function() {" +
                "setTimeout(function() {" +
                "   var nameSpan = document.querySelector('.user_name .name');" +
                "   var imgTag = document.querySelector('.user_img img');" +
                "   if(nameSpan && imgTag) {" +
                "       var name = nameSpan.innerText;" +
                "       var imgUrl = imgTag.getAttribute('src');" +
                "       window.Android.updateProfile(name, imgUrl);" +
                "   }" +
                "}, 1500);" +
                "})();";
        view.loadUrl(profileScript);
    }

    // 3. 실제 저장 로직
    private void saveCredentialsToPrefs() {
        if (!tempEmail.isEmpty() && !tempPw.isEmpty()) {
            SharedPreferences prefs = getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
            prefs.edit()
                    .putString(KEY_ID, tempId)
                    .putString(KEY_EMAIL, tempEmail)
                    .putString(KEY_PW, tempPw)
                    .apply();

            Toast.makeText(MainActivity.this, "자동 로그인 정보가 성공적으로 저장되었습니다.", Toast.LENGTH_SHORT).show();

            // 저장이 끝났으므로 임시 변수는 비워줍니다.
            tempId = "";
            tempEmail = "";
            tempPw = "";
        }
    }


    // 자바스크립트를 거치지 않고 자바 로직에서 직접 SharedPreferences에 저장
    private void saveCredentials() {
        if (!tempEmail.isEmpty() && !tempPw.isEmpty()) {
            SharedPreferences prefs = getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
            prefs.edit()
                    .putString(KEY_ID, tempId)
                    .putString(KEY_EMAIL, tempEmail)
                    .putString(KEY_PW, tempPw)
                    .apply();

            runOnUiThread(() -> Toast.makeText(MainActivity.this, "자동 로그인 정보가 저장되었습니다.", Toast.LENGTH_SHORT).show());
            tempId = "";
            tempEmail = "";
            tempPw = ""; // 초기화
        }
    }

    private void handleMySpacePage(WebView view) {
        String domScript = "javascript:(function() {" +
                "document.querySelectorAll('.row.col-9').forEach(function(el) {" +
                "   el.classList.remove('col-9'); el.classList.add('col-12');" +
                "});" +
                "document.querySelectorAll('.form-group.row, .form-label.col-3').forEach(function(el) {" +
                "   el.className = '';" +
                "});" +
                "document.querySelectorAll('span').forEach(function(span) {" +
                "   var text = span.textContent.trim();" +
                "   if (text === '기준일' || text === '출근' || text === '퇴근') { span.remove(); }" +
                "});" +
                "})();";
        view.loadUrl(domScript);
    }

    private void applyGlobalStylesAndFixes(WebView view) {
        String hideAndFixScript = "javascript:(function() {" +
                "$('#workscmn_header, .top_banner_area, .widget_fixed_col, .popup-header').hide();" +
                "$('.muuri-item').each(function() { if($(this).attr('class').indexOf('attendance time_clock') == -1) { $(this).hide() } });" +
                "$('.time_clock').css({ 'maxWidth':'100%', 'left':'0', 'top':'0' });" +
                "})();";
        view.loadUrl(hideAndFixScript);

        String css = "javascript:(function() {" +
                "var style = document.createElement('style');" +
                "style.type = 'text/css';" +
                "style.innerHTML = '" +
                "   #wrap { background: rgba(241, 243, 249, 1) !important; }" +
                "   #rest_area { display: none !important; }" +
                "   .d-table-fixed { width: 100% !important; }" +
                "   .widget_cover { height: 40vh !important; }" +
                "   .btn_attendance { height: 10vh !important; font-size: xxx-large !important; border: 1px solid black !important; }" +
                "   .task_area, .task-left, .task-right {  display: none !important; }" +
                "   .widget_cover.attendance .widget_header::after {  display: none !important; }" +
                "   .widget_title { font-size: xxx-large !important; margin-top:1vh !important; margin-bottom:2vh !important; }" +
                "   .time_info { font-size: xxx-large !important; }" +
                "   .attendance_state { font-size: xx-large !important; height: 3vh !important; width: 5vh !important; line-height: 3vh !important; border-radius: 3vh !important; text-align: center !important; }" +
                "   .time { font-size: xxx-large !important; margin-top:1vh !important; margin-bottom:1vh !important; }" +
                "   .copyright {  font-size: xxx-large !important; }" +
                "   .form-bottom #btn_cancel { display: none !important; }" +
                "   .form-bottom button:not(#btn_cancel) { width: 100% !important; height: 10vh !important; margin: 2vh !important; font-size: xxx-large !important; border: 1px solid black !important; }" +
                "   .form-control { height: 10vh !important; font-size: xxx-large !important; width: 100% !important; min-width: 100% !important; }" +
                "   span { font-size: xx-large !important; }" +
                "';" +
                "document.head.appendChild(style);" +
                "})();";
        view.loadUrl(css);
    }

    // =================================================================
    // 4. Utility Methods (Menu, Glide, Back Button, Logout)
    // =================================================================
    private void loadProfileImageToActionBar(String imageUrl) {
        String cookie = CookieManager.getInstance().getCookie(imageUrl);
        GlideUrl glideUrl = new GlideUrl(imageUrl, new LazyHeaders.Builder().addHeader("Cookie", cookie).build());

        Glide.with(this).asBitmap().load(glideUrl).circleCrop().into(new CustomTarget<Bitmap>() {
            @Override
            public void onResourceReady(@NonNull Bitmap resource, @Nullable Transition<? super Bitmap> transition) {
                int halfWidth = Math.max(resource.getWidth() / 2, 1);
                int halfHeight = Math.max(resource.getHeight() / 2, 1);
                Bitmap scaledBitmap = Bitmap.createScaledBitmap(resource, halfWidth, halfHeight, true);
                Drawable resizedDrawable = new BitmapDrawable(getResources(), scaledBitmap);

                if (getSupportActionBar() != null) {
                    getSupportActionBar().setDisplayShowHomeEnabled(true);
                    getSupportActionBar().setDisplayUseLogoEnabled(true);
                    getSupportActionBar().setLogo(resizedDrawable);
                }
            }

            @Override
            public void onLoadCleared(@Nullable Drawable placeholder) {
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.action_view_info) {
            SharedPreferences prefs = getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
            String savedEmail = prefs.getString(KEY_EMAIL, "Empty");
            String savedId = prefs.getString(KEY_ID, "Empty");
            String savedPw = prefs.getString(KEY_PW, "Empty");

            String hiddenId = savedId;
            String hiddenPw = "";

            String delimiter = "@";
            try {
                if (savedId.contains(delimiter)) {
                    hiddenId = savedId.split(delimiter)[0]; // 문자가 있을 때만 split
                }
            } catch (Exception e) {
            }

            // 비밀번호 hidden
            if (!savedPw.equals("Empty")) {
                if (savedPw.length() > 5) {
                    hiddenPw = savedPw.substring(0, 3) + "*".repeat(savedPw.length() - 3);
                } else {
                    hiddenPw = savedPw.substring(0, 1) + "****";
                }
            }

            String message = "▪ 이메일: " + savedEmail + "\n" +
                    "▪ 아이디: " + hiddenId + "\n" +
                    "▪ 비밀번호: " + hiddenPw;

            new AlertDialog.Builder(this)
                    .setTitle("계정 정보")
                    .setMessage(message)
                    .setPositiveButton("확인", (dialog, which) -> dialog.dismiss())
                    .show();
            return true;
        }

        if (id == R.id.action_settings) {
            new AlertDialog.Builder(this)
                    .setTitle("로그아웃")
                    .setMessage("현재 계정정보를 삭제하고 로그아웃 하시겠습니까?")
                    .setPositiveButton("예", (dialog, which) -> performLogout())
                    .setNegativeButton("아니오", (dialog, which) -> dialog.dismiss())
                    .show();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    // 계정정보 삭제 및 로그아웃
    private void performLogout() {
        getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE).edit().clear().apply();
        CookieManager.getInstance().removeAllCookies(value ->
                new Handler(Looper.getMainLooper()).post(() -> {
                    Toast.makeText(MainActivity.this, "로그아웃 되었습니다.", Toast.LENGTH_SHORT).show();
                    finish();
                })
        );
    }

    @Override
    public void onBackPressed() {
        if (mWebView.canGoBack()) {
            mWebView.goBack();
            backPressCount = 0;
            return;
        }

        long currentTime = System.currentTimeMillis();
        if (currentTime - backPressedTime > 2000) {
            backPressCount = 1;
            backPressedTime = currentTime;
        } else {
            backPressCount++;
            backPressedTime = currentTime;
        }

        if (backPressCount == 3) {
            if (toast != null) toast.cancel();
            toast = Toast.makeText(this, "종료하겠습니다. 한 번 더 누르면 종료됩니다.", Toast.LENGTH_SHORT);
            toast.show();
        } else if (backPressCount >= 4) {
            if (toast != null) toast.cancel();
            finish();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        CookieManager.getInstance().flush();
    }

    @Override
    protected void onDestroy() {
        CookieManager.getInstance().flush();
        super.onDestroy();
    }

    @Override
    public boolean onSupportNavigateUp() {
        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment_content_main);
        return NavigationUI.navigateUp(navController, appBarConfiguration) || super.onSupportNavigateUp();
    }
}
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
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
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
    private WebSettings mWebSettings;

    //https://home.worksmobile.com
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

    // 1. 자바스크립트 인터페이스
    public class WebAppInterface {
        Context mContext;

        WebAppInterface(Context c) {
            mContext = c;
        }

        @JavascriptInterface
        public void tempSaveCredentials(String id, String pw) {
            if (id != null && !id.isEmpty()) {
                tempId = id;

                if (!id.contains("@")) {
                    tempEmail = id + "@naver.com";
                } else {
                    tempEmail = id;
                }
            }
            if (pw != null && !pw.isEmpty()) {
                tempPw = pw;
            }
        }

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

        @JavascriptInterface
        public void saveCredentials(String id, String pw) {
            if (id != null && !id.isEmpty() && pw != null && !pw.isEmpty()) {
                SharedPreferences prefs = mContext.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
                prefs.edit().putString(KEY_ID, id).putString(KEY_PW, pw).apply();

                new Handler(Looper.getMainLooper()).post(() ->
                        Toast.makeText(mContext, "자동 로그인 정보가 저장되었습니다.", Toast.LENGTH_SHORT).show()
                );
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
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // 시스템 영역 패딩 처리 (Edge-to-Edge)
        ViewCompat.setOnApplyWindowInsetsListener(binding.getRoot(), (v, windowInsets) -> {
            Insets systemBars = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return windowInsets;
        });

        setSupportActionBar(binding.toolbar);

        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment_content_main);
        appBarConfiguration = new AppBarConfiguration.Builder(navController.getGraph()).build();
        NavigationUI.setupActionBarWithNavController(this, navController, appBarConfiguration);

        mWebView = findViewById(R.id.webView);
        mWebView.addJavascriptInterface(new WebAppInterface(this), "Android");

        binding.fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String url = mWebView.getUrl();
                SharedPreferences prefs = getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
                String savedId = prefs.getString(KEY_ID, "");
                String savedPw = prefs.getString(KEY_PW, "");
                String savedEmail = prefs.getString(KEY_EMAIL, "");

                if (savedId.isEmpty() || savedPw.isEmpty()) {
                    Toast.makeText(MainActivity.this, "저장된 정보가 없습니다.", Toast.LENGTH_SHORT).show();
                    return;
                }

                boolean isStep2 = url != null && url.contains("loginParam=");
                String manualScript;

                if (!isStep2) {
                    // 1단계 화면에서 누른 경우
                    manualScript = "javascript:(function() {" +
                            "   var idField = document.querySelector('#user_id');" +
                            "   var startBtn = document.querySelector('#loginStart');" +
                            "   if(idField) idField.value = '" + savedEmail + "';" +
                            "   if(startBtn) startBtn.click();" +
                            "})();";
                } else {
                    // 2단계 화면에서 누른 경우
                    manualScript = "javascript:(function() {" +
                            "   var idField = document.querySelector('#user_id');" +
                            "   var pwField = document.querySelector('#user_pwd');" +
                            "   var loginBtn = document.querySelector('#loginBtn');" +
                            "   if(idField) idField.value = '" + savedId + "';" +
                            "   if(pwField) pwField.value = '" + savedPw + "';" +
                            "   if(loginBtn) loginBtn.click();" +
                            "})();";
                }
                mWebView.loadUrl(manualScript);
            }
        });

        // 2. 웹뷰 클라이언트 설정
        mWebView.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);

                // 로그인 페이지 URL 판별 (전달해주신 두 URL 모두 포함됨)
                if (url.contains("auth.worksmobile.com/login/login")) {
                    handleLoginPage(view);
                } else if (url.contains("home.worksmobile.com")) {
                    handleHomePage(view);
                } else if (url.contains("workplace.worksmobile.com/my-space/")) {
                    handleMySpacePage(view);
                }

                applyGlobalStylesAndFixes(view);
            }
        });

        // 3. 웹뷰 셋팅
        setupWebViewSettings();
        mWebView.loadUrl(targetURL);
    }

    private void handleLoginPage(WebView view) {
        String url = view.getUrl();
        SharedPreferences prefs = getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        String savedEmail = prefs.getString(KEY_EMAIL, ""); // 👈 이메일 불러오기
        String savedPw = prefs.getString(KEY_PW, "");

        boolean isStep2 = url != null && url.contains("loginParam=");

        if (!savedEmail.isEmpty() && !savedPw.isEmpty()) {
            // [자동 로그인 모드]
            if (!isStep2) {
                // 1단계: 저장된 '이메일' 형식을 넣고 로그인 클릭
                view.loadUrl("javascript:(function() {" +
                        "   var idField = document.querySelector('#user_id');" +
                        "   var startBtn = document.querySelector('#loginStart');" +
                        "   if(idField && startBtn) {" +
                        "       idField.value = '" + savedEmail + "';" +
                        "       setTimeout(function() { startBtn.click(); }, 500);" +
                        "   }" +
                        "})();");
            } else {
                // 2단계: 저장된 비밀번호 넣고 로그인 클릭
                view.loadUrl("javascript:(function() {" +
                        "   var pwField = document.querySelector('#user_pwd');" +
                        "   var loginBtn = document.querySelector('#loginBtn');" +
                        "   if(pwField && loginBtn) {" +
                        "       pwField.value = '" + savedPw + "';" +
                        "       setTimeout(function() { loginBtn.click(); }, 500);" +
                        "   }" +
                        "})();");
            }
        } else {
            // [정보 수집 모드] 캡처 로직은 이전과 동일 (tempSaveCredentials에서 처리함)
            String captureScript = "javascript:(function() {" +
                    "   var idField = document.querySelector('#user_id');" +
                    "   var pwField = document.querySelector('#user_pwd');" +
                    "   var update = function() {" +
                    "       window.Android.tempSaveCredentials(idField ? idField.value : '', pwField ? pwField.value : '');" +
                    "   };" +
                    "   if(idField) idField.addEventListener('blur', update);" +
                    "   if(pwField) pwField.addEventListener('blur', update);" +
                    "   var btns = document.querySelectorAll('#loginStart, #loginBtn');" +
                    "   btns.forEach(function(btn) { btn.addEventListener('click', update); });" +
                    "})();";
            view.loadUrl(captureScript);
        }
    }

    private void handleHomePage(WebView view) {
        CookieManager.getInstance().flush(); // 세션 유지

        if (!tempEmail.isEmpty() && !tempPw.isEmpty()) {
            SharedPreferences prefs = getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
            prefs.edit()
                    .putString(KEY_ID, tempId)
                    .putString(KEY_EMAIL, tempEmail)
                    .putString(KEY_PW, tempPw)
                    .apply();

            Toast.makeText(MainActivity.this, "자동 로그인 정보가 저장되었습니다.", Toast.LENGTH_SHORT).show();
            tempId = ""; tempEmail = ""; tempPw = ""; // 초기화
        }

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

    private void handleMySpacePage(WebView view) {
        // 불필요한 클래스 제거 및 DOM 요소 교체 (my-space 전용)
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
        // 1. 공통 DOM 조작 및 요소 숨김
        String hideAndFixScript = "javascript:(function() {" +
                "$('#workscmn_header, .top_banner_area, .widget_fixed_col, .popup-header').hide();" +
                "$('.muuri-item').each(function() { if($(this).attr('class').indexOf('attendance time_clock') == -1) { $(this).hide() } });" +
                "$('.time_clock').css({ 'maxWidth':'100%', 'left':'0', 'top':'0' });" +
                "})();";
        view.loadUrl(hideAndFixScript);

        // 2. 통합 CSS 주입 (추가하신 커스텀 스타일 모두 포함)
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

    private void setupWebViewSettings() {
        mWebSettings = mWebView.getSettings();
        mWebSettings.setJavaScriptEnabled(true);

        String pcUserAgent = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/123.0.0.0 Safari/537.36";
        mWebSettings.setUserAgentString(pcUserAgent);

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
    }

    // =======================================================
    // 그 외 기능 (Glide 이미지 로드, 백버튼, 로그아웃 등) 유지
    // =======================================================

    private void loadProfileImageToActionBar(String imageUrl) {
        String cookie = CookieManager.getInstance().getCookie(imageUrl);
        GlideUrl glideUrl = new GlideUrl(imageUrl, new LazyHeaders.Builder().addHeader("Cookie", cookie).build());

        Glide.with(this).asBitmap().load(glideUrl).circleCrop().into(new CustomTarget<Bitmap>() {
            @Override
            public void onResourceReady(@NonNull Bitmap resource, @Nullable Transition<? super Bitmap> transition) {
                int halfWidth = Math.max(resource.getWidth() / 2 , 1);
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
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.action_settings) {
            new AlertDialog.Builder(this)
                    .setTitle("로그아웃")
                    .setMessage("계정정보를 지우고 앱을 종료 하시겠습니까?")
                    .setPositiveButton("예", (dialog, which) -> performLogout())
                    .setNegativeButton("아니오", (dialog, which) -> dialog.dismiss())
                    .show();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

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
package com.naddic.worksfornaddic;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
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
import android.webkit.ValueCallback;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.model.GlideUrl;
import com.bumptech.glide.load.model.LazyHeaders;
import com.bumptech.glide.request.target.CustomTarget;
import com.bumptech.glide.request.transition.Transition;
// import com.google.android.material.snackbar.Snackbar; // 사용 안함
import com.naddic.worksfornaddic.databinding.ActivityMainBinding;

public class MainActivity extends AppCompatActivity {

    private AppBarConfiguration appBarConfiguration;
    private ActivityMainBinding binding;

    private WebView mWebView;
    private WebSettings mWebSettings;

    private String targetURL = "https://auth.worksmobile.com/login/login?accessUrl=http%3A%2F%2Fnaddic.ncpworkplace.com%2Fv%2Fhome%2F";

    // 저장소 식별자
    private static final String PREF_NAME = "LoginPrefs";
    private static final String KEY_ID = "USER_ID";
    private static final String KEY_PW = "USER_PW";

    // 백버튼 처리용 변수
    private long backPressedTime = 0;
    private int backPressCount = 0;
    private Toast toast;

    // 1. 자바스크립트와 통신할 인터페이스 클래스
    public class WebAppInterface {
        Context mContext;

        WebAppInterface(Context c) {
            mContext = c;
        }

        // 로그인 정보 저장 (JS -> Android)
        @JavascriptInterface
        public void saveCredentials(String id, String pw) {
            if (id != null && !id.isEmpty() && pw != null && !pw.isEmpty()) {
                SharedPreferences prefs = mContext.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
                SharedPreferences.Editor editor = prefs.edit();
                editor.putString(KEY_ID, id);
                editor.putString(KEY_PW, pw);
                editor.apply();

                new Handler(Looper.getMainLooper()).post(() -> {
                    Toast.makeText(mContext, "자동 로그인 정보가 저장되었습니다.", Toast.LENGTH_SHORT).show();
                });
            }
        }

        // 프로필 정보 업데이트 (JS -> Android)
        @JavascriptInterface
        public void updateProfile(String name, String imgUrl) {
            new Handler(Looper.getMainLooper()).post(() -> {
                if (getSupportActionBar() != null) {
                    // 타이틀(라벨) 변경
                    getSupportActionBar().setTitle(" 안녕하세요, " + name + "님");

                    // 이미지 경로 처리 (상대경로일 경우 도메인 추가)
                    String fullImageUrl = imgUrl;
                    if (imgUrl.startsWith("/")) {
                        fullImageUrl = "https://home.worksmobile.com" + imgUrl;
                    }

                    // 썸네일 이미지 로드 함수 호출
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

        setSupportActionBar(binding.toolbar);

        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment_content_main);
        appBarConfiguration = new AppBarConfiguration.Builder(navController.getGraph()).build();
        NavigationUI.setupActionBarWithNavController(this, navController, appBarConfiguration);

        mWebView = findViewById(R.id.webView);

        // 자바스크립트 인터페이스 등록
        mWebView.addJavascriptInterface(new WebAppInterface(this), "Android");

        /*
        // 앞으로 사용하지 않을 기능이므로 주석 처리함
        binding.fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Snackbar.make(view, "UI 정리 스크립트 재실행", Snackbar.LENGTH_LONG)
                        .setAnchorView(R.id.fab).show();

                mWebView.loadUrl("javascript:$('.widget_fixed_col').hide();");
                mWebView.loadUrl("javascript:$('.muuri-item').each(function() { if($(this).attr('class').indexOf('attendance time_clock') == -1) { $(this).hide() } });");
                mWebView.loadUrl("javascript:$('.time_clock').css('maxWidth','100%');");
                mWebView.loadUrl("javascript:$('.time_clock').css('left','0');");
                mWebView.loadUrl("javascript:$('.time_clock').css('top','0');");
            }
        });
        */

        // 2. 웹뷰 클라이언트 설정 (스크립트 주입)
        mWebView.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);

                // [로그인 페이지] 자동로그인 & 계정정보 가로채기
                if (url.contains("login")) {
                    SharedPreferences prefs = getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
                    String savedId = prefs.getString(KEY_ID, "");
                    String savedPw = prefs.getString(KEY_PW, "");

                    if (!savedId.isEmpty() && !savedPw.isEmpty()) {
                        String autoLoginScript = "javascript:(function() {" +
                                "var idField = document.querySelector('#login_param');" + // 실제 ID selector 확인 요망
                                "var pwField = document.querySelector('#password');" + // 실제 PW selector 확인 요망
                                "var loginBtn = document.querySelector('#loginBtn');" + // 실제 로그인 버튼 selector 확인 요망
                                "if(idField && pwField && loginBtn) {" +
                                "   idField.value = '" + savedId + "';" +
                                "   pwField.value = '" + savedPw + "';" +
                                "   loginBtn.click();" +
                                "}" +
                                "})();";
                        view.loadUrl(autoLoginScript);
                    } else {
                        String injectCaptureScript = "javascript:(function() {" +
                                "var loginBtn = document.querySelector('#loginBtn');" +
                                "if(loginBtn) {" +
                                "   loginBtn.addEventListener('click', function() {" +
                                "       var id = document.querySelector('#login_param').value;" +
                                "       var pw = document.querySelector('#password').value;" +
                                "       window.Android.saveCredentials(id, pw);" +
                                "   });" +
                                "}" +
                                "})();";
                        view.loadUrl(injectCaptureScript);
                    }
                }
                // [홈 페이지] 프로필 정보 크롤링 & 세션 강제 유지
                else if (url.contains("home.worksmobile.com")) {
                    CookieManager.getInstance().flush(); // 세션 유지

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

                // 기존 UI 정리 스크립트 + 요청하신 2가지 요소 숨김 처리 추가
                view.loadUrl("javascript:$('.widget_fixed_col').hide();");
                view.loadUrl("javascript:$('.muuri-item').each(function() { if($(this).attr('class').indexOf('attendance time_clock') == -1) { $(this).hide() } });");
                view.loadUrl("javascript:$('.time_clock').css('maxWidth','100%');");
                view.loadUrl("javascript:$('.time_clock').css('left','0');");
                view.loadUrl("javascript:$('.time_clock').css('top','0');");

                // 새로 추가된 숨김 처리 스크립트
                view.loadUrl("javascript:$('#workscmn_header').hide();");
                view.loadUrl("javascript:$('.top_banner_area').hide();");
                // 4. 출퇴근 버튼 및 모달(팝업) 버튼 스타일 강제 주입 (동적 렌더링 완벽 대응)
                String customCssScript = "javascript:(function() {" +
                        "var style = document.createElement('style');" +
                        "style.type = 'text/css';" +
                        "style.innerHTML = '" +
                        "   /* 1. 처음 화면 출퇴근 버튼 높이 고정 */" +
                        "   .btn_attendance { height: 10vh !important; font-size: xxx-large !important; border: 1px solid black !important; }" +
                        "   /* 2. 모달창 내 취소 버튼 숨김 */" +
                        "   .form-bottom #btn_cancel { display: none !important; }" +
                        "   /* 3. 모달창 내 취소가 아닌 나머지 버튼들 스타일 강제 적용 */" +
                        "   .form-bottom button:not(#btn_cancel) { " +
                        "       width: 100% !important; " +
                        "       height: 10vh !important; " +
                        "       margin: 2vh !important; " +
                        "       font-size: xxx-large !important; border: 1px solid black !important;" +
                        "   }" +
                        "   .widget_cover { height: 40vh !important; } " +
                        "';" +
                        "document.head.appendChild(style);" +
                        "})();";

                view.loadUrl(customCssScript);
            }
        });

        // 3. 웹뷰 기본 설정
        mWebSettings = mWebView.getSettings();
        mWebSettings.setJavaScriptEnabled(true);

        // PC 환경으로 다시 Agent 변경 (동작 문제 해결)
        String pcUserAgent = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/123.0.0.0 Safari/537.36";
        mWebSettings.setUserAgentString(pcUserAgent);

        mWebSettings.setUseWideViewPort(true);
        mWebSettings.setLoadWithOverviewMode(true);
        mWebSettings.setSupportZoom(false);
        mWebSettings.setBuiltInZoomControls(false);
        mWebSettings.setDisplayZoomControls(false);
        mWebSettings.setLayoutAlgorithm(WebSettings.LayoutAlgorithm.TEXT_AUTOSIZING);
        mWebSettings.setDomStorageEnabled(true);
        mWebSettings.setAllowContentAccess(true);
        mWebSettings.setSaveFormData(true);

        // 쿠키(세션) 허용 설정
        CookieManager cookieManager = CookieManager.getInstance();
        cookieManager.setAcceptCookie(true);
        cookieManager.setAcceptThirdPartyCookies(mWebView, true);

        mWebView.loadUrl(targetURL);
    }

    // 4. 프로필 이미지를 1/2로 줄여서 액션바에 적용 (Glide 사용)
    private void loadProfileImageToActionBar(String imageUrl) {
        String cookie = CookieManager.getInstance().getCookie(imageUrl);

        GlideUrl glideUrl = new GlideUrl(imageUrl, new LazyHeaders.Builder()
                .addHeader("Cookie", cookie)
                .build());

        Glide.with(this)
                .asBitmap()
                .load(glideUrl)
                .circleCrop()
                .into(new CustomTarget<Bitmap>() {
                    @Override
                    public void onResourceReady(@NonNull Bitmap resource, @Nullable Transition<? super Bitmap> transition) {
                        // 1/2로 크기 축소
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
                    public void onLoadCleared(@Nullable Drawable placeholder) {}
                });
    }

    // 5. 백버튼 로직: 히스토리 뒤로가기 우선, 없을 시 3번 알림 후 4번 종료
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
        }
        else if (backPressCount >= 4) {
            if (toast != null) toast.cancel();
            finish();
        }
    }

    // 6. 상단 메뉴 팝업 설정
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    // 7. 로그아웃 확인 팝업 및 처리
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.action_settings) {
            new AlertDialog.Builder(this)
                    .setTitle("로그아웃")
                    .setMessage("저장된 정보를 지우고 로그아웃하시겠습니까?")
                    .setPositiveButton("예", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            performLogout();
                        }
                    })
                    .setNegativeButton("아니오", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.dismiss();
                        }
                    })
                    .show();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private void performLogout() {
        // 정보 삭제
        SharedPreferences prefs = getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        prefs.edit().clear().apply();

        // 쿠키(세션) 삭제
        CookieManager cookieManager = CookieManager.getInstance();
        cookieManager.removeAllCookies(new ValueCallback<Boolean>() {
            @Override
            public void onReceiveValue(Boolean value) {
                new Handler(Looper.getMainLooper()).post(() -> {
                    Toast.makeText(MainActivity.this, "로그아웃 되었습니다.", Toast.LENGTH_SHORT).show();
                    finish(); // 앱 종료
                });
            }
        });
    }

    // 8. 앱 생명주기에 따른 세션 강제 유지 (중요)
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
package com.naddic.worksfornaddic;

import android.os.Bundle;

import com.google.android.material.snackbar.Snackbar;

import androidx.appcompat.app.AppCompatActivity;

import android.view.View;

import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;

import com.naddic.worksfornaddic.databinding.ActivityMainBinding;

import android.view.Menu;
import android.view.MenuItem;
import android.webkit.JavascriptInterface;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;

/*
$('.commute-button-box.row').children('button:eq(0)').css('height','106px;');
$('.commute-button-box.row').children('button:eq(0)').css('position','absolute');
$('.commute-button-box.row').children('button:eq(0)').css('top','-144px');
$('.commute-button-box.row').children('button:eq(0)').css('left','-21px');
$('.commute-button-box.row').children('button:eq(0)').css('z-index','9999');

$('.commute-button-box.row').children('button:eq(1)').css('height','106px ');
$('.commute-button-box.row').children('button:eq(1)').css('position','absolute');
$('.commute-button-box.row').children('button:eq(1)').css('top','-144px');
$('.commute-button-box.row').children('button:eq(1)').css('left','428px');
$('.commute-button-box.row').children('button:eq(1)').css('z-index','9999');
*/
public class MainActivity extends AppCompatActivity {

    private AppBarConfiguration appBarConfiguration;
    private ActivityMainBinding binding;

    // 웹뷰 선언 및 세팅설정
    private WebView mWebView;
    private WebSettings mWebSettings;

    private String targetURL = "https://auth.worksmobile.com/login/login?accessUrl=http%3A%2F%2Fnaddic.ncpworkplace.com%2Fv%2Fhome%2F";



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setSupportActionBar(binding.toolbar);

        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment_content_main);
        appBarConfiguration = new AppBarConfiguration.Builder(navController.getGraph()).build();
        NavigationUI.setupActionBarWithNavController(this, navController, appBarConfiguration);

        // 웹뷰 시작
        mWebView = (WebView) findViewById(R.id.webView);



        binding.fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Snackbar.make(view, "버튼-디버그모드", Snackbar.LENGTH_LONG)
                        .setAnchorView(R.id.fab)
                        .setAction("Action", null).show();

//                mWebView.getSettings().setUserAgentString("Mozilla/5.0 (Windows NT 6.1) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/41.0.2228.0 Safari/537.36");
                mWebView.getSettings().setUserAgentString("Mozilla/5.0 (Linux; Android 6.0; Nexus 5 Build/MRA58N) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/138.0.0.0 Mobile Safari/537.36");
                //mWebView.loadUrl("javascript:$('head')[0].remove();");
//                mWebView.loadUrl("javascript:$('script').remove();");
                // works css addon
                //mWebView.loadUrl("javascript:document.getElementsByTagName('head')[0].insertAdjacentHTML('beforeend','<link href=\"https://ss.ncpworkplace.com/uikit/dist/css/app.min.css?20250410-1\" rel=\"stylesheet\" >');");

//                mWebView.loadUrl("javascript:$('.btn').attr('disabled', false);");
                // bootstrap 5.0
//                mWebView.loadUrl("javascript:document.getElementsByTagName('head')[0].insertAdjacentHTML('beforeend','<link href=\"https://cdn.jsdelivr.net/npm/bootstrap@5.2.3/dist/css/bootstrap.min.css\" rel=\"stylesheet\" integrity=\"sha384-rbsA2VBKQhggwzxH7pPCaAqO46MgnOM80zW1RWuH61DGLwZJEdK2Kadq2F9CUG65\" crossorigin=\"anonymous\">');");
//                mWebView.loadUrl("javascript:document.getElementsByTagName('head')[0].insertAdjacentHTML('beforeend','<script src=\"https://cdn.jsdelivr.net/npm/bootstrap@5.2.3/dist/js/bootstrap.bundle.min.js\" integrity=\"sha384-kenU1KFdBIe4zVF0s0G1M5b4hcpxyD9F7jL+jjXkk+Q2h455rYXK/7HAuoJl+0I4\" crossorigin=\"anonymous\"></script>');");

//                mWebView.loadUrl("javascript:$('.commute-button-box.row').children(1).click();;");
//                mWebView.loadUrl("javascript:$('.btn').attr('disabled', false);");
//                mWebView.loadUrl("javascript:$('#popup_commute').addClass('show');");
//                mWebView.loadUrl("javascript:$('#popup_commute').css('display','block');");
//                mWebView.loadUrl("javascript:$('#popup_commute').attr('aria-hidden', '');;");

//                mWebView.loadUrl("javascript:$('#workscmn_header').css('display','none');");
//                mWebView.loadUrl("javascript:$('.content-header').css('display','none');");
//                mWebView.loadUrl("javascript:$('.header-bottom-right').css('display','none');");
//                mWebView.loadUrl("javascript:$('.wp-footer').css('display','none');");
//                mWebView.loadUrl("javascript:$('.contents-bottom-btn-area').css('display','none');");
                // 조직도 삭제:remove target class
//                mWebView.loadUrl("javascript:$('.personal-info-box').css('display','none');");
//                mWebView.loadUrl("javascript:$('.commute-button-box.row').children(0).addClass('btn-success');");
//                mWebView.loadUrl("javascript:$('.commute-button-box.row').children(1).addClass('btn-danger');");
//                mWebView.loadUrl("javascript:$('#popup_commute').css('display','block');");
//                mWebView.loadUrl("javascript:$('#popup_commute').attr('aria-hidden', '');");
//                mWebView.loadUrl("javascript:$('#popup_commute').removeClass('modal');");
//                mWebView.loadUrl("javascript:$('#popup_commute').children(0).removeClass('modal-dialog');");
//                mWebView.reload();

                // New-code for javascript....
//                mWebView.loadUrl("javascript:document.getElementsByClassName('widget_fixed_col')[0].style.display = 'none';");
//                mWebView.loadUrl("javascript:const itemsArray = Array.from(document.getElementsByClassName('widget_cover muuri-item'));");
//                mWebView.loadUrl("javascript:itemsArray.forEach(item => { item.className.indexOf('attendance time_clock') == -1){ item.style.display = 'none'; } });");
//                mWebView.loadUrl("javascript:document.getElementsByClassName('widget_cover muuri-item type_small')[0].style.top = '0';");
//                mWebView.loadUrl("javascript:document.getElementsByClassName('widget_cover muuri-item type_small')[0].style.left = '0';");
//                mWebView.loadUrl("javascript:document.getElementsByClassName('widget_cover muuri-item type_small')[0].style.maxWidth = '100%';");
                // 혹시모르니 버튼으로도 리로드...
                mWebView.loadUrl("javascript:$('.widget_fixed_col').hide();");
                // $(".muuri-item").each(function() { console.log($(this).attr('class'))});
                mWebView.loadUrl("javascript:$('.muuri-item').each(function() { if($(this).attr('class').indexOf('attendance time_clock') == -1) { $(this).hide() } });");
                mWebView.loadUrl("javascript:$('.time_clock').css('maxWidth','100%');");
                mWebView.loadUrl("javascript:$('.time_clock').css('left','0');");
                mWebView.loadUrl("javascript:$('.time_clock').css('top','0');");
            }
        });

        String url = "https://auth.worksmobile.com/login/login?accessUrl=http%3A%2F%2Fnaddic.ncpworkplace.com%2Fv%2Fhome%2F";
//        url = "https://getbootstrap.kr/docs/5.0/components/modal/";

        //mWebView.setWebViewClient(new WebViewClient()); // 클릭시 새창 안뜨게
        mWebView.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                view.loadUrl("javascript: alert('Loading completed.');");
                view.loadUrl("javascript:$('.widget_fixed_col').hide();");
                view.loadUrl("javascript:$('.muuri-item').each(function() { if($(this).attr('class').indexOf('attendance time_clock') == -1) { $(this).hide() } });");
                view.loadUrl("javascript:$('.time_clock').css('maxWidth','100%');");
                view.loadUrl("javascript:$('.time_clock').css('left','0');");
                view.loadUrl("javascript:$('.time_clock').css('top','0');");
            }
        });

        mWebSettings = mWebView.getSettings();                          // 세부 세팅 등록
        mWebSettings.setJavaScriptEnabled(true);                        // 웹페이지 자바스크립트 허용 여부
//        mWebSettings.setSupportMultipleWindows(true);                  // 새창 띄우기 허용 여부
//        mWebSettings.setJavaScriptCanOpenWindowsAutomatically(true);   // 자바스크립트가 창을 자동으로 열 수 있게할지 여부
        mWebSettings.setSupportZoom(true);                              // 화면 줌 허용 여부
//        mWebSettings.setBuiltInZoomControls(true);                      // 줌컨트롤
        mWebSettings.setLayoutAlgorithm(WebSettings.LayoutAlgorithm.TEXT_AUTOSIZING); // 컨텐츠 사이즈 맞추기, WebSettings.LayoutAlgorithm.SINGLE_COLUMN
        mWebSettings.setLoadWithOverviewMode(true);
        mWebSettings.setUseWideViewPort(true);
//        mWebSettings.setCacheMode(WebSettings.LOAD_NO_CACHE);           // 브라우저 캐시 허용 여부
        mWebSettings.setDomStorageEnabled(true);                        // 로컬저장소 허용 여부
//        mWebSettings.setLoadWithOverviewMode(true);                     // 메타태그 허용 여부
//        mWebSettings.setUseWideViewPort(true);

        mWebSettings.setPluginState(WebSettings.PluginState.ON_DEMAND);
        mWebSettings.setAllowContentAccess(true);
        mWebSettings.setEnableSmoothTransition(true);
        mWebSettings.setSaveFormData(true);

        // 웹뷰에 크롬 사용 허용, 이 부분이 없으면 크롬에서 alert 뜨지 않음
//        mWebView.setWebChromeClient(new WebChromeClient());


        // 웹뷰에 표시할 웹사이트 주소, 웹뷰 시작
        //mWebView.loadUrl("https://auth.worksmobile.com/login/login?accessUrl=http%3A%2F%2Fnaddic.ncpworkplace.com%2Fv%2Fhome%2F");
        mWebView.loadUrl(targetURL);

        // desktopMode
        String newUserAgent = mWebView.getSettings().getUserAgentString();
        try {
            String ua = mWebView.getSettings().getUserAgentString();
            String androidOSString = mWebView.getSettings().getUserAgentString().substring(ua.indexOf("("), ua.indexOf(")") + 1);
//          newUserAgent = mWebView.getSettings().getUserAgentString().replace(androidOSString, "(X11; Linux x86_64)");
//          mWebView.getSettings().setUserAgentString(newUserAgent);
            // 출석관련하여 user-agent 이슈가있으므로 아래설정으로 하드코딩 (For web)
            //mWebView.getSettings().setUserAgentString("Mozilla/5.0 (Windows NT 6.1) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/41.0.2228.0 Safari/537.36");
//            mWebView.getSettings().setUserAgentString("Mozilla/5.0 (Linux; Android 6.0; Nexus 5 Build/MRA58N) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/138.0.0.0 Mobile Safari/537.36");
//            mWebView.reload();

            // New-code
//            mWebView.loadUrl("javascript:document.getElementsByClassName('widget_fixed_col')[0].style.display = 'none';");
//            mWebView.loadUrl("javascript:const itemsArray = Array.from(document.getElementsByClassName('widget_cover muuri-item'));");
//            mWebView.loadUrl("javascript:itemsArray.forEach(item => { item.className.indexOf('attendance time_clock') == -1){ item.style.display = 'none'; } });");
//            mWebView.loadUrl("javascript:document.getElementsByClassName('widget_cover muuri-item type_small')[0].style.top = '0';");
//            mWebView.loadUrl("javascript:document.getElementsByClassName('widget_cover muuri-item type_small')[0].style.left = '0';");
//            mWebView.loadUrl("javascript:document.getElementsByClassName('widget_cover muuri-item type_small')[0].style.maxWidth = '100%';");

            mWebView.loadUrl("javascript:$('.widget_fixed_col').hide();");
            // sample:: $(".muuri-item").each(function() { console.log($(this).attr('class'))});
            mWebView.loadUrl("javascript:$('.muuri-item').each(function() { if($(this).attr('class').indexOf('attendance time_clock') == -1) { $(this).hide() } });");
            mWebView.loadUrl("javascript:$('.time_clock').css('maxWidth','100%');");
            mWebView.loadUrl("javascript:$('.time_clock').css('left','0');");
            mWebView.loadUrl("javascript:$('.time_clock').css('top','0');");

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    //webView 뒤로 가기 처리
    private long time = 0;
    private Toast toast;


    @Override
    public void onBackPressed() {
        if (System.currentTimeMillis() - time >= 2000) {
            // CASE 이전에 뒤로가기 키를 누른 시간이 2초 이상인 경우 (뒤로가기를 두번 누르지 않은걸로 판단)
            // Step01. 현재 누른 시간을 time에 담아줍니다. (뒤로가기를 두번 누른 간격 체크를 위해)
            time = System.currentTimeMillis();
            // Step02. 한 번 더 눌러야 종료가 됨을 Toast로 알려줍니다.
            //toast = Toast.makeText(getApplicationContext(), "뒤로 버튼을 한번 더 누르면 종료합니다.", Toast.LENGTH_SHORT);
            //toast.show();
            // 메인으로 리로드
            mWebView.loadUrl(targetURL);

        } else {
            // CASE 뒤로가기 키를 2초 이내에 두번 누른 경우
            // Step01. 이전에 Toast메시지를 지워줍니다(지우지 않으면 어플이 종료되도 일정시간 이후에 사라지기 때문)
            toast.cancel();
            // Step02. 어플을 종료해줍니다.
            finish();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            // 클릭시 서비스종료
            finish();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onSupportNavigateUp() {
        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment_content_main);
        return NavigationUI.navigateUp(navController, appBarConfiguration)
                || super.onSupportNavigateUp();
    }
}
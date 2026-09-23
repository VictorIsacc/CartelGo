package com.victorisacc.cartelgo;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.webkit.JavascriptInterface;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.TextView;

public class MainActivity extends Activity {
    private static final String START_URL = "https://victorisacc.github.io/CartelGo/";
    private static final int FILE_CHOOSER_REQUEST = 3107;

    private WebView webView;
    private ValueCallback<Uri[]> filePathCallback;

    @Override
    @SuppressLint({"SetJavaScriptEnabled", "JavascriptInterface"})
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        try {
            configureWindow();
            createWebView(savedInstanceState);
        } catch (Throwable error) {
            showStartupError(error);
        }
    }

    private void configureWindow() {
        Window window = getWindow();
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        window.addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
        window.setStatusBarColor(Color.BLACK);
        window.setNavigationBarColor(Color.BLACK);
        applyImmersiveMode();
    }

    @SuppressLint({"SetJavaScriptEnabled", "JavascriptInterface"})
    private void createWebView(Bundle savedInstanceState) {
        webView = new WebView(this);
        webView.setBackgroundColor(Color.BLACK);
        setContentView(webView);

        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setDatabaseEnabled(true);
        settings.setAllowFileAccess(true);
        settings.setAllowContentAccess(true);
        settings.setMediaPlaybackRequiresUserGesture(false);
        settings.setBuiltInZoomControls(false);
        settings.setDisplayZoomControls(false);

        String ua = settings.getUserAgentString();
        settings.setUserAgentString((ua == null ? "" : ua) + " CartelGoAndroid/1.1");

        webView.addJavascriptInterface(new CartelBridge(), "CartelNative");

        webView.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                injectNativeHooks();
                applyImmersiveMode();
            }

            @Override
            public boolean shouldOverrideUrlLoading(WebView view, android.webkit.WebResourceRequest request) {
                Uri uri = request.getUrl();
                String host = uri.getHost();
                if (host != null && host.equalsIgnoreCase("victorisacc.github.io")) {
                    return false;
                }
                try {
                    startActivity(new Intent(Intent.ACTION_VIEW, uri));
                    return true;
                } catch (Exception ignored) {
                    return false;
                }
            }
        });

        webView.setWebChromeClient(new WebChromeClient() {
            @Override
            public boolean onShowFileChooser(
                    WebView webView,
                    ValueCallback<Uri[]> filePathCallbackParam,
                    FileChooserParams fileChooserParams) {
                if (filePathCallback != null) {
                    filePathCallback.onReceiveValue(null);
                }
                filePathCallback = filePathCallbackParam;
                try {
                    Intent intent = fileChooserParams.createIntent();
                    startActivityForResult(intent, FILE_CHOOSER_REQUEST);
                    return true;
                } catch (Exception e) {
                    filePathCallback = null;
                    return false;
                }
            }
        });

        if (savedInstanceState == null) {
            webView.loadUrl(START_URL);
        } else {
            webView.restoreState(savedInstanceState);
        }
    }

    private void showStartupError(Throwable error) {
        TextView message = new TextView(this);
        message.setTextColor(Color.WHITE);
        message.setBackgroundColor(Color.rgb(7, 26, 66));
        message.setTextSize(18f);
        message.setGravity(Gravity.CENTER);
        message.setPadding(40, 40, 40, 40);
        String type = error.getClass().getSimpleName();
        String detail = error.getMessage();
        message.setText("CartelGo no ha podido iniciar.\n\n" + type +
                (detail == null ? "" : "\n" + detail) +
                "\n\nHaz una captura de esta pantalla para poder corregirlo.");
        setContentView(message);
        applyImmersiveMode();
    }

    private void injectNativeHooks() {
        if (webView == null) return;

        String js = "(function(){"
                + "if(window.__cartelGoNativeHooked)return;window.__cartelGoNativeHooked=true;"
                + "function sync(){try{var d=document.getElementById('displayView');"
                + "if(d&&!d.classList.contains('hidden')){CartelNative.enterPoster();}"
                + "else{CartelNative.exitPoster();}}catch(e){}}"
                + "var d=document.getElementById('displayView');"
                + "if(d){new MutationObserver(sync).observe(d,{attributes:true,attributeFilter:['class']});}"
                + "var s=document.getElementById('showBtn');"
                + "if(s)s.addEventListener('click',function(){CartelNative.enterPoster();},true);"
                + "var q=document.getElementById('startSequenceBtn');"
                + "if(q)q.addEventListener('click',function(){CartelNative.enterPoster();},true);"
                + "var e=document.getElementById('editBtn');"
                + "if(e)e.addEventListener('click',function(){CartelNative.exitPoster();},true);"
                + "sync();"
                + "})();";

        try {
            webView.evaluateJavascript(js, null);
        } catch (Throwable ignored) {}
    }

    @SuppressWarnings("deprecation")
    private void applyImmersiveMode() {
        try {
            final int flags = View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                    | View.SYSTEM_UI_FLAG_FULLSCREEN
                    | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                    | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                    | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                    | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION;
            getWindow().getDecorView().setSystemUiVisibility(flags);
        } catch (Throwable ignored) {}
    }

    private void enterPosterNative() {
        try {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE);
        } catch (Throwable ignored) {}
        applyImmersiveMode();
    }

    private void exitPosterNative() {
        try {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED);
        } catch (Throwable ignored) {}
        applyImmersiveMode();
    }

    private class CartelBridge {
        @JavascriptInterface
        public void enterPoster() {
            runOnUiThread(() -> enterPosterNative());
        }

        @JavascriptInterface
        public void exitPoster() {
            runOnUiThread(() -> exitPosterNative());
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        applyImmersiveMode();
        if (webView != null) {
            injectNativeHooks();
        }
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) {
            applyImmersiveMode();
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        if (webView != null) {
            try {
                webView.saveState(outState);
            } catch (Throwable ignored) {}
        }
        super.onSaveInstanceState(outState);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == FILE_CHOOSER_REQUEST && filePathCallback != null) {
            Uri[] results = WebChromeClient.FileChooserParams.parseResult(resultCode, data);
            filePathCallback.onReceiveValue(results);
            filePathCallback = null;
            applyImmersiveMode();
        }
    }

    @Override
    @SuppressWarnings("deprecation")
    public void onBackPressed() {
        if (webView == null) {
            super.onBackPressed();
            return;
        }

        String js = "(function(){var d=document.getElementById('displayView');"
                + "if(d&&!d.classList.contains('hidden')){var b=document.getElementById('editBtn');"
                + "if(b)b.click();return 'poster';}return 'normal';})();";

        webView.evaluateJavascript(js, value -> {
            if (value != null && value.contains("poster")) {
                return;
            }
            if (webView.canGoBack()) {
                webView.goBack();
            } else {
                MainActivity.super.onBackPressed();
            }
        });
    }

    @Override
    protected void onDestroy() {
        if (webView != null) {
            try {
                webView.removeJavascriptInterface("CartelNative");
                webView.destroy();
            } catch (Throwable ignored) {}
        }
        super.onDestroy();
    }
}

package fr.tabbya.instantvisio;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.webkit.WebSettings;
import android.webkit.WebView;

public class VideoCallActivity extends Activity {
    private WebView mWebView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.video_call_activity);
        mWebView = findViewById(R.id.webview);
        enableJavascript();
        String visioUrl = getVisioUrl();
        Log.d("[VideoCallActivity]", "starting video call on address: " + visioUrl);
        mWebView.loadUrl(visioUrl);
    }

    public String getVisioUrl() {
        Intent intent = this.getIntent();
        String visioUrl = intent.getStringExtra(MainActivity.VISIO_URL_EXTRA);
        return visioUrl;
    }

    public void enableJavascript() {
        WebSettings webSettings = mWebView.getSettings();
        webSettings.setJavaScriptEnabled(true);
    }
}

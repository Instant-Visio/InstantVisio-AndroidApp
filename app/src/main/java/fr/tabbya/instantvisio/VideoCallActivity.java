package fr.tabbya.instantvisio;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.webkit.PermissionRequest;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import androidx.appcompat.app.AppCompatActivity;

public class VideoCallActivity extends AppCompatActivity {
    private WebView mWebView;
    private String mPhoneNumber;
    private String mSmsBody;

    public VideoCallActivity() {}

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.video_call_activity);
        mWebView = findViewById(R.id.webview);
        configureWebview();

        String visioUrl = getVisioUrl();
        Log.d("[VideoCallActivity]", "starting video call on address: " + visioUrl);
        mWebView.loadUrl(visioUrl);

        Uri uri = Uri.parse("smsto:" + mPhoneNumber);
        Intent intent = new Intent(Intent.ACTION_SENDTO, uri);
        intent.putExtra("address","06000000");
        intent.putExtra("sms_body", mSmsBody);
        startActivity(intent);
    }

    public String getVisioUrl() {
        Intent intent = this.getIntent();
        String visioUrl = intent.getStringExtra(MainActivity.VISIO_URL_EXTRA);
        mPhoneNumber = intent.getStringExtra(MainActivity.VISIO_INVITE_PHONE_NUMBER);
        mSmsBody = intent.getStringExtra(MainActivity.VISIO_INVITE_SMS_BODY);
        return visioUrl;
    }

    public void configureWebview() {
        WebSettings webSettings = mWebView.getSettings();
        webSettings.setJavaScriptEnabled(true);
        mWebView.getSettings().setJavaScriptEnabled(true);
        mWebView.getSettings().setJavaScriptCanOpenWindowsAutomatically(true);

        mWebView.getSettings().setSaveFormData(true);
        mWebView.getSettings().setSupportZoom(false);
        mWebView.getSettings().setCacheMode(WebSettings.LOAD_NO_CACHE);
        mWebView.setWebChromeClient(getWebChromeClient());
    }

    public WebChromeClient getWebChromeClient() {
        return new WebChromeClient() {
            @Override
            public void onPermissionRequest(final PermissionRequest request) {
                try {
                    request.grant(new String[]{
                        PermissionRequest.RESOURCE_VIDEO_CAPTURE,
                        PermissionRequest.RESOURCE_AUDIO_CAPTURE
                    });
                } catch (Exception e) {
                    Log.d("VideoCallActivity", "cant grant video perms: " + e.toString());
                    e.printStackTrace();
                }
            }
        };
    }

    @Override
    public void onBackPressed() {
        if (mWebView.canGoBack()) {
            mWebView.goBack();
        } else {
            super.onBackPressed();
        }
    }
}


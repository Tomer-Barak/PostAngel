package com.postangel.screenshare;

import android.os.Bundle;
import android.webkit.WebView;

import androidx.appcompat.app.AppCompatActivity;

public class PrivacyPolicyActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        WebView webView = new WebView(this);
        setContentView(webView);
        
        // Load the privacy policy from the string resource
        String privacyPolicy = getString(R.string.privacy_policy_html);
        webView.loadDataWithBaseURL(null, privacyPolicy, "text/html", "UTF-8", null);
    }
}

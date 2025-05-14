package com.gmscan.activity;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.webkit.WebResourceRequest;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AppCompatActivity;

import com.gmscan.R;

public class WebViewActivity extends AppCompatActivity {

    private static final String TAG = "WebViewActivity";
    private WebView webView;
    private ProgressBar progressBar;
    private boolean isAuthRequest = false;
    private String redirectUriBase = null;

    @SuppressLint("SetJavaScriptEnabled")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.layout_webview);

        ImageView imgBack = findViewById(R.id.imgBack);
        TextView txtTitle = findViewById(R.id.txtTitle);
        webView = findViewById(R.id.webView);
        progressBar = findViewById(R.id.progressBar);

        // Get intent extras with null checks
        String url = getIntent().getStringExtra("url");
        String title = getIntent().getStringExtra("title");
        isAuthRequest = getIntent().getBooleanExtra("auth_request", false);
        redirectUriBase = getIntent().getStringExtra("redirect_uri_base");

        // Check if required values are present
        if (url == null) {
            Log.e(TAG, "URL is missing in intent extras");
            finish();
            return;
        }

        Log.d(TAG, "Loading URL: " + url);
        if (isAuthRequest) {
            Log.d(TAG, "This is an auth request");
            Log.d(TAG, "Redirect URI base: " + redirectUriBase);
        }

        // Configure WebView
        webView.getSettings().setJavaScriptEnabled(true);
        webView.getSettings().setDomStorageEnabled(true);
        webView.getSettings().setUserAgentString("Mozilla/5.0 (iPhone; CPU iPhone OS 15_0 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/15.0 Mobile/15E148 Safari/604.1");

        webView.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageStarted(WebView view, String url, android.graphics.Bitmap favicon) {
                progressBar.setVisibility(View.VISIBLE);
                Log.d(TAG, "Page loading started: " + url);
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                progressBar.setVisibility(View.GONE);
                Log.d(TAG, "Page loading finished: " + url);
            }

            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                String requestUrl = request.getUrl().toString();
                Log.d(TAG, "URL loading: " + requestUrl);

                // Check if this is a redirect back from authentication
                if (isAuthRequest && redirectUriBase != null &&
                        (requestUrl.startsWith(redirectUriBase) || requestUrl.contains(redirectUriBase))) {

                    Log.d(TAG, "Auth redirect detected!");
                    // Handle the redirect for authentication
                    handleAuthRedirect(request.getUrl());
                    return true;
                }

                return false; // Let the WebView handle the URL
            }
        });

        // Load the URL
        webView.loadUrl(url);

        // Set the title if available
        if (title != null) {
            txtTitle.setText(title);
        } else {
            txtTitle.setText(R.string.app_name); // Fallback to app name
        }

        imgBack.setOnClickListener(v -> finish());

        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (webView.canGoBack()) {
                    webView.goBack();
                } else {
                    finish();
                }
            }
        });
    }

    private void handleAuthRedirect(Uri uri) {
        try {
            Log.d(TAG, "Handling auth redirect: " + uri.toString());

            // For response_mode=query (which we changed in AppleAuthManager)
            String code = uri.getQueryParameter("code");
            String state = uri.getQueryParameter("state");
            String error = uri.getQueryParameter("error");

            Log.d(TAG, "Auth code: " + (code != null ? "received" : "null"));
            Log.d(TAG, "State: " + (state != null ? state : "null"));
            Log.d(TAG, "Error: " + (error != null ? error : "null"));

            // Create result intent with auth data
            Intent resultIntent = new Intent();
            if (code != null) {
                resultIntent.putExtra("auth_code", code);
            }
            if (state != null) {
                resultIntent.putExtra("state", state);
            }
            if (error != null) {
                resultIntent.putExtra("error", error);
            }

            // Set result and finish
            setResult(RESULT_OK, resultIntent);
            finish();

        } catch (Exception e) {
            Log.e(TAG, "Error handling auth redirect: " + e.getMessage(), e);
            setResult(RESULT_CANCELED);
            finish();
        }
    }
}
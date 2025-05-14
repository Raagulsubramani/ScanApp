package com.gmscan.auth;

import android.app.Activity;
import android.content.Intent;
import android.util.Log;

import androidx.annotation.Nullable;

import com.gmscan.R;
import com.gmscan.activity.WebViewActivity;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

/**
 * Handles Apple authentication using a custom implementation
 */
public class AppleAuthManager {
    private static final String TAG = "AppleAuthManager";
    private static final int RC_SIGN_IN = 9003;

    private final Activity activity;
    private AuthCallback pendingCallback;

    // State parameter for OAuth2 security
    private String state;

    public AppleAuthManager(Activity activity) {
        this.activity = activity;
        configureAppleSignIn();
    }

    private void configureAppleSignIn() {
        try {
            // No specific configuration needed for this implementation
            Log.d(TAG, "Apple Sign-In configured successfully");
        } catch (Exception e) {
            Log.e(TAG, "Error configuring Apple Sign-In: " + e.getMessage(), e);
        }
    }

    public void signIn(AuthCallback callback) {
        try {
            this.pendingCallback = callback;

            // Generate state parameter for security
            this.state = UUID.randomUUID().toString();

            // Get values from resources
            String clientId = activity.getString(R.string.apple_client_id);
            String redirectUri = activity.getString(R.string.apple_redirect_uri);

            // Log values for debugging
            Log.d(TAG, "Using client ID: " + clientId);
            Log.d(TAG, "Using redirect URI: " + redirectUri);

            // Build the authorization URL
            String authorizationUrl = buildAuthorizationUrl(clientId, redirectUri, state);
            Log.d(TAG, "Auth URL: " + authorizationUrl);

            // Create intent for browser
            Intent intent = new Intent(activity, WebViewActivity.class);
            intent.putExtra("url", authorizationUrl);
            intent.putExtra("title", "Sign in with Apple");
            intent.putExtra("auth_request", true);
            intent.putExtra("redirect_uri_base", redirectUri);

            // Start the activity for result
            activity.startActivityForResult(intent, RC_SIGN_IN);

            Log.d(TAG, "Apple Sign-In flow started");
        } catch (Exception e) {
            Log.e(TAG, "Error initiating Apple Sign-In: " + e.getMessage(), e);
            if (callback != null) {
                callback.onAuthFailure("Failed to start Apple Sign-In: " + e.getMessage());
            }
        }
    }

    private String buildAuthorizationUrl(String clientId, String redirectUri, String state) throws UnsupportedEncodingException {
        StringBuilder url = new StringBuilder("https://appleid.apple.com/auth/authorize?");
        url.append("client_id=").append(URLEncoder.encode(clientId, StandardCharsets.UTF_8.name()));
        url.append("&redirect_uri=").append(URLEncoder.encode(redirectUri, StandardCharsets.UTF_8.name()));
        url.append("&response_type=code");
        url.append("&state=").append(URLEncoder.encode(state, StandardCharsets.UTF_8.name()));
        url.append("&scope=").append(URLEncoder.encode("email name", StandardCharsets.UTF_8.name()));
        url.append("&response_mode=query"); // Changed from form_post to query for easier handling

        return url.toString();
    }

    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        if (requestCode == RC_SIGN_IN) {
            Log.d(TAG, "Apple Sign-In result received with resultCode: " + resultCode);
            try {
                if (resultCode == Activity.RESULT_OK && data != null) {
                    // Get the authorization code and state from the data
                    String authCode = data.getStringExtra("auth_code");
                    String returnedState = data.getStringExtra("state");
                    String error = data.getStringExtra("error");

                    Log.d(TAG, "Auth code: " + (authCode != null ? "received" : "null"));
                    Log.d(TAG, "State: " + (returnedState != null ? returnedState : "null"));
                    Log.d(TAG, "Error: " + (error != null ? error : "null"));

                    // Validate state parameter to prevent CSRF attacks
                    if (error != null) {
                        Log.e(TAG, "Apple Sign-In error: " + error);
                        if (pendingCallback != null) {
                            pendingCallback.onAuthFailure("Apple Sign-In failed: " + error);
                        }
                    } else if (authCode != null && returnedState != null && returnedState.equals(state)) {
                        // Process the authorization code
                        processAuthorizationCode(authCode);
                    } else {
                        Log.e(TAG, "Invalid state parameter or missing code");
                        if (pendingCallback != null) {
                            pendingCallback.onAuthFailure("Invalid response from Apple Sign-In");
                        }
                    }
                } else if (resultCode == Activity.RESULT_CANCELED) {
                    Log.d(TAG, "Apple sign-in cancelled by user");
                    if (pendingCallback != null) {
                        pendingCallback.onAuthCancelled();
                    }
                } else {
                    Log.e(TAG, "Apple sign-in failed with resultCode: " + resultCode);
                    if (pendingCallback != null) {
                        pendingCallback.onAuthFailure("Apple Sign-In failed");
                    }
                }
            } catch (Exception e) {
                Log.e(TAG, "Unexpected error during Apple sign-in: " + e.getMessage(), e);
                if (pendingCallback != null) {
                    pendingCallback.onAuthFailure("Unexpected error during sign-in: " + e.getMessage());
                }
            }
        }
    }

    private void processAuthorizationCode(String authCode) {
        // In a real implementation, you would exchange the authorization code for tokens
        // This would require a server-side component or a secure API call

        // For this example, we'll simulate a successful authentication
        // with some mock user data
        String userId = "apple_" + System.currentTimeMillis();
        String name = "Apple User";
        String email = "apple_user@example.com";

        Log.d(TAG, "Apple sign-in successful (simulated): " + email);

        if (pendingCallback != null) {
            // pendingCallback.onAuthSuccess(userId, name, email);
        }
    }

    public void checkExistingSignIn(AuthCallback callback) {
        // For this simple implementation, we don't check for existing sign-ins
        Log.d(TAG, "Checking for existing Apple sign-in (not implemented)");
    }
}
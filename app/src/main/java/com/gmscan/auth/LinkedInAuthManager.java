package com.gmscan.auth;

import static com.gmscan.GmScanApplication.getPreferenceManager;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.util.Log;
import android.widget.Toast;

import com.gmscan.activity.SelectDocumentsActivity;
import com.gmscan.model.loginRegister.LoginRegisterResponse;
import com.gmscan.model.loginRegister.User;
import com.gmscan.service.RestApiBuilder;
import com.gmscan.service.RestApiService;
import com.gmscan.utility.LoaderHelper;
import com.gmscan.utility.PreferenceManager;

import java.io.IOException;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class LinkedInAuthManager {
    private static final String TAG = "LinkedInAuthManager";
    private final Activity activity;
    private AuthCallback callback;
    private PreferenceManager preferenceManager;

    public LinkedInAuthManager(Activity activity) {
        this.activity = activity;
        this.preferenceManager = new PreferenceManager(activity);
        Log.d(TAG, "LinkedInAuthManager initialized");
    }

    public void signIn(AuthCallback callback) {
        this.callback = callback;
        Log.d(TAG, "Starting LinkedIn sign-in process");
        initiateServerSideAuth();
    }

    private void initiateServerSideAuth() {
        RestApiService apiService = RestApiBuilder.getService();

        // Use the original method without state parameter to avoid API changes
        Call<Void> call = apiService.initiateLinkedinAuth();

        call.enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                if (response.isSuccessful() && response.raw().request().url() != null) {
                    // Get the redirect URL from response
                    String authUrl = response.raw().request().url().toString();
                    Log.d(TAG, "LinkedIn auth initiated, opening URL: " + authUrl);

                    // Open the URL in a browser
                    try {
                        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(authUrl));
                        activity.startActivity(intent);
                    } catch (Exception e) {
                        Log.e(TAG, "Failed to launch browser for auth", e);
                        if (callback != null) {
                            callback.onAuthFailure("Failed to open browser: " + e.getMessage());
                        }
                    }
                } else {
                    Log.e(TAG, "Failed to initiate LinkedIn auth, response code: " +
                            (response.isSuccessful() ? "successful" : response.code()));
                    if (callback != null) {
                        callback.onAuthFailure("Failed to initiate LinkedIn auth");
                    }
                }
            }

            @Override
            public void onFailure(Call<Void> call, Throwable t) {
                Log.e(TAG, "Network error during LinkedIn auth initiation", t);
                if (callback != null) {
                    callback.onAuthFailure("Network error: " + t.getMessage());
                }
            }
        });
    }

    public void handleAuthCallback(Uri uri) {
        Log.d(TAG, "Handling LinkedIn auth callback URI: " + uri);

        // Safety check for null URI
        if (uri == null) {
            Log.e(TAG, "Received null URI in handleAuthCallback");
            if (callback != null) {
                callback.onAuthFailure("Invalid authentication response");
            }
            return;
        }

        LoaderHelper.showLoader(activity, true);

        // Check for error parameter in the callback URI
        String error = uri.getQueryParameter("error");
        if (error != null && !error.isEmpty()) {
            String errorDescription = uri.getQueryParameter("error_description");
            Log.e(TAG, "LinkedIn auth error: " + error + " - " + errorDescription);
            LoaderHelper.hideLoader();
            if (callback != null) {
                callback.onAuthFailure("Authentication error: " +
                        (errorDescription != null ? errorDescription : error));
            }
            return;
        }

        // Process the LinkedIn auth callback
        RestApiService apiService = RestApiBuilder.getService();
        String fullUrl = uri.toString();

        Call<LoginRegisterResponse> call = apiService.linkedinAuthCallback(fullUrl);

        call.enqueue(new Callback<>() {
            @Override
            public void onResponse(Call<LoginRegisterResponse> call, Response<LoginRegisterResponse> response) {
                try {
                    if (response.isSuccessful() && response.body() != null) {
                        Log.d(TAG, "LinkedIn auth callback successful");
                        LoginRegisterResponse authResponse = response.body();

                        final User user = authResponse.getUser();
                        user.setAccessToken(authResponse.getAccessToken());
                        getPreferenceManager().saveUser(user);
                        getPreferenceManager().setLoginIn(true);


                        if (authResponse.getUser() != null) {
                            Log.d(TAG, "User data received, completing auth");

                            // Notify callback of success if available
                            if (callback != null) {
                                callback.onAuthSuccess(
                                        authResponse.getUser().getId(),
                                        authResponse.getUser().getFirstName() + authResponse.getUser().getLastName(),
                                        authResponse.getUser().getEmail()
                                );
                            }

                            // Navigate to SelectDocumentsActivity
                            navigateToSelectDocuments();
                        } else {
                            Log.e(TAG, "Auth response has no user data");
                            LoaderHelper.hideLoader();
                            if (callback != null) {
                                callback.onAuthFailure("Auth response missing user data");
                            }
                        }
                    } else {
                        LoaderHelper.hideLoader();
                        int code = response.code();
                        Log.e(TAG, "LinkedIn auth callback failed with HTTP " + code);
                        if (callback != null) {
                            String errorBody = "";
                            try {
                                if (response.errorBody() != null) {
                                    errorBody = response.errorBody().string();
                                    Log.e(TAG, "Error body: " + errorBody);
                                }
                            } catch (IOException e) {
                                Log.e(TAG, "Error reading error body", e);
                            }
                            callback.onAuthFailure("Authentication failed: HTTP " + code + " " + errorBody);
                        }
                    }
                } catch (Exception e) {
                    LoaderHelper.hideLoader();
                    Log.e(TAG, "Exception during auth response processing", e);
                    if (callback != null) {
                        callback.onAuthFailure("Processing error: " + e.getMessage());
                    }
                }
            }

            @Override
            public void onFailure(Call<LoginRegisterResponse> call, Throwable t) {
                LoaderHelper.hideLoader();
                Log.e(TAG, "Network error during LinkedIn auth callback", t);
                if (callback != null) {
                    callback.onAuthFailure("Network error: " + t.getMessage());
                }
            }
        });
    }

    private void navigateToSelectDocuments() {
        try {
            LoaderHelper.hideLoader();
            Intent intent = new Intent(activity, SelectDocumentsActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            activity.startActivity(intent);
            activity.finish();
        } catch (Exception e) {
            Log.e(TAG, "Error navigating to SelectDocumentsActivity", e);
            LoaderHelper.hideLoader();
            Toast.makeText(activity, "Error opening document selection screen", Toast.LENGTH_SHORT).show();
        }
    }

    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        Log.d(TAG, "onActivityResult: requestCode=" + requestCode + ", resultCode=" + resultCode);

        try {
            if (resultCode == Activity.RESULT_OK && data != null) {
                // Extract data from the Intent
                Uri uri = data.getData();
                if (uri != null) {
                    Log.d(TAG, "Received callback URI: " + uri);
                    handleAuthCallback(uri);
                } else {
                    Log.e(TAG, "No URI data in activity result");
                    if (callback != null) {
                        callback.onAuthFailure("No authentication data received");
                    }
                }
            } else if (resultCode == Activity.RESULT_CANCELED) {
                Log.d(TAG, "LinkedIn authentication was canceled by user");
                if (callback != null) {
                    callback.onAuthFailure("Authentication canceled");
                }
            } else {
                Log.e(TAG, "LinkedIn authentication failed, resultCode: " + resultCode);
                if (callback != null) {
                    callback.onAuthFailure("Authentication failed");
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error in onActivityResult", e);
            if (callback != null) {
                callback.onAuthFailure("Error processing authentication result: " + e.getMessage());
            }
        }
    }
}
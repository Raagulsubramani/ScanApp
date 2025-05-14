package com.gmscan.auth;

import static com.gmscan.GmScanApplication.getPreferenceManager;

import android.app.Activity;
import android.content.Intent;
import android.util.Log;

import androidx.annotation.Nullable;

import com.gmscan.R;
import com.gmscan.model.loginRegister.LoginRegisterResponse;
import com.gmscan.model.loginRegister.User;
import com.gmscan.service.RestApiBuilder;
import com.gmscan.service.RestApiService;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.common.api.CommonStatusCodes;
import com.google.android.gms.tasks.Task;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class GoogleAuthManager {
    private static final String TAG = "GoogleAuthManager";
    private static final int RC_SIGN_IN = 9001;
    private final Activity activity;
    private final GoogleSignInClient googleSignInClient;
    private AuthCallback callback;

    public GoogleAuthManager(Activity activity) {
        this.activity = activity;

        // Check Google Play Services availability
        checkGooglePlayServices();

        // Configure Google Sign-In - FIXED: Use web client ID directly instead of string resource
        GoogleSignInOptions gso;
        try {
            // IMPORTANT: Replace YOUR_WEB_CLIENT_ID with your actual web client ID from Google Developer Console
            // This must match the client ID registered in your Google Cloud Console for OAuth 2.0 client IDs
            String webClientId = activity.getString(R.string.default_web_client_id);

            // OPTION 1: If you still want to use the resource string but ensure it's correct
            try {
                webClientId = activity.getString(R.string.default_web_client_id);
                Log.d(TAG, "Using web client ID from resources: " + webClientId);
            } catch (Exception e) {
                Log.e(TAG, "Error getting web client ID from resources", e);
            }

            gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                    .requestEmail()
                    .requestIdToken(webClientId)
                    .build();

            Log.d(TAG, "GoogleSignInOptions configured successfully");
        } catch (Exception e) {
            Log.e(TAG, "Error configuring GoogleSignInOptions", e);
            // Fall back to basic configuration without ID token if there's an issue
            gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                    .requestEmail()
                    .build();

            Log.d(TAG, "Falling back to basic GoogleSignInOptions without ID token");
        }

        try {
            this.googleSignInClient = GoogleSignIn.getClient(activity, gso);
            Log.d(TAG, "GoogleSignInClient created successfully");
        } catch (Exception e) {
            Log.e(TAG, "Failed to create GoogleSignInClient", e);
            throw new RuntimeException("Failed to initialize Google Sign-In", e);
        }
    }

    private void checkGooglePlayServices() {
        GoogleApiAvailability googleApiAvailability = GoogleApiAvailability.getInstance();
        int resultCode = googleApiAvailability.isGooglePlayServicesAvailable(activity);

        if (resultCode != ConnectionResult.SUCCESS) {
            Log.e(TAG, "Google Play Services not available: " + resultCode);
            if (googleApiAvailability.isUserResolvableError(resultCode)) {
                googleApiAvailability.getErrorDialog(activity, resultCode, 9000).show();
            }
        } else {
            Log.d(TAG, "Google Play Services is available");
        }
    }

    public void signIn(AuthCallback callback) {
        this.callback = callback;
        Log.d(TAG, "Starting sign-in process");

        try {
            // Sign out first to clear any potential issues with previous sign-in state
            googleSignInClient.signOut().addOnCompleteListener(task -> {
                startSignInFlow();
            });
        } catch (Exception e) {
            Log.e(TAG, "Exception during sign-in setup", e);
            if (callback != null) {
                callback.onAuthFailure("Sign-in process error: " + e.getMessage());
            }
        }
    }

    private void startSignInFlow() {
        try {
            Intent signInIntent = googleSignInClient.getSignInIntent();
            if (signInIntent != null) {
                Log.d(TAG, "Launching sign-in intent");
                activity.startActivityForResult(signInIntent, RC_SIGN_IN);
            } else {
                Log.e(TAG, "Failed to get sign-in intent");
                if (callback != null) {
                    callback.onAuthFailure("Failed to create sign-in intent");
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Exception during sign-in process", e);
            if (callback != null) {
                callback.onAuthFailure("Sign-in process error: " + e.getMessage());
            }
        }
    }

    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        Log.d(TAG, "onActivityResult - requestCode: " + requestCode + ", resultCode: " + resultCode);

        if (requestCode == RC_SIGN_IN) {
            try {
                Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
                if (task == null) {
                    Log.e(TAG, "Sign-in task is null");
                    if (callback != null) {
                        callback.onAuthFailure("Sign-in process returned null task");
                    }
                    return;
                }

                handleSignInResult(task);
            } catch (Exception e) {
                Log.e(TAG, "Exception in onActivityResult", e);
                if (callback != null) {
                    callback.onAuthFailure("Error processing sign-in result: " + e.getMessage());
                }
            }
        }
    }

    private void handleSignInResult(Task<GoogleSignInAccount> completedTask) {
        try {
            Log.d(TAG, "Processing sign-in result");
            GoogleSignInAccount account = completedTask.getResult(ApiException.class);

            if (account != null) {
                Log.d(TAG, "Sign-in successful for email: " +
                        (account.getEmail() != null ? account.getEmail() : "null"));

                // Get token and send to backend
                String idToken = account.getIdToken();
                if (idToken != null) {
                    Log.d(TAG, "ID token obtained, verifying with backend");
                    verifyGoogleToken(idToken, account);
                } else {
                    Log.d(TAG, "No ID token available, using account info directly");
                    // If no token is available, just use the Google account information
                    if (callback != null) {
                        String userId = account.getId();
                        String name = account.getDisplayName();
                        String email = account.getEmail();

                        if (userId != null && email != null) {
                            callback.onAuthSuccess(userId, name != null ? name : "", email);
                        } else {
                            callback.onAuthFailure("Incomplete account information");
                        }
                    }
                }
            } else {
                Log.e(TAG, "Sign-in successful but account is null");
                if (callback != null) {
                    callback.onAuthFailure("Received null account from Google");
                }
            }
        } catch (ApiException e) {
            int statusCode = e.getStatusCode();
            String statusMessage = getStatusCodeString(statusCode);

            Log.e(TAG, "Google sign-in failed with status code: " + statusCode +
                    " (" + statusMessage + ")", e);

            if (statusCode == CommonStatusCodes.CANCELED || statusCode == 12501) {
                if (callback != null) {
                    callback.onAuthCancelled();
                }
            } else if (statusCode == CommonStatusCodes.DEVELOPER_ERROR) {
                Log.e(TAG, "Developer error - check your Google Cloud Console configuration");
                if (callback != null) {
                    callback.onAuthFailure("Google Sign-In configuration error. Please check your setup.");
                }
            } else {
                String errorDetails = "Code: " + statusCode + " (" + statusMessage + ")";
                if (callback != null) {
                    callback.onAuthFailure("Google sign in failed: " + errorDetails);
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Unexpected error handling sign-in result", e);
            if (callback != null) {
                callback.onAuthFailure("Unexpected error: " + e.getMessage());
            }
        }
    }

    private String getStatusCodeString(int statusCode) {
        switch (statusCode) {
            case CommonStatusCodes.SUCCESS: return "SUCCESS";
            case CommonStatusCodes.TIMEOUT: return "TIMEOUT";
            case CommonStatusCodes.CANCELED: return "CANCELED";
            case CommonStatusCodes.API_NOT_CONNECTED: return "API_NOT_CONNECTED";
            case CommonStatusCodes.DEVELOPER_ERROR: return "DEVELOPER_ERROR";
            case CommonStatusCodes.ERROR: return "ERROR";
            case CommonStatusCodes.INTERNAL_ERROR: return "INTERNAL_ERROR";
            case CommonStatusCodes.INVALID_ACCOUNT: return "INVALID_ACCOUNT";
            case CommonStatusCodes.NETWORK_ERROR: return "NETWORK_ERROR";
            case CommonStatusCodes.RESOLUTION_REQUIRED: return "RESOLUTION_REQUIRED";
            case CommonStatusCodes.SIGN_IN_REQUIRED: return "SIGN_IN_REQUIRED";
            case 12500: return "SIGN_IN_CANCELLED";
            case 12501: return "SIGN_IN_CURRENTLY_IN_PROGRESS";
            case 12502: return "SIGN_IN_FAILED";
            default: return "UNKNOWN (" + statusCode + ")";
        }
    }

    private void verifyGoogleToken(String idToken, GoogleSignInAccount account) {
        Log.d(TAG, "Verifying Google token with backend");
        Map<String, String> tokenData = new HashMap<>();
        tokenData.put("token", idToken);

        RestApiService apiService = RestApiBuilder.getService();
        Call<LoginRegisterResponse> call = apiService.verifyGoogleToken(tokenData);

        call.enqueue(new Callback<>() {
            @Override
            public void onResponse(Call<LoginRegisterResponse> call, Response<LoginRegisterResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    Log.d(TAG, "Token verification successful");
                    LoginRegisterResponse authResponse = response.body();

                    final User user = authResponse.getUser();
                    user.setFileUrl(account.getPhotoUrl().toString());
                    user.setAccessToken(authResponse.getAccessToken());
                    getPreferenceManager().saveUser(user);
                    getPreferenceManager().setLoginIn(true);


                    if (callback != null) {
                        if (authResponse.getUser() != null) {
                            Log.d(TAG, "User data received from backend");
                            callback.onAuthSuccess(
                                    authResponse.getUser().getId(),
                                    authResponse.getUser().getFirstName() + authResponse.getUser().getLastName(),
                                    authResponse.getUser().getEmail()
                            );
                        } else {
                            Log.e(TAG, "Backend response missing user data");
                            // Fall back to using Google account data
                            String userId = account.getId();
                            String name = account.getDisplayName();
                            String email = account.getEmail();

                            if (userId != null && email != null) {
                                callback.onAuthSuccess(userId, name != null ? name : "", email);
                            } else {
                                callback.onAuthFailure("Backend response missing user data");
                            }
                        }
                    }
                } else {
                    int code = response.code();
                    Log.e(TAG, "Token verification failed with code " + code);
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

                        // Fall back to using Google account data on API failure
                        String userId = account.getId();
                        String name = account.getDisplayName();
                        String email = account.getEmail();

                        if (userId != null && email != null) {
                            Log.d(TAG, "API failed, using Google account info instead");
                            callback.onAuthSuccess(userId, name != null ? name : "", email);
                        } else {
                            callback.onAuthFailure("Token verification failed: HTTP " + code + " " + errorBody);
                        }
                    }
                }
            }

            @Override
            public void onFailure(Call<LoginRegisterResponse> call, Throwable t) {
                Log.e(TAG, "Network error during token verification", t);
                if (callback != null) {
                    // Fall back to Google account info on network failure
                    String userId = account.getId();
                    String name = account.getDisplayName();
                    String email = account.getEmail();

                    if (userId != null && email != null) {
                        Log.d(TAG, "Network error, falling back to Google account info");
                        callback.onAuthSuccess(userId, name != null ? name : "", email);
                    } else {
                        callback.onAuthFailure("Network error: " + t.getMessage());
                    }
                }
            }
        });
    }

    public void signOut() {
        try {
            Log.d(TAG, "Starting sign-out process");
            googleSignInClient.signOut().addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    Log.d(TAG, "User signed out successfully");
                } else {
                    Log.e(TAG, "Sign-out failed", task.getException());
                }
            });
        } catch (Exception e) {
            Log.e(TAG, "Error during sign-out", e);
        }
    }

    public void checkExistingSignIn(AuthCallback authCallback) {
    }
}
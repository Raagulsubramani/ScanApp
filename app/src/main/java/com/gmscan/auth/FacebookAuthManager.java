package com.gmscan.auth;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

import androidx.annotation.Nullable;

import com.facebook.AccessToken;
import com.facebook.CallbackManager;
import com.facebook.FacebookCallback;
import com.facebook.FacebookException;
import com.facebook.FacebookSdk;
import com.facebook.GraphRequest;
import com.facebook.GraphResponse;
import com.facebook.login.LoginManager;
import com.facebook.login.LoginResult;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Arrays;

/**
 * Handles Facebook authentication
 */
public class FacebookAuthManager {
    private static final String TAG = "FacebookAuthManager";

    private Activity activity;
    private CallbackManager callbackManager;
    private AuthCallback pendingCallback;

    public FacebookAuthManager(Activity activity) {
        this.activity = activity;
        configureFacebookLogin();
    }

    private void configureFacebookLogin() {
        try {
            // Initialize Facebook SDK
            FacebookSdk.sdkInitialize(activity.getApplicationContext());
            callbackManager = CallbackManager.Factory.create();

            LoginManager.getInstance().registerCallback(callbackManager,
                    new FacebookCallback<LoginResult>() {
                        @Override
                        public void onSuccess(LoginResult loginResult) {
                            Log.d(TAG, "Facebook login successful");
                            handleFacebookAccessToken(loginResult.getAccessToken());
                        }

                        @Override
                        public void onCancel() {
                            Log.d(TAG, "Facebook login canceled");
                            if (pendingCallback != null) {
                                pendingCallback.onAuthCancelled();
                            }
                        }

                        @Override
                        public void onError(FacebookException exception) {
                            Log.e(TAG, "Facebook login error", exception);
                            if (pendingCallback != null) {
                                pendingCallback.onAuthFailure("Facebook login failed: " + exception.getMessage());
                            }
                        }
                    });
            Log.d(TAG, "Facebook Login configured successfully");
        } catch (Exception e) {
            Log.e(TAG, "Error configuring Facebook Login: " + e.getMessage());
        }
    }

    public void signIn(AuthCallback callback) {
        try {
            this.pendingCallback = callback;

            // Clear any previous login state
            LoginManager.getInstance().logOut();

            // Request email and public profile permissions
            LoginManager.getInstance().logInWithReadPermissions(activity, Arrays.asList("email", "public_profile"));

            Log.d(TAG, "Facebook login initiated");
        } catch (Exception e) {
            Log.e(TAG, "Error initiating Facebook Login: " + e.getMessage());
            if (callback != null) {
                callback.onAuthFailure("Failed to start Facebook Login: " + e.getMessage());
            }
        }
    }

    private void handleFacebookAccessToken(AccessToken accessToken) {
        Log.d(TAG, "handleFacebookAccessToken:" + accessToken);

        // Get user profile data
        GraphRequest request = GraphRequest.newMeRequest(
                accessToken,
                new GraphRequest.GraphJSONObjectCallback() {
                    @Override
                    public void onCompleted(JSONObject object, GraphResponse response) {
                        try {
                            if (object != null) {
                                String id = object.getString("id");
                                String name = object.getString("name");
                                String email = "";
                                if (object.has("email")) {
                                    email = object.getString("email");
                                }

                                Log.d(TAG, "Facebook user ID: " + id);
                                Log.d(TAG, "Facebook user name: " + name);
                                Log.d(TAG, "Facebook user email: " + email);

                                // Handle successful login
                                if (pendingCallback != null) {
                                    pendingCallback.onAuthSuccess(id, name, email);
                                }
                            } else {
                                Log.e(TAG, "Facebook user data is null");
                                if (pendingCallback != null) {
                                    pendingCallback.onAuthFailure("Failed to get Facebook user data");
                                }
                            }
                        } catch (JSONException e) {
                            Log.e(TAG, "Error parsing Facebook user data", e);
                            if (pendingCallback != null) {
                                pendingCallback.onAuthFailure("Error parsing Facebook user data: " + e.getMessage());
                            }
                        }
                    }
                });

        Bundle parameters = new Bundle();
        parameters.putString("fields", "id,name,email");
        request.setParameters(parameters);
        request.executeAsync();
    }

    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        // Pass the activity result to the Facebook SDK
        callbackManager.onActivityResult(requestCode, resultCode, data);
    }

    public void checkExistingSignIn(AuthCallback callback) {
        try {
            // Check if user is already signed in with Facebook
            AccessToken accessToken = AccessToken.getCurrentAccessToken();
            boolean isLoggedIn = accessToken != null && !accessToken.isExpired();

            if (isLoggedIn) {
                Log.d(TAG, "User already signed in with Facebook");
                // Get user info from the access token
                handleFacebookAccessToken(accessToken);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error checking existing Facebook sign-in: " + e.getMessage(), e);
        }
    }
}

package com.gmscan.auth;

import android.app.Activity;
import android.content.Intent;

import androidx.annotation.Nullable;

/**
 * Central manager for handling authentication methods
 */
public class AuthenticationManager {
    private final GoogleAuthManager googleAuthManager;
    private final LinkedInAuthManager linkedInAuthManager;

    public AuthenticationManager(Activity activity) {
        this.googleAuthManager = new GoogleAuthManager(activity);
        this.linkedInAuthManager = new LinkedInAuthManager(activity);
    }

    public void signInWithGoogle(AuthCallback callback) {
        googleAuthManager.signIn(callback);
    }

    public void signInWithLinkedIn(AuthCallback callback) {
        linkedInAuthManager.signIn(callback);
    }

    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        googleAuthManager.onActivityResult(requestCode, resultCode, data);
        linkedInAuthManager.onActivityResult(requestCode, resultCode, data);
    }

    public void checkExistingSignIn(AuthCallback authCallback) {
        googleAuthManager.checkExistingSignIn(authCallback);
        // LinkedIn doesn't maintain persistent sessions in the same way as Google
        // so we don't check for existing LinkedIn sign-ins
    }
}
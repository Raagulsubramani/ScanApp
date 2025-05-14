package com.gmscan.activity;

import static com.gmscan.GmScanApplication.getPreferenceManager;
import static com.gmscan.utility.AppConstant.SPLASH_DISPLAY_DURATION;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.view.Window;
import android.view.WindowManager;
import android.util.Log;

import androidx.appcompat.app.AppCompatActivity;

import com.gmscan.R;

/**
 * SplashActivity displays a splash screen before navigating to the appropriate activity.
 */
@SuppressLint("CustomSplashScreen")
public class SplashActivity extends AppCompatActivity {
    private static final String TAG = "SplashActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.layout_splash);

        new Handler().postDelayed(() -> {
            Intent intent;

            boolean isWelcomeCompleted = getPreferenceManager().isWelcomeCompleted();
            boolean isOnboardingCompleted = getPreferenceManager().isOnboardingCompleted();
            boolean isLoggedIn = getPreferenceManager().isLoggedIn();

            Log.d(TAG, "Welcome completed: " + isWelcomeCompleted);
            Log.d(TAG, "Onboarding completed: " + isOnboardingCompleted);
            Log.d(TAG, "Logged in: " + isLoggedIn);

            if (!isWelcomeCompleted) {
                intent = new Intent(SplashActivity.this, WelcomeActivity.class);
                Log.d(TAG, "Navigating to WelcomeActivity");
            } else if (!isOnboardingCompleted) {
                intent = new Intent(SplashActivity.this, OnBoardActivity.class);
                Log.d(TAG, "Navigating to OnBoardActivity");
            } else if (!isLoggedIn) {
                intent = new Intent(SplashActivity.this, LoginActivity.class);
                Log.d(TAG, "Navigating to LoginActivity");
            } else {
                intent = new Intent(SplashActivity.this, SelectDocumentsActivity.class);
                Log.d(TAG, "Navigating to SelectDocumentsActivity");
            }

            startActivity(intent);
            finish();
        }, SPLASH_DISPLAY_DURATION);
    }
}
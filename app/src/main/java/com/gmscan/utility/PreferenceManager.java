package com.gmscan.utility;

import android.content.Context;
import android.content.SharedPreferences;

import com.gmscan.model.loginRegister.User;
import com.google.gson.Gson;

public class PreferenceManager {
    private static final String PREF_NAME = "gmscan_preferences";
    private static final String KEY_WELCOME_COMPLETED = "welcome_completed";
    private static final String KEY_ONBOARDING_COMPLETED = "onboarding_completed";
    private static final String KEY_IS_LOGIN = "is_login";
    private static final String KEY_USER_OBJECT = "user_object"; // Key for storing User object

    private SharedPreferences sharedPreferences;
    private SharedPreferences.Editor editor;
    private Gson gson;

    public PreferenceManager(Context context) {
        sharedPreferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        editor = sharedPreferences.edit();
        gson = new Gson(); // Initialize Gson
    }

    // Store the User object as JSON
    public void saveUser(User user) {
        String userJson = gson.toJson(user); // Convert user object to JSON
        editor.putString(KEY_USER_OBJECT, userJson);
        editor.apply();
    }

    // Retrieve the User object
    public User getUser() {
        String userJson = sharedPreferences.getString(KEY_USER_OBJECT, null);
        if (userJson != null) {
            return gson.fromJson(userJson, User.class); // Convert JSON back to User object
        }
        return null;
    }

    // Remove the stored user object (e.g., on logout)
    public void clearUser() {
        editor.remove(KEY_USER_OBJECT);
        editor.apply();
    }

    // Check if Welcome Screen is completed
    public boolean isWelcomeCompleted() {
        return sharedPreferences.getBoolean(KEY_WELCOME_COMPLETED, false);
    }

    // Mark Welcome Screen as completed
    public void setWelcomeCompleted(boolean isCompleted) {
        editor.putBoolean(KEY_WELCOME_COMPLETED, isCompleted);
        editor.apply();
    }

    // Check if Onboarding Screen is completed
    public boolean isOnboardingCompleted() {
        return sharedPreferences.getBoolean(KEY_ONBOARDING_COMPLETED, false);
    }

    // Mark Onboarding Screen as completed
    public void setOnboardingCompleted(boolean isCompleted) {
        editor.putBoolean(KEY_ONBOARDING_COMPLETED, isCompleted);
        editor.apply();
    }

    // Check if the user is logged in
    public boolean isLoggedIn() {
        return sharedPreferences.getBoolean(KEY_IS_LOGIN, false);
    }

    // Set login status
    public void setLoginIn(boolean isLogin) {
        editor.putBoolean(KEY_IS_LOGIN, isLogin);
        editor.apply();
    }

    public String getString(String linkedinStateParam, String s) {
        return linkedinStateParam;
    }

    public void saveString(String linkedinStateParam, String stateParam) {

    }
}

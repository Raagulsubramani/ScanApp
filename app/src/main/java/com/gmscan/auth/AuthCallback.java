package com.gmscan.auth;

/**
 * Common interface for all authentication callbacks
 */
public interface AuthCallback {
    void onAuthSuccess(String userId, String name, String email);
    void onAuthFailure(String errorMessage);
    void onAuthCancelled();
}
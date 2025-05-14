package com.gmscan.activity;

import static com.gmscan.GmScanApplication.getPreferenceManager;
import static com.gmscan.utility.RegexUtils.isValidEmail;
import static com.gmscan.utility.RegexUtils.isValidPassword;

import android.content.Intent;
import android.graphics.Typeface;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.gmscan.R;
import com.gmscan.auth.AuthCallback;
import com.gmscan.auth.AuthenticationManager;
import com.gmscan.model.loginRegister.ErrorResponse;
import com.gmscan.model.loginRegister.LoginRegisterResponse;
import com.gmscan.model.loginRegister.User;
import com.gmscan.service.RestApiBuilder;
import com.gmscan.service.RestApiService;
import com.gmscan.utility.LoaderHelper;
import com.gmscan.utility.NetworkUtils;
import com.google.android.material.textfield.TextInputEditText;
import com.google.gson.Gson;

import java.util.Objects;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class LoginActivity extends AppCompatActivity {
    // Sign In Views
    private TextInputEditText emailEditText;
    private TextInputEditText passwordEditText;
    private TextView forgotPasswordText;
    private Button signInButton;

    // Sign Up Views
    private TextInputEditText fullNameEditText;
    private TextInputEditText signUpEmailEditText;
    private TextInputEditText signUpPasswordEditText;
    private Button signUpButton;

    // Common Views
    private TextView signInTab;
    private TextView signUpTab;
    private ImageButton googleAuthButton;
    private ImageButton linkedInAuthButton; // Added LinkedIn auth button

    // Containers for login/signup forms
    private LinearLayout signInContainer;
    private LinearLayout signUpContainer;

    // Welcome text
    private TextView welcomeText;
    private TextView subtitleText;

    // Underline Views
    private View signInUnderline;
    private View signUpUnderline;

    private static final String TAG = "LoginActivity";

    // Authentication Manager
    private AuthenticationManager authManager;

    private void initializeViews() {
        signInTab = findViewById(R.id.signInTab);
        signUpTab = findViewById(R.id.signUpTab);
        googleAuthButton = findViewById(R.id.googleAuthButton);
        linkedInAuthButton = findViewById(R.id.linkedinAuthButton); // Initialize LinkedIn button
        welcomeText = findViewById(R.id.welcomeText);
        subtitleText = findViewById(R.id.subtitleText);

        // Underline views
        signInUnderline = findViewById(R.id.signInUnderline);
        signUpUnderline = findViewById(R.id.signUpUnderline);

        // Containers
        signInContainer = findViewById(R.id.signInContainer);
        signUpContainer = findViewById(R.id.signUpContainer);

        // Sign In views
        emailEditText = findViewById(R.id.emailEditText);
        passwordEditText = findViewById(R.id.passwordEditText);
        forgotPasswordText = findViewById(R.id.forgotPasswordText);
        signInButton = findViewById(R.id.signInButton);

        // Sign Up views
        fullNameEditText = findViewById(R.id.fullNameEditText);
        signUpEmailEditText = findViewById(R.id.signUpEmailEditText);
        signUpPasswordEditText = findViewById(R.id.signUpPasswordEditText);
        signUpButton = findViewById(R.id.signUpButton);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        try {
            setContentView(R.layout.layout_activity_sign_in);

            // Initialize views
            initializeViews();

            // Initialize Authentication Manager
            authManager = new AuthenticationManager(this);

            // Set click listeners
            setClickListeners();

            // Default to sign in
            showSignInForm();
        } catch (Exception e) {
            Log.e(TAG, "Error in onCreate: " + e.getMessage(), e);
            Toast.makeText(this, "Error initializing app: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private void setClickListeners() {
        // Tab Navigation
        signInTab.setOnClickListener(v -> showSignInForm());
        signUpTab.setOnClickListener(v -> showSignUpForm());

        // Sign In Button
        signInButton.setOnClickListener(v -> {
            String email = Objects.requireNonNull(emailEditText.getText()).toString().trim();
            String password = Objects.requireNonNull(passwordEditText.getText()).toString().trim();
            performSignIn(email, password);
        });

        // Sign Up Button
        signUpButton.setOnClickListener(v -> {
            String fullName = Objects.requireNonNull(fullNameEditText.getText()).toString().trim();
            String email = Objects.requireNonNull(signUpEmailEditText.getText()).toString().trim();
            String password = Objects.requireNonNull(signUpPasswordEditText.getText()).toString().trim();
            performSignUp(fullName, email, password);
        });

        // Forgot Password
        forgotPasswordText.setOnClickListener(v -> {
            Intent intent = new Intent(LoginActivity.this, ForgotPasswordActivity.class);
            startActivity(intent);
        });

        // Google Auth Button
        googleAuthButton.setOnClickListener(v -> signInWithGoogle());

        // LinkedIn Auth Button
        linkedInAuthButton.setOnClickListener(v -> signInWithLinkedIn());
    }

    private void signInWithGoogle() {
        authManager.signInWithGoogle(createAuthCallback("Google"));
    }

    private void signInWithLinkedIn() {
        authManager.signInWithLinkedIn(createAuthCallback("LinkedIn"));
    }

    private AuthCallback createAuthCallback(final String provider) {
        return new AuthCallback() {
            @Override
            public void onAuthSuccess(String userId, String name, String email) {
                handleSuccessfulSocialLogin(userId, name, email, provider);
            }

            @Override
            public void onAuthFailure(String errorMessage) {
                Log.e(TAG, provider + " login error: " + errorMessage);
                Toast.makeText(LoginActivity.this, errorMessage, Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onAuthCancelled() {
                Log.d(TAG, provider + " login cancelled");
                Toast.makeText(LoginActivity.this, provider + " login cancelled", Toast.LENGTH_SHORT).show();
            }
        };
    }

    private void handleSuccessfulSocialLogin(String userId, String name, String email, String provider) {
        Log.d(TAG, provider + " login successful - User ID: " + userId + ", Name: " + name + ", Email: " + email);
        Toast.makeText(this, "Signed in as " + name + " via " + provider, Toast.LENGTH_SHORT).show();

        // Navigate to next activity
        Intent intent = new Intent(LoginActivity.this, SelectDocumentsActivity.class);
        startActivity(intent);
        finish(); // Close login activity
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        // Pass activity results to the auth manager
        authManager.onActivityResult(requestCode, resultCode, data);
    }

    private void showSignInForm() {
        // Update tab styling
        signInTab.setTextColor(ContextCompat.getColor(this, android.R.color.white));
        signInTab.setAlpha(1.0f);
        signInTab.setTextSize(16);
        signInTab.setTypeface(signInTab.getTypeface(), Typeface.BOLD);

        signUpTab.setTextColor(ContextCompat.getColor(this, android.R.color.white));
        signUpTab.setAlpha(0.7f);
        signUpTab.setTextSize(16);
        signUpTab.setTypeface(signUpTab.getTypeface(), Typeface.NORMAL);

        // Toggle underlines
        signInUnderline.setVisibility(View.VISIBLE);
        signUpUnderline.setVisibility(View.INVISIBLE);

        // Update welcome text
        welcomeText.setText(getString(R.string.welcome_back));
        subtitleText.setText(getString(R.string.sign_in_to_continue));

        // Show/hide appropriate containers
        signInContainer.setVisibility(View.VISIBLE);
        signUpContainer.setVisibility(View.GONE);
    }

    private void showSignUpForm() {
        // Update tab styling
        signUpTab.setTextColor(ContextCompat.getColor(this, android.R.color.white));
        signUpTab.setAlpha(1.0f);
        signUpTab.setTextSize(16);
        signUpTab.setTypeface(signUpTab.getTypeface(), Typeface.BOLD);

        signInTab.setTextColor(ContextCompat.getColor(this, android.R.color.white));
        signInTab.setAlpha(0.7f);
        signInTab.setTextSize(16);
        signInTab.setTypeface(signInTab.getTypeface(), Typeface.NORMAL);

        // Toggle underlines
        signInUnderline.setVisibility(View.INVISIBLE);
        signUpUnderline.setVisibility(View.VISIBLE);

        // Update welcome text
        welcomeText.setText(getString(R.string.create_an_account));
        subtitleText.setText(getString(R.string.sign_up_to_get_started));

        // Show/hide appropriate containers
        signInContainer.setVisibility(View.GONE);
        signUpContainer.setVisibility(View.VISIBLE);
    }

    private void performSignIn(String email, String password) {
        if (email.isEmpty()) {
            Toast.makeText(this, getString(R.string.please_enter_email), Toast.LENGTH_SHORT).show();
            return;
        }
        if (!isValidEmail(email)) {
            Toast.makeText(this, getString(R.string.invalid_email), Toast.LENGTH_SHORT).show();
            return;
        }
        if (password.isEmpty()) {
            Toast.makeText(this, getString(R.string.please_enter_password), Toast.LENGTH_SHORT).show();
            return;
        }
        if (!isValidPassword(password)) {
            Toast.makeText(this, getString(R.string.invalid_password), Toast.LENGTH_LONG).show();
            return;
        }
        User user = new User();
        user.isLogin = true;
        user.setEmail(email);
        user.setPassword(password);
        manageUserApiCall(user);
    }

    private void performSignUp(String fullName, String email, String password) {
        if (fullName.isEmpty()) {
            Toast.makeText(this, getString(R.string.please_enter_fullname), Toast.LENGTH_SHORT).show();
            return;
        }
        if (email.isEmpty()) {
            Toast.makeText(this, getString(R.string.please_enter_email), Toast.LENGTH_SHORT).show();
            return;
        }
        if (!isValidEmail(email)) {
            Toast.makeText(this, getString(R.string.invalid_email), Toast.LENGTH_SHORT).show();
            return;
        }
        if (password.isEmpty()) {
            Toast.makeText(this, getString(R.string.please_enter_password), Toast.LENGTH_SHORT).show();
            return;
        }
        if (!isValidPassword(password)) {
            Toast.makeText(this, getString(R.string.invalid_password), Toast.LENGTH_LONG).show();
            return;
        }

        User user = new User();
        user.isLogin = false;
        user.setFirstName(fullName);
        user.setLastName(fullName);
        user.setEmail(email);
        user.setPassword(password);
        manageUserApiCall(user);
    }

    @Override
    protected void onStart() {
        super.onStart();

        // Check if user is already signed in with Google
        authManager.checkExistingSignIn(new AuthCallback() {
            @Override
            public void onAuthSuccess(String userId, String name, String email) {
                // User is already logged in, skip to next screen
                Log.d(TAG, "User already signed in: " + name);

                if (getPreferenceManager() != null && getPreferenceManager().isLoggedIn()) {
                    Intent intent = new Intent(LoginActivity.this, SelectDocumentsActivity.class);
                    startActivity(intent);
                    finish();
                }
            }

            @Override
            public void onAuthFailure(String errorMessage) {
                // Not logged in or error checking login state, do nothing
                Log.d(TAG, "No existing sign-in found or error: " + errorMessage);
            }

            @Override
            public void onAuthCancelled() {
                // Not applicable for checking existing sign-in
            }
        });
    }

    /**
     * Handles user authentication (login or registration) through the API.
     */
    private void manageUserApiCall(User user) {
        if (!NetworkUtils.isInternetAvailable(this)) {
            Toast.makeText(this, getString(R.string.no_internet), Toast.LENGTH_SHORT).show();
            return;
        }
        LoaderHelper.showLoader(this, true);

        RestApiService apiService = RestApiBuilder.getService();
        Call<LoginRegisterResponse> call;

        if (user.isLogin) {
            call = apiService.loginUser(user);
        } else {
            call = apiService.registerUser(user);
        }

        call.enqueue(new Callback<>() {
            @Override
            public void onResponse(@NonNull Call<LoginRegisterResponse> call, @NonNull Response<LoginRegisterResponse> response) {
                LoaderHelper.hideLoader();

                if (response.isSuccessful()) {
                    LoginRegisterResponse loginResponse = response.body();
                    if (loginResponse != null) {
                        final User user = loginResponse.getUser();
                        user.setAccessToken(loginResponse.getAccessToken());
                        getPreferenceManager().saveUser(user);
                        getPreferenceManager().setLoginIn(true);
                        Intent intent = new Intent(LoginActivity.this, SelectDocumentsActivity.class);
                        startActivity(intent);
                        finish();
                    }
                } else {
                    try {
                        if (response.errorBody() != null) {
                            Gson gson = new Gson();
                            ErrorResponse errorResponse = gson.fromJson(response.errorBody().charStream(), ErrorResponse.class);
                            String errorMessage = errorResponse != null && errorResponse.getDetail() != null
                                    ? errorResponse.getDetail()
                                    : getString(R.string.default_error_message);

                            Toast.makeText(LoginActivity.this, errorMessage, Toast.LENGTH_LONG).show();
                        }
                    } catch (Exception e) {
                        Toast.makeText(LoginActivity.this, getString(R.string.default_error_message), Toast.LENGTH_LONG).show();
                    }
                }
            }


            @Override
            public void onFailure(@NonNull Call<LoginRegisterResponse> call, @NonNull Throwable t) {
                LoaderHelper.hideLoader();
                Toast.makeText(LoginActivity.this, getString(R.string.network_error), Toast.LENGTH_LONG).show();
            }
        });
    }
}
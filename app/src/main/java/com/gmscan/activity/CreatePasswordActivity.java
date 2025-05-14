package com.gmscan.activity;

import static com.gmscan.utility.RegexUtils.isValidPassword;

import android.app.Dialog;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.util.Log;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.Toast;
import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.gmscan.model.BaseResponse;
import com.gmscan.model.loginRegister.ErrorResponse;
import com.gmscan.model.resetPassword.ResetPasswordRequest;
import com.gmscan.service.RestApiBuilder;
import com.gmscan.service.RestApiService;
import com.gmscan.utility.IntentKeys;
import com.gmscan.utility.LoaderHelper;
import com.gmscan.utility.NetworkUtils;
import com.google.android.material.textfield.TextInputEditText;
import com.gmscan.R;
import com.google.gson.Gson;

import android.content.Intent;

import java.util.Objects;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class CreatePasswordActivity extends AppCompatActivity {
    private TextInputEditText newPasswordEditText, confirmPasswordEditText;
    private Button continueButton;
    private ImageButton backButton;
    private static final String TAG = "CreatePasswordActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.layout_create_password);
        initializeView();
        setClickListeners();
    }

    private void setClickListeners() {
        // Set up button click listener
        continueButton.setOnClickListener(v -> validatePasswords());

        // Set up back button click listener
        backButton.setOnClickListener(v -> {
            finish(); // Navigate to the previous screen
        });

        // Handle back press using OnBackPressedDispatcher
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                finish();
            }
        });
    }

    private void initializeView() {
        // Initialize views
        newPasswordEditText = findViewById(R.id.newPasswordEditText);
        confirmPasswordEditText = findViewById(R.id.confirmPasswordEditText);
        continueButton = findViewById(R.id.continueButton);
        backButton = findViewById(R.id.backButton);
    }

    private void validatePasswords() {
        String password = Objects.requireNonNull(newPasswordEditText.getText()).toString().trim();
        String confirmPass = Objects.requireNonNull(confirmPasswordEditText.getText()).toString().trim();

        if (password.isEmpty() || confirmPass.isEmpty()) {
            Toast.makeText(this, "Please enter both fields", Toast.LENGTH_SHORT).show();
        } else if (!password.equals(confirmPass)) {
            Toast.makeText(this, "Passwords do not match", Toast.LENGTH_SHORT).show();
        }
        else if (!isValidPassword(password)) {
            Toast.makeText(this, getString(R.string.invalid_password), Toast.LENGTH_LONG).show();
        }
        else if (!isValidPassword(confirmPass)) {
            Toast.makeText(this, getString(R.string.invalid_password), Toast.LENGTH_LONG).show();
        }
        else {
            ResetPasswordRequest resetPasswordRequest = new ResetPasswordRequest(password,password,getIntent().getStringExtra(IntentKeys.OTP));
            resetPasswordApiCall(resetPasswordRequest);
        }
    }

    private void showPasswordResetSuccessDialog() {
        final Dialog dialog = new Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.password_reset_popup);
        Objects.requireNonNull(dialog.getWindow()).setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        dialog.setCancelable(false);
        dialog.setCanceledOnTouchOutside(false);

        Button btnGoToSignIn = dialog.findViewById(R.id.btnGoToSignIn);

        if (btnGoToSignIn != null) {
            btnGoToSignIn.setOnClickListener(v -> {
                dialog.dismiss();

                // Navigate to sign in page
                Intent intent = new Intent(CreatePasswordActivity.this, LoginActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
                finish();
            });
        } else {
            dialog.dismiss();

            // Navigate to sign in page in fallback case as well
            Intent intent = new Intent(CreatePasswordActivity.this, LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        }

        // Set dialog width to match screen width with proper margins
        Window window = dialog.getWindow();
        if (window != null) {
            window.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        }

        dialog.show();
    }

    private void resetPasswordApiCall(ResetPasswordRequest resetPasswordRequest) {
        if (!NetworkUtils.isInternetAvailable(this)) {
            Toast.makeText(this, getString(R.string.no_internet), Toast.LENGTH_SHORT).show();
            return;
        }

        LoaderHelper.showLoader(this, true);

        RestApiService apiService = RestApiBuilder.getService();
        Call<BaseResponse> call = apiService.resetPassword(resetPasswordRequest);

        call.enqueue(new Callback<>() {
            @Override
            public void onResponse(@NonNull Call<BaseResponse> call, @NonNull Response<BaseResponse> response) {
                LoaderHelper.hideLoader();

                if (response.isSuccessful()) {
                    Log.d(TAG, "Password reset success: " + response.message());
                    showPasswordResetSuccessDialog();
                } else {
                    try {
                        if (response.errorBody() != null) {
                            Gson gson = new Gson();
                            ErrorResponse errorResponse = gson.fromJson(response.errorBody().charStream(), ErrorResponse.class);
                            String errorMessage = errorResponse != null && errorResponse.getDetail() != null
                                    ? errorResponse.getDetail()
                                    : getString(R.string.default_error_message);

                            Toast.makeText(CreatePasswordActivity.this, errorMessage, Toast.LENGTH_LONG).show();
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "Error parsing errorBody", e);
                        Toast.makeText(CreatePasswordActivity.this, getString(R.string.default_error_message), Toast.LENGTH_LONG).show();
                    }
                }
            }

            @Override
            public void onFailure(@NonNull Call<BaseResponse> call, @NonNull Throwable t) {
                LoaderHelper.hideLoader();
                Log.e(TAG, "API call failed: " + t.getMessage());
                Toast.makeText(CreatePasswordActivity.this, getString(R.string.network_error), Toast.LENGTH_LONG).show();
            }
        });
    }
}
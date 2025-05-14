package com.gmscan.activity;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.gmscan.R;
import com.gmscan.model.BaseResponse;
import com.gmscan.model.loginRegister.ErrorResponse;
import com.gmscan.model.loginRegister.User;
import com.gmscan.model.resetPassword.ResetPasswordRequest;
import com.gmscan.service.RestApiBuilder;
import com.gmscan.service.RestApiService;
import com.gmscan.utility.IntentKeys;
import com.gmscan.utility.LoaderHelper;
import com.gmscan.utility.NetworkUtils;
import com.google.gson.Gson;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class VerificationCodeActivity extends AppCompatActivity {

    private EditText digit1, digit2, digit3, digit4;
    private ImageButton backButton;

    private Button submitButton;
    private static final String TAG = "VerificationCodeActivity";


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.layout_verification_code);
        initializeView();
        setupOtpInputs();
        setClickListeners();
    }

    private void setClickListeners() {
        backButton.setOnClickListener(v -> finish());
        TextView resendCodeText = findViewById(R.id.resendCodeText);
        resendCodeText.setOnClickListener(v -> {
            User user = new User();
            user.setEmail(getIntent().getStringExtra(IntentKeys.EMAIL));
            resendOTPApiCall(user);
        });
        submitButton.setOnClickListener(v -> openCreatePasswordActivity());
    }

    private void resendOTPApiCall(User user) {
        if (!NetworkUtils.isInternetAvailable(this)) {
            Toast.makeText(this, getString(R.string.no_internet), Toast.LENGTH_SHORT).show();
            return;
        }

        LoaderHelper.showLoader(this, true);

        RestApiService apiService = RestApiBuilder.getService();
        Call<BaseResponse> call = apiService.forgotPassword(user);

        call.enqueue(new Callback<>() {
            @Override
            public void onResponse(@NonNull Call<BaseResponse> call, @NonNull Response<BaseResponse> response) {
                LoaderHelper.hideLoader();

                if (response.isSuccessful()) {
                    Log.d(TAG, "OTP resent successfully: " + response.message());
                    Toast.makeText(VerificationCodeActivity.this, getString(R.string.otp_resent_successfully), Toast.LENGTH_SHORT).show();
                } else {
                    try {
                        if (response.errorBody() != null) {
                            Gson gson = new Gson();
                            ErrorResponse errorResponse = gson.fromJson(response.errorBody().charStream(), ErrorResponse.class);
                            String errorMessage = errorResponse != null && errorResponse.getDetail() != null
                                    ? errorResponse.getDetail()
                                    : getString(R.string.default_error_message);

                            Toast.makeText(VerificationCodeActivity.this, errorMessage, Toast.LENGTH_LONG).show();
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "Parsing error response failed", e);
                        Toast.makeText(VerificationCodeActivity.this, getString(R.string.default_error_message), Toast.LENGTH_LONG).show();
                    }
                }
            }

            @Override
            public void onFailure(@NonNull Call<BaseResponse> call, @NonNull Throwable t) {
                LoaderHelper.hideLoader();
                Log.e(TAG, "API call failed: " + t.getMessage());
                Toast.makeText(VerificationCodeActivity.this, getString(R.string.network_error), Toast.LENGTH_LONG).show();
            }
        });
    }



    private void initializeView() {
        backButton = findViewById(R.id.backButton);
        TextView emailText = findViewById(R.id.emailText);
        digit1 = findViewById(R.id.digit1);
        digit2 = findViewById(R.id.digit2);
        digit3 = findViewById(R.id.digit3);
        digit4 = findViewById(R.id.digit4);
        submitButton = findViewById(R.id.submitButton);
        emailText.setText(getIntent().getStringExtra(IntentKeys.EMAIL));
    }

    private void openCreatePasswordActivity() {
        String d1 = digit1.getText().toString().trim();
        String d2 = digit2.getText().toString().trim();
        String d3 = digit3.getText().toString().trim();
        String d4 = digit4.getText().toString().trim();

        if (d1.isEmpty() || d2.isEmpty() || d3.isEmpty() || d4.isEmpty()) {
            Toast.makeText(this, getString(R.string.otp_empty_error), Toast.LENGTH_SHORT).show();
            return;
        }

        String otp = d1 + d2 + d3 + d4;
        if (otp.length() != 4 || !otp.matches("\\d{4}")) {
            Toast.makeText(this, getString(R.string.otp_invalid_error), Toast.LENGTH_SHORT).show();
            return;
        }

        String email = getIntent().getStringExtra(IntentKeys.EMAIL);
        ResetPasswordRequest resetPasswordRequest = new ResetPasswordRequest(otp, email);
        verifyOtpApiCall(resetPasswordRequest);
    }



    private void setupOtpInputs() {
        digit1.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (s.length() == 1) {
                    digit2.requestFocus();
                }
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });

        digit2.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (s.length() == 1) {
                    digit3.requestFocus();
                } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.VANILLA_ICE_CREAM) {
                    if (s.isEmpty()) {
                        digit1.requestFocus();
                    }
                }
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });

        digit3.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (s.length() == 1) {
                    digit4.requestFocus();
                } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.VANILLA_ICE_CREAM) {
                    if (s.isEmpty()) {
                        digit2.requestFocus();
                    }
                }
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });

        digit4.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.VANILLA_ICE_CREAM) {
                    if (s.isEmpty()) {
                        digit3.requestFocus();
                    }
                }
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });
    }

    private void verifyOtpApiCall(ResetPasswordRequest resetPasswordRequest) {
        if (!NetworkUtils.isInternetAvailable(this)) {
            Toast.makeText(this, getString(R.string.no_internet), Toast.LENGTH_SHORT).show();
            return;
        }

        LoaderHelper.showLoader(this, true);

        RestApiService apiService = RestApiBuilder.getService();
        Call<BaseResponse> call = apiService.verifyOtp(resetPasswordRequest);

        call.enqueue(new Callback<>() {
            @Override
            public void onResponse(@NonNull Call<BaseResponse> call, @NonNull Response<BaseResponse> response) {
                LoaderHelper.hideLoader();

                if (response.isSuccessful()) {
                    Log.d(TAG, "OTP verified: " + response.message());
                    Toast.makeText(VerificationCodeActivity.this, getString(R.string.otp_verified_successfully), Toast.LENGTH_SHORT).show();

                    Intent intent = new Intent(VerificationCodeActivity.this, CreatePasswordActivity.class);
                    intent.putExtra(IntentKeys.OTP, resetPasswordRequest.otp);
                    startActivity(intent);
                } else {
                    try {
                        if (response.errorBody() != null) {
                            Gson gson = new Gson();
                            ErrorResponse errorResponse = gson.fromJson(response.errorBody().charStream(), ErrorResponse.class);
                            String errorMessage = errorResponse != null && errorResponse.getDetail() != null
                                    ? errorResponse.getDetail()
                                    : getString(R.string.default_error_message);

                            Toast.makeText(VerificationCodeActivity.this, errorMessage, Toast.LENGTH_LONG).show();
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "Parsing error response failed", e);
                        Toast.makeText(VerificationCodeActivity.this, getString(R.string.default_error_message), Toast.LENGTH_LONG).show();
                    }
                }
            }

            @Override
            public void onFailure(@NonNull Call<BaseResponse> call, @NonNull Throwable t) {
                LoaderHelper.hideLoader();
                Log.e(TAG, "API call failed: " + t.getMessage());
                Toast.makeText(VerificationCodeActivity.this, getString(R.string.network_error), Toast.LENGTH_LONG).show();
            }
        });
    }
}

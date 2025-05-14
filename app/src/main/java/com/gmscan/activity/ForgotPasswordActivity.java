package com.gmscan.activity;

import static com.gmscan.utility.RegexUtils.isValidEmail;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.gmscan.R;
import com.gmscan.model.BaseResponse;
import com.gmscan.model.loginRegister.ErrorResponse;
import com.gmscan.model.loginRegister.User;
import com.gmscan.service.RestApiBuilder;
import com.gmscan.service.RestApiService;
import com.gmscan.utility.IntentKeys;
import com.gmscan.utility.LoaderHelper;
import com.gmscan.utility.NetworkUtils;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.gson.Gson;

import java.util.Objects;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ForgotPasswordActivity extends AppCompatActivity {

    private TextInputEditText emailEditText;
    private MaterialButton sendCodeButton;
    private ImageButton backButton;

    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.layout_forgot_password);
        initializeView();
        setClickListeners();
    }

    private void initializeView() {
        emailEditText = findViewById(R.id.emailEditText);
        sendCodeButton = findViewById(R.id.sendCodeButton);
        backButton = findViewById(R.id.backButton);
    }

    private void setClickListeners() {
        backButton.setOnClickListener(v -> finish());

        // Send Code button click handler
        sendCodeButton.setOnClickListener(v -> {
            String email = Objects.requireNonNull(emailEditText.getText()).toString().trim();
            if (email.isEmpty()) {
                Toast.makeText(this, getString(R.string.please_enter_email), Toast.LENGTH_SHORT).show();
                emailEditText.setError(getString(R.string.please_enter_email));
                return;
            }
            if (!isValidEmail(email)) {
                Toast.makeText(this, getString(R.string.invalid_email), Toast.LENGTH_SHORT).show();
                return;
            }
            User user = new User();
            user.setEmail(email);
            forgotPasswordApiCall(user);
        });
    }

    private void forgotPasswordApiCall(User user) {
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
                    Toast.makeText(ForgotPasswordActivity.this, Objects.requireNonNull(response.body()).getMessage(), Toast.LENGTH_LONG).show();
                    Intent intent = new Intent(ForgotPasswordActivity.this, VerificationCodeActivity.class);
                    intent.putExtra(IntentKeys.EMAIL, user.getEmail());
                    startActivity(intent);
                } else {
                    try {
                        if (response.errorBody() != null) {
                            Gson gson = new Gson();
                            ErrorResponse errorResponse = gson.fromJson(response.errorBody().charStream(), ErrorResponse.class);
                            String errorMessage = errorResponse != null && errorResponse.getDetail() != null
                                    ? errorResponse.getDetail()
                                    : getString(R.string.default_error_message);

                            Toast.makeText(ForgotPasswordActivity.this, errorMessage, Toast.LENGTH_LONG).show();
                        }
                    } catch (Exception e) {
                        Toast.makeText(ForgotPasswordActivity.this, getString(R.string.default_error_message), Toast.LENGTH_LONG).show();
                    }
                }
            }
            @Override
            public void onFailure(@NonNull Call<BaseResponse> call, @NonNull Throwable t) {
                LoaderHelper.hideLoader();
                Toast.makeText(ForgotPasswordActivity.this, getString(R.string.network_error), Toast.LENGTH_LONG).show();
            }
        });
    }
}
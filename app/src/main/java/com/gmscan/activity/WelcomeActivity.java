package com.gmscan.activity;

import static com.gmscan.GmScanApplication.getPreferenceManager;
import android.content.Intent;
import android.os.Bundle;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.TextPaint;
import android.text.method.LinkMovementMethod;
import android.text.style.ClickableSpan;
import android.text.style.ForegroundColorSpan;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.gmscan.R;
import com.gmscan.utility.IntentKeys;

public class WelcomeActivity extends AppCompatActivity {

    private Button btnGetStarted;
    private TextView txtPrivacy;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.layout_welcome);
        initializeView();
        privacyPolicy();
        setClickListeners();
    }

    private void privacyPolicy() {
        String text = getString(R.string.continue_by);
        SpannableString spannableString = new SpannableString(text);

        int termsStart = text.indexOf(getString(R.string.terms_of_use));
        int termsEnd = termsStart + getString(R.string.terms_of_use).length();
        int privacyStart = text.indexOf(getString(R.string.privacy_policy));
        int privacyEnd = privacyStart + getString(R.string.privacy_policy).length();

        // ClickableSpan for Terms of Use
        ClickableSpan termsClickable = new ClickableSpan() {
            @Override
            public void onClick(@NonNull View widget) {
                Intent intent = new Intent(WelcomeActivity.this, WebViewActivity.class);
                intent.putExtra(IntentKeys.TITLE, getString(R.string.terms_of_use));
                intent.putExtra(IntentKeys.URL, getString(R.string.terms_of_use_url));
                startActivity(intent);
            }

            @Override
            public void updateDrawState(@NonNull TextPaint ds) {
                super.updateDrawState(ds);
                ds.setColor(getColor(R.color.purple_500));
                ds.setUnderlineText(false); // Removes underline
            }
        };

        // ClickableSpan for Privacy Policy
        ClickableSpan privacyClickable = new ClickableSpan() {
            @Override
            public void onClick(@NonNull View widget) {
                Intent intent = new Intent(WelcomeActivity.this, WebViewActivity.class);
                intent.putExtra(IntentKeys.TITLE, getString(R.string.privacy_policy));
                intent.putExtra(IntentKeys.URL, getString(R.string.privacy_policy_url));
                startActivity(intent);
            }

            @Override
            public void updateDrawState(@NonNull TextPaint ds) {
                super.updateDrawState(ds);
                ds.setColor(getColor(R.color.purple_500));
                ds.setUnderlineText(false); // Removes underline
            }
        };

        // Apply ClickableSpans
        spannableString.setSpan(termsClickable, termsStart, termsEnd, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        spannableString.setSpan(privacyClickable, privacyStart, privacyEnd, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);

        // Apply color spans
        spannableString.setSpan(new ForegroundColorSpan(getColor(R.color.purple_500)), termsStart, termsEnd, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        spannableString.setSpan(new ForegroundColorSpan(getColor(R.color.purple_500)), privacyStart, privacyEnd, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);

        // Set the text and make it clickable
        txtPrivacy.setText(spannableString);
        txtPrivacy.setMovementMethod(LinkMovementMethod.getInstance());

        // Ensure the TextView is clickable and focusable
        txtPrivacy.setClickable(true);
        txtPrivacy.setFocusable(true);
        txtPrivacy.setFocusableInTouchMode(true);
    }

    /**
     * Initializes all views in the activity.
     */
    private void initializeView() {
        btnGetStarted = findViewById(R.id.btnGetStarted);
        txtPrivacy = findViewById(R.id.txtPrivacy);
    }

    /**
     * Sets click listeners for all buttons.
     */
    private void setClickListeners() {
        btnGetStarted.setOnClickListener(v -> {
            getPreferenceManager().setWelcomeCompleted(true);
            Intent intent = new Intent(WelcomeActivity.this, OnBoardActivity.class);
            startActivity(intent);
            finish();
        });
    }
}

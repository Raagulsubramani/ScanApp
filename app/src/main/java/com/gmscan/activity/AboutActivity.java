package com.gmscan.activity;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import com.gmscan.R;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;

public class AboutActivity extends AppCompatActivity {

    private ImageView ivLogo;
    private ImageView btnBack;
    private TextView tvAppName, tvVersion, tvAboutAppDescription;
    private MaterialButton btnShare, btnRating;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.layout_about);

        // Initialize views
        initViews();

        // Set click listeners
        setClickListeners();
    }

    private void initViews() {
        btnBack = findViewById(R.id.btnBack);
        ivLogo = findViewById(R.id.ivLogo);
        tvAppName = findViewById(R.id.tvAppName);
        tvVersion = findViewById(R.id.tvVersion);
        tvAboutAppDescription = findViewById(R.id.tvAboutAppDescription);
        btnShare = findViewById(R.id.btnShare);
        btnRating = findViewById(R.id.btnRating);
    }

    private void setClickListeners() {
        btnShare.setOnClickListener(v -> shareApp());
        btnRating.setOnClickListener(v -> rateApp());
        btnBack.setOnClickListener(view -> finish());
    }

    private void shareApp() {
        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setType("text/plain");
        shareIntent.putExtra(Intent.EXTRA_SUBJECT, "GMScan - Document Scanner App");
        String shareMessage = "Check out GMScan, a powerful document scanning app for your mobile device: " +
                "https://play.google.com/store/apps/details?id=" + getPackageName();
        shareIntent.putExtra(Intent.EXTRA_TEXT, shareMessage);
        startActivity(Intent.createChooser(shareIntent, "Share via"));
    }

    private void rateApp() {
        Uri uri = Uri.parse("market://details?id=" + getPackageName());
        Intent rateIntent = new Intent(Intent.ACTION_VIEW, uri);

        // If Play Store is not installed, open in browser
        if (rateIntent.resolveActivity(getPackageManager()) == null) {
            rateIntent = new Intent(Intent.ACTION_VIEW,
                    Uri.parse("https://play.google.com/store/apps/details?id=" + getPackageName()));
        }
        startActivity(rateIntent);
    }
}

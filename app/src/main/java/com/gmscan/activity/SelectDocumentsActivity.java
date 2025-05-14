package com.gmscan.activity;

import static com.gmscan.utility.CustomBottomSheetDialog.VIEW_SCANNER;

import android.animation.ObjectAnimator;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.DecelerateInterpolator;
import android.widget.ImageView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;

import com.airbnb.lottie.LottieAnimationView;
import com.gmscan.R;
import com.gmscan.fragements.FaviouriteFragment;
import com.gmscan.fragements.HelpFragment;
import com.gmscan.fragements.HomeFragment;
import com.gmscan.fragements.ProfileFragment;
import com.gmscan.utility.CustomBottomSheetDialog;
import com.google.android.material.bottomnavigation.BottomNavigationView;

/**
 * Activity to display scan history and provide options to scan different document types.
 */
public class SelectDocumentsActivity extends AppCompatActivity implements CustomBottomSheetDialog.OnViewScannerButtonClickListener{

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.layout_select_documents);

        // Initialize BottomNavigationView and Floating Scan Button
        BottomNavigationView bottomNavigationView = findViewById(R.id.bottomNavigationView);
        LottieAnimationView scanQR = findViewById(R.id.scanQr);
        ImageView scanCancel = findViewById(R.id.scanCancel);

        // Load the default home fragment
        loadFragment(HomeFragment.newInstance());

        // Handle bottom navigation item selection
        bottomNavigationView.setOnItemSelectedListener(item -> {
            Fragment selectedFragment = null;

            if (item.getItemId() == R.id.nav_home) {
                selectedFragment = HomeFragment.newInstance();
            } else if (item.getItemId() == R.id.nav_favorite) {
                selectedFragment = FaviouriteFragment.newInstance();
            } else if (item.getItemId() == R.id.nav_help) {
                selectedFragment = HelpFragment.newInstance();
            } else if (item.getItemId() == R.id.nav_profile) {
                selectedFragment = ProfileFragment.newInstance();
            }

            if (selectedFragment != null) {
                loadFragment(selectedFragment);
            }
            return true;
        });

        // Set click listener for the scan button
        scanQR.setOnClickListener(v -> {
            // Show bottom sheet for scan options
            CustomBottomSheetDialog bottomSheet = CustomBottomSheetDialog.newInstance(VIEW_SCANNER);
            bottomSheet.setOnViewScannerButtonClickListener(this);
            bottomSheet.setOnBottomSheetDismissListener(() -> {
                animateDissolve(scanCancel, scanQR); // Fade out scanCancel and fade in scanQR
            });
            bottomSheet.show(getSupportFragmentManager(), "ScanOptionsBottomSheet");

            animateDissolve(scanQR, scanCancel); // Fade out scanQR and fade in scanCancel

            // Apply a rotation animation to the cancel button
            ObjectAnimator rotate = ObjectAnimator.ofFloat(scanCancel, "rotation", 0f, 180f);
            rotate.setDuration(500);
            rotate.setInterpolator(new DecelerateInterpolator());
            rotate.start();
        });
    }

    /**
     * Loads the given fragment into the fragment container.
     *
     * @param fragment The fragment to load
     */
    private void loadFragment(Fragment fragment) {
        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        transaction.replace(R.id.fragment_container, fragment);
        transaction.commit();
    }

    /**
     * Applies an ease-in dissolve animation to fade out one view and fade in another.
     *
     * @param fadeOutView The view to fade out
     * @param fadeInView  The view to fade in
     */
    private void animateDissolve(View fadeOutView, View fadeInView) {
        fadeOutView.animate().alpha(0f) // Fade out
                .setDuration(300).setInterpolator(new AccelerateInterpolator()) // Ease in
                .withEndAction(() -> {
                    fadeOutView.setVisibility(View.GONE);
                    fadeInView.setAlpha(0f);
                    fadeInView.setVisibility(View.VISIBLE);
                    fadeInView.animate().alpha(1f) // Fade in
                            .setDuration(300).setInterpolator(new DecelerateInterpolator()) // Ease out
                            .start();
                }).start();
    }

    @Override
    public void onCameraClick() {
        startActivity(new Intent(this, IDScanningActivity.class));
    }

    @Override
    public void onGalleryClick() {
        startActivity(new Intent(this, IDScanningActivity.class));
    }
}
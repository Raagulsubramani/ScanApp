package com.gmscan.fragements;

import static com.gmscan.GmScanApplication.preferenceManager;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.gmscan.R;
import com.gmscan.activity.AboutActivity;
import com.gmscan.activity.LanguageActivity;
import com.gmscan.activity.LoginActivity;
import com.gmscan.activity.PersonalInfoActivity;
import com.gmscan.activity.SettingsActivity;
import com.gmscan.model.loginRegister.User;
import com.gmscan.service.RestApiBuilder;
import com.gmscan.service.RestApiService;
import com.gmscan.utility.CustomBottomSheetDialog;
import com.gmscan.utility.ImageUtil;
import com.gmscan.utility.LoaderHelper;
import com.gmscan.utility.NetworkUtils;
import com.gmscan.utility.ZoomImageDialog;

import de.hdodenhof.circleimageview.CircleImageView;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ProfileFragment extends Fragment implements CustomBottomSheetDialog.OnViewSignOutButtonClickListener {

    private static final String TAG = "ProfileFragment";
    private ImageView editProfileIcon;
    private Button signOutButton;
    private TextView userNameTextView;
    private LinearLayout personalInfoOption;
    private LinearLayout settingsOption;
    private LinearLayout languageOption;
    private LinearLayout contactOption;
    private LinearLayout aboutOption;
    private CircleImageView profilePicture;
    private ProgressBar imageProgress;

    public ProfileFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.layout_activity_profile, container, false);
    }

    public static ProfileFragment newInstance() {
        return new ProfileFragment();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Initialize views
        initializeViews(view);

        // Set click listeners
        setupClickListeners();

        // Set user information

        userProfileApiCall();
    }

    private void initializeViews(View view) {
        editProfileIcon = view.findViewById(R.id.edit_profile);
        signOutButton = view.findViewById(R.id.sign_out_button);
        userNameTextView = view.findViewById(R.id.user_name);
        personalInfoOption = view.findViewById(R.id.personal_info_option);
        settingsOption = view.findViewById(R.id.settings_option);
        languageOption = view.findViewById(R.id.language_option);
        contactOption = view.findViewById(R.id.contact_option);
        aboutOption = view.findViewById(R.id.about_option);
        imageProgress = view.findViewById(R.id.imageProgress);
        profilePicture = view.findViewById(R.id.profile_picture);
    }

    private void setupClickListeners() {

        // Edit profile click listener
        editProfileIcon.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                navigateToEditProfile();
            }
        });

        // Sign out button click listener
        signOutButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                performSignOut();
            }
        });

        // Personal Info option click listener
        personalInfoOption.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                navigateToPersonalInfo();
            }
        });

        // Settings option click listener
        settingsOption.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                navigateToSettings();
            }
        });

        // Language option click listener
        languageOption.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                navigateToLanguageSettings();
            }
        });

        // Contact option click listener
        contactOption.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                navigateToContactUs();
            }
        });

        // About option click listener
        aboutOption.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                navigateToAboutGMScan();
            }
        });
    }

    @SuppressLint("SetTextI18n")
    private void setUserInformation(User user) {
        if (!isAdded() || getActivity() == null) {
            // Fragment is not attached anymore, skip updating UI
            return;
        }

        preferenceManager.saveUser(user);
        userNameTextView.setText(user.getFirstName());

        if (user.getFileUrl() != null && !user.getFileUrl().isEmpty()) {
            ImageUtil.load(getActivity(), user.getFileUrl(), profilePicture, imageProgress);
            profilePicture.setOnClickListener(v -> ZoomImageDialog.show(getActivity(), user.getFileUrl()));
        }
    }

    private void navigateToEditProfile() {
        // TODO: Implement edit profile navigation
        // Example:
        // Intent intent = new Intent(requireActivity(), EditProfileActivity.class);
        // startActivity(intent);
    }

    private void performSignOut() {
        CustomBottomSheetDialog bottomSheet = CustomBottomSheetDialog.newInstance(2);
        bottomSheet.setOnViewSignOutButtonClickListener(this);
        bottomSheet.show(getChildFragmentManager(), "ProfileBottomSheet");
/*
        AlertDialog.Builder builder = new AlertDialog.Builder(requireActivity());
        View dialogView = LayoutInflater.from(requireActivity()).inflate(R.layout.layout_signout_popup, null);
        builder.setView(dialogView);

        final AlertDialog dialog = builder.create();

        // Make the dialog background transparent
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            dialog.getWindow().setWindowAnimations(R.style.DialogAnimation); // Apply the animation
            dialog.getWindow().setGravity(android.view.Gravity.BOTTOM); // Align at the bottom
        }

        // Initialize buttons
        Button cancelButton = dialogView.findViewById(R.id.btn_cancel);
        Button signOutButton = dialogView.findViewById(R.id.btn_sign_out);

        cancelButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();
            }
        });

        signOutButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();
                // Add sign-out logic here
            }
        });

        dialog.show();*/
    }

    @Override
    public void onSignOutClick() {
        Log.d(TAG, "onSignOutClick: Attempting to log out user");

        if (!NetworkUtils.isInternetAvailable(requireContext())) {
            Toast.makeText(requireContext(), "No internet connection available", Toast.LENGTH_SHORT).show();
            return;
        }

        LoaderHelper.showLoader(requireActivity(), true);

        RestApiService apiService = RestApiBuilder.getService();
        // Call the logout method without parameters
        Call<ResponseBody> call = apiService.logout();

        call.enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(@NonNull Call<ResponseBody> call, @NonNull Response<ResponseBody> response) {
                LoaderHelper.hideLoader();

                if (response.isSuccessful()) {
                    Log.d(TAG, "Logout API call successful: " + response.code());

                    // Update login status to false
                    preferenceManager.setLoginIn(false);
                    preferenceManager.clearUser();

                    Toast.makeText(requireContext(), "Successfully logged out", Toast.LENGTH_SHORT).show();

                    // Navigate to login activity and clear activity stack
                    Intent intent = new Intent(requireActivity(), LoginActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                    requireActivity().finish();
                } else {
                    Log.e(TAG, "Logout API call failed: " + response.code() + " - " + response.message());
                    Toast.makeText(requireContext(), "Failed to log out. Please try again.", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(@NonNull Call<ResponseBody> call, @NonNull Throwable t) {
                LoaderHelper.hideLoader();
                Log.e(TAG, "Logout API call failed with exception: " + t.getMessage(), t);
                Toast.makeText(requireContext(), "Network error. Please check your connection and try again.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private final ActivityResultLauncher<Intent> personalInfoLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
        if (result.getResultCode() == Activity.RESULT_OK) {
            setUserInformation(preferenceManager.getUser());
        }
    });

    private void navigateToPersonalInfo() {
        Intent intent = new Intent(requireActivity(), PersonalInfoActivity.class);
        personalInfoLauncher.launch(intent);
    }

    private void navigateToSettings() {
        // Create an Intent to navigate to the SettingsActivity
        Intent intent = new Intent(requireActivity(), SettingsActivity.class);
        startActivity(intent);
    }

    private void navigateToLanguageSettings() {
        // TODO: Implement navigation to Language Settings screen
        Intent intent = new Intent(requireActivity(), LanguageActivity.class);
        startActivity(intent);
    }

    private void navigateToContactUs() {
        // Create an intent to open email app with predefined email address
        Intent emailIntent = new Intent(Intent.ACTION_SENDTO);
        emailIntent.setData(Uri.parse("mailto:support@gmscan.com"));
        emailIntent.putExtra(Intent.EXTRA_SUBJECT, "Support Request - GMScan App");

        try {
            startActivity(Intent.createChooser(emailIntent, "Send email using..."));
        } catch (android.content.ActivityNotFoundException ex) {
            // Handle case where no email app is available
            Toast.makeText(requireActivity(), "No email applications installed.", Toast.LENGTH_SHORT).show();
        }
    }

    private void navigateToAboutGMScan() {
        // TODO: Implement navigation to About GM Scan screen
        Intent intent = new Intent(requireActivity(), AboutActivity.class);
        startActivity(intent);
    }

    private void userProfileApiCall() {

        if (!isAdded() || getContext() == null) return;

        if (!NetworkUtils.isInternetAvailable(getActivity())) {
            Toast.makeText(getActivity(), getString(R.string.no_internet), Toast.LENGTH_SHORT).show();
            return;
        }
        LoaderHelper.showLoader(getActivity(), true);

        RestApiService apiService = RestApiBuilder.getService();
        Call<User> call = apiService.getUserProfile();


        call.enqueue(new Callback<>() {
            @Override
            public void onResponse(@NonNull Call<User> call, @NonNull Response<User> response) {
                LoaderHelper.hideLoader();
                if (response.isSuccessful()) {
                    String accessToken = preferenceManager.getUser().getAccessToken();
                    User user = response.body();
                    if (user != null) {
                        user.setAccessToken(accessToken);
                        setUserInformation(user);
                    }
                }
            }

            @Override
            public void onFailure(@NonNull Call<User> call, @NonNull Throwable t) {
                LoaderHelper.hideLoader();
            }
        });
    }
}

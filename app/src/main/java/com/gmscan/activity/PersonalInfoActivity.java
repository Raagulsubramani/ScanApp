package com.gmscan.activity;

import static com.gmscan.GmScanApplication.preferenceManager;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;

import com.gmscan.R;
import com.gmscan.model.loginRegister.LoginRegisterResponse;
import com.gmscan.model.loginRegister.User;
import com.gmscan.model.uploads.UploadsResponse;
import com.gmscan.service.RestApiBuilder;
import com.gmscan.service.RestApiService;
import com.gmscan.utility.ImageUtil;
import com.gmscan.utility.LoaderHelper;
import com.gmscan.utility.NetworkUtils;
import com.gmscan.utility.ZoomImageDialog;
import com.google.android.material.button.MaterialButton;
import com.hbb20.CountryCodePicker;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import de.hdodenhof.circleimageview.CircleImageView;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class PersonalInfoActivity extends AppCompatActivity {

    private ImageButton btnBack;
    private ImageButton btnEditProfile;
    private EditText editFirstName;
    private EditText editLastName;
    private EditText editEmail;
    private EditText editPhoneNumber;
    private MaterialButton btnSave;
    private CircleImageView profileImage;
    private ProgressBar imageProgress;

    // Flag to track if any changes have been made

    // For getting result from gallery & camera
    private ActivityResultLauncher<Intent> galleryLauncher;
    private ActivityResultLauncher<Intent> cameraLauncher;
    private ActivityResultLauncher<String> galleryPermissionLauncher;
    private ActivityResultLauncher<String> cameraPermissionLauncher;

    // Uri to store selected image
    private Uri selectedImageUri;
    private Uri cameraImageUri;
    User user;
    CountryCodePicker ccp ;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.layout_personal_info);

        // Initialize permission launchers
        initializePermissionLaunchers();

        // Initialize views
        initializeViews();

        // Initialize activity result launchers
        initializeActivityResultLaunchers();

        // Set click listeners
        setupClickListeners();
        loadUserData();
    }

    private void initializePermissionLaunchers() {
        // Gallery permission launcher
        galleryPermissionLauncher = registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
            if (isGranted) {
                openGallery();
            } else {
                Toast.makeText(this, "Storage permission is required to select an image", Toast.LENGTH_SHORT).show();
            }
        });

        // Camera permission launcher
        cameraPermissionLauncher = registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
            if (isGranted) {
                openCamera();
            } else {
                Toast.makeText(this, "Camera permission is required to take a picture", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void initializeViews() {
        ccp = findViewById(R.id.ccp);
        btnBack = findViewById(R.id.btnBack);
        btnEditProfile = findViewById(R.id.btnEditProfile);
        editFirstName = findViewById(R.id.editFirstName);
        editLastName = findViewById(R.id.editLastName);
        editEmail = findViewById(R.id.editEmail);
        editPhoneNumber = findViewById(R.id.editPhoneNumber);
        profileImage = findViewById(R.id.profileImage);
        imageProgress = findViewById(R.id.imageProgress);
        btnSave = findViewById(R.id.btnSave);
    }

    private void initializeActivityResultLaunchers() {
        // Gallery launcher
        galleryLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
            if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                selectedImageUri = result.getData().getData();
                try {
                    Bitmap bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), selectedImageUri);
                    profileImage.setImageBitmap(bitmap);
                } catch (IOException e) {
                    Toast.makeText(this, "Failed to load image" + e.getMessage(), Toast.LENGTH_SHORT).show();
                }
            }
        });

        // Camera launcher
        cameraLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
            if (result.getResultCode() == RESULT_OK) {
                try {
                    selectedImageUri = cameraImageUri;
                    Bitmap bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), selectedImageUri);
                    profileImage.setImageBitmap(bitmap);
                } catch (IOException e) {
                    Toast.makeText(this, "Failed to load captured image: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private void setupClickListeners() {
        // Back button click listener
        btnBack.setOnClickListener(v -> finish());

        // Edit profile image button click listener
        btnEditProfile.setOnClickListener(v -> showImageSourceDialog());

        // Save button click listener
        btnSave.setOnClickListener(v -> savePersonalInfo());
    }

    private void showImageSourceDialog() {
        String[] options = {"Take Photo", "Choose from Gallery", "Cancel"};
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Choose Profile Picture");
        builder.setItems(options, (dialog, which) -> {
            if (which == 0) {
                checkCameraPermissionAndOpen();
            } else if (which == 1) {
                checkGalleryPermissionAndOpen();
            } else {
                dialog.dismiss();
            }
        });
        builder.show();
    }

    private void checkGalleryPermissionAndOpen() {
        // For Android 13 (API 33) and above, use photo picker
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            galleryLauncher.launch(intent);
            return;
        }

        // For Android 10 (API 29) to 12 (API 32)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            galleryLauncher.launch(intent);
            return;
        }

        // For older versions, check and request READ_EXTERNAL_STORAGE permission
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            // Request READ permission
            galleryPermissionLauncher.launch(Manifest.permission.READ_EXTERNAL_STORAGE);
        } else {
            // Permission already granted, open gallery
            openGallery();
        }
    }

    private void checkCameraPermissionAndOpen() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            cameraPermissionLauncher.launch(Manifest.permission.CAMERA);
        } else {
            openCamera();
        }
    }

    private void openGallery() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        galleryLauncher.launch(intent);
    }

    private void openCamera() {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);

        // Ensure that there's a camera activity to handle the intent
        // Create the file where the photo should go
        File photoFile;
        try {
            photoFile = createImageFile();
        } catch (IOException ex) {
            Toast.makeText(this, "Error creating image file", Toast.LENGTH_SHORT).show();
            return;
        }

        // Continue only if the file was successfully created
        cameraImageUri = FileProvider.getUriForFile(this, "com.gmscan.fileprovider", // Make sure this matches your file provider authority in the manifest
                photoFile);
        takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, cameraImageUri);
        cameraLauncher.launch(takePictureIntent);
    }

    private File createImageFile() throws IOException {
        // Create an image file name
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "_";
        File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        return File.createTempFile(imageFileName,  /* prefix */
                ".jpg",         /* suffix */
                storageDir      /* directory */);
    }

    private void savePersonalInfo() {
        // Get current values
        String firstName = editFirstName.getText().toString().trim();
        String email = editEmail.getText().toString().trim();
        String phoneNumber = editPhoneNumber.getText().toString().trim();

        // Validate fields
        if (firstName.isEmpty() | email.isEmpty() || phoneNumber.isEmpty()) {
            Toast.makeText(this, "All fields must be filled", Toast.LENGTH_SHORT).show();
            return;
        }

        // Validate email format
        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            Toast.makeText(this, "Please enter a valid email address", Toast.LENGTH_SHORT).show();
            return;
        }

        if (selectedImageUri == null) {
            updateProfile(user.getFileUrl());
        } else {
            uploadImage();
        }
    }

    private void loadUserData() {
        if (isFinishing() || isDestroyed()) {
            return;
        }

        user = preferenceManager.getUser();
        editFirstName.setText(user.getFirstName());
        editEmail.setText(user.getEmail());

        String[] mobileNumberWithCountryCode = user.getMobileNumber().split(" ");

        if(mobileNumberWithCountryCode.length>1){
            editPhoneNumber.setText(mobileNumberWithCountryCode[1].trim());
            ccp.setFullNumber(mobileNumberWithCountryCode[0].trim());
        }else{
            editPhoneNumber.setText(user.getMobileNumber());
            ccp.setFullNumber("+91");
        }

        if (user.getFileUrl() != null && !user.getFileUrl().isEmpty()) {
            ImageUtil.load(this, user.getFileUrl(), profileImage, imageProgress);
            profileImage.setOnClickListener(v -> ZoomImageDialog.show(this, user.getFileUrl()));
        }
    }

    private void uploadImage() {
        try {
            if (!NetworkUtils.isInternetAvailable(this)) {
                Toast.makeText(this, getString(R.string.no_internet), Toast.LENGTH_SHORT).show();
                return;
            }
            LoaderHelper.showLoader(this, true);

            byte[] imageBytes = ImageUtil.readBytesFromUri(this, selectedImageUri);
            if (imageBytes.length == 0) {
                Toast.makeText(this, "Failed to read image data", Toast.LENGTH_SHORT).show();
                LoaderHelper.hideLoader();
                return;
            }

            String uniqueFileName = "user_image_" + System.currentTimeMillis() + ".png";

            RequestBody requestBody = RequestBody.create(MediaType.parse("image/*"), imageBytes);
            MultipartBody.Part imagePart = MultipartBody.Part.createFormData("file", uniqueFileName, requestBody);

            RestApiService apiService = RestApiBuilder.getService();
            apiService.uploadImage(imagePart).enqueue(new Callback<>() {
                @Override
                public void onResponse(@NonNull Call<UploadsResponse> call, @NonNull Response<UploadsResponse> response) {
                    LoaderHelper.hideLoader();
                    if (response.isSuccessful() && response.body() != null) {
                        updateProfile(response.body().getFileUrl());
                    } else {
                        Toast.makeText(getApplicationContext(), "Upload failed", Toast.LENGTH_SHORT).show();
                    }
                }

                @Override
                public void onFailure(@NonNull Call<UploadsResponse> call, @NonNull Throwable t) {
                    LoaderHelper.hideLoader();
                    Toast.makeText(getApplicationContext(), "Error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                }
            });

        } catch (Exception e) {
            LoaderHelper.hideLoader();
            Toast.makeText(this, "Upload error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }


    private void updateProfile(String url) {
        try {
            if (!NetworkUtils.isInternetAvailable(this)) {
                Toast.makeText(this, getString(R.string.no_internet), Toast.LENGTH_SHORT).show();
                return;
            }
            LoaderHelper.showLoader(this, true);


            String codeWithPlus = ccp.getSelectedCountryCodeWithPlus();


            // Create request body for text fields
            RequestBody firstName = RequestBody.create(editFirstName.getText().toString(), MultipartBody.FORM);
            RequestBody lastName = RequestBody.create(editLastName.getText().toString(), MultipartBody.FORM);
            RequestBody email = RequestBody.create(editEmail.getText().toString(), MultipartBody.FORM);
            RequestBody mobile = RequestBody.create(codeWithPlus +" " + editPhoneNumber.getText().toString(), MultipartBody.FORM);
            RequestBody fileUrl = RequestBody.create(url, MultipartBody.FORM);

            // Get API service
            RestApiService apiService = RestApiBuilder.getService();

            // Call API
            apiService.updateUserProfile(firstName, lastName, fileUrl, email, mobile).enqueue(new Callback<>() {
                @Override
                public void onResponse(@NonNull Call<LoginRegisterResponse> call, @NonNull Response<LoginRegisterResponse> response) {
                    LoaderHelper.hideLoader();
                    User user;
                    if (response.body() != null) {
                        user = response.body().getUser();
                        user.setEmail(editEmail.getText().toString());
                        user.setAccessToken(preferenceManager.getUser().getAccessToken());
                        preferenceManager.saveUser(user);
                        Intent resultIntent = new Intent();
                        setResult(RESULT_OK, resultIntent);
                        finish();
                    }
                }

                @Override
                public void onFailure(@NonNull Call<LoginRegisterResponse> call, @NonNull Throwable t) {
                    LoaderHelper.hideLoader();
                    Toast.makeText(getApplicationContext(), "Upload error: ", Toast.LENGTH_SHORT).show();
                }
            });

        } catch (Exception e) {
            LoaderHelper.hideLoader();
            Toast.makeText(this, "Upload error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }
}
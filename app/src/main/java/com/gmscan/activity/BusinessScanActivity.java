package com.gmscan.activity;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.MenuItem;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.IntentSenderRequest;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;

import com.gmscan.R;
import com.gmscan.model.getAllDocuments.ScannedDocument;
import com.gmscan.model.uploads.UploadsResponse;
import com.gmscan.service.RestApiBuilder;
import com.gmscan.service.RestApiService;
import com.gmscan.utility.DocumentType;
import com.gmscan.utility.ImageUtil;
import com.gmscan.utility.LoaderHelper;
import com.gmscan.utility.NetworkUtils;
import com.gmscan.utility.RegexUtils;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.documentscanner.GmsDocumentScannerOptions;
import com.google.mlkit.vision.documentscanner.GmsDocumentScanning;
import com.google.mlkit.vision.documentscanner.GmsDocumentScanningResult;
import com.google.mlkit.vision.text.Text;
import com.google.mlkit.vision.text.TextRecognition;
import com.google.mlkit.vision.text.TextRecognizer;
import com.google.mlkit.vision.text.latin.TextRecognizerOptions;
import com.permissionx.guolindev.PermissionX;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class BusinessScanActivity extends AppCompatActivity {
    private TextRecognizer textRecognizer;
    private EditText bnameEditText, jobtitleEditText, companynameEditText, numberEditText,
            emailEditText, websiteEditText, baddressEditText;
    private TextView fullTextView;
    private static final String TAG = "BusinessCardActivity";
    private ActivityResultLauncher<IntentSenderRequest> scannerLauncher;
    private ArrayList<Uri> scannedPages;
    private final List<String> extractedTexts = new ArrayList<>();
    Uri imageUri;

    @RequiresApi(api = Build.VERSION_CODES.O)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.layout_business_scan);
        initializeViews();
        setupScannerLauncher();
        checkPermissions();
    }

    private void initializeViews() {
        fullTextView = findViewById(R.id.fullInfoTextView);
        bnameEditText = findViewById(R.id.bnameEditText);
        jobtitleEditText = findViewById(R.id.jobtitleEditText);
        companynameEditText = findViewById(R.id.companynameEditText);
        numberEditText = findViewById(R.id.numberEditText);
        emailEditText = findViewById(R.id.emailEditText);
        websiteEditText = findViewById(R.id.websiteEditText);
        baddressEditText = findViewById(R.id.baddressEditText);
        fullTextView = findViewById(R.id.fullInfoTextView);
        textRecognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS);

        TextView txtTitle = findViewById(R.id.txtTitle);
        ImageView imgBack = findViewById(R.id.imgBack);
        txtTitle.setText(getString(R.string.business_scan));
        imgBack.setOnClickListener(v -> finish());
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    private void checkPermissions() {
        PermissionX.init(this)
                .permissions(Manifest.permission.CAMERA)
                .request((allGranted, grantedList, deniedList) -> {
                    if (allGranted) {
                        startNewScan();
                    } else {
                        Toast.makeText(this, getString(R.string.permissions_denied) + deniedList, Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void setupScannerLauncher() {
        scannerLauncher = registerForActivityResult(
                new ActivityResultContracts.StartIntentSenderForResult(),
                this::handleScanResult
        );
    }

    private void startNewScan() {
        try {
            GmsDocumentScannerOptions options = new GmsDocumentScannerOptions.Builder()
                    .setScannerMode(GmsDocumentScannerOptions.SCANNER_MODE_FULL)
                    .setResultFormats(GmsDocumentScannerOptions.RESULT_FORMAT_PDF,
                            GmsDocumentScannerOptions.RESULT_FORMAT_JPEG)
                    .setGalleryImportAllowed(false)
                    .setPageLimit(3)
                    .build();

            GmsDocumentScanning.getClient(options)
                    .getStartScanIntent(this)
                    .addOnSuccessListener(intentSender ->
                            scannerLauncher.launch(new IntentSenderRequest.Builder(intentSender).build()))
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "Failed to launch scanner", e);
                        finish();
                    });
        } catch (Exception e) {
            Log.e(TAG, "Error starting camera", e);
            finish();
        }
    }

    private void handleScanResult(ActivityResult activityResult) {
        try {
            int resultCode = activityResult.getResultCode();
            Intent data = activityResult.getData();
            GmsDocumentScanningResult result = GmsDocumentScanningResult.fromActivityResultIntent(data);

            if (resultCode == Activity.RESULT_OK && result != null) {
                scannedPages = new ArrayList<>();
                if (result.getPages() != null) {
                    result.getPages().forEach(page -> scannedPages.add(page.getImageUri()));
                }
                processScannedPages();
            }
        } catch (Exception e) {
            Log.e(TAG, "Error handling scan result", e);
            Toast.makeText(this, "Error processing scan", Toast.LENGTH_SHORT).show();
        }
    }

    private void processScannedPages() {
        if (scannedPages != null && !scannedPages.isEmpty()) {
            // Clear previous extracted texts before processing new pages
            extractedTexts.clear();
            textRecognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                processNextPage(0);
            }
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    private void processNextPage(int index) {
        if (index >= scannedPages.size()) {
            parseExtractedInformation();
            return;
        }

        try {
            InputImage inputImage = InputImage.fromFilePath(this, scannedPages.get(index));
            imageUri = scannedPages.get(index);
            Log.e(TAG, "imageUri " + imageUri);

            textRecognizer.process(inputImage)
                    .addOnSuccessListener(result -> {
                        StringBuilder fullText = new StringBuilder();
                        for (Text.TextBlock block : result.getTextBlocks()) {
                            fullText.append(block.getText()).append("\n");
                        }
                        extractedTexts.add(fullText.toString().trim());
                        processNextPage(index + 1);
                    })
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "Text recognition failed", e);
                        processNextPage(index + 1);
                    });
        } catch (IOException e) {
            Log.e(TAG, "Error processing image", e);
            processNextPage(index + 1);
        }
    }
    ScannedDocument scannedDocument ;

    @RequiresApi(api = Build.VERSION_CODES.O)
    private void parseExtractedInformation() {
        scannedDocument = new ScannedDocument();
        StringBuilder recognizedText = new StringBuilder();
        for (String text : extractedTexts) {
            recognizedText.append(text).append("\n");
        }
        String[] lines = recognizedText.toString().split("\n");

        // Variables for extracted information
        String name = "";
        String jobTitle = "";
        String companyName = "";
        ArrayList<String> phoneNumbers = new ArrayList<>();
        ArrayList<String> emails = new ArrayList<>();
        ArrayList<String> websites = new ArrayList<>();
        StringBuilder address = new StringBuilder();

        boolean foundName = false;
        boolean foundJobTitle = false;

        // Process each line of the recognized text
        for (String originalLine : lines) {
            String line = originalLine.trim();
            if (line.isEmpty()) continue;

            // Extract phone numbers, emails, and websites using regular expressions
            phoneNumbers.addAll(RegexUtils.extractPhoneNumbers(line));
            line = line.replaceAll(RegexUtils.PHONE_PATTERN, "").trim();

            emails.addAll(RegexUtils.extractEmails(line));
            line = line.replaceAll(RegexUtils.EMAIL_PATTERN, "").trim();

            websites.addAll(RegexUtils.extractWebsites(line));
            line = line.replaceAll(RegexUtils.WEBSITE_PATTERN, "").trim();

            if (line.isEmpty()) continue;

            // Extract name, job title, company name, and address based on regex matching
            if (!foundName && RegexUtils.isName(line)) {
                name = line;
                foundName = true;
                continue;
            }

            if (foundName && !foundJobTitle && RegexUtils.isJobTitle(line)) {
                jobTitle = line;
                foundJobTitle = true;
                continue;
            }

            if (line.contains("& Co.") || line.contains("Inc") || line.contains("LLC") ||
                    line.contains("Ltd") || line.contains("Real Estate") ||
                    line.matches(".*(?:Company|Corporation|Associates|Group).*")) {
                companyName = line;
                continue;
            }

            if (RegexUtils.isAddress(line)) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.VANILLA_ICE_CREAM) {
                    if (!address.isEmpty()) {
                        address.append(", ");
                    }
                }
                address.append(line);
            }
        }

        // Assign parsed information to the scanned document
        scannedDocument.setName(name);
        scannedDocument.setJobTitle(jobTitle);
        scannedDocument.setCompanyTitle(companyName);
        scannedDocument.setPhoneNumbers(phoneNumbers);
        scannedDocument.setEmail(TextUtils.join("\n", emails));
        scannedDocument.setWebSite(TextUtils.join("\n", websites));
        scannedDocument.setAddress(address.toString());
        scannedDocument.setOcrText(recognizedText.toString());
        scannedDocument.setScanType(DocumentType.BUSINESS.getDisplayName());

        // Update UI with the extracted information
        runOnUiThread(() -> {
            bnameEditText.setText(scannedDocument.getName() != null ? scannedDocument.getName() : "");
            jobtitleEditText.setText(scannedDocument.getJobTitle() != null ? scannedDocument.getJobTitle() : "");
            companynameEditText.setText(scannedDocument.getCompanyTitle() != null ? scannedDocument.getCompanyTitle() : "");
            numberEditText.setText(!scannedDocument.getPhoneNumbers().isEmpty() ?
                    TextUtils.join("\n", scannedDocument.getPhoneNumbers()) : "");
            emailEditText.setText(scannedDocument.getEmail() != null ? scannedDocument.getEmail() : "");
            websiteEditText.setText(scannedDocument.getWebSite() != null ? scannedDocument.getWebSite() : "");
            baddressEditText.setText(scannedDocument.getAddress() != null ? scannedDocument.getAddress() : "");
            fullTextView.setText(scannedDocument.getOcrText());

            if (!scannedDocument.getName().isEmpty()) {
                uploadImage(imageUri);
            }
        });
    }

    private void uploadImage(Uri uri) {
        try {
            if (!NetworkUtils.isInternetAvailable(this)) {
                Toast.makeText(this, getString(R.string.no_internet), Toast.LENGTH_SHORT).show();
                return;
            }

            if (uri == null) {
                Toast.makeText(this, "No image selected", Toast.LENGTH_SHORT).show();
                return;
            }

            LoaderHelper.showLoader(this, true);

            byte[] imageBytes = ImageUtil.readBytesFromUri(this,uri);
            if (imageBytes.length == 0) {
                Toast.makeText(this, "Failed to read image data", Toast.LENGTH_SHORT).show();
                LoaderHelper.hideLoader();
                return;
            }

            String uniqueFileName = "business_image_" + System.currentTimeMillis() + ".png";

            RequestBody requestBody = RequestBody.create(MediaType.parse("image/*"), imageBytes);
            MultipartBody.Part imagePart = MultipartBody.Part.createFormData("file", uniqueFileName, requestBody);

            RestApiService apiService = RestApiBuilder.getService();
            apiService.uploadImage(imagePart).enqueue(new Callback<UploadsResponse>() {
                @Override
                public void onResponse(@NonNull Call<UploadsResponse> call, @NonNull Response<UploadsResponse> response) {
                    LoaderHelper.hideLoader();
                    if (response.isSuccessful() && response.body() != null) {
                        scannedDocument.setFileUrl(response.body().getFileUrl());
                        createDocumentApiCall(scannedDocument);
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
            e.printStackTrace();
        }
    }


    private void createDocumentApiCall(ScannedDocument scannedDocument) {
        if (!NetworkUtils.isInternetAvailable(this)) {
            Toast.makeText(this, getString(R.string.no_internet), Toast.LENGTH_SHORT).show();
            return;
        }

        RestApiService apiService = RestApiBuilder.getService();

        // Create proper business card document name and data
        String name = scannedDocument.getName() != null ? scannedDocument.getName() : "Unknown";

        RequestBody documentName = RequestBody.create(MediaType.parse("text/plain"),
                "Business - " + name);
        RequestBody scanType = RequestBody.create(MediaType.parse("text/plain"), "business");
        RequestBody personName = RequestBody.create(MediaType.parse("text/plain"),
                scannedDocument.getOcrText() != null ? scannedDocument.getOcrText() : "");
        RequestBody profession = RequestBody.create(MediaType.parse("text/plain"),
                scannedDocument.getJobTitle() != null ? scannedDocument.getJobTitle() : "");

        String emailStr = scannedDocument.getEmail();
        if (emailStr == null || emailStr.trim().isEmpty()) {
            emailStr = "default@example.com"; // Leave empty for business cards if not found
        }
        RequestBody email = RequestBody.create(MediaType.parse("text/plain"), emailStr);

        RequestBody mobileNumber = RequestBody.create(MediaType.parse("text/plain"),
                TextUtils.join(",", scannedDocument.getPhoneNumbers()));
        RequestBody address = RequestBody.create(MediaType.parse("text/plain"),
                scannedDocument.getAddress() != null ? scannedDocument.getAddress() : "");
        RequestBody companyName = RequestBody.create(MediaType.parse("text/plain"),
                scannedDocument.getCompanyTitle() != null ? scannedDocument.getCompanyTitle() : "");
        RequestBody website = RequestBody.create(MediaType.parse("text/plain"),
                scannedDocument.getWebSite() != null ? scannedDocument.getWebSite() : "");
        RequestBody fileUrl = RequestBody.create(scannedDocument.getFileUrl(), MediaType.parse("text/plain"));

        Call<ResponseBody> call = apiService.createIDCard(
                documentName,
                scanType,
                personName,
                profession,
                email,
                mobileNumber,
                address,
                companyName,
                website,
                fileUrl
        );

        call.enqueue(new Callback<>() {
            @Override
            public void onResponse(@NonNull Call<ResponseBody> call, @NonNull Response<ResponseBody> response) {
                if (response.isSuccessful()) {
                    Toast.makeText(BusinessScanActivity.this,
                            getString(R.string.document_created_successfully),
                            Toast.LENGTH_SHORT).show();

                    Intent resultIntent = new Intent();
                    setResult(RESULT_OK, resultIntent);
                    finish();
                } else {
                    Log.e(TAG, "Error creating document: " + response.code());
                    try {
                        if (response.errorBody() != null) {
                            Log.e(TAG, "Error body: " + response.errorBody().string());
                        }
                    } catch (IOException e) {
                        Log.e(TAG, "Error reading error body", e);
                    }
                    Toast.makeText(BusinessScanActivity.this,
                            getString(R.string.error_creating_document),
                            Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(@NonNull Call<ResponseBody> call, @NonNull Throwable t) {
                Log.e(TAG, "API call failed: " + t.getMessage());
                Toast.makeText(BusinessScanActivity.this,
                        getString(R.string.api_call_failed),
                        Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            getOnBackPressedDispatcher().onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
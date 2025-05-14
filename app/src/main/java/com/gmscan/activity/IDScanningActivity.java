package com.gmscan.activity;

import static android.os.Build.VERSION_CODES;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
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
import com.gmscan.model.CreateDocuments.CreateDoc;
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
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class IDScanningActivity extends AppCompatActivity {
    private TextRecognizer textRecognizer;
    private EditText nameEditText, emailEditText, addressEditText;
    private TextView fullTextView;
    private static final String TAG = "IDScanningActivity";
    private ActivityResultLauncher<IntentSenderRequest> scannerLauncher;
    private ArrayList<Uri> scannedPages;
    private final List<String> extractedTexts = new ArrayList<>();
    private ScannedDocument scannedDocument;
    Uri imageUri ;
    @RequiresApi(api = VERSION_CODES.VANILLA_ICE_CREAM)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.layout_id_scan);
        initializeView();
        setupScannerLauncher();
        checkPermissions();
    }

    /**
     * Initialize the UI components for the document scanning activity.
     * This method sets up the necessary views and prepares components for
     * displaying the scanned information and handling text recognition.
     */
    private void initializeView() {
        // Initialize the EditText fields for name, email, and address, where the extracted info will be displayed
        nameEditText = findViewById(R.id.nameEditText);
        emailEditText = findViewById(R.id.emailEditText);
        addressEditText = findViewById(R.id.addressEditText);

        // Initialize the TextView for displaying the full extracted text from OCR
        fullTextView = findViewById(R.id.fullTextView);
        ImageView imgBack = findViewById(R.id.imgBack);
        TextView txtTitle = findViewById(R.id.txtTitle);
        txtTitle.setText(getString(R.string.id_scan_activity));
        // Initialize the TextRecognizer from ML Kit, used for text recognition in captured images
        textRecognizer = TextRecognition.getClient(new TextRecognizerOptions.Builder().build());
        imgBack.setOnClickListener(v -> finish());
    }

    /**
     * Check for camera permissions. If granted, start the camera.
     */
    @RequiresApi(api = VERSION_CODES.O)
    private void checkPermissions() {
        PermissionX.init(this).permissions(Manifest.permission.CAMERA).request((allGranted, grantedList, deniedList) -> {
            if (allGranted) {
                // Permissions granted, start camera
                startNewScan();
            } else {
                // Permissions denied, show a toast message with denied permissions
                Toast.makeText(this, getString(R.string.permissions_denied) + deniedList, Toast.LENGTH_SHORT).show();
            }
        });
    }

    /**
     * Sets up the activity result launcher for handling document scanning results
     */
    @RequiresApi(api = VERSION_CODES.VANILLA_ICE_CREAM)
    private void setupScannerLauncher() {
        scannerLauncher = registerForActivityResult(new ActivityResultContracts.StartIntentSenderForResult(), this::handleScanResult);
    }

    /**
     * Initiates a new document scan using Google's ML Kit Document Scanner
     * Configures scanner options including:
     * - Full scanner mode
     * - PDF and JPEG output formats
     * - Maximum 3 pages per scan
     * - Gallery import disabled
     */
    private void startNewScan() {
        try {
            GmsDocumentScannerOptions options = new GmsDocumentScannerOptions.Builder().setScannerMode(GmsDocumentScannerOptions.SCANNER_MODE_FULL).setResultFormats(GmsDocumentScannerOptions.RESULT_FORMAT_PDF, GmsDocumentScannerOptions.RESULT_FORMAT_JPEG).setGalleryImportAllowed(false).setPageLimit(2).build();

            GmsDocumentScanning.getClient(options).getStartScanIntent(this).addOnSuccessListener(intentSender -> scannerLauncher.launch(new IntentSenderRequest.Builder(intentSender).build())).addOnFailureListener(e -> {
                Log.e(TAG, "Failed to launch scanner", e);
                finish();
            });
        } catch (Exception e) {
            Log.e(TAG, "Error starting camera", e);
            finish();
        }
    }

    /**
     * Handles the result returned from the document scanner
     * Processes both PDF and image results if available
     */
    @RequiresApi(api = VERSION_CODES.VANILLA_ICE_CREAM)
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

    /**
     * Initiates the processing of scanned pages for text extraction
     */
    @RequiresApi(api = VERSION_CODES.VANILLA_ICE_CREAM)
    private void processScannedPages() {
        if (scannedPages != null && !scannedPages.isEmpty()) {
            // Clear previous extracted texts before processing new pages
            extractedTexts.clear();
            processNextPage(0);
        }
    }

    /**
     * Recursively processes each scanned page for text extraction using ML Kit's text recognition
     * Processes pages one at a time and accumulates extracted text
     */
    @RequiresApi(api = VERSION_CODES.VANILLA_ICE_CREAM)
    private void processNextPage(int index) {
        if (index >= scannedPages.size()) {
            parseExtractedInformation();
            return;
        }

        try {
            InputImage inputImage = InputImage.fromFilePath(this, scannedPages.get(index));
            imageUri  = scannedPages.get(index);

            textRecognizer.process(inputImage).addOnSuccessListener(result -> {
                StringBuilder fullText = new StringBuilder();
                for (Text.TextBlock block : result.getTextBlocks()) {
                    fullText.append(block.getText()).append("\n");
                }
                extractedTexts.add(fullText.toString().trim());
                processNextPage(index + 1);
            }).addOnFailureListener(e -> {
                Log.e(TAG, "Text recognition failed", e);
                processNextPage(index + 1);
            });
        } catch (IOException e) {
            Log.e(TAG, "Error processing image", e);
            processNextPage(index + 1);
        }
    }

    /**
     * Parse the extracted text and populate the fields (name, email, address, etc.).
     */
    @RequiresApi(api = VERSION_CODES.VANILLA_ICE_CREAM)
    @SuppressLint({"SetTextI18n", "ObsoleteSdkInt"})
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

            if (line.contains("& Co.") || line.contains("Inc") || line.contains("LLC") || line.contains("Ltd") || line.contains("Real Estate") || line.matches(".*(?:Company|Corporation|Associates|Group).*")) {
                companyName = line;
                continue;
            }

            if (RegexUtils.isAddress(line)) {
                if (!address.isEmpty()) {
                    address.append(", ");
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
        scannedDocument.setScanType(DocumentType.ID.getDisplayName());

        // Update the UI with the extracted information
        runOnUiThread(() -> {
            nameEditText.setText(getString(R.string.name_label) + (scannedDocument.getName() != null ? scannedDocument.getName() : ""));
            emailEditText.setText(getString(R.string.email_label) + (scannedDocument.getEmail() != null ? scannedDocument.getEmail() : ""));
            addressEditText.setText(getString(R.string.address_label) + (scannedDocument.getAddress() != null ? scannedDocument.getAddress() : ""));
            fullTextView.setText(scannedDocument.getOcrText());
            uploadImage(imageUri);
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

            String uniqueFileName = "id_image_" + System.currentTimeMillis() + ".png";

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



    /**
     * Makes an API call to create a document in the backend service
     * @param scannedDocument The document to be created
     */
    private void createDocumentApiCall(ScannedDocument scannedDocument) {
        if (!NetworkUtils.isInternetAvailable(this)) {
            Toast.makeText(this, getString(R.string.no_internet), Toast.LENGTH_SHORT).show();
            return;
        }

        RestApiService apiService = RestApiBuilder.getService();

        // Create CreateDoc object with appropriate fields
        CreateDoc createDocRequest = new CreateDoc();

        String name = scannedDocument.getName() != null ? scannedDocument.getName() : "Unknown";
        createDocRequest.setDocumentName("ID Document - " + name);
        createDocRequest.setName("ID Document - " + name);
        createDocRequest.setProfession(scannedDocument.getJobTitle());
        createDocRequest.setCompanyName(scannedDocument.getCompanyTitle());
        createDocRequest.setMobileNumber(TextUtils.join(",", scannedDocument.getPhoneNumbers()));
        createDocRequest.setEmail(scannedDocument.getEmail());
        createDocRequest.setWebsite(scannedDocument.getWebSite());
        createDocRequest.setAddress(scannedDocument.getAddress());

        // Set document content and metadata
        createDocRequest.setContent(scannedDocument.getOcrText());
        createDocRequest.setScanType(scannedDocument.getScanType());
        createDocRequest.setTitle("ID Scan: " + scannedDocument.getName());

        // Set default values
        createDocRequest.setIsFavorite(false);

        // Set timestamps
        String currentTimestamp = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US)
                .format(new Date());
        createDocRequest.setCreatedAt(currentTimestamp);
        createDocRequest.setUpdatedAt(currentTimestamp);

        // Get user ID from shared preferences
        SharedPreferences sharedPreferences = getSharedPreferences("UserPrefs", MODE_PRIVATE);
        String userId = sharedPreferences.getString("USER_ID", "");
        createDocRequest.setUserId(userId);

        // Set file type for ID document
        createDocRequest.setFileType("id_scan");

        // Initialize empty tags list
        createDocRequest.setTags(new ArrayList<>());
        createDocRequest.setFileUrl(scannedDocument.getFileUrl());


        RequestBody documentName = RequestBody.create("id Card - "+createDocRequest.getDocumentName(), MediaType.parse("text/plain"));
        RequestBody scanType = RequestBody.create("id", MediaType.parse("text/plain"));
        RequestBody personName = RequestBody.create(scannedDocument.getOcrText(), MediaType.parse("text/plain"));
        RequestBody profession = RequestBody.create(createDocRequest.getProfession(), MediaType.parse("text/plain"));

        String emailStr = createDocRequest.getEmail();
        if (emailStr == null || emailStr.trim().isEmpty()) {
            emailStr = "default@example.com"; // for testing only
        }
        RequestBody email = RequestBody.create(emailStr, MediaType.parse("text/plain"));
        RequestBody mobileNumber = RequestBody.create(createDocRequest.getMobileNumber(), MediaType.parse("text/plain"));
        RequestBody address = RequestBody.create(createDocRequest.getAddress(), MediaType.parse("text/plain"));
        RequestBody companyName = RequestBody.create(createDocRequest.getCompanyName(), MediaType.parse("text/plain"));
        RequestBody website = RequestBody.create(createDocRequest.getWebsite(), MediaType.parse("text/plain"));
        RequestBody fileUrl = RequestBody.create(createDocRequest.getFileUrl(), MediaType.parse("text/plain"));


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
                    Toast.makeText(IDScanningActivity.this,
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
                    Toast.makeText(IDScanningActivity.this,
                            getString(R.string.error_creating_document),
                            Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(@NonNull Call<ResponseBody> call, @NonNull Throwable t) {
                Log.e(TAG, "API call failed: " + t.getMessage());
                Toast.makeText(IDScanningActivity.this,
                        getString(R.string.api_call_failed),
                        Toast.LENGTH_SHORT).show();
            }
        });
    }
}
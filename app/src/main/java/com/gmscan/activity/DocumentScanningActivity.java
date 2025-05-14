package com.gmscan.activity;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
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
import androidx.appcompat.widget.Toolbar;

import com.gmscan.R;
import com.gmscan.model.getAllDocuments.ScannedDocument;
import com.gmscan.model.uploads.UploadsResponse;
import com.gmscan.service.RestApiBuilder;
import com.gmscan.service.RestApiService;
import com.gmscan.utility.DocumentType;
import com.gmscan.utility.ImageUtil;
import com.gmscan.utility.IntentKeys;
import com.gmscan.utility.LoaderHelper;
import com.gmscan.utility.NetworkUtils;
import com.google.android.material.button.MaterialButton;
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

public class DocumentScanningActivity extends AppCompatActivity {
    private TextRecognizer textRecognizer;
    private EditText summaryContent;
    private MaterialButton exportButton;
    private static final String TAG = "DocumentScanningActivity";
    private ActivityResultLauncher<IntentSenderRequest> scannerLauncher;
    private ArrayList<Uri> scannedPages;
    private final List<String> extractedTexts = new ArrayList<>();
    private ScannedDocument scannedDocument;
    Uri imageUri;

    @RequiresApi(api = Build.VERSION_CODES.O)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.document_details);
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
        // Initialize toolbar
        TextView txtTitle = findViewById(R.id.txtTitle);
        ImageView imgBack = findViewById(R.id.imgBack);
        txtTitle.setText(getString(R.string.document_scan));
        imgBack.setOnClickListener(v -> finish());


        // Initialize the export button that will save the document
        exportButton = findViewById(R.id.exportButton);
        exportButton.setVisibility(View.GONE); // Hide initially until scanning is complete

        // Initialize content views
        summaryContent = findViewById(R.id.summaryContent);

        // Set up edit button for summary
        ImageButton editSummaryBtn = findViewById(R.id.editSummary);
        editSummaryBtn.setOnClickListener(v -> {
            summaryContent.setEnabled(true);
            summaryContent.requestFocus();
        });

        // Initialize the TextRecognizer from ML Kit, used for text recognition in captured images
        textRecognizer = TextRecognition.getClient(new TextRecognizerOptions.Builder().build());
    }

    /**
     * Check for camera permissions. If granted, start the camera.
     */
    @RequiresApi(api = Build.VERSION_CODES.O)
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
    private void setupScannerLauncher() {
        scannerLauncher = registerForActivityResult(new ActivityResultContracts.StartIntentSenderForResult(), this::handleScanResult);
    }

    /**
     * Initiates a new document scan using Google's ML Kit Document Scanner
     * Configures scanner options including:
     * - Full scanner mode
     * - PDF and JPEG output formats
     * - Maximum 5 pages per scan
     * - Gallery import disabled
     */
    private void startNewScan() {
        try {
            GmsDocumentScannerOptions options = new GmsDocumentScannerOptions.Builder()
                    .setScannerMode(GmsDocumentScannerOptions.SCANNER_MODE_FULL)
                    .setResultFormats(GmsDocumentScannerOptions.RESULT_FORMAT_PDF, GmsDocumentScannerOptions.RESULT_FORMAT_JPEG)
                    .setGalleryImportAllowed(false)
                    .setPageLimit(5)
                    .build();

            GmsDocumentScanning.getClient(options)
                    .getStartScanIntent(this)
                    .addOnSuccessListener(intentSender ->
                            scannerLauncher.launch(new IntentSenderRequest.Builder(intentSender).build()))
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "Failed to launch scanner", e);
                        Toast.makeText(this, "Failed to launch scanner", Toast.LENGTH_SHORT).show();
                        finish();
                    });
        } catch (Exception e) {
            Log.e(TAG, "Error starting camera", e);
            Toast.makeText(this, "Error starting camera", Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    /**
     * Handles the result returned from the document scanner
     * Processes both PDF and image results if available
     */
    private void handleScanResult(ActivityResult activityResult) {
        try {
            int resultCode = activityResult.getResultCode();
            Intent data = activityResult.getData();

            if (resultCode != Activity.RESULT_OK || data == null) {
                Log.d(TAG, "Scan cancelled or failed");
                return;
            }

            GmsDocumentScanningResult result = GmsDocumentScanningResult.fromActivityResultIntent(data);

            if (result != null) {
                scannedPages = new ArrayList<>();
                if (result.getPages() != null) {
                    for (int i = 0; i < result.getPages().size(); i++) {
                        scannedPages.add(result.getPages().get(i).getImageUri());
                    }

                    if (!scannedPages.isEmpty()) {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                            processScannedPages();
                        } else {
                            Toast.makeText(this, "This feature requires Android O or higher", Toast.LENGTH_SHORT).show();
                            finish();
                        }
                    } else {
                        Toast.makeText(this, "No pages scanned", Toast.LENGTH_SHORT).show();
                    }
                }
            } else {
                Toast.makeText(this, "Scan failed to produce results", Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            Log.e(TAG, "Error handling scan result", e);
            Toast.makeText(this, "Error processing scan", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Initiates the processing of scanned pages for text extraction
     */
    @RequiresApi(api = Build.VERSION_CODES.O)
    private void processScannedPages() {
        if (scannedPages != null && !scannedPages.isEmpty()) {
            // Show loading indicator
            extractedTexts.clear();
            processNextPage(0);
        }
    }

    /**
     * Recursively processes each scanned page for text extraction using ML Kit's text recognition
     * Processes pages one at a time and accumulates extracted text
     */
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
            Toast.makeText(this, "Error processing page " + (index + 1), Toast.LENGTH_SHORT).show();
            processNextPage(index + 1);
        }
    }

    /**
     * Parse the extracted text and populate the fields
     */
    @RequiresApi(api = Build.VERSION_CODES.O)
    @SuppressLint("SetTextI18n")
    private void parseExtractedInformation() {
        // Hide loading indicator

        scannedDocument = new ScannedDocument();
        StringBuilder recognizedText = new StringBuilder();
        for (String text : extractedTexts) {
            recognizedText.append(text).append("\n");
        }

        // Extract the first line as a potential title
        String fullText = recognizedText.toString().trim();
        String[] lines = fullText.split("\n");
        String title = lines.length > 0 ? lines[0] : "Untitled Document";

        // Assign parsed information to the scanned document
        scannedDocument.setTitle(title);
        scannedDocument.setOcrText(fullText);
        scannedDocument.setScanType(DocumentType.DOCUMENT.getDisplayName());

        // Generate timestamp for document creation
        String currentTimestamp = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US)
                .format(new Date());
        scannedDocument.setCreatedAt(currentTimestamp);
        scannedDocument.setUpdatedAt(currentTimestamp);

        // Generate summary
        String summary = generateSummary(fullText);

        // Update the UI with the extracted information
        runOnUiThread(() -> {

            // Set summary content
            if (summaryContent != null) {
                summaryContent.setText(summary);
            }


            // Show the export button and set up its click listener
            exportButton.setVisibility(View.VISIBLE);
            exportButton.setOnClickListener(
                    new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {

                        }
                    }
            );
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


            byte[] imageBytes = ImageUtil.readBytesFromUri(this, uri);
            if (imageBytes.length == 0) {
                Toast.makeText(this, "Failed to read image data", Toast.LENGTH_SHORT).show();
                return;
            }
            LoaderHelper.showLoader(this, true);


            String uniqueFileName = "document_image_" + System.currentTimeMillis() + ".png";

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
                        Log.e(TAG, "Retrieved document is null" + scannedDocument.getFileUrl());
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
     * This updated version simplifies the API call using the pattern from IDScanningActivity
     *
     * @param scannedDocument The document to be created
     */
    private void createDocumentApiCall(ScannedDocument scannedDocument) {
        if (!NetworkUtils.isInternetAvailable(this)) {
            Toast.makeText(this, getString(R.string.no_internet), Toast.LENGTH_SHORT).show();
            return;
        }

        LoaderHelper.showLoader(this, true);

        try {
            String summary = summaryContent.getText().toString();
            String[] lines = summary.split("\n");
            String documentTitle = lines.length > 0 ? lines[0] : "";


            RestApiService apiService = RestApiBuilder.getService();
            RequestBody documentName = RequestBody.create(MediaType.parse("text/plain"), documentTitle);
            RequestBody scanType = RequestBody.create(MediaType.parse("text/plain"), DocumentType.DOCUMENT.getDisplayName());
            RequestBody documentSummary = RequestBody.create(MediaType.parse("text/plain"), summary);
            RequestBody fileUrl = RequestBody.create(scannedDocument.getFileUrl(), MediaType.parse("text/plain"));


            // Make API call to create document - using the specified documentScan method
            Call<ResponseBody> call = apiService.documentScan(
                    documentName,
                    scanType,
                    documentSummary,
                    fileUrl
            );

            call.enqueue(new Callback<>() {
                @Override
                public void onResponse(@NonNull Call<ResponseBody> call, @NonNull Response<ResponseBody> response) {

                    LoaderHelper.hideLoader();
                    if (response.isSuccessful()) {
                        Toast.makeText(DocumentScanningActivity.this,
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
                        Toast.makeText(DocumentScanningActivity.this,
                                getString(R.string.error_creating_document),
                                Toast.LENGTH_SHORT).show();
                    }
                }

                @Override
                public void onFailure(@NonNull Call<ResponseBody> call, @NonNull Throwable t) {
                    Log.e(TAG, "API call failed: " + t.getMessage());
                    LoaderHelper.hideLoader();
                    Toast.makeText(DocumentScanningActivity.this,
                            getString(R.string.api_call_failed),
                            Toast.LENGTH_SHORT).show();
                }
            });
        } catch (Exception e) {
            LoaderHelper.hideLoader();
            Log.e(TAG, "Error preparing API call", e);
            Toast.makeText(this, "Error preparing document upload: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Generate a summary of the document content
     * For simplicity, takes the first few sentences or a specific length
     *
     * @param content The full content of the document
     * @return A summary of the document content
     */
    private String generateSummary(String content) {
        if (TextUtils.isEmpty(content)) {
            return "";
        }

        // Simple summary generation: take first 200 characters or up to three sentences
        int maxLength = Math.min(content.length(), 200);
        String truncatedContent = content.substring(0, maxLength);

        // Find the last complete sentence within the truncated content
        int lastPeriod = truncatedContent.lastIndexOf('.');
        int lastQuestion = truncatedContent.lastIndexOf('?');
        int lastExclamation = truncatedContent.lastIndexOf('!');

        int lastSentenceEnd = Math.max(Math.max(lastPeriod, lastQuestion), lastExclamation);

        if (lastSentenceEnd > 0 && lastSentenceEnd < truncatedContent.length() - 1) {
            return truncatedContent.substring(0, lastSentenceEnd + 1);
        }

        return truncatedContent + (content.length() > maxLength ? "..." : "");
    }
}
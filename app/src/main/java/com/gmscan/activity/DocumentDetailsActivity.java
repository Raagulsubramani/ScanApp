package com.gmscan.activity;

import static com.gmscan.utility.CustomBottomSheetDialog.VIEW_EXPORT;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import com.gmscan.R;
import com.gmscan.model.documentUpdate.DocumentUpdateRequest;
import com.gmscan.model.getAllDocuments.ScannedDocument;
import com.gmscan.model.getDocumentById.GetDocumentById;
import com.gmscan.model.loginRegister.ErrorResponse;
import com.gmscan.service.RestApiBuilder;
import com.gmscan.service.RestApiService;
import com.gmscan.utility.CustomBottomSheetDialog;
import com.gmscan.utility.DocumentType;
import com.gmscan.utility.DocumentUtil;
import com.gmscan.utility.ImageUtil;
import com.gmscan.utility.IntentKeys;
import com.gmscan.utility.LoaderHelper;
import com.gmscan.utility.NetworkUtils;
import com.gmscan.utility.StringUtils;
import com.gmscan.utility.ZoomImageDialog;
import com.google.android.material.button.MaterialButton;
import com.google.gson.Gson;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class DocumentDetailsActivity extends AppCompatActivity implements CustomBottomSheetDialog.OnViewExportButtonClickListener {

    private static final String TAG = "DocumentDetailsActivity";
    private EditText summaryContent;
    private ImageView documentCoverImageView;
    private MaterialButton exportButton;
    private LinearLayout addFieldButton;
    private ExecutorService executor;
    private ScannedDocument currentDocument;
    private RestApiService apiService;
    private String documentID;
    private ImageView imgBack;
    private TextView txtTitle;
    private MaterialButton btnSave;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        try {
            setContentView(R.layout.document_details);

            // Initialize executor first to handle background tasks
            executor = Executors.newSingleThreadExecutor();

            // Initialize API service
            apiService = RestApiBuilder.getService();

            // Get document ID from intent - Updated to handle String and fallback to int
            documentID = getIntent().getStringExtra(IntentKeys.DOCUMENT_ID);
            if (documentID == null) {
                // Fallback to get as int and convert to String (for backward compatibility)
                int docIdInt = getIntent().getIntExtra(IntentKeys.DOCUMENT_ID, -1);
                if (docIdInt != -1) {
                    documentID = String.valueOf(docIdInt);
                } else {
                    documentID = "-1";
                }
            }

            if (documentID.equals("-1")) {
                Toast.makeText(this, "Invalid document ID", Toast.LENGTH_SHORT).show();
                finish();
                return;
            }

            initializeViews();
            setupClickListeners();

            // Get document details if we have a valid ID
            if (!documentID.equals("-1")) {
                getDocumentById(documentID);
            } else {
                // Try to load from recognized text if available
                String recognizedText = getIntent().getStringExtra(IntentKeys.OCR_TEXT);
                if (recognizedText != null) {
                    setOCRText(recognizedText);
                }
            }

        } catch (Exception e) {
            Log.e(TAG, "Error initializing activity: " + e.getMessage(), e);
            Toast.makeText(this, "Error loading document details", Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    /**
     * Initialize UI components and set their default visibility states
     */
    @SuppressLint("SetTextI18n")
    private void initializeViews() {
        try {
            // Initialize views from the updated layout
            txtTitle = findViewById(R.id.txtTitle);
            imgBack = findViewById(R.id.imgBack);

            String documentType = StringUtils.capitalizeFirstLetter(getIntent().getStringExtra(IntentKeys.DOCUMENT_TYPE));
            txtTitle.setText(documentType + " " + getString(R.string.details));

            imgBack.setOnClickListener(v -> finish());
            summaryContent = findViewById(R.id.summaryContent);
            documentCoverImageView = findViewById(R.id.documentCoverImageView);
            exportButton = findViewById(R.id.exportButton);
            addFieldButton = findViewById(R.id.addFieldButton);
            btnSave = findViewById(R.id.btnSave);

            // Initially disable summary editing
            summaryContent.setBackgroundResource(android.R.color.transparent);

            // Create a new ScannedDocument instance if it doesn't exist
            if (currentDocument == null) {
                currentDocument = new ScannedDocument();
            }

            if (btnSave != null) {
                btnSave.setOnClickListener(v -> {
                    // Call updateDocument directly without showing popup
                    updateDocument();
                });
            } else {
                Log.e(TAG, "Save button not found");
            }

        } catch (Exception e) {
            Log.e(TAG, "Error initializing views: " + e.getMessage(), e);
            throw e; // Re-throw to be caught by onCreate
        }
    }

    /**
     * Setup click listeners for buttons
     */
    private void setupClickListeners() {
        try {

            // Setup export button
            if (exportButton != null) {
                exportButton.setOnClickListener(v -> {
                    Log.d(TAG, "Export button clicked");
                    CustomBottomSheetDialog bottomSheet = CustomBottomSheetDialog.newInstance(VIEW_EXPORT);
                    bottomSheet.setOnExportButtonClickListener(this);
                    bottomSheet.show(getSupportFragmentManager(), "ExportBottomSheet");
                });
            } else {
                Log.e(TAG, "Export button not found");
            }

            // Setup add field button
            if (addFieldButton != null) {
                addFieldButton.setOnClickListener(v -> addNewField());
            }
        } catch (Exception e) {
            Log.e(TAG, "Error setting up click listeners: " + e.getMessage(), e);
        }
    }

    private ArrayList<String> getFormattedDocumentData() {
        StringBuilder formattedData = new StringBuilder();

        if (currentDocument != null && currentDocument.getOcrText() != null) {
            // Use the current text from the summary field
            String documentText = summaryContent.getText().toString().trim();
            if (!documentText.isEmpty()) {
                formattedData.append(documentText);
            } else {
                formattedData.append(currentDocument.getOcrText());
            }
        } else {
            // If no document is available, return a message
            formattedData.append("No document data available");
        }

        return new ArrayList<>(Arrays.asList(formattedData.toString()));
    }

    /**
     * Update document in database and on the server
     */
    private void updateDocument() {
        if (currentDocument == null) {
            Toast.makeText(this, "No document to update", Toast.LENGTH_SHORT).show();
            return;
        }

        // Get the updated values from summary field
        String updatedText = summaryContent.getText().toString().trim();

        if (updatedText.isEmpty()) {
            Toast.makeText(this, "Text cannot be empty", Toast.LENGTH_SHORT).show();
            return;
        }

        // Initialize executor if needed
        if (executor == null || executor.isShutdown()) {
            executor = Executors.newSingleThreadExecutor();
        }

        executor.execute(() -> {
            try {
                // Update the document in the local database
                currentDocument.setOcrText(updatedText);

                // Create API request object
                DocumentUpdateRequest updateRequest = new DocumentUpdateRequest();
                updateRequest.title = currentDocument.getTitle() != null ?
                        currentDocument.getTitle() : currentDocument.getName(); // Use name as fallback
                updateRequest.description = updatedText;

                // Make API call after local update
                runOnUiThread(() -> {
                    makeApiUpdateCall(documentID, updateRequest);
                });
            } catch (Exception e) {
                Log.e(TAG, "Error updating document: " + e.getMessage(), e);
                runOnUiThread(() -> Toast.makeText(DocumentDetailsActivity.this,
                        "Failed to update document in local database", Toast.LENGTH_SHORT).show());
            }
        });
    }

    private void makeApiUpdateCall(String documentId, DocumentUpdateRequest updateRequest) {
        Log.d(TAG, "Starting multipart API update call for document: " + documentId);

        // Show loading indicator
        LoaderHelper.showLoader(this, true);

        // Check network connectivity
        if (!NetworkUtils.isInternetAvailable(this)) {
            LoaderHelper.hideLoader();
            Toast.makeText(this, getString(R.string.no_internet), Toast.LENGTH_SHORT).show();
            Log.e(TAG, "Update aborted: No internet connection available");
            return;
        }

        RequestBody documentNameBody = RequestBody.create(
                okhttp3.MediaType.parse("text/plain"),
                updateRequest.title != null ? updateRequest.title : "");

        // Use current file URL or empty if not available
        RequestBody fileUrlBody = RequestBody.create(
                okhttp3.MediaType.parse("text/plain"),
                currentDocument != null && currentDocument.getFileUrl() != null ?
                        currentDocument.getFileUrl() : "");

        // For scan type - assuming "document" for business cards
        RequestBody scanTypeBody = RequestBody.create(
                okhttp3.MediaType.parse("text/plain"), DocumentType.DOCUMENT.getDisplayName());

        // For description/summary - using the description from updateRequest
        RequestBody summaryBody = RequestBody.create(
                okhttp3.MediaType.parse("text/plain"),
                updateRequest.description != null ? updateRequest.description : "");

        // Other empty fields that may need valid defaults
        RequestBody emptyStringBody = RequestBody.create(okhttp3.MediaType.parse("text/plain"), "");
        RequestBody emptyContentBody = RequestBody.create(okhttp3.MediaType.parse("text/plain"), "");

        // Log the request parameters
        Log.d(TAG, "Multipart request parameters:");
        Log.d(TAG, "- document_name: " + updateRequest.title);
        Log.d(TAG, "- name: " + updateRequest.getName());
        Log.d(TAG, "- profession: " + updateRequest.getProfession());
        Log.d(TAG, "- company_name: " + updateRequest.companyName);
        Log.d(TAG, "- email: " + updateRequest.email);
        Log.d(TAG, "- mobile_number: " + updateRequest.getMobileNumber());
        Log.d(TAG, "- address: " + updateRequest.address);
        Log.d(TAG, "- website: " + updateRequest.getWebsite());
        Log.d(TAG, "- number_of_pages: 0");  // Log the explicit value

        // Make the API call using multipart
        apiService.updateDocumentWithFormData(
                documentId,
                documentNameBody,     // document_name
                scanTypeBody,         // scan_type
                fileUrlBody,          // file_url
                summaryBody          // summary
        ).enqueue(new Callback<>() {
            @Override
            public void onResponse(Call<ResponseBody> call,
                                   Response<ResponseBody> response) {
                LoaderHelper.hideLoader();

                if (isFinishing()) {
                    Log.d(TAG, "Activity is finishing, ignoring response");
                    return;
                }

                if (response.isSuccessful() && response.body() != null) {
                    Log.d(TAG, "API update successful. Response code: " + response.code());
                    Log.d(TAG, "API update successful. Response body: " + new Gson().toJson(response.body()));

                    Toast.makeText(DocumentDetailsActivity.this,
                            "Document updated successfully",
                            Toast.LENGTH_SHORT).show();

                    Intent resultIntent = new Intent();
                    setResult(RESULT_OK, resultIntent);
                    finish();                    // Refresh the document to verify updates were applied
                } else {
                    handleErrorResponse(response);
                }
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                LoaderHelper.hideLoader();
                Log.e(TAG, "API update call failed", t);
                Log.e(TAG, "API update failure details: " + t.getMessage());
                Log.e(TAG, "Request URL: " + call.request().url());
                Log.e(TAG, "Request method: " + call.request().method());

                if (isFinishing()) {
                    Log.d(TAG, "Activity is finishing, ignoring failure");
                    return;
                }

                Toast.makeText(DocumentDetailsActivity.this,
                        "Error: " + t.getMessage(),
                        Toast.LENGTH_SHORT).show();
            }
        });
    }

    // Updated method to handle the array-based error response format
    private void handleErrorResponse(Response<?> response) {
        try {
            Log.e(TAG, "API update failed. HTTP status: " + response.code());

            if (response.errorBody() != null) {
                String errorBodyString = response.errorBody().string();
                Log.e(TAG, "Error response body: " + errorBodyString);

                // First try to extract a user-friendly message from the error body
                String userFriendlyMessage = extractErrorMessage(errorBodyString);

                Toast.makeText(DocumentDetailsActivity.this,
                        userFriendlyMessage,
                        Toast.LENGTH_LONG).show();
            } else {
                Log.e(TAG, "Error updating document. Empty error body. HTTP status: " + response.code());
                Toast.makeText(DocumentDetailsActivity.this,
                        "Failed to update document (HTTP " + response.code() + ")",
                        Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            Log.e(TAG, "Error parsing error response", e);
            Toast.makeText(DocumentDetailsActivity.this,
                    getString(R.string.default_error_message),
                    Toast.LENGTH_LONG).show();
        }
    }

    /**
     * Extract a user-friendly error message from the error response
     */
    private String extractErrorMessage(String errorBodyString) {
        try {
            // Try to parse as ErrorResponse first
            Gson gson = new Gson();
            ErrorResponse errorResponse = gson.fromJson(errorBodyString, ErrorResponse.class);

            if (errorResponse != null && errorResponse.getDetail() != null) {
                return errorResponse.getDetail();
            }

            // If that fails, return a default message
            return "Failed to update document. Please try again.";
        } catch (Exception e) {
            Log.e(TAG, "Error extracting error message: " + e.getMessage(), e);
            return "Failed to update document. Please try again.";
        }
    }

    /**
     * Sets the OCR text to the TextViews and processes the recognized text.
     *
     * @param recognizedText The text extracted via OCR to display and process.
     */
    @SuppressLint("SetTextI18n")
    private void setOCRText(String recognizedText) {
        if (currentDocument == null) {
            currentDocument = new ScannedDocument();
        }
        currentDocument.setOcrText(recognizedText);

        if (summaryContent != null) {
            summaryContent.setText(recognizedText);
        }
    }

    /**
     * Add a new field to the document
     */
    private void addNewField() {
        try {
            Toast.makeText(this, "Adding new field", Toast.LENGTH_SHORT).show();
            // Here you would implement the functionality to add a new field
        } catch (Exception e) {
            Log.e(TAG, "Error adding new field: " + e.getMessage(), e);
            Toast.makeText(this, "Failed to add new field", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (executor != null && !executor.isShutdown()) {
            executor.shutdown();
        }
    }

    @Override
    public void onExportPNGClick() {
        try {
            // Get document content
            ArrayList<String> documentData = getFormattedDocumentData();

            // Use DocumentUtil to generate and save image
            Uri imageUri = DocumentUtil.generateImage(documentData, this);

            if (imageUri != null) {
                Toast.makeText(this, "Document exported as image", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Failed to export document as image", Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            Log.e(TAG, "Error exporting document as PNG: " + e.getMessage(), e);
            Toast.makeText(this, "Error occurred while exporting as image", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onExportPDFClick() {
        try {
            // Get document content
            ArrayList<String> documentData = getFormattedDocumentData();

            // Use DocumentUtil to generate and save PDF
            Uri pdfUri = DocumentUtil.generatePDF(documentData, this);

            if (pdfUri != null) {
                Toast.makeText(this, "Document exported as PDF", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Failed to export document as PDF", Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            Log.e(TAG, "Error exporting document as PDF: " + e.getMessage(), e);
            Toast.makeText(this, "Error occurred while exporting as PDF", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Makes an API call to get document details by document ID
     *
     * @param documentId The ID of the document to retrieve
     */
    private void getDocumentById(String documentId) {
        if (!NetworkUtils.isInternetAvailable(this)) {
            Toast.makeText(this, getString(R.string.no_internet), Toast.LENGTH_SHORT).show();
            return;
        }

        // Show loader while retrieving data
        LoaderHelper.showLoader(this, true);

        // Log the document ID for debugging
        Log.d(TAG, "Fetching document with ID: " + documentId);

        Call<GetDocumentById> call = apiService.getDocumentById(documentId);

        call.enqueue(new Callback<GetDocumentById>() {
            @Override
            public void onResponse(@NonNull Call<GetDocumentById> call, @NonNull Response<GetDocumentById> response) {
                // Hide loader after response received
                LoaderHelper.hideLoader();

                if (isFinishing()) return; // Check if activity is still active

                Log.d(TAG, "Document API response code: " + response.code());

                if (response.isSuccessful()) {
                    GetDocumentById document = response.body();
                    if (document != null) {
                        // Process the retrieved document
                        Log.d(TAG, "Document retrieved successfully: " + document.toString());

                        // Convert the API document to ScannedDocument for display
                        if (currentDocument == null) {
                            currentDocument = new ScannedDocument();
                        }

                        // Map API response fields to ScannedDocument fields
                        currentDocument.setOcrText(document.getContent());
                        currentDocument.setName(document.getDocumentName());
                        currentDocument.setEmail(document.getEmail());
                        currentDocument.setCompanyTitle(document.getCompanyName());
                        currentDocument.setJobTitle(document.getProfession());
                        currentDocument.setAddress(document.getAddress());
                        currentDocument.setWebSite(document.getWebSite());
                        currentDocument.setTitle(document.getDocumentName());
                        currentDocument.setOcrText(document.getSummary());
                        currentDocument.setFileUrl(document.getFileUrl());

                        // Handle phone numbers if present
                        if (document.getMobileNumber() != null && !document.getMobileNumber().isEmpty()) {
                            ArrayList<String> phoneList = new ArrayList<>(Arrays.asList(document.getMobileNumber().split(",")));
                            currentDocument.setPhoneNumbers(phoneList);
                        }

                        // Update UI with the retrieved data
                        updateUIWithDocumentData();

                        Toast.makeText(DocumentDetailsActivity.this,
                                getString(R.string.document_retrieved_successfully),
                                Toast.LENGTH_SHORT).show();
                    } else {
                        Log.e(TAG, "Retrieved document is null");
                        Toast.makeText(DocumentDetailsActivity.this,
                                getString(R.string.error_empty_document),
                                Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Log.e(TAG, "Error response code: " + response.code());
                    // Print the raw error response body for debugging
                    try {
                        if (response.errorBody() != null) {
                            String errorBodyString = response.errorBody().string();
                            Log.e(TAG, "Error body: " + errorBodyString);

                            // Try to parse the error response
                            Gson gson = new Gson();
                            ErrorResponse errorResponse = gson.fromJson(errorBodyString, ErrorResponse.class);
                            String errorMessage = errorResponse != null && errorResponse.getDetail() != null
                                    ? errorResponse.getDetail()
                                    : getString(R.string.error_retrieving_document);

                            Toast.makeText(DocumentDetailsActivity.this, errorMessage, Toast.LENGTH_LONG).show();
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "Error parsing error body: " + e.getMessage());
                        Toast.makeText(DocumentDetailsActivity.this,
                                getString(R.string.error_retrieving_document),
                                Toast.LENGTH_SHORT).show();
                    }
                }
            }

            @Override
            public void onFailure(@NonNull Call<GetDocumentById> call, @NonNull Throwable t) {
                // Hide loader on failure
                LoaderHelper.hideLoader();

                if (isFinishing()) return; // Check if activity is still active

                Log.e(TAG, "API call failed: " + t.getMessage(), t);
                Toast.makeText(DocumentDetailsActivity.this,
                        getString(R.string.api_call_failed) + ": " + t.getMessage(),
                        Toast.LENGTH_SHORT).show();
            }
        });
    }

    /**
     * Updates the UI with data from the retrieved document
     */
    private void updateUIWithDocumentData() {
        if (currentDocument != null) {
            if (summaryContent != null && currentDocument.getOcrText() != null) {
                summaryContent.setText(currentDocument.getOcrText());
            }
            ImageUtil.load(this,currentDocument.getFileUrl(),documentCoverImageView,findViewById(R.id.progressBar));
            documentCoverImageView.setOnClickListener(v -> ZoomImageDialog.show(this, currentDocument.getFileUrl()));
        }
    }
}
package com.gmscan.activity;

import static com.gmscan.utility.CustomBottomSheetDialog.VIEW_EXPORT;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.PopupWindow;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.gmscan.R;
import com.gmscan.model.UpdateDocumentResponse;
import com.gmscan.model.getAllDocuments.ScannedDocument;
import com.gmscan.model.getDocumentById.GetDocumentById;
import com.gmscan.model.loginRegister.ErrorResponse;
import com.gmscan.model.documentUpdate.DocumentUpdateRequest;
import com.gmscan.service.RestApiBuilder;
import com.gmscan.service.RestApiService;
import com.gmscan.utility.CustomBottomSheetDialog;
import com.gmscan.utility.DocumentType;
import com.gmscan.utility.DocumentUtil;
import com.gmscan.utility.ImageUtil;
import com.gmscan.utility.IntentKeys;
import com.gmscan.utility.LoaderHelper;
import com.gmscan.utility.NetworkUtils;
import com.gmscan.utility.RegexUtils;
import com.gmscan.utility.StringUtils;
import com.gmscan.utility.ZoomImageDialog;
import com.google.android.material.button.MaterialButton;
import com.google.gson.Gson;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class IDDetailsActivity extends AppCompatActivity implements CustomBottomSheetDialog.OnViewExportButtonClickListener {
    private static final String TAG = "DetailsActivity";

    // Declare TextViews for displaying OCR details
    private EditText nameEditText;
    private EditText professionEditText;
    private EditText emailEditText;
    private EditText mobileNumberEditText;
    private EditText addressEditText;
    private EditText tagsEditText;
    private MaterialButton btnSave;
    private PopupWindow popupWindow;
    private ScannedDocument currentDocument;
    private MaterialButton exportButton;
    private String documentID;
    private RestApiService apiService;
    private ExecutorService executor;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.layout_details);

        // Initialize executor service
        executor = Executors.newSingleThreadExecutor();
        // Initialize the UI components
        initializeViews();
        // Retrieve the OCR text from the Intent
        String recognizedText = getIntent().getStringExtra(IntentKeys.OCR_TEXT);
        // Process and display the OCR text details
        if (recognizedText != null) {
            setOCRText(recognizedText);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Shutdown executor service to prevent memory leaks
        if (executor != null && !executor.isShutdown()) {
            executor.shutdown();
        }
    }

    /**
     * Initializes the views used in the activity.
     * This method links the UI components with their corresponding views.
     */
    @SuppressLint("SetTextI18n")
    private void initializeViews() {
        nameEditText = findViewById(R.id.nameEditText);
        professionEditText = findViewById(R.id.professionEditText);
        mobileNumberEditText = findViewById(R.id.phoneEditText);
        emailEditText = findViewById(R.id.emailEditText);
        addressEditText = findViewById(R.id.addressEditText);
        exportButton = findViewById(R.id.exportButton);

        TextView txtTitle = findViewById(R.id.txtTitle);
        ImageView imgBack = findViewById(R.id.imgBack);
        String documentType = StringUtils.capitalizeFirstLetter(getIntent().getStringExtra(IntentKeys.DOCUMENT_TYPE));
        txtTitle.setText(documentType + " " + getString(R.string.details));

        // Fix for the ClassCastException: get document ID as String instead of int
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

        imgBack.setOnClickListener(v -> finish());

        // Initialize save button
        btnSave = findViewById(R.id.btnSave);

        // Check if btnSave is not null before setting OnClickListener
        if (btnSave != null) {
            btnSave.setOnClickListener(v -> {
                // First update the document in database and API
                updateDocument();
            });
        } else {
            Log.e(TAG, "btnSave Button not found in layout");
        }

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
        apiService = RestApiBuilder.getService();
        if (documentID != null && !documentID.equals("-1")) {
            getDocumentById(documentID);
        }
    }

    private void updateDocument() {
        if (currentDocument == null) {
            Toast.makeText(this, "No document to update", Toast.LENGTH_SHORT).show();
            return;
        }

        // Get the updated values from EditText fields
        String name = nameEditText.getText().toString().trim();
        String profession = professionEditText.getText().toString().trim();
        String email = emailEditText.getText().toString().trim();
        String mobileNumber = mobileNumberEditText.getText().toString().trim();
        String address = addressEditText.getText().toString().trim();

        // Execute update in background
        executor.execute(() -> {
            try {
                // Update the current document object with new values
                currentDocument.setName(name);
                currentDocument.setEmail(email);

                // For phone numbers, handle the comma-separated string
                if (!mobileNumber.isEmpty()) {
                    ArrayList<String> phoneList = new ArrayList<>(Arrays.asList(mobileNumber.split(",")));
                    // Trim each phone number
                    for (int i = 0; i < phoneList.size(); i++) {
                        phoneList.set(i, phoneList.get(i).trim());
                    }
                    currentDocument.setPhoneNumbers(phoneList);
                } else {
                    currentDocument.setPhoneNumbers(new ArrayList<>());
                }

                currentDocument.setAddress(address);

                // Create API request object
                DocumentUpdateRequest updateRequest = new DocumentUpdateRequest();

                // Set properties on the request object
                updateRequest.title = name;  // Use name as the title
                updateRequest.setName(name);
                updateRequest.setProfession(profession);
                updateRequest.email = email;
                updateRequest.setMobileNumber(mobileNumber);
                updateRequest.address = address;

                // If company name field exists, set it
                if (currentDocument.getCompanyTitle() != null) {
                    updateRequest.companyName = currentDocument.getCompanyTitle();
                }

                // If website field exists, set it
                if (currentDocument.getWebSite() != null) {
                    updateRequest.setWebsite(currentDocument.getWebSite());
                }

                // Create a description from the fields
                StringBuilder description = new StringBuilder();
                if (!profession.isEmpty())
                    description.append("Profession: ").append(profession).append("\n");
                if (!email.isEmpty()) description.append("Email: ").append(email).append("\n");
                if (!mobileNumber.isEmpty())
                    description.append("Phone: ").append(mobileNumber).append("\n");
                if (!address.isEmpty()) description.append("Address: ").append(address);

                updateRequest.description = description.toString();

                // Make API call after local update
                String documentIdStr = documentID;
                runOnUiThread(() -> {
                    makeApiUpdateCall(documentIdStr, updateRequest);
                });
            } catch (Exception e) {
                runOnUiThread(() -> {
                    Toast.makeText(IDDetailsActivity.this,
                            "Error updating document: " + e.getMessage(),
                            Toast.LENGTH_SHORT).show();
                });
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

        // Create RequestBody objects for each field
        RequestBody nameBody = RequestBody.create(
                okhttp3.MediaType.parse("text/plain"),
                updateRequest.getName() != null ? updateRequest.getName() : "");

        RequestBody professionBody = RequestBody.create(
                okhttp3.MediaType.parse("text/plain"),
                updateRequest.getProfession() != null ? updateRequest.getProfession() : "");

        RequestBody emailBody = RequestBody.create(
                okhttp3.MediaType.parse("text/plain"),
                updateRequest.email != null ? updateRequest.email : "");

        RequestBody mobileNumberBody = RequestBody.create(
                okhttp3.MediaType.parse("text/plain"),
                updateRequest.getMobileNumber() != null ? updateRequest.getMobileNumber() : "");

        RequestBody addressBody = RequestBody.create(
                okhttp3.MediaType.parse("text/plain"),
                updateRequest.address != null ? updateRequest.address : "");

        RequestBody companyNameBody = RequestBody.create(
                okhttp3.MediaType.parse("text/plain"),
                updateRequest.companyName != null ? updateRequest.companyName : "");

        RequestBody websiteBody = RequestBody.create(
                okhttp3.MediaType.parse("text/plain"),
                updateRequest.getWebsite() != null ? updateRequest.getWebsite() : "");

        RequestBody documentNameBody = RequestBody.create(
                okhttp3.MediaType.parse("text/plain"),
                updateRequest.title != null ? updateRequest.title : "");

        // For boolean values
        RequestBody isFavoriteBody = RequestBody.create(
                okhttp3.MediaType.parse("text/plain"), "false");

        // Use current file URL or empty if not available
        RequestBody fileUrlBody = RequestBody.create(
                okhttp3.MediaType.parse("text/plain"),
                currentDocument != null && currentDocument.getFileUrl() != null ?
                        currentDocument.getFileUrl() : "");

        // For scan type - assuming "id" for business cards
        RequestBody scanTypeBody = RequestBody.create(
                okhttp3.MediaType.parse("text/plain"),  DocumentType.ID.getDisplayName());

        // For description/summary - using the description from updateRequest
        RequestBody summaryBody = RequestBody.create(
                okhttp3.MediaType.parse("text/plain"),
                updateRequest.description != null ? updateRequest.description : "");

        // Create numeric fields with valid values
        RequestBody numberOfPagesBody = RequestBody.create(
                okhttp3.MediaType.parse("text/plain"), "0");

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
        Log.d(TAG, "- number_of_pages: 0");

        // Make the API call using multipart
        apiService.updateDocumentWithFormData(
                documentId,
                documentNameBody,     // document_name
                scanTypeBody,         // scan_type
                isFavoriteBody,       // is_favorite
                fileUrlBody,          // file_url
                nameBody,             // name
                professionBody,       // profession
                emailBody,            // email
                mobileNumberBody,     // mobile_number
                addressBody,          // address
                companyNameBody,      // company_name
                websiteBody,          // website
                emptyStringBody,      // isbn_no
                emptyStringBody,      // book_name
                emptyStringBody,      // author_name
                emptyStringBody,      // publication
                numberOfPagesBody,    // number_of_pages
                emptyStringBody,      // subject
                summaryBody,          // summary
                emptyContentBody      // content
        ).enqueue(new Callback<UpdateDocumentResponse>() {
            @Override
            public void onResponse(Call<UpdateDocumentResponse> call,
                                   Response<UpdateDocumentResponse> response) {
                LoaderHelper.hideLoader();

                if (isFinishing()) {
                    Log.d(TAG, "Activity is finishing, ignoring response");
                    return;
                }

                if (response.isSuccessful() && response.body() != null) {
                    Log.d(TAG, "API update successful. Response code: " + response.code());
                    Log.d(TAG, "API update successful. Response body: " + new Gson().toJson(response.body()));

                    Toast.makeText(IDDetailsActivity.this,
                            "ID updated successfully",
                            Toast.LENGTH_SHORT).show();

                    Intent resultIntent = new Intent();
                    setResult(RESULT_OK, resultIntent);
                    finish();
                } else {
                    handleErrorResponse(response);
                }
            }

            @Override
            public void onFailure(Call<UpdateDocumentResponse> call, Throwable t) {
                LoaderHelper.hideLoader();
                Log.e(TAG, "API update call failed", t);
                Log.e(TAG, "API update failure details: " + t.getMessage());
                Log.e(TAG, "Request URL: " + call.request().url());
                Log.e(TAG, "Request method: " + call.request().method());

                if (isFinishing()) {
                    Log.d(TAG, "Activity is finishing, ignoring failure");
                    return;
                }

                Toast.makeText(IDDetailsActivity.this,
                        "Error: " + t.getMessage(),
                        Toast.LENGTH_SHORT).show();
            }
        });
    }

    // Handle error responses
    private void handleErrorResponse(Response<?> response) {
        try {
            Log.e(TAG, "API update failed. HTTP status: " + response.code());

            if (response.errorBody() != null) {
                String errorBodyString = response.errorBody().string();
                Log.e(TAG, "Error response body: " + errorBodyString);

                // First try to extract a user-friendly message from the error body
                String userFriendlyMessage = extractErrorMessage(errorBodyString);

                Toast.makeText(IDDetailsActivity.this,
                        userFriendlyMessage,
                        Toast.LENGTH_LONG).show();
            } else {
                Log.e(TAG, "Error updating document. Empty error body. HTTP status: " + response.code());
                Toast.makeText(IDDetailsActivity.this,
                        "Failed to update document (HTTP " + response.code() + ")",
                        Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            Log.e(TAG, "Error parsing error response", e);
            Toast.makeText(IDDetailsActivity.this,
                    getString(R.string.default_error_message),
                    Toast.LENGTH_LONG).show();
        }
    }

    // Helper method to extract error messages from the response
    private String extractErrorMessage(String errorBody) {
        try {
            // Try to parse as JSON and extract error details
            JSONObject jsonObject = new JSONObject(errorBody);

            // Check for "detail" field commonly used in error responses
            if (jsonObject.has("detail")) {
                return jsonObject.getString("detail");
            }

            // Check for array format errors
            if (jsonObject.has("errors") && jsonObject.get("errors") instanceof JSONArray) {
                JSONArray errors = jsonObject.getJSONArray("errors");
                if (errors.length() > 0) {
                    // Get first error message
                    return errors.getJSONObject(0).getString("message");
                }
            }

            // Default generic message
            return getString(R.string.error_updating_document);
        } catch (JSONException e) {
            Log.e(TAG, "Failed to parse error JSON", e);
            return getString(R.string.error_updating_document);
        }
    }

    private ArrayList<String> getFormattedIdData() {
        StringBuilder formattedData = new StringBuilder();

        // Add original OCR text as fallback if fields are null
        if (currentDocument != null && currentDocument.getOcrText() != null) {
            // We'll use the extracted fields from EditTexts if they exist, otherwise fall back to the original data

            // Add ID card data in structured format with null checks
            if (nameEditText != null) {
                String name = nameEditText.getText().toString().trim();
                if (!name.isEmpty()) {
                    formattedData.append(name).append("\n");
                } else if (currentDocument.getName() != null) {
                    formattedData.append(currentDocument.getName()).append("\n");
                }
            } else if (currentDocument.getName() != null) {
                formattedData.append(currentDocument.getName()).append("\n");
            }

            if (professionEditText != null) {
                String profession = professionEditText.getText().toString().trim();
                if (!profession.isEmpty()) {
                    formattedData.append(profession).append("\n\n");
                }
            }

            if (emailEditText != null) {
                String email = emailEditText.getText().toString().trim();
                if (!email.isEmpty()) {
                    formattedData.append("Email: ").append(email).append("\n");
                } else if (currentDocument.getEmail() != null) {
                    formattedData.append("Email: ").append(currentDocument.getEmail()).append("\n");
                }
            } else if (currentDocument.getEmail() != null) {
                formattedData.append("Email: ").append(currentDocument.getEmail()).append("\n");
            }

            if (mobileNumberEditText != null) {
                String phone = mobileNumberEditText.getText().toString().trim();
                if (!phone.isEmpty()) {
                    formattedData.append("Phone: ").append(phone).append("\n");
                } else if (currentDocument.getPhoneNumbers() != null && !currentDocument.getPhoneNumbers().isEmpty()) {
                    formattedData.append("Phone: ").append(String.join(", ", currentDocument.getPhoneNumbers())).append("\n");
                }
            } else if (currentDocument.getPhoneNumbers() != null && !currentDocument.getPhoneNumbers().isEmpty()) {
                formattedData.append("Phone: ").append(String.join(", ", currentDocument.getPhoneNumbers())).append("\n");
            }

            if (addressEditText != null) {
                String address = addressEditText.getText().toString().trim();
                if (!address.isEmpty()) {
                    formattedData.append("Address: ").append(address).append("\n");
                } else if (currentDocument.getAddress() != null) {
                    formattedData.append("Address: ").append(currentDocument.getAddress()).append("\n");
                }
            } else if (currentDocument.getAddress() != null) {
                formattedData.append("Address: ").append(currentDocument.getAddress()).append("\n");
            }

            // If somehow we still have no formatted data, use the raw OCR text
            if (formattedData.length() == 0) {
                formattedData.append(currentDocument.getOcrText());
            }
        } else {
            // If no document is available, return a message
            formattedData.append("No ID card data available");
        }

        return new ArrayList<>(Arrays.asList(formattedData.toString()));
    }

    /**
     * Sets the OCR text to the TextViews and processes the recognized text.
     * It extracts useful information like Name, Email, Phone numbers, and Address.
     *
     * @param recognizedText The text extracted via OCR to display and process.
     */
    @SuppressLint("SetTextI18n")
    private void setOCRText(String recognizedText) {
        // Split the recognized text by line breaks for easier processing
        String[] lines = recognizedText.split("\n");
        currentDocument = new ScannedDocument();
        currentDocument.setOcrText(recognizedText);

        // Extract and set email using RegexUtils utility method
        RegexUtils.extractEmails(recognizedText).stream().findFirst().ifPresent(currentDocument::setEmail);

        // Extract and set phone numbers using RegexUtils utility method
        currentDocument.setPhoneNumbers(RegexUtils.extractPhoneNumbers(recognizedText));

        // Set the first line as the name (commonly the first line in OCR)
        if (lines.length > 0) {
            currentDocument.setName(lines[0].trim());
        }

        // Initialize a StringBuilder to build the address
        StringBuilder addressBuilder = new StringBuilder();

        // Iterate over each line to find potential address-related lines
        for (String line : lines) {
            line = line.trim();

            // Skip empty lines and lines containing emails
            if (line.isEmpty() || line.contains("@")) {
                continue;
            }

            // Skip lines that look like phone numbers
            if (line.replaceAll("[^0-9+]", "").length() > (line.length() / 2)) {
                continue;
            }

            // Skip the line if it matches the name already set
            if (line.equals(currentDocument.getName())) {
                continue;
            }

            // Check if the line could be part of an address using RegexUtils
            if (RegexUtils.isAddress(line)) {
                if (addressBuilder.length() > 0) {
                    // Append comma if addressBuilder already contains some data
                    addressBuilder.append(", ");
                }
                // Append the line to the address
                addressBuilder.append(line.trim());
            }
        }

        // Clean up the final address by removing extra spaces and commas
        String finalAddress = addressBuilder.toString().replaceAll("\\s+", " ")          // Replace multiple spaces with a single space
                .replaceAll("\\s*,\\s*", ", ")    // Ensure spaces around commas are cleaned
                .replaceAll(",,", ",")            // Remove any consecutive commas
                .trim();

        // Set the final address in the ScannedDocument object if it's not empty
        if (!finalAddress.isEmpty()) {
            currentDocument.setAddress(finalAddress);
        }

        // Populate EditTexts with extracted information - added null checks
        safeSetText(nameEditText, currentDocument.getName());
        safeSetText(emailEditText, currentDocument.getEmail());
        safeSetText(mobileNumberEditText, currentDocument.getPhoneNumbers() != null ? String.join(", ", currentDocument.getPhoneNumbers()) : "");
        safeSetText(addressEditText, currentDocument.getAddress());
    }

    /**
     * Safely sets text to an EditText, preventing NullPointerException
     *
     * @param editText The EditText to set text on
     * @param text     The text to set
     */
    private void safeSetText(EditText editText, String text) {
        if (editText != null && text != null) {
            editText.setText(text);
        }
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            getOnBackPressedDispatcher().onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onExportPNGClick() {
        // Get formatted id card data
        ArrayList<String> extractedTexts = getFormattedIdData();

        // Use DocumentUtil to generate and save image
        Uri imageUri = DocumentUtil.generateImage(extractedTexts, this);

        if (imageUri != null) {
            Toast.makeText(this, "Id card exported as image", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this, "Failed to export Id card as image", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onExportPDFClick() {
        // Get formatted id card data
        ArrayList<String> extractedTexts = getFormattedIdData();

        // Use DocumentUtil to generate and save PDF
        Uri pdfUri = DocumentUtil.generatePDF(extractedTexts, this);

        if (pdfUri != null) {
            Toast.makeText(this, "Id card exported as PDF", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this, "Failed to export Id card as PDF", Toast.LENGTH_SHORT).show();
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
                        currentDocument.setOcrText(document.getName());
                        currentDocument.setEmail(document.getEmail());
                        currentDocument.setCompanyTitle(document.getCompanyName());
                        currentDocument.setJobTitle(document.getProfession());
                        currentDocument.setAddress(document.getAddress());
                        currentDocument.setWebSite(document.getWebSite());
                        currentDocument.setFileUrl(document.getFileUrl());

                        // Handle phone numbers if present
                        if (document.getMobileNumber() != null && !document.getMobileNumber().isEmpty()) {
                            ArrayList<String> phoneList = new ArrayList<>(Arrays.asList(document.getMobileNumber().split(",")));
                            currentDocument.setPhoneNumbers(phoneList);
                        }

                        // Update UI with the retrieved data
                        updateUIWithDocumentData();

                        Toast.makeText(IDDetailsActivity.this,
                                getString(R.string.document_retrieved_successfully),
                                Toast.LENGTH_SHORT).show();
                    } else {
                        Log.e(TAG, "Retrieved document is null");
                        Toast.makeText(IDDetailsActivity.this,
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

                            Toast.makeText(IDDetailsActivity.this, errorMessage, Toast.LENGTH_LONG).show();
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "Error parsing error body: " + e.getMessage());
                        Toast.makeText(IDDetailsActivity.this,
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
                Toast.makeText(IDDetailsActivity.this,
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
            safeSetText(nameEditText, currentDocument.getName());
            safeSetText(professionEditText, currentDocument.getJobTitle());
            safeSetText(emailEditText, currentDocument.getEmail());
            safeSetText(mobileNumberEditText, currentDocument.getPhoneNumbers() != null ?
                    String.join(", ", currentDocument.getPhoneNumbers()) : "");
            safeSetText(addressEditText, currentDocument.getAddress());
            ImageUtil.load(this,currentDocument.getFileUrl(),findViewById(R.id.businessCardImage),findViewById(R.id.imgProgress));
            findViewById(R.id.businessCardImage).setOnClickListener(v -> ZoomImageDialog.show(this, currentDocument.getFileUrl()));
        }
    }
}
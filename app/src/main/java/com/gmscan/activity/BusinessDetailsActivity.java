package com.gmscan.activity;

import static com.gmscan.utility.CustomBottomSheetDialog.VIEW_EXPORT;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.widget.ImageView;
import android.widget.EditText;
import android.widget.TextView;
import com.gmscan.R;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.gmscan.model.getAllDocuments.ScannedDocument;
import com.gmscan.model.getDocumentById.GetDocumentById;
import com.gmscan.service.RestApiBuilder;
import com.gmscan.service.RestApiService;
import com.gmscan.model.documentUpdate.DocumentUpdateRequest;
import com.gmscan.utility.CustomBottomSheetDialog;
import com.gmscan.utility.DocumentType;
import com.gmscan.utility.ImageUtil;
import com.gmscan.utility.NetworkUtils;
import com.gmscan.utility.StringUtils;
import com.gmscan.utility.ZoomImageDialog;
import com.google.android.material.button.MaterialButton;
import com.gmscan.utility.DocumentUtil;
import com.gmscan.utility.IntentKeys;
import com.gmscan.utility.RegexUtils;

import java.util.ArrayList;
import java.util.List;

import com.gmscan.utility.LoaderHelper;
import retrofit2.Call;
import com.google.gson.Gson;
import retrofit2.Callback;
import retrofit2.Response;
import com.gmscan.model.ErrorResponse;

import org.json.JSONArray;
import org.json.JSONObject;

import okhttp3.RequestBody;

public class BusinessDetailsActivity extends AppCompatActivity implements CustomBottomSheetDialog.OnViewExportButtonClickListener{
    private static final String TAG = "BusinessDetailsActivity";
    private ImageView imgBack;
    private TextView txtTitle;
    private EditText companyNameEditText;
    private EditText bnameEditText;
    private EditText jobtitleEditText;
    private EditText emailEditText;
    private EditText mobileNumberEditText;
    private EditText addressEditText;
    private EditText websiteEditText;
    private MaterialButton btnSave;
    private MaterialButton exportButton;
    private ScannedDocument currentDocument;
    private RestApiService apiService;
    private String documentID;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.layout_business_card_details);

        // Initialize currentDocument at the beginning
        currentDocument = new ScannedDocument();
        apiService = RestApiBuilder.getService();

        initializeViews();

        // Get OCR text from intent
        String recognizedText = getIntent().getStringExtra(IntentKeys.OCR_TEXT);
        documentID = getIntent().getStringExtra(IntentKeys.DOCUMENT_ID);
        Log.d(TAG, "Document ID received: " + documentID);
        Log.d(TAG, "Received OCR Text: " + recognizedText);

        if (recognizedText != null && !recognizedText.isEmpty()) {
            setOCRText(recognizedText);
        } else {
            Log.e(TAG, "No OCR text received");
            setEmptyState();
        }

        getDocumentById(String.valueOf(documentID));
    }

    @SuppressLint("SetTextI18n")
    private void initializeViews() {
        txtTitle = findViewById(R.id.txtTitle);
        imgBack = findViewById(R.id.imgBack);
        String documentType = StringUtils.capitalizeFirstLetter(getIntent().getStringExtra(IntentKeys.DOCUMENT_TYPE));
        txtTitle.setText(documentType + " " + getString(R.string.details));
        imgBack.setOnClickListener(v -> finish());
        bnameEditText = findViewById(R.id.nameEditText);
        jobtitleEditText = findViewById(R.id.jobtitleEditText);
        companyNameEditText = findViewById(R.id.companyNameEditText);
        mobileNumberEditText = findViewById(R.id.phoneEditText);
        emailEditText = findViewById(R.id.emailEditText);
        websiteEditText = findViewById(R.id.websiteEditText);
        addressEditText = findViewById(R.id.addressEditText);
//      tagsEditText = findViewById(R.id.tagsEditText);

        exportButton = findViewById(R.id.exportButton);
        btnSave = findViewById(R.id.btnSave);

        if (btnSave != null) {
            btnSave.setOnClickListener(v -> {
                // Call updateDocument directly without showing popup
                updateDocument();
            });
        } else {
            Log.e(TAG, "Save button not found");
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

        // Add debug check at the end
        Log.d(TAG, "View initialization complete. Checking references:");
        Log.d(TAG, "bnameEditText: " + (bnameEditText != null ? "initialized" : "NULL"));
        Log.d(TAG, "jobtitleEditText: " + (jobtitleEditText != null ? "initialized" : "NULL"));
        Log.d(TAG, "companyNameEditText: " + (companyNameEditText != null ? "initialized" : "NULL"));
        Log.d(TAG, "mobileNumberEditText: " + (mobileNumberEditText != null ? "initialized" : "NULL"));
        Log.d(TAG, "emailEditText: " + (emailEditText != null ? "initialized" : "NULL"));
        Log.d(TAG, "websiteEditText: " + (websiteEditText != null ? "initialized" : "NULL"));
        Log.d(TAG, "addressEditText: " + (addressEditText != null ? "initialized" : "NULL"));
    }

    private void updateDocument() {
        if (currentDocument == null) {
            Log.e(TAG, "updateDocument: currentDocument is null");
            Toast.makeText(this, "No document to update", Toast.LENGTH_SHORT).show();
            return;
        }

        // Show loading indicator before starting the update process

        Log.d(TAG, "Starting document update process");

        // Get the values from EditText fields
        String name = bnameEditText.getText().toString().trim();
        String jobTitle = jobtitleEditText.getText().toString().trim();
        String companyName = companyNameEditText.getText().toString().trim();
        String email = emailEditText.getText().toString().trim();
        String mobileNumber = mobileNumberEditText.getText().toString().trim();
        String address = addressEditText.getText().toString().trim();
        String website = websiteEditText.getText().toString().trim();

        Log.d(TAG, "Values from UI fields:");
        Log.d(TAG, "- name: " + name);
        Log.d(TAG, "- jobTitle: " + jobTitle);
        Log.d(TAG, "- companyName: " + companyName);
        Log.d(TAG, "- email: " + email);
        Log.d(TAG, "- mobileNumber: " + mobileNumber);
        Log.d(TAG, "- address: " + address);
        Log.d(TAG, "- website: " + website);

        // Create a description
        currentDocument.setAddress(address);
        currentDocument.setWebSite(website);

        StringBuilder description = new StringBuilder();
        if (!name.isEmpty()) description.append("Name: ").append(name).append("\n");
        if (!jobTitle.isEmpty()) description.append("Job Title: ").append(jobTitle).append("\n");
        if (!companyName.isEmpty()) description.append("Company: ").append(companyName).append("\n");
        if (!email.isEmpty()) description.append("Email: ").append(email).append("\n");
        if (!mobileNumber.isEmpty()) description.append("Phone: ").append(mobileNumber).append("\n");
        if (!address.isEmpty()) description.append("Address: ").append(address).append("\n");
        if (!website.isEmpty()) description.append("Website: ").append(website);

        // Create API request object
        DocumentUpdateRequest updateRequest = new DocumentUpdateRequest();
        Log.d(TAG, "Created new DocumentUpdateRequest object");

        // Set values on update request
        updateRequest.setTitle(name);
        updateRequest.setDescription(description.toString());
        updateRequest.setProfession(jobTitle);  // Use this if the API expects "profession"
        updateRequest.setName(name);
        updateRequest.setCompanyName(companyName);
        updateRequest.setEmail(email);
        updateRequest.setMobileNumber(mobileNumber);  // Use setter instead of direct assignment
        updateRequest.setAddress(address);
        updateRequest.setWebsite(website);  // Use setter instead of direct assignment

        Log.d(TAG, "DocumentUpdateRequest populated with values");
        Log.d(TAG, "Final request object: " + new Gson().toJson(updateRequest));

        // Make the API call
        if (documentID != null && !documentID.isEmpty()) {
            Log.d(TAG, "Proceeding with update for document ID: " + documentID);
            makeApiUpdateCall(documentID, updateRequest);
        } else {

            Log.e(TAG, "Update aborted: Document ID is missing");
            Toast.makeText(BusinessDetailsActivity.this,
                    "Error: Document ID is missing",
                    Toast.LENGTH_SHORT).show();
        }
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

        // For scan type - assuming "business" for business cards
        RequestBody scanTypeBody = RequestBody.create(
                okhttp3.MediaType.parse("text/plain"),  DocumentType.BUSINESS.getDisplayName());

        // For description/summary - using the description from updateRequest
        RequestBody summaryBody = RequestBody.create(
                okhttp3.MediaType.parse("text/plain"),
                updateRequest.description != null ? updateRequest.description : "");

        // Create numeric fields with valid values since the error is related to number_of_pages
        RequestBody numberOfPagesBody = RequestBody.create(
                okhttp3.MediaType.parse("text/plain"), "0");  // Use "0" instead of empty string

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
                numberOfPagesBody,    // number_of_pages - using "0" instead of empty string
                emptyStringBody,      // subject
                summaryBody,          // summary
                emptyContentBody      // content
        ).enqueue(new Callback<com.gmscan.model.UpdateDocumentResponse>() {
            @Override
            public void onResponse(Call<com.gmscan.model.UpdateDocumentResponse> call,
                                   Response<com.gmscan.model.UpdateDocumentResponse> response) {
                LoaderHelper.hideLoader();

                if (isFinishing()) {
                    Log.d(TAG, "Activity is finishing, ignoring response");
                    return;
                }

                if (response.isSuccessful() && response.body() != null) {
                    Log.d(TAG, "API update successful. Response code: " + response.code());
                    Log.d(TAG, "API update successful. Response body: " + new Gson().toJson(response.body()));

                    Toast.makeText(BusinessDetailsActivity.this,
                            "Business card updated successfully",
                            Toast.LENGTH_SHORT).show();

                    Intent resultIntent = new Intent();
                    setResult(RESULT_OK, resultIntent);
                    finish();
                } else {
                    handleErrorResponse(response);
                }
            }

            @Override
            public void onFailure(Call<com.gmscan.model.UpdateDocumentResponse> call, Throwable t) {
                LoaderHelper.hideLoader();
                Log.e(TAG, "API update call failed", t);
                Log.e(TAG, "API update failure details: " + t.getMessage());
                Log.e(TAG, "Request URL: " + call.request().url());
                Log.e(TAG, "Request method: " + call.request().method());

                if (isFinishing()) {
                    Log.d(TAG, "Activity is finishing, ignoring failure");
                    return;
                }

                Toast.makeText(BusinessDetailsActivity.this,
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

                Toast.makeText(BusinessDetailsActivity.this,
                        userFriendlyMessage,
                        Toast.LENGTH_LONG).show();
            } else {
                Log.e(TAG, "Error updating document. Empty error body. HTTP status: " + response.code());
                Toast.makeText(BusinessDetailsActivity.this,
                        "Failed to update business card (HTTP " + response.code() + ")",
                        Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            Log.e(TAG, "Error parsing error response", e);
            Toast.makeText(BusinessDetailsActivity.this,
                    getString(R.string.default_error_message),
                    Toast.LENGTH_LONG).show();
        }
    }

    // Helper method to extract user-friendly error message
    private String extractErrorMessage(String errorBodyString) {
        try {
            // Try to parse as JSON directly first
            JSONObject jsonObject = new JSONObject(errorBodyString);

            // Check if detail is an array
            if (jsonObject.has("detail") && jsonObject.get("detail") instanceof JSONArray) {
                JSONArray detailArray = jsonObject.getJSONArray("detail");
                if (detailArray.length() > 0) {
                    JSONObject firstError = detailArray.getJSONObject(0);
                    if (firstError.has("msg")) {
                        return firstError.getString("msg");
                    }
                }
            }
            // Check if detail is a string
            else if (jsonObject.has("detail") && jsonObject.get("detail") instanceof String) {
                return jsonObject.getString("detail");
            }

            return getString(R.string.default_error_message);
        } catch (Exception e) {
            Log.e(TAG, "Error extracting error message", e);
            return getString(R.string.default_error_message);
        }
    }

    private void setEmptyState() {
        // No fullInfoTextView to set, so just make views visible
        makeAllViewsVisible();
    }

    private void makeAllViewsVisible() {
        // Removed fullInfoTextView visibility setting
    }



    private void saveAsImage() {
        if (currentDocument == null) {
            Toast.makeText(this, "No data to save", Toast.LENGTH_SHORT).show();
            return;
        }

        // Prepare formatted text from all fields
        ArrayList<String> extractedTexts = getFormattedBusinessCardData();

        // Use DocumentUtil to generate and save image
        Uri imageUri = DocumentUtil.generateImage(extractedTexts, this);

        if (imageUri != null) {
            Toast.makeText(this, "Business card saved as image", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this, "Failed to save business card as image", Toast.LENGTH_SHORT).show();
        }
    }

    private void saveAsPdf() {
        if (currentDocument == null) {
            Toast.makeText(this, "No data to save", Toast.LENGTH_SHORT).show();
            return;
        }

        // Prepare formatted text from all fields
        ArrayList<String> extractedTexts = getFormattedBusinessCardData();

        // Use DocumentUtil to generate and save PDF
        Uri pdfUri = DocumentUtil.generatePDF(extractedTexts, this);

        if (pdfUri != null) {
            Toast.makeText(this, "Business card saved as PDF", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this, "Failed to save business card as PDF", Toast.LENGTH_SHORT).show();
        }
    }


    @SuppressLint("NewApi")
    private ArrayList<String> getFormattedBusinessCardData() {
        StringBuilder formattedData = new StringBuilder();
        boolean hasAddedContent = false; // Flag to track if we've added any real content

        // Check if document exists and has OCR text
        if (currentDocument != null) {
            // We'll use the extracted fields from EditTexts if they exist, otherwise fall back to the original data

            // Add business card data in structured format with null checks
            if (bnameEditText != null) {
                String name = bnameEditText.getText().toString().trim();
                if (!name.isEmpty()) {
                    formattedData.append(name).append("\n");
                    hasAddedContent = true;
                } else if (currentDocument.getName() != null && !currentDocument.getName().trim().isEmpty()) {
                    formattedData.append(currentDocument.getName()).append("\n");
                    hasAddedContent = true;
                }
            } else if (currentDocument.getName() != null && !currentDocument.getName().trim().isEmpty()) {
                formattedData.append(currentDocument.getName()).append("\n");
                hasAddedContent = true;
            }

            if (jobtitleEditText != null) {
                String jobTitle = jobtitleEditText.getText().toString().trim();
                if (!jobTitle.isEmpty()) {
                    formattedData.append(jobTitle).append("\n");
                    hasAddedContent = true;
                } else if (currentDocument.getJobTitle() != null && !currentDocument.getJobTitle().trim().isEmpty()) {
                    formattedData.append(currentDocument.getJobTitle()).append("\n");
                    hasAddedContent = true;
                }
            } else if (currentDocument.getJobTitle() != null && !currentDocument.getJobTitle().trim().isEmpty()) {
                formattedData.append(currentDocument.getJobTitle()).append("\n");
                hasAddedContent = true;
            }

            if (companyNameEditText != null) {
                String company = companyNameEditText.getText().toString().trim();
                if (!company.isEmpty()) {
                    formattedData.append(company).append("\n\n");
                    hasAddedContent = true;
                } else if (currentDocument.getCompanyTitle() != null && !currentDocument.getCompanyTitle().trim().isEmpty()) {
                    formattedData.append(currentDocument.getCompanyTitle()).append("\n\n");
                    hasAddedContent = true;
                }
            } else if (currentDocument.getCompanyTitle() != null && !currentDocument.getCompanyTitle().trim().isEmpty()) {
                formattedData.append(currentDocument.getCompanyTitle()).append("\n\n");
                hasAddedContent = true;
            }

            if (emailEditText != null) {
                String email = emailEditText.getText().toString().trim();
                if (!email.isEmpty()) {
                    formattedData.append("Email: ").append(email).append("\n");
                    hasAddedContent = true;
                } else if (currentDocument.getEmail() != null && !currentDocument.getEmail().trim().isEmpty()) {
                    formattedData.append("Email: ").append(currentDocument.getEmail()).append("\n");
                    hasAddedContent = true;
                }
            } else if (currentDocument.getEmail() != null && !currentDocument.getEmail().trim().isEmpty()) {
                formattedData.append("Email: ").append(currentDocument.getEmail()).append("\n");
                hasAddedContent = true;
            }

            if (mobileNumberEditText != null) {
                String phone = mobileNumberEditText.getText().toString().trim();
                if (!phone.isEmpty()) {
                    formattedData.append("Phone: ").append(phone).append("\n");
                    hasAddedContent = true;
                } else if (currentDocument.getPhoneNumbers() != null && !currentDocument.getPhoneNumbers().isEmpty() &&
                        !currentDocument.getPhoneNumbers().get(0).trim().isEmpty()) {
                    formattedData.append("Phone: ").append(currentDocument.getPhoneNumbers().get(0)).append("\n");
                    hasAddedContent = true;
                }
            } else if (currentDocument.getPhoneNumbers() != null && !currentDocument.getPhoneNumbers().isEmpty() &&
                    !currentDocument.getPhoneNumbers().get(0).trim().isEmpty()) {
                formattedData.append("Phone: ").append(currentDocument.getPhoneNumbers().get(0)).append("\n");
                hasAddedContent = true;
            }

            if (websiteEditText != null) {
                String website = websiteEditText.getText().toString().trim();
                if (!website.isEmpty()) {
                    formattedData.append("Website: ").append(website).append("\n");
                    hasAddedContent = true;
                } else if (currentDocument.getWebSite() != null && !currentDocument.getWebSite().trim().isEmpty()) {
                    formattedData.append("Website: ").append(currentDocument.getWebSite()).append("\n");
                    hasAddedContent = true;
                }
            } else if (currentDocument.getWebSite() != null && !currentDocument.getWebSite().trim().isEmpty()) {
                formattedData.append("Website: ").append(currentDocument.getWebSite()).append("\n");
                hasAddedContent = true;
            }

            if (addressEditText != null) {
                String address = addressEditText.getText().toString().trim();
                if (!address.isEmpty()) {
                    formattedData.append("Address: ").append(address).append("\n");
                    hasAddedContent = true;
                } else if (currentDocument.getAddress() != null && !currentDocument.getAddress().trim().isEmpty()) {
                    formattedData.append("Address: ").append(currentDocument.getAddress()).append("\n");
                    hasAddedContent = true;
                }
            } else if (currentDocument.getAddress() != null && !currentDocument.getAddress().trim().isEmpty()) {
                formattedData.append("Address: ").append(currentDocument.getAddress()).append("\n");
                hasAddedContent = true;
            }

            // If we still have no formatted data, check if raw OCR text is available
            if (!hasAddedContent && currentDocument.getOcrText() != null && !currentDocument.getOcrText().trim().isEmpty()) {
                formattedData.append(currentDocument.getOcrText());
                hasAddedContent = true;
            }
        }

        // If no content was added, return a message
        if (!hasAddedContent) {
            formattedData = new StringBuilder("No business card data available");
        }

        // Log the data being returned for debugging
        Log.d(TAG, "Formatted business card data: " + formattedData.toString());

        return new ArrayList<>(List.of(formattedData.toString()));
    }

    private void setOCRText(String recognizedText) {
        if (recognizedText == null || recognizedText.isEmpty()) {
            Log.e(TAG, "OCR text is null or empty");
            return;
        }
        Log.d(TAG, "Processing OCR Text: " + recognizedText);

        // Make sure currentDocument is initialized
        if (currentDocument == null) {
            currentDocument = new ScannedDocument();
        }

        currentDocument.setOcrText(recognizedText);
        String[] lines = recognizedText.split("\n");

        // First pass: extract explicitly labeled information
        for (String line : lines) {
            line = line.trim();
            if (line.isEmpty()) continue;
            Log.d(TAG, "Processing line: " + line);

            if (line.toLowerCase().matches("^name:.*")) {
                String name = line.replaceFirst("(?i)^Name:\\s*", "").trim();
                if (!name.isEmpty()) {
                    currentDocument.setName(name);
                    Log.d(TAG, "Extracted name: " + name);
                }
            } else if (line.toLowerCase().matches("^(title|designation|role):.*")) {
                String jobTitle = line.replaceFirst("(?i)^(Title|Designation|Role):\\s*", "").trim();
                currentDocument.setJobTitle(jobTitle);
                Log.d(TAG, "Extracted job title: " + jobTitle);
            } else if (line.toLowerCase().matches("^(company|organization):.*")) {
                String company = line.replaceFirst("(?i)^(Company|Organization):\\s*", "").trim();
                currentDocument.setCompanyTitle(company);
                Log.d(TAG, "Extracted company: " + company);
            } else if (line.toLowerCase().matches("^address:.*")) {
                String address = line.replaceFirst("(?i)^Address:\\s*", "").trim();
                currentDocument.setAddress(address);
                Log.d(TAG, "Extracted address: " + address);
            } else if (line.toLowerCase().matches("^website:.*")) {
                String website = line.replaceFirst("(?i)^Website:\\s*", "").trim();
                if (!website.isEmpty()) {
                    currentDocument.setWebSite(website);
                    Log.d(TAG, "Extracted website: " + website);
                }
            } else if (line.toLowerCase().matches("^email:.*")) {
                String email = line.replaceFirst("(?i)^Email:\\s*", "").trim();
                if (!email.isEmpty()) {
                    currentDocument.setEmail(email);
                    Log.d(TAG, "Extracted email: " + email);
                }
            } else if (line.toLowerCase().matches("^(phone|tel|mobile|cell):.*")) {
                String phone = line.replaceFirst("(?i)^(Phone|Tel|Mobile|Cell):\\s*", "").trim();
                if (!phone.isEmpty()) {
                    ArrayList<String> phones = new ArrayList<>();
                    phones.add(phone);
                    currentDocument.setPhoneNumbers(phones);
                    Log.d(TAG, "Extracted phone: " + phone);
                }
            }
        }

        // Second pass: use RegexUtils for other fields and unlabeled information
        for (String line : lines) {
            line = line.trim();
            if (line.isEmpty()) continue;

            // Extract email if not already set
            if (currentDocument.getEmail() == null || currentDocument.getEmail().isEmpty()) {
                ArrayList<String> emails = RegexUtils.extractEmails(line);
                if (!emails.isEmpty()) {
                    currentDocument.setEmail(emails.get(0));
                    Log.d(TAG, "Extracted email with regex: " + emails.get(0));
                }
            }

            // Extract phone numbers if not already set
            if (currentDocument.getPhoneNumbers() == null || currentDocument.getPhoneNumbers().isEmpty()) {
                ArrayList<String> phones = RegexUtils.extractPhoneNumbers(line);
                if (!phones.isEmpty()) {
                    currentDocument.setPhoneNumbers(phones);
                    Log.d(TAG, "Extracted phone with regex: " + phones.get(0));
                }
            }

            // Extract website if not already set
            if (currentDocument.getWebSite() == null || currentDocument.getWebSite().isEmpty()) {
                ArrayList<String> websites = RegexUtils.extractWebsites(line);
                if (!websites.isEmpty()) {
                    currentDocument.setWebSite(websites.get(0));
                    Log.d(TAG, "Extracted website with regex: " + websites.get(0));
                }
            }

            // Try to identify name if not already set
            if ((currentDocument.getName() == null || currentDocument.getName().isEmpty()) && RegexUtils.isName(line)) {
                currentDocument.setName(line);
                Log.d(TAG, "Extracted name with regex: " + line);
            }

            // Try to identify job title if not already set
            if ((currentDocument.getJobTitle() == null || currentDocument.getJobTitle().isEmpty()) && RegexUtils.isJobTitle(line)) {
                currentDocument.setJobTitle(line);
                Log.d(TAG, "Extracted job title with regex: " + line);
            }

            // Try to identify address if not already set
            if ((currentDocument.getAddress() == null || currentDocument.getAddress().isEmpty()) && RegexUtils.isAddress(line)) {
                currentDocument.setAddress(line);
                Log.d(TAG, "Extracted address with regex: " + line);
            }
        }

        // After processing, let's explicitly log what we've extracted
        Log.d(TAG, "Extraction complete. Updating UI with extracted data.");

        // Call updateUI with the newly populated currentDocument
        updateUI(currentDocument);
    }

    private void updateUI(ScannedDocument doc) {
        Log.d(TAG, "Updating UI with extracted data:");
        Log.d(TAG, "Name: " + (doc.getName() != null ? doc.getName() : "null"));
        Log.d(TAG, "Job Title: " + (doc.getJobTitle() != null ? doc.getJobTitle() : "null"));
        Log.d(TAG, "Company: " + (doc.getCompanyTitle() != null ? doc.getCompanyTitle() : "null"));
        Log.d(TAG, "Email: " + (doc.getEmail() != null ? doc.getEmail() : "null"));
        Log.d(TAG, "Phone: " + (doc.getPhoneNumbers() != null && !doc.getPhoneNumbers().isEmpty() ?
                doc.getPhoneNumbers().get(0) : "null"));
        Log.d(TAG, "Address: " + (doc.getAddress() != null ? doc.getAddress() : "null"));
        Log.d(TAG, "Website: " + (doc.getWebSite() != null ? doc.getWebSite() : "null"));

        // Use safeSetText for populating EditTexts with extracted information
        safeSetText(bnameEditText, doc.getName());
        safeSetText(jobtitleEditText, doc.getJobTitle());
        safeSetText(companyNameEditText, doc.getCompanyTitle());
        safeSetText(emailEditText, doc.getEmail());
        safeSetText(mobileNumberEditText,
                doc.getPhoneNumbers() != null ? doc.getPhoneNumbers().get(0) : "");
        safeSetText(addressEditText, doc.getAddress());
        safeSetText(websiteEditText, doc.getWebSite());
        ImageUtil.load(this,currentDocument.getFileUrl(),findViewById(R.id.businessCardImage),findViewById(R.id.imgProgress));
        findViewById(R.id.businessCardImage).setOnClickListener(v -> ZoomImageDialog.show(this, currentDocument.getFileUrl()));


        // Make sure the views are visible
        makeAllViewsVisible();

        // Force UI update
        if (bnameEditText != null) bnameEditText.invalidate();
        if (jobtitleEditText != null) jobtitleEditText.invalidate();
        if (companyNameEditText != null) companyNameEditText.invalidate();
        if (emailEditText != null) emailEditText.invalidate();
        if (mobileNumberEditText != null) mobileNumberEditText.invalidate();
        if (addressEditText != null) addressEditText.invalidate();
        if (websiteEditText != null) websiteEditText.invalidate();
    }

    // Helper method to safely set text on EditText views
    private void safeSetText(EditText editText, String text) {
        if (editText != null) {
            String textToSet = (text != null) ? text : "";
            editText.setText(textToSet);
            Log.d(TAG, "Setting EditText " + editText.getId() + " to: " + textToSet);
        } else {
            Log.e(TAG, "EditText is null when trying to set text: " + text);
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
    public void onBackPressed() {
        super.onBackPressed();
    }

    @Override
    public void onExportPNGClick() {
        // Get formatted business card data
        ArrayList<String> extractedTexts = getFormattedBusinessCardData();

        // Use DocumentUtil to generate and save image
        Uri imageUri = DocumentUtil.generateImage(extractedTexts, this);

        if (imageUri != null) {
            Toast.makeText(this, "Business card exported as image", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this, "Failed to export business card as image", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onExportPDFClick() {
        // Get formatted business card data
        ArrayList<String> extractedTexts = getFormattedBusinessCardData();

        // Use DocumentUtil to generate and save PDF
        Uri pdfUri = DocumentUtil.generatePDF(extractedTexts, this);

        if (pdfUri != null) {
            Toast.makeText(this, "Business card exported as PDF", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this, "Failed to export business card as PDF", Toast.LENGTH_SHORT).show();
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

        LoaderHelper.showLoader(this, true);

        RestApiService apiService = RestApiBuilder.getService();
        Call<GetDocumentById> call = apiService.getDocumentById(documentId);

        call.enqueue(new Callback<GetDocumentById>() {
            @Override
            public void onResponse(@NonNull Call<GetDocumentById> call, @NonNull Response<GetDocumentById> response) {
                LoaderHelper.hideLoader();

                if (response.isSuccessful()) {
                    GetDocumentById document = response.body();
                    if (document != null) {

                        currentDocument = new ScannedDocument();
                        currentDocument.setName(document.getDocumentName());
                        currentDocument.setEmail(document.getEmail());
                        currentDocument.setOcrText(document.getName());
                        currentDocument.setAddress(document.getAddress());
                        currentDocument.setCompanyTitle(document.getCompanyName());
                        // Process the retrieved document
                        Log.d(TAG, "Document retrieved successfully: " + document.toString());

                        // Example: Display success message
                        Toast.makeText(BusinessDetailsActivity.this,
                                getString(R.string.document_retrieved_successfully),
                                Toast.LENGTH_SHORT).show();

                        // For phone number, you need to create an ArrayList
                        if (document.getMobileNumber() != null) {
                            ArrayList<String> phoneNumbers = new ArrayList<>();
                            phoneNumbers.add(document.getMobileNumber());
                            currentDocument.setPhoneNumbers(phoneNumbers);
                        }
                        currentDocument.setJobTitle(document.getProfession());
                        // Log the retrieved data
                        Log.d(TAG, "Retrieved document data: " + new Gson().toJson(currentDocument));
                        currentDocument.setJobTitle(document.getProfession());
                        currentDocument.setWebSite(document.getWebSite());
                        currentDocument.setFileUrl(document.getFileUrl());
                        updateUI(currentDocument);
                    } else {
                        Toast.makeText(BusinessDetailsActivity.this,
                                getString(R.string.error_empty_document),
                                Toast.LENGTH_SHORT).show();
                    }
                } else {
                    try {
                        if (response.errorBody() != null) {
                            Gson gson = new Gson();
                            ErrorResponse errorResponse = gson.fromJson(response.errorBody().charStream(), ErrorResponse.class);
                            String errorMessage = errorResponse != null && errorResponse.getDetail() != null
                                    ? errorResponse.getDetail()
                                    : getString(R.string.default_error_message);

                            Toast.makeText(BusinessDetailsActivity.this, errorMessage, Toast.LENGTH_LONG).show();
                        }
                    } catch (Exception e) {
                        Toast.makeText(BusinessDetailsActivity.this, getString(R.string.default_error_message), Toast.LENGTH_LONG).show();
                    }
                }
            }

            @Override
            public void onFailure(@NonNull Call<GetDocumentById> call, @NonNull Throwable t) {
                LoaderHelper.hideLoader();
                Log.e(TAG, "API call failed: " + t.getMessage());
                Toast.makeText(BusinessDetailsActivity.this, getString(R.string.network_error), Toast.LENGTH_LONG).show();
            }
        });
    }
}
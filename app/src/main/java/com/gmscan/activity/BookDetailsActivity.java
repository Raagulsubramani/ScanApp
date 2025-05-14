package com.gmscan.activity;

import static com.gmscan.utility.CustomBottomSheetDialog.VIEW_EXPORT;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.gmscan.R;
import com.gmscan.model.documentUpdate.DocumentUpdateRequest;
import com.gmscan.model.getAllDocuments.ScannedDocument;
import com.gmscan.model.getDocumentById.GetDocumentById;
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

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class BookDetailsActivity extends AppCompatActivity implements CustomBottomSheetDialog.OnViewExportButtonClickListener {
    private static final String TAG = "BookDetailsActivity";

    // UI Elements
    private ImageView imgBack;
    private Button btnSave;
    private ImageView bookCoverImageView;
    private MaterialButton addFieldButton;
    private ExecutorService executor;
    private String bookId;
    private ScannedDocument currentBook;

    // EditText fields
    private EditText isbnEditText;
    private EditText bookTitleEditText;
    private EditText authorEditText;
    private EditText publishDateEditText;
    private EditText pagesEditText;
    private EditText subjectEditText;

    // Edit buttons
    private ImageButton editIsbnButton;
    private ImageButton editBookNameButton;
    private ImageButton editAuthorButton;
    private ImageButton editPublishButton;
    private ImageButton editPagesButton;
    private ImageButton editSubjectButton;
    private MaterialButton exportButton;
    private ScannedDocument currentDocument;
    private RestApiService apiService;
    ProgressBar progressBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.book_details);
        initializeViews();
        setupClickListeners();
        // Get OCR text from intent (if applicable)
        String recognizedText = getIntent().getStringExtra(IntentKeys.OCR_TEXT);
        Log.d(TAG, "Received OCR Text: " + recognizedText);

        if (recognizedText != null) {
            setOCRText(recognizedText);
        } else {
            Log.e(TAG, "No OCR text received");
            setEmptyState();
        }
    }

    @SuppressLint("SetTextI18n")
    private void initializeViews() {
        // Top section
        imgBack = findViewById(R.id.imgBack);
        TextView txtTitle = findViewById(R.id.txtTitle);
        btnSave = findViewById(R.id.btnSave);
        exportButton = findViewById(R.id.exportButton);
        progressBar = findViewById(R.id.progressBar); // Replace with your ProgressBar ID

        // Book cover image
        bookCoverImageView = findViewById(R.id.bookCoverImageView);

        bookCoverImageView.setOnClickListener(v -> ZoomImageDialog.show(this, currentDocument.getFileUrl()));

        // Details section
        addFieldButton = findViewById(R.id.addFieldButton);

        // Form fields - EditTexts
        isbnEditText = findViewById(R.id.isbnEditText);
        bookTitleEditText = findViewById(R.id.bookTitleEditText);
        authorEditText = findViewById(R.id.authorEditText);
        publishDateEditText = findViewById(R.id.publishDateEditText);
        pagesEditText = findViewById(R.id.pagesEditText);
        subjectEditText = findViewById(R.id.subjectEditText);

        // Edit buttons
        editIsbnButton = findViewById(R.id.editName);
        editBookNameButton = findViewById(R.id.editbookname);
        editAuthorButton = findViewById(R.id.editauthor);
        editPublishButton = findViewById(R.id.editpublish);
        editPagesButton = findViewById(R.id.editpages);
        editSubjectButton = findViewById(R.id.editsubject);
        String documentID = getIntent().getStringExtra(IntentKeys.DOCUMENT_ID);

        // Export button
        btnSave = findViewById(R.id.btnSave);
        exportButton = findViewById(R.id.exportButton);

        String documentType = StringUtils.capitalizeFirstLetter(getIntent().getStringExtra(IntentKeys.DOCUMENT_TYPE));
        txtTitle.setText(documentType + " " + getString(R.string.details));

        apiService = RestApiBuilder.getService();
        // Get book ID from intent
        bookId = getIntent().getStringExtra(IntentKeys.DOCUMENT_ID);
        executor = Executors.newSingleThreadExecutor();

        getDocumentById(String.valueOf(documentID));
    }

    private void updateDocument() {
        if (currentBook == null) {
            Toast.makeText(this, "No document to update", Toast.LENGTH_SHORT).show();
            return;
        }

        // Get the updated values from EditText fields
        String title = bookTitleEditText.getText().toString().trim();
        String isbn = isbnEditText.getText().toString().trim();
        String author = authorEditText.getText().toString().trim();
        String publishDate = publishDateEditText.getText().toString().trim();
        String pages = pagesEditText.getText().toString().trim();
        String subject = subjectEditText.getText().toString().trim();

        // Update local database first
        executor.execute(() -> {
            try {
                // Update the current book object with new values
                currentBook.setTitle(title);
                currentBook.setIsbn(isbn);
                currentBook.setAuthor(author);
                currentBook.setPublishDate(publishDate);
                currentBook.setPages(pages);
                currentBook.setSubject(subject);

                // Create API request object
                DocumentUpdateRequest updateRequest = new DocumentUpdateRequest();

                // These lines have errors - use proper setter methods based on DocumentUpdateRequest class
                // updateRequest.setTitle(title);

                // Create a description from the other fields
                StringBuilder description = new StringBuilder();
                if (!isbn.isEmpty()) description.append("ISBN: ").append(isbn).append("\n");
                if (!author.isEmpty()) description.append("Author: ").append(author).append("\n");
                if (!publishDate.isEmpty())
                    description.append("Publish Date: ").append(publishDate).append("\n");
                if (!pages.isEmpty()) description.append("Pages: ").append(pages).append("\n");
                if (!subject.isEmpty()) description.append("Subject: ").append(subject);

                // Use proper setter methods or field assignment based on your DocumentUpdateRequest implementation
                updateRequest.setBookName(title);
                updateRequest.setDocumentName(title);
                updateRequest.setTitle(title);
                updateRequest.setDescription(description.toString());
                updateRequest.setIsbnNo(Long.parseLong(isbn));
                updateRequest.setAuthorName(author);
                updateRequest.setPublication(publishDate);
                updateRequest.setNumberOfPages(Integer.parseInt(pages));
                updateRequest.setSubject(subject);
                // Make API call after local update
                String documentIdStr = String.valueOf(bookId);
                runOnUiThread(() -> makeApiUpdateCall(documentIdStr, updateRequest));
            } catch (Exception e) {
                runOnUiThread(() -> {
                    Toast.makeText(BookDetailsActivity.this,
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

        RequestBody isbnBody = RequestBody.create(okhttp3.MediaType.parse("text/plain"), "" + updateRequest.getIsbnNo());
        RequestBody documentNameBody = RequestBody.create(
                okhttp3.MediaType.parse("text/plain"),
                updateRequest.getTitle() != null ? updateRequest.getTitle() : "");

        RequestBody authorNameBody = RequestBody.create(
                okhttp3.MediaType.parse("text/plain"),
                updateRequest.getAuthorName() != null ? updateRequest.getAuthorName() : "");

        RequestBody scanTypeBody = RequestBody.create(
                okhttp3.MediaType.parse("text/plain"), DocumentType.BOOK.getDisplayName());

        // Use current file URL or empty if not available
        RequestBody fileUrlBody = RequestBody.create(
                okhttp3.MediaType.parse("text/plain"),
                currentDocument != null && currentDocument.getFileUrl() != null ?
                        currentDocument.getFileUrl() : "");

        // Create numeric fields with valid values since the error is related to number_of_pages
        RequestBody numberOfPagesBody = RequestBody.create(
                okhttp3.MediaType.parse("text/plain"), currentDocument.getPages());  // Use "0" instead of empty string
        // Create numeric fields with valid values since the error is related to number_of_pages
        RequestBody publicationBody = RequestBody.create(
                okhttp3.MediaType.parse("text/plain"), currentDocument.getPublishDate());  // Use "0" instead of empty string

        RequestBody subjectBody = RequestBody.create(
                okhttp3.MediaType.parse("text/plain"), currentDocument.getSubject());  // Use "0" instead of empty string


        // Log the request parameters
        Log.d(TAG, "Multipart request parameters:");
        Log.d(TAG, "- document_name: " + updateRequest.getTitle());
        Log.d(TAG, "- name: " + updateRequest.getName());
        Log.d(TAG, "- profession: " + updateRequest.getProfession());
        Log.d(TAG, "- mobile_number: " + updateRequest.getMobileNumber());

        // Make the API call using multipart
        apiService.updateDocumentWithFormData(
                documentId,
                documentNameBody,
                documentNameBody,
                scanTypeBody,
                fileUrlBody,
                isbnBody,
                authorNameBody,
                publicationBody,
                numberOfPagesBody,
                subjectBody
        ).enqueue(new Callback<ResponseBody>() {
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

                    Toast.makeText(BookDetailsActivity.this,
                            "Book is updated successfully",
                            Toast.LENGTH_SHORT).show();

                    Intent resultIntent = new Intent();
                    setResult(RESULT_OK, resultIntent);
                    finish();
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

                Toast.makeText(BookDetailsActivity.this,
                        "Error: " + t.getMessage(),
                        Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void handleErrorResponse(Response<?> response) {
        try {
            Log.e(TAG, "API update failed. HTTP status: " + response.code());

            if (response.errorBody() != null) {
                String errorBodyString = response.errorBody().string();
                Log.e(TAG, "Error response body: " + errorBodyString);

                // First try to extract a user-friendly message from the error body
                String userFriendlyMessage = extractErrorMessage(errorBodyString);

                Toast.makeText(BookDetailsActivity.this,
                        userFriendlyMessage,
                        Toast.LENGTH_LONG).show();
            } else {
                Log.e(TAG, "Error updating document. Empty error body. HTTP status: " + response.code());
                Toast.makeText(BookDetailsActivity.this,
                        "Failed to update business card (HTTP " + response.code() + ")",
                        Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            Log.e(TAG, "Error parsing error response", e);
            Toast.makeText(BookDetailsActivity.this,
                    getString(R.string.default_error_message),
                    Toast.LENGTH_LONG).show();
        }
    }

    private String extractErrorMessage(String errorBodyString) {
        // First check if the response is a plain text error message (not JSON)
        if (!errorBodyString.startsWith("{") && !errorBodyString.startsWith("[")) {
            // If it's not JSON, just return the error message as is
            return errorBodyString;
        }

        try {
            // Try to parse as JSON if it looks like JSON
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
            // If JSON parsing fails, return original error message if it's not empty
            if (errorBodyString != null && !errorBodyString.isEmpty()) {
                return errorBodyString;
            }
            return getString(R.string.default_error_message);
        }
    }

    private void setupClickListeners() {
        // Back button
        if (imgBack != null) {
            imgBack.setOnClickListener(v -> finish());
        }

        // Book cover click - Show zoomable dialog
        if (bookCoverImageView != null) {
            bookCoverImageView.setOnClickListener(v -> ZoomImageDialog.show(this, currentDocument.getFileUrl()));
        }

        // Add field button
        if (addFieldButton != null) {
            addFieldButton.setOnClickListener(v -> {
                // Handle adding new custom field
                Toast.makeText(this, "Add new custom field feature", Toast.LENGTH_SHORT).show();
            });
        }

        // Edit buttons
        setupEditButtonListeners();

        // Export button
        if (exportButton != null) {
            exportButton.setOnClickListener(v -> showBottomSheet());
        }

        // Save button - Now triggers both the update and the popup menu
        if (btnSave != null) {
            btnSave.setOnClickListener(v -> {
                // First update the document
                updateDocument();
            });
        }
    }

    private void showBottomSheet() {
        CustomBottomSheetDialog bottomSheet = CustomBottomSheetDialog.newInstance(VIEW_EXPORT);
        bottomSheet.setOnExportButtonClickListener(this);
        bottomSheet.show(getSupportFragmentManager(), "ExportBottomSheet");
    }

    /**
     * Loads book data from the database and updates the UI.
     */
    private void loadBookData() {
        executor.execute(() -> {
            try {
                // Also set currentDocument to currentBook for consistency
                currentBook = currentDocument;

                runOnUiThread(() -> {
                    if (currentBook != null) {
                        // Update UI with the book data
                        updateUI(currentBook);

                        // Load book cover image if available
                        if (currentBook.getFileUrl() != null && !currentBook.getFileUrl().isEmpty()) {
                            // Pass the ProgressBar if it exists, otherwise pass null
                            ImageUtil.load(this, currentBook.getFileUrl(), bookCoverImageView, progressBar);
                        } else {
                            bookCoverImageView.setImageResource(R.drawable.ic_placeholderbook);
                        }
                    } else {
                        Toast.makeText(this, "Book not found", Toast.LENGTH_SHORT).show();
                        finish();
                    }
                });
            } catch (Exception e) {
                runOnUiThread(() -> {
                    Toast.makeText(this, "Error loading book: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    finish();
                });
            }
        });
    }

    private void setupEditButtonListeners() {
        // ISBN edit button
        if (editIsbnButton != null) {
            editIsbnButton.setOnClickListener(v -> {
                isbnEditText.requestFocus();
                isbnEditText.setSelection(isbnEditText.getText().length());
            });
        }

        // Book name edit button
        if (editBookNameButton != null) {
            editBookNameButton.setOnClickListener(v -> {
                bookTitleEditText.requestFocus();
                bookTitleEditText.setSelection(bookTitleEditText.getText().length());
            });
        }

        // Author edit button
        if (editAuthorButton != null) {
            editAuthorButton.setOnClickListener(v -> {
                authorEditText.requestFocus();
                authorEditText.setSelection(authorEditText.getText().length());
            });
        }

        // Publish date edit button
        if (editPublishButton != null) {
            editPublishButton.setOnClickListener(v -> {
                publishDateEditText.requestFocus();
                publishDateEditText.setSelection(publishDateEditText.getText().length());
            });
        }

        // Pages edit button
        if (editPagesButton != null) {
            editPagesButton.setOnClickListener(v -> {
                pagesEditText.requestFocus();
                pagesEditText.setSelection(pagesEditText.getText().length());
            });
        }

        // Subject edit button
        if (editSubjectButton != null) {
            editSubjectButton.setOnClickListener(v -> {
                subjectEditText.requestFocus();
                subjectEditText.setSelection(subjectEditText.getText().length());
            });
        }
    }

    private void setEmptyState() {
        // Initialize with empty state
        if (currentDocument == null) {
            currentDocument = new ScannedDocument();
        }
        updateUI(currentDocument);
    }

    private ArrayList<String> getFormattedBookData() {
        StringBuilder formattedData = new StringBuilder();

        // Get data from edit text fields
        String isbn = isbnEditText.getText().toString().trim();
        if (!isbn.isEmpty()) {
            formattedData.append("ISBN: ").append(isbn).append("\n");
        }

        String title = bookTitleEditText.getText().toString().trim();
        if (!title.isEmpty()) {
            formattedData.append("Title: ").append(title).append("\n");
        }

        String author = authorEditText.getText().toString().trim();
        if (!author.isEmpty()) {
            formattedData.append("Author: ").append(author).append("\n");
        }

        String publishDate = publishDateEditText.getText().toString().trim();
        if (!publishDate.isEmpty()) {
            formattedData.append("Publish Date: ").append(publishDate).append("\n");
        }

        String pages = pagesEditText.getText().toString().trim();
        if (!pages.isEmpty()) {
            formattedData.append("Pages: ").append(pages).append("\n");
        }

        String subject = subjectEditText.getText().toString().trim();
        if (!subject.isEmpty()) {
            formattedData.append("Subject: ").append(subject).append("\n");
        }

        // If no data was entered, use the OCR text if available
        if (formattedData.length() == 0 && currentDocument != null && currentDocument.getOcrText() != null) {
            formattedData.append(currentDocument.getOcrText());
        } else if (formattedData.length() == 0) {
            formattedData.append("No book data available");
        }

        return new ArrayList<>(List.of(formattedData.toString()));
    }

    private void setOCRText(String recognizedText) {
        Log.d(TAG, "Processing OCR Text: " + recognizedText);

        currentDocument = new ScannedDocument();
        currentDocument.setOcrText(recognizedText);

        // Extract book details directly without regex
        // Each field will be searched and extracted separately
        extractAndSetBookDetails(recognizedText);

        updateUI(currentDocument);
    }

    private void extractAndSetBookDetails(String text) {
        // Split the text into lines for processing
        String[] lines = text.split("\n");

        for (String line : lines) {
            line = line.trim();
            if (line.isEmpty()) continue;

            // Direct string contains checking instead of regex
            if (line.toLowerCase().contains("isbn:") || line.toLowerCase().contains("isbn no:")) {
                String isbn = extractValueAfterLabel(line, "ISBN:");
                if (isbn.isEmpty()) {
                    isbn = extractValueAfterLabel(line, "ISBN No:");
                }
                currentDocument.setIsbn(isbn);
            } else if (line.toLowerCase().contains("book name:") || line.toLowerCase().contains("title:")) {
                String title = extractValueAfterLabel(line, "Book Name:");
                if (title.isEmpty()) {
                    title = extractValueAfterLabel(line, "Title:");
                }
                currentDocument.setTitle(title);
            } else if (line.toLowerCase().contains("author:") || line.toLowerCase().contains("author name:")) {
                String author = extractValueAfterLabel(line, "Author:");
                if (author.isEmpty()) {
                    author = extractValueAfterLabel(line, "Author Name:");
                }
                currentDocument.setAuthor(author);
            } else if (line.toLowerCase().contains("publish date:") ||
                    line.toLowerCase().contains("publish on:") ||
                    line.toLowerCase().contains("published:") ||
                    line.toLowerCase().contains("publication date:") ||
                    line.toLowerCase().contains("year:") ||
                    line.toLowerCase().contains("publish year:")) {
                String publishDate = extractValueAfterLabel(line, "Publish Date:");
                if (publishDate.isEmpty()) {
                    publishDate = extractValueAfterLabel(line, "Publish on:");
                }
                if (publishDate.isEmpty()) {
                    publishDate = extractValueAfterLabel(line, "Published:");
                }
                if (publishDate.isEmpty()) {
                    publishDate = extractValueAfterLabel(line, "Publication Date:");
                }
                if (publishDate.isEmpty()) {
                    publishDate = extractValueAfterLabel(line, "Year:");
                }
                if (publishDate.isEmpty()) {
                    publishDate = extractValueAfterLabel(line, "Publish Year:");
                }
                currentDocument.setPublishDate(publishDate);
            } else if (line.toLowerCase().contains("pages:") ||
                    line.toLowerCase().contains("number of pages:") ||
                    line.toLowerCase().contains("number of pages:")) {
                String pages = extractValueAfterLabel(line, "Pages:");
                if (pages.isEmpty()) {
                    pages = extractValueAfterLabel(line, "Number of Pages:");
                }
                if (pages.isEmpty()) {
                    pages = extractValueAfterLabel(line, "Number Of Pages:");
                }
                currentDocument.setPages(pages);
            } else if (line.toLowerCase().contains("subject:")) {
                String subject = extractValueAfterLabel(line, "Subject:");
                currentDocument.setSubject(subject);
            }
        }

        Log.d(TAG, "Extracted Book Details:");
        Log.d(TAG, "ISBN: " + currentDocument.getIsbn());
        Log.d(TAG, "Title: " + currentDocument.getTitle());
        Log.d(TAG, "Author: " + currentDocument.getAuthor());
        Log.d(TAG, "Publish Date: " + currentDocument.getPublishDate());
        Log.d(TAG, "Pages: " + currentDocument.getPages());
        Log.d(TAG, "Subject: " + currentDocument.getSubject());
    }

    private String extractValueAfterLabel(String line, String label) {
        int indexOfLabel = line.toLowerCase().indexOf(label.toLowerCase());
        if (indexOfLabel != -1) {
            int startIndex = indexOfLabel + label.length();
            return line.substring(startIndex).trim();
        }
        return "";
    }

    private void updateUI(ScannedDocument doc) {
        // Update EditText fields
        if (isbnEditText != null) {
            isbnEditText.setText(doc.getIsbn() != null ? doc.getIsbn() : "");
        }

        if (bookTitleEditText != null) {
            bookTitleEditText.setText(doc.getTitle() != null ? doc.getTitle() : "");
        }

        if (authorEditText != null) {
            authorEditText.setText(doc.getAuthor() != null ? doc.getAuthor() : "");
        }

        if (publishDateEditText != null) {
            publishDateEditText.setText(doc.getPublishDate() != null ? doc.getPublishDate() : "");
        }

        if (pagesEditText != null) {
            pagesEditText.setText(doc.getPages() != null ? doc.getPages() : "");
        }

        if (subjectEditText != null) {
            subjectEditText.setText(doc.getSubject() != null ? doc.getSubject() : "");
        }

        // Load cover image if available
        if (doc.getFileUrl() != null && !doc.getFileUrl().isEmpty() && bookCoverImageView != null) {
            ImageUtil.load(this, doc.getFileUrl(), bookCoverImageView, progressBar);
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
        // Get formatted Book data
        ArrayList<String> extractedTexts = getFormattedBookData();

        // Use DocumentUtil to generate and save image
        Uri imageUri = DocumentUtil.generateImage(extractedTexts, this);

        if (imageUri != null) {
            Toast.makeText(this, "Book details exported as image", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this, "Failed to export Book details as image", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onExportPDFClick() {
        // Get formatted Book data
        ArrayList<String> extractedTexts = getFormattedBookData();

        // Use DocumentUtil to generate and save PDF
        Uri pdfUri = DocumentUtil.generatePDF(extractedTexts, this);

        if (pdfUri != null) {
            Toast.makeText(this, "Book details exported as PDF", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this, "Failed to export Book details as PDF", Toast.LENGTH_SHORT).show();
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

        RestApiService apiService = RestApiBuilder.getService();
        Call<GetDocumentById> call = apiService.getDocumentById(documentId);

        call.enqueue(new Callback<>() {
            @Override
            public void onResponse(@NonNull Call<GetDocumentById> call, @NonNull Response<GetDocumentById> response) {
                if (response.isSuccessful()) {
                    GetDocumentById document = response.body();
                    if (document != null) {
                        // Process the retrieved document
                        // For example, you might display it or pass it to another activity/fragment
                        Log.d(TAG, "Document retrieved successfully: " + document);

                        // Convert the API document to ScannedDocument for display
                        if (currentDocument == null) {
                            currentDocument = new ScannedDocument();
                        }

                        // Map API response fields to ScannedDocument fields
                        currentDocument.setOcrText(document.getContent());
                        currentDocument.setName(document.getDocumentName());
                        currentDocument.setOcrText(document.getOcrText());
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

                        currentDocument.setTitle(document.getDocumentName());
                        currentDocument.setIsbn(document.getIsbnNo());
                        currentDocument.setAuthor(document.getAuthorName());
                        currentDocument.setPublishDate(document.getPublication());
                        currentDocument.setPages(document.getNumberOfPages());
                        currentDocument.setSubject(document.getSubject());

                        // Example: Display a success message
                        Toast.makeText(BookDetailsActivity.this,
                                getString(R.string.document_retrieved_successfully),
                                Toast.LENGTH_SHORT).show();
                        loadBookData();
                    } else {
                        Log.e(TAG, "Retrieved document is null");
                        Toast.makeText(BookDetailsActivity.this,
                                getString(R.string.error_empty_document),
                                Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Log.e(TAG, "Error retrieving document: " + response.code());
                    Toast.makeText(BookDetailsActivity.this,
                            getString(R.string.error_retrieving_document),
                            Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(@NonNull Call<GetDocumentById> call, @NonNull Throwable t) {
                Log.e(TAG, "API call failed: " + t.getMessage());
                Toast.makeText(BookDetailsActivity.this,
                        getString(R.string.api_call_failed),
                        Toast.LENGTH_SHORT).show();
            }
        });
    }
}
package com.gmscan.activity;

import static android.view.View.GONE;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SearchView;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.gmscan.R;
import com.gmscan.adapter.ScanHistoryAdapter;
import com.gmscan.model.BaseResponse;
import com.gmscan.model.ErrorResponse;
import com.gmscan.model.UpdateDocumentResponse;
import com.gmscan.model.getAllDocuments.GetAllDocumentsResponseItem;
import com.gmscan.service.RestApiBuilder;
import com.gmscan.service.RestApiService;
import com.gmscan.utility.DocumentType;
import com.gmscan.utility.DocumentUtil;
import com.gmscan.utility.IntentKeys;
import com.gmscan.utility.NetworkUtils;
import com.gmscan.utility.LoaderHelper;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.button.MaterialButton;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import okhttp3.MediaType;
import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ScanHistoryActivity extends AppCompatActivity {
    private RecyclerView scanHistoryRecyclerView;
    private View includeNoHistory;
    private ScanHistoryAdapter adapter;
    private SearchView searchView;
    private final List<GetAllDocumentsResponseItem> originalList = new ArrayList<>();
    private RestApiService apiService;
    SwipeRefreshLayout swipeRefreshLayout;
    private ActivityResultLauncher<Intent> detailLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.layout_history);
        TextView txtTitle = findViewById(R.id.txtTitle);
        ImageView imgBack = findViewById(R.id.imgBack);
        txtTitle.setText(getString(R.string.scan_history));
        imgBack.setOnClickListener(v -> finish());
        scanHistoryRecyclerView = findViewById(R.id.scanHistoryRecyclerView);
        searchView = findViewById(R.id.searchView);
        includeNoHistory = findViewById(R.id.includeNoHistory);
        swipeRefreshLayout = findViewById(R.id.swipeRefreshLayout);
        View includeAppBar = findViewById(R.id.includeAppBar);
        View txtSave = includeAppBar.findViewById(R.id.btnSave);
        scanHistoryRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        txtSave.setVisibility(GONE);

        // Initialize API service
        apiService = RestApiBuilder.getService();

        initializeScanHistoryView();
        setupSearchView();

        swipeRefreshLayout.setOnRefreshListener(() -> {
            setupSearchView();
            initializeScanHistoryView();
            swipeRefreshLayout.setRefreshing(false);
        });


        // Register result launcher
        detailLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                        Toast.makeText(this, "Returned: ", Toast.LENGTH_SHORT).show();
                    }
                }
        );
    }

    /**
     * Initializes the scan history view and displays scan history if available.
     */
    private void initializeScanHistoryView() {
        if (!NetworkUtils.isInternetAvailable(this)) {
            Toast.makeText(this, getString(R.string.no_internet), Toast.LENGTH_SHORT).show();
            return;
        }

        LoaderHelper.showLoader(this, true);

        apiService.getAllDocuments().enqueue(new Callback<>() {
            @Override
            public void onResponse(@NonNull Call<List<GetAllDocumentsResponseItem>> call,
                                   @NonNull Response<List<GetAllDocumentsResponseItem>> response) {
                LoaderHelper.hideLoader();

                Log.d("API_CALL", "Calling getAllDocuments");

                if (response.isSuccessful() && response.body() != null) {
                    List<GetAllDocumentsResponseItem> documents = response.body();
                    originalList.clear();
                    originalList.addAll(documents);

                    if (documents.isEmpty()) {
                        includeNoHistory.setVisibility(View.VISIBLE);
                        scanHistoryRecyclerView.setVisibility(GONE);
                    } else {
                        includeNoHistory.setVisibility(GONE);
                        scanHistoryRecyclerView.setVisibility(View.VISIBLE);
                        adapter = new ScanHistoryAdapter(ScanHistoryActivity.this, documents, ScanHistoryActivity.this::launchDetailActivity);
                        adapter.setOnFavoriteClickListener(ScanHistoryActivity.this::toggleFavoriteStatus);
                        adapter.setOnDeleteClickListener(ScanHistoryActivity.this::showDeleteConfirmationDialog);
                        adapter.setOnSaveClickListener(ScanHistoryActivity.this::showSaveBottomSheet);
                        adapter.setOnRenameClickListener(ScanHistoryActivity.this::renameBottomSheet);
                        scanHistoryRecyclerView.setAdapter(adapter);
                    }
                } else {
                    try {
                        if (response.errorBody() != null) {
                            Gson gson = new Gson();
                            ErrorResponse errorResponse = gson.fromJson(response.errorBody().charStream(),
                                    ErrorResponse.class);
                            String errorMessage = errorResponse != null && errorResponse.getDetail() != null
                                    ? errorResponse.getDetail()
                                    : getString(R.string.default_error_message);

                            Toast.makeText(ScanHistoryActivity.this, errorMessage, Toast.LENGTH_LONG).show();
                        }
                    } catch (Exception e) {
                        Toast.makeText(ScanHistoryActivity.this,
                                getString(R.string.default_error_message),
                                Toast.LENGTH_LONG).show();
                    }
                }
            }

            @Override
            public void onFailure(@NonNull Call<List<GetAllDocumentsResponseItem>> call,
                                  @NonNull Throwable t) {
                LoaderHelper.hideLoader();
                Toast.makeText(ScanHistoryActivity.this,
                        getString(R.string.network_error),
                        Toast.LENGTH_LONG).show();
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

    /**
     * Sets up search view with appropriate listeners
     */
    private void setupSearchView() {
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                searchDocumentsFromApi(query);
                return true;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                // Only perform search when user submits or when the query is empty (to reset)
                if (newText.isEmpty()) {
                    initializeScanHistoryView();
                }
                return false;
            }
        });
    }

    /**
     * Searches documents using the API instead of local filtering
     *
     * @param query The search query text
     */
    private void searchDocumentsFromApi(String query) {
        if (query == null || query.trim().isEmpty()) {
            Toast.makeText(this, "Search query cannot be empty", Toast.LENGTH_SHORT).show();
            initializeScanHistoryView();
            return;
        }

        LoaderHelper.showLoader(this, true);
        Log.d("API_CALL", "Calling searchDocuments with query: " + query);

        apiService.searchDocuments(query).enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(@NonNull Call<ResponseBody> call,
                                   @NonNull Response<ResponseBody> response) {
                LoaderHelper.hideLoader();

                if (response.isSuccessful() && response.body() != null) {
                    try {
                        String responseBody = response.body().string(); // Consume the response
                        Log.d("API_RESPONSE", "Raw response: " + responseBody);

                        // Parse it as a List of the document model
                        Gson gson = new Gson();
                        Type listType = new TypeToken<List<GetAllDocumentsResponseItem>>() {
                        }.getType();
                        List<GetAllDocumentsResponseItem> searchResults = gson.fromJson(responseBody, listType);

                        if (searchResults.isEmpty()) {
                            includeNoHistory.setVisibility(View.VISIBLE);
                            scanHistoryRecyclerView.setVisibility(GONE);
                            Toast.makeText(ScanHistoryActivity.this, "No results found", Toast.LENGTH_SHORT).show();
                        } else {
                            includeNoHistory.setVisibility(GONE);
                            scanHistoryRecyclerView.setVisibility(View.VISIBLE);

                            adapter = new ScanHistoryAdapter(ScanHistoryActivity.this, searchResults,ScanHistoryActivity.this::launchDetailActivity);
                            adapter.setOnDeleteClickListener(ScanHistoryActivity.this::showDeleteConfirmationDialog);
                            adapter.setOnFavoriteClickListener(ScanHistoryActivity.this::toggleFavoriteStatus);
                            adapter.setOnSaveClickListener(ScanHistoryActivity.this::showSaveBottomSheet);
                            adapter.setOnRenameClickListener(ScanHistoryActivity.this::renameBottomSheet);
                            scanHistoryRecyclerView.setAdapter(adapter);
                        }
                    } catch (Exception e) {
                        Log.e("API_PARSE_ERROR", "Failed to parse search response", e);
                        Toast.makeText(ScanHistoryActivity.this, "Failed to load search results", Toast.LENGTH_SHORT).show();
                    }
                } else {
                    handleErrorResponse(response);
                }
            }

            @Override
            public void onFailure(@NonNull Call<ResponseBody> call,
                                  @NonNull Throwable t) {
                handleApiFailure(t);
            }
        });
    }

    /**
     * Handles error responses from the API
     *
     * @param response The error response from the API
     */
    private void handleErrorResponse(Response<?> response) {
        try {
            if (response.errorBody() != null) {
                String errorBodyString = response.errorBody().string();
                Log.e("API_ERROR", "Error body: " + errorBodyString);

                Gson gson = new Gson();
                ErrorResponse errorResponse = gson.fromJson(errorBodyString, ErrorResponse.class);
                String errorMessage = errorResponse != null && errorResponse.getDetail() != null
                        ? errorResponse.getDetail()
                        : getString(R.string.default_error_message);

                Toast.makeText(ScanHistoryActivity.this, errorMessage, Toast.LENGTH_LONG).show();
            } else {
                Toast.makeText(ScanHistoryActivity.this,
                        getString(R.string.default_error_message),
                        Toast.LENGTH_LONG).show();
            }
        } catch (IOException e) {
            Log.e("API_ERROR", "Error parsing error response", e);
            Toast.makeText(ScanHistoryActivity.this,
                    getString(R.string.default_error_message),
                    Toast.LENGTH_LONG).show();
        }
    }

    /**
     * Handles API call failures
     *
     * @param t The throwable that caused the failure
     */
    private void handleApiFailure(Throwable t) {
        LoaderHelper.hideLoader();
        Log.e("API_FAILURE", "API call failed", t);
        Toast.makeText(ScanHistoryActivity.this,
                getString(R.string.network_error),
                Toast.LENGTH_LONG).show();
    }

    /**
     * Toggles the favorite status of a document and updates it through the API
     *
     * @param documentId The ID of the document to update
     * @param isFavorite The new favorite status to set
     */
    public void toggleFavoriteStatus(String documentId, boolean isFavorite) {
        Log.d("FavoriteUpdate", "Starting toggle favorite status. DocumentId: " + documentId + ", Setting favorite to: " + isFavorite);

        if (!NetworkUtils.isInternetAvailable(this)) {
            Log.e("FavoriteUpdate", "No internet connection available");
            Toast.makeText(this, getString(R.string.no_internet), Toast.LENGTH_SHORT).show();
            return;
        }

        LoaderHelper.showLoader(this, true);
        Log.d("FavoriteUpdate", "Loader displayed, preparing API request");

        // Create form data for the is_favorite field
        RequestBody isFavoriteBody = RequestBody.create(MediaType.parse("text/plain"), String.valueOf(isFavorite));

        // Make the API call with form data
        Log.d("FavoriteUpdate", "Sending API request to update document: " + documentId);
        Call<UpdateDocumentResponse> call = apiService.updateDocumentWithFormData(
                documentId,
                null,  // document_name
                null,  // scan_type
                isFavoriteBody,  // is_favorite
                null,  // file_url
                null,  // name
                null,  // profession
                null,  // email
                null,  // mobile_number
                null,  // address
                null,  // company_name
                null,  // website
                null,  // isbn_no
                null,  // book_name
                null,  // author_name
                null,  // publication
                null,  // number_of_pages
                null,  // subject
                null,  // summary
                null   // content
        );

        call.enqueue(new Callback<>() {
            @Override
            public void onResponse(@NonNull Call<UpdateDocumentResponse> call, @NonNull Response<UpdateDocumentResponse> response) {
                LoaderHelper.hideLoader();
                Log.d("FavoriteUpdate", "Received API response. Status code: " + response.code());

                if (response.isSuccessful() && response.body() != null) {
                    String successMessage = isFavorite ?
                            "Document added to favorites" :
                            "Document removed from favorites";
                    Log.d("FavoriteUpdate", "Update successful: " + successMessage);
                    Toast.makeText(ScanHistoryActivity.this, successMessage, Toast.LENGTH_SHORT).show();

                    // Refresh the document list to show updated data
                    Log.d("FavoriteUpdate", "Refreshing document list");
                    initializeScanHistoryView();
                } else {
                    Log.e("FavoriteUpdate", "API error. Response code: " + response.code());
                    try {
                        if (response.errorBody() != null) {
                            String errorBodyString = response.errorBody().string();
                            Log.e("FavoriteUpdate", "Error body: " + errorBodyString);

                            Gson gson = new Gson();
                            ErrorResponse errorResponse = gson.fromJson(errorBodyString, ErrorResponse.class);
                            String errorMessage = errorResponse != null && errorResponse.getDetail() != null
                                    ? errorResponse.getDetail()
                                    : getString(R.string.default_error_message);

                            Log.e("FavoriteUpdate", "Error message: " + errorMessage);
                            Toast.makeText(ScanHistoryActivity.this, errorMessage, Toast.LENGTH_LONG).show();
                        }
                    } catch (Exception e) {
                        Log.e("FavoriteUpdate", "Exception parsing error response", e);
                        Toast.makeText(ScanHistoryActivity.this, getString(R.string.default_error_message), Toast.LENGTH_LONG).show();
                    }
                }
            }

            @Override
            public void onFailure(@NonNull Call<UpdateDocumentResponse> call, @NonNull Throwable t) {
                LoaderHelper.hideLoader();
                Log.e("FavoriteUpdate", "API call failed", t);
                Toast.makeText(ScanHistoryActivity.this, getString(R.string.network_error), Toast.LENGTH_LONG).show();
            }
        });
    }

    public void deleteDocument(String documentId) {
        Log.d("DeleteDebug", "deleteDocument method called with ID: " + documentId);

        if (documentId == null || documentId.isEmpty()) {
            Log.e("DeleteDebug", "Invalid documentId: " + documentId);
            Toast.makeText(this, "Invalid document ID", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!NetworkUtils.isInternetAvailable(this)) {
            Log.e("DeleteDebug", "No internet connection");
            Toast.makeText(this, getString(R.string.no_internet), Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            LoaderHelper.showLoader(this, true);
            Log.d("DeleteDebug", "Making API call to delete document: " + documentId);

            apiService.deleteDocument(documentId).enqueue(new Callback<>() {
                @Override
                public void onResponse(@NonNull Call<BaseResponse> call, @NonNull Response<BaseResponse> response) {
                    LoaderHelper.hideLoader();
                    Log.d("DeleteDebug", "Delete API response code: " + response.code());

                    // Use ScanHistoryActivity.this to reference the activity context from within the callback
                    // 204 No Content is a success for delete operations, even with null body
                    if (response.isSuccessful()) {
                        Log.d("DeleteDebug", "Document deleted successfully");
                        Toast.makeText(ScanHistoryActivity.this, "Document deleted successfully", Toast.LENGTH_SHORT).show();
                        // Refresh document list to show updated data
                        initializeScanHistoryView();
                    } else {
                        try {
                            String errorMsg = "Failed to delete document";
                            if (response.errorBody() != null) {
                                errorMsg += ": " + response.errorBody().string();
                            }
                            Log.e("DeleteDebug", errorMsg);
                            Toast.makeText(ScanHistoryActivity.this, errorMsg, Toast.LENGTH_SHORT).show();
                        } catch (Exception e) {
                            Log.e("DeleteDebug", "Error parsing error response", e);
                            Toast.makeText(ScanHistoryActivity.this, "Failed to delete document", Toast.LENGTH_SHORT).show();
                        }
                    }
                }

                @Override
                public void onFailure(@NonNull Call<BaseResponse> call, @NonNull Throwable t) {
                    LoaderHelper.hideLoader();
                    Log.e("DeleteDebug", "API call failed", t);
                    Toast.makeText(ScanHistoryActivity.this, "Error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                }
            });
        } catch (Exception e) {
            Log.e("DeleteDebug", "Exception making delete API call", e);
            LoaderHelper.hideLoader();
            Toast.makeText(ScanHistoryActivity.this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Shows a custom delete confirmation dialog
     *
     * @param documentId The ID of the document to delete
     */
    public void showDeleteConfirmationDialog(String documentId) {
        Log.d("DeleteDebug", "showDeleteConfirmationDialog called with ID: " + documentId);

        try {
            // Create a dialog using your custom layout
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            View dialogView = getLayoutInflater().inflate(R.layout.layout_delete_popup, null);
            builder.setView(dialogView);

            // Create the dialog
            AlertDialog dialog = builder.create();
            dialog.setCancelable(true);

            // Set button click listeners
            @SuppressLint({"MissingInflatedId", "LocalSuppress"}) MaterialButton cancelButton = dialogView.findViewById(R.id.btn_cancel);
            MaterialButton deleteButton = dialogView.findViewById(R.id.btn_delete);

            if (cancelButton == null || deleteButton == null) {
                Log.e("DeleteDebug", "Buttons not found in dialog layout");
                return;
            }

            cancelButton.setOnClickListener(v -> {
                Log.d("DeleteDebug", "Cancel button clicked");
                dialog.dismiss();
            });

            deleteButton.setOnClickListener(v -> {
                Log.d("DeleteDebug", "Delete button clicked, calling deleteDocument for ID: " + documentId);
                deleteDocument(documentId);
                dialog.dismiss();
            });

            // Show the dialog
            dialog.show();
            Log.d("DeleteDebug", "Dialog shown successfully");
        } catch (Exception e) {
            Log.e("DeleteDebug", "Error showing delete dialog", e);
        }
    }

    public void launchDetailActivity(GetAllDocumentsResponseItem document) {
        Intent intent;
        try {
            DocumentType type = DocumentType.fromDisplayName(document.getScanType());

            intent = switch (type) {
                case BUSINESS -> new Intent(this, BusinessDetailsActivity.class);
                case BOOK -> new Intent(this, BookDetailsActivity.class);
                case DOCUMENT -> new Intent(this, DocumentDetailsActivity.class);
                default -> new Intent(this, IDDetailsActivity.class);
            };
        } catch (IllegalArgumentException e) {
            intent = new Intent(this, DocumentDetailsActivity.class);
        }

        intent.putExtra(IntentKeys.DOCUMENT_ID, document.getId());
        intent.putExtra(IntentKeys.OCR_TEXT, document.getSummary());
        intent.putExtra(IntentKeys.DOCUMENT_TYPE, document.getScanType());
        detailLauncher.launch(intent);
    }



    private void bookNameUpdateApiCall(GetAllDocumentsResponseItem documentsResponseItem) {
        // Show loading indicator
        LoaderHelper.showLoader(this, true);

        // Check network connectivity
        if (!NetworkUtils.isInternetAvailable(this)) {
            LoaderHelper.hideLoader();
            Toast.makeText(this, getString(R.string.no_internet), Toast.LENGTH_SHORT).show();
            return;
        }
        RequestBody isbnBody = RequestBody.create(okhttp3.MediaType.parse("text/plain"),""+documentsResponseItem.getIsbnNo());
        RequestBody documentNameBody = RequestBody.create(
                okhttp3.MediaType.parse("text/plain"),
                documentsResponseItem.getDocumentName());
        RequestBody authorNameBody = RequestBody.create(
                okhttp3.MediaType.parse("text/plain"),
                documentsResponseItem.getAuthorName());
        RequestBody scanTypeBody = RequestBody.create(
                okhttp3.MediaType.parse("text/plain"), DocumentType.BOOK.getDisplayName());
        RequestBody fileUrlBody = RequestBody.create(
                okhttp3.MediaType.parse("text/plain"),
                documentsResponseItem .getFileUrl());
        RequestBody numberOfPagesBody = RequestBody.create(
                okhttp3.MediaType.parse("text/plain"),documentsResponseItem.getNumberOfPages());  // Use "0" instead of empty string
        // Create numeric fields with valid values since the error is related to number_of_pages
        RequestBody publicationBody = RequestBody.create(
                okhttp3.MediaType.parse("text/plain"),documentsResponseItem.getPublication());  // Use "0" instead of empty string
        RequestBody subjectBody = RequestBody.create(
                okhttp3.MediaType.parse("text/plain"),documentsResponseItem.getSubject());  // Use "0" instead of empty string

        // Make the API call using multipart
        apiService.updateDocumentWithFormData(
                documentsResponseItem.getId(),
                documentNameBody,
                documentNameBody,
                scanTypeBody,
                fileUrlBody,
                isbnBody,
                authorNameBody,
                publicationBody,
                numberOfPagesBody,
                subjectBody
        ).enqueue(new Callback<>() {
            @Override
            public void onResponse(Call<ResponseBody> call,
                                   Response<ResponseBody> response) {
                LoaderHelper.hideLoader();
                if (response.isSuccessful() && response.body() != null) {
                    initializeScanHistoryView();
                } else {
                    handleErrorResponse(response);
                }
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                LoaderHelper.hideLoader();
                Toast.makeText(getApplicationContext(),
                        "Error: " + t.getMessage(),
                        Toast.LENGTH_SHORT).show();
            }
        });
    }


    private void documentNameUpdateApiCall(GetAllDocumentsResponseItem documentsResponseItem) {
        // Show loading indicator
        LoaderHelper.showLoader(this, true);

        // Check network connectivity
        if (!NetworkUtils.isInternetAvailable(this)) {
            LoaderHelper.hideLoader();
            Toast.makeText(this, getString(R.string.no_internet), Toast.LENGTH_SHORT).show();
            return;
        }

        RequestBody documentNameBody = RequestBody.create(
                okhttp3.MediaType.parse("text/plain"), documentsResponseItem.getDocumentName());

        // Use current file URL or empty if not available
        RequestBody fileUrlBody = RequestBody.create(
                okhttp3.MediaType.parse("text/plain"), documentsResponseItem.getFileUrl());

        // For scan type - assuming "document" for business cards
        RequestBody scanTypeBody = RequestBody.create(
                okhttp3.MediaType.parse("text/plain"), DocumentType.DOCUMENT.getDisplayName());

        // For description/summary - using the description from updateRequest
        RequestBody summaryBody = RequestBody.create(
                okhttp3.MediaType.parse("text/plain"),
                documentsResponseItem.getSummary());


        // Make the API call using multipart
        apiService.updateDocumentWithFormData(
                documentsResponseItem.getId(),
                documentNameBody,     // document_name
                scanTypeBody,         // scan_type
                fileUrlBody,          // file_url
                summaryBody          // summary
        ).enqueue(new Callback<>() {
            @Override
            public void onResponse(Call<ResponseBody> call,
                                   Response<ResponseBody> response) {
                LoaderHelper.hideLoader();
                if (response.isSuccessful() && response.body() != null) {
                    initializeScanHistoryView();
                } else {
                    handleErrorResponse(response);
                }
            }
            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                LoaderHelper.hideLoader();
                Toast.makeText(getApplicationContext(),
                        "Error: " + t.getMessage(),
                        Toast.LENGTH_SHORT).show();
            }
        });
    }


    private void idBusinessNameUpdateApiCall(GetAllDocumentsResponseItem documentsResponseItem) {
        // Show loading indicator
        LoaderHelper.showLoader(this, true);

        // Check network connectivity
        if (!NetworkUtils.isInternetAvailable(this)) {
            LoaderHelper.hideLoader();
            Toast.makeText(this, getString(R.string.no_internet), Toast.LENGTH_SHORT).show();
            return;
        }

        // Create RequestBody objects for each field
        RequestBody nameBody = RequestBody.create(
                okhttp3.MediaType.parse("text/plain"),
                documentsResponseItem.getName() );

        RequestBody professionBody = RequestBody.create(
                okhttp3.MediaType.parse("text/plain"),
                documentsResponseItem.getProfession());

        RequestBody emailBody = RequestBody.create(
                okhttp3.MediaType.parse("text/plain"),
                documentsResponseItem.getEmail());

        RequestBody mobileNumberBody = RequestBody.create(
                okhttp3.MediaType.parse("text/plain"),
                documentsResponseItem.getMobileNumber());

        RequestBody addressBody = RequestBody.create(
                okhttp3.MediaType.parse("text/plain"),
                documentsResponseItem.getAddress());

        RequestBody companyNameBody = RequestBody.create(
                okhttp3.MediaType.parse("text/plain"),
                documentsResponseItem.getCompanyName());

        RequestBody websiteBody = RequestBody.create(
                okhttp3.MediaType.parse("text/plain"),
                documentsResponseItem.getWebsite());

        RequestBody documentNameBody = RequestBody.create(
                okhttp3.MediaType.parse("text/plain"),
                documentsResponseItem.getDocumentName());

        // For boolean values
        RequestBody isFavoriteBody = RequestBody.create(
                okhttp3.MediaType.parse("text/plain"), "false");

        // Use current file URL or empty if not available
        RequestBody fileUrlBody = RequestBody.create(
                okhttp3.MediaType.parse("text/plain"),
                documentsResponseItem.getFileUrl());

        // For scan type - assuming "id" for business cards
        RequestBody scanTypeBody = RequestBody.create(
                okhttp3.MediaType.parse("text/plain"),  documentsResponseItem.getScanType());

        // For description/summary - using the description from updateRequest
        RequestBody summaryBody = RequestBody.create(
                okhttp3.MediaType.parse("text/plain"),
                "");

        // Create numeric fields with valid values
        RequestBody numberOfPagesBody = RequestBody.create(
                okhttp3.MediaType.parse("text/plain"), "0");

        // Other empty fields that may need valid defaults
        RequestBody emptyStringBody = RequestBody.create(okhttp3.MediaType.parse("text/plain"), "");
        RequestBody emptyContentBody = RequestBody.create(okhttp3.MediaType.parse("text/plain"), "");

        // Make the API call using multipart
        apiService.updateDocumentWithFormData(
                documentsResponseItem.getId(),
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
                if (response.isSuccessful() && response.body() != null) {
                    initializeScanHistoryView();
                } else {
                    handleErrorResponse(response);
                }
            }
            @Override
            public void onFailure(Call<UpdateDocumentResponse> call, Throwable t) {
                LoaderHelper.hideLoader();
                Toast.makeText(getApplicationContext(),
                        "Error: " + t.getMessage(),
                        Toast.LENGTH_SHORT).show();
            }
        });
    }


    private void renameBottomSheet(GetAllDocumentsResponseItem document) {
        @SuppressLint("InflateParams") View view = LayoutInflater.from(this).inflate(R.layout.layout_rename_popup, null);

        BottomSheetDialog dialog = new BottomSheetDialog(this);
        dialog.setContentView(view);

        // Make sure dialog resizes with keyboard
        if (dialog.getWindow() != null) {
            dialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
        }

        EditText edRename = view.findViewById(R.id.edRename);
        edRename.setText(document.getDocumentName());

        Button btnRename = view.findViewById(R.id.btn_rename);
        Button btnCancel = view.findViewById(R.id.btn_cancel);
        btnRename.setOnClickListener(view1 -> {
            dialog.dismiss();
            document.setDocumentName(edRename.getText().toString());
            if (document.getScanType().equalsIgnoreCase(DocumentType.DOCUMENT.getDisplayName())) {
                documentNameUpdateApiCall(document);
            } else if (document.getScanType().equalsIgnoreCase(DocumentType.BOOK.getDisplayName())) {
                bookNameUpdateApiCall(document);
            } else {
                idBusinessNameUpdateApiCall(document);
            }
        });
        btnCancel.setOnClickListener(v -> dialog.dismiss());

        // Automatically focus and show keyboard
        edRename.requestFocus();
        edRename.postDelayed(() -> {
            InputMethodManager imm = (InputMethodManager) this.getSystemService(Context.INPUT_METHOD_SERVICE);
            if (imm != null) {
                imm.showSoftInput(edRename, InputMethodManager.SHOW_IMPLICIT);
            }
        }, 200);
        dialog.show();
    }

    String finalDesc = "" ;
    private void showSaveBottomSheet(GetAllDocumentsResponseItem document) {
        @SuppressLint("InflateParams") View view = LayoutInflater.from(this).inflate(R.layout.layout_export_popup, null);

        BottomSheetDialog dialog = new BottomSheetDialog(this);
        dialog.setContentView(view);

        ImageView btnClose = view.findViewById(R.id.btnClose);
        CardView btnExportPNG = view.findViewById(R.id.btnExportPNG);
        CardView btnExportPDF = view.findViewById(R.id.btnExportPDF);

        if(document.getScanType().equalsIgnoreCase(DocumentType.BOOK.getDisplayName())){
            finalDesc = document.getBookName() ;
        }else if(document.getScanType().equalsIgnoreCase(DocumentType.DOCUMENT.getDisplayName())){
            finalDesc = document.getSummary() ;
        }else {
            finalDesc = document.getName() ;
        }

        btnExportPNG.setOnClickListener(view2 -> {
            onExportPNGClick(finalDesc);
            dialog.dismiss();
        });

        btnExportPDF.setOnClickListener(view1 -> {
            onExportPDFClick(finalDesc);
            dialog.dismiss();
        });

        btnClose.setOnClickListener(v -> dialog.dismiss());
        dialog.show();
    }

    public void onExportPNGClick(String desc) {
        try {
            // Get document content
            ArrayList<String> documentData = getFormattedDocumentData(desc);

            // Use DocumentUtil to generate and save image
            Uri imageUri = DocumentUtil.generateImage(documentData, this);

            if (imageUri != null) {
                Toast.makeText(this, "Document exported as image", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Failed to export document as image", Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            Toast.makeText(this, "Error occurred while exporting as image", Toast.LENGTH_SHORT).show();
        }
    }

    public void onExportPDFClick(String desc) {
        try {
            // Get document content
            ArrayList<String> documentData = getFormattedDocumentData(desc);
            Uri pdfUri = DocumentUtil.generatePDF(documentData, this);

            if (pdfUri != null) {
                Toast.makeText(this, "Document exported as PDF", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Failed to export document as PDF", Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            Toast.makeText(this, "Error occurred while exporting as PDF", Toast.LENGTH_SHORT).show();
        }
    }

    private ArrayList<String> getFormattedDocumentData(String desc) {
        StringBuilder formattedData = new StringBuilder();
        if (desc != null) {
            String documentText = desc.trim();
            if (!documentText.isEmpty()) {
                formattedData.append(documentText);
            } else {
                formattedData.append(desc);
            }
        } else {
            formattedData.append("No document data available");
        }
        return new ArrayList<>(Arrays.asList(formattedData.toString()));
    }
}
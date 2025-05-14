package com.gmscan.fragements;


import static android.app.Activity.RESULT_OK;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
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
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.SearchView;
import androidx.cardview.widget.CardView;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.gmscan.R;
import com.gmscan.activity.BookDetailsActivity;
import com.gmscan.activity.BusinessDetailsActivity;
import com.gmscan.activity.DocumentDetailsActivity;
import com.gmscan.activity.IDDetailsActivity;
import com.gmscan.adapter.FaviouriteAdapter;
import com.gmscan.model.BaseResponse;
import com.gmscan.model.ErrorResponse;
import com.gmscan.model.UpdateDocumentResponse;
import com.gmscan.model.getAllDocuments.GetAllDocumentsResponseItem;
import com.gmscan.model.getAllDocuments.ScannedDocument;
import com.gmscan.service.RestApiBuilder;
import com.gmscan.service.RestApiService;
import com.gmscan.utility.ChipUtils;
import com.gmscan.utility.DocumentType;
import com.gmscan.utility.DocumentUtil;
import com.gmscan.utility.IntentKeys;
import com.gmscan.utility.LoaderHelper;
import com.gmscan.utility.NetworkUtils;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.ChipGroup;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import okhttp3.MediaType;
import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * FaviouriteFragment displays the favorite scanned documents.
 * Users can filter their scan history using selectable chips.
 */
public class FaviouriteFragment extends Fragment {

    private View includeNoHistory;
    private RecyclerView recyclerView;
    private ChipGroup chipGroup;
    private SearchView searchView;
    private List<GetAllDocumentsResponseItem> originalList = new ArrayList<>();
    private List<GetAllDocumentsResponseItem> filteredList = new ArrayList<>();
    private FaviouriteAdapter faviouriteAdapter;
    private RestApiService apiService;
    SwipeRefreshLayout swipeRefreshLayout;
    private String currentScanTypeFilter = "all"; // Track current filter

    // List of chip labels for filtering scan history
    private final List<String> chipLabels = Arrays.asList("All", "ID Scan", "Business Card", "Book", "Document");
    private ActivityResultLauncher<Intent> detailLauncher;


    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Register result launcher
        detailLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
            if (result.getResultCode() == RESULT_OK) {
                loadFavoriteDocuments();
            }
        });
    }

    /**
     * Inflates the fragment layout and initializes UI components.
     */
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_favourite, container, false);
        apiService = RestApiBuilder.getService();
        initializeViews(view);
        return view;
    }

    /**
     * Factory method to create a new instance of FaviouriteFragment.
     */
    public static FaviouriteFragment newInstance() {
        return new FaviouriteFragment();
    }

    /**
     * Initializes UI components and sets up the chip group.
     *
     * @param view The root view of the fragment layout.
     */
    private void initializeViews(View view) {
        View includeAppBar = view.findViewById(R.id.includeAppBar);
        TextView txtTitle = includeAppBar.findViewById(R.id.txtTitle);
        recyclerView = view.findViewById(R.id.scanHistoryRecyclerView);
        includeNoHistory = view.findViewById(R.id.includeNoHistory);
        searchView = view.findViewById(R.id.searchView);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        chipGroup = view.findViewById(R.id.chipGroup);
        swipeRefreshLayout = view.findViewById(R.id.swipeRefreshLayout);

        // Set the title for the screen
        txtTitle.setText(getString(R.string.favourite));

        // Setting up search functionality
        setupSearchView();

        // Setting up the ChipGroup dynamically
        setupChips();
        swipeRefreshLayout.setOnRefreshListener(() -> {
            setupChips();
            // Keep current filter when refreshing
            loadFavoriteDocuments();
            swipeRefreshLayout.setRefreshing(false);
        });
    }

    /**
     * Sets up search view with appropriate listeners
     */
    private void setupSearchView() {
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                if (query != null && !query.trim().isEmpty()) {
                    searchDocumentsFromApi(query);
                }
                return true;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                // Only perform search when user submits or when the query is empty (to reset)
                if (newText == null || newText.trim().isEmpty()) {
                    loadFavoriteDocuments();
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
    /**
     * Searches documents using the API instead of local filtering
     *
     * @param query The search query text
     */
    private void searchDocumentsFromApi(String query) {
        if (query == null || query.trim().isEmpty()) {
            Toast.makeText(getContext(), "Search query cannot be empty", Toast.LENGTH_SHORT).show();
            loadFavoriteDocuments();
            return;
        }

        if (!NetworkUtils.isInternetAvailable(requireContext())) {
            Toast.makeText(requireContext(), getString(R.string.no_internet), Toast.LENGTH_SHORT).show();
            return;
        }

        LoaderHelper.showLoader(requireActivity(), true);
        Log.d("API_CALL", "Calling searchDocuments with query: " + query);

        apiService.searchDocuments(query).enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(@NonNull Call<ResponseBody> call, @NonNull Response<ResponseBody> response) {
                LoaderHelper.hideLoader();

                if (getContext() == null) return;

                if (response.isSuccessful() && response.body() != null) {
                    try {
                        String responseBody = response.body().string(); // Consume the response
                        Log.d("API_RESPONSE", "Raw response: " + responseBody);

                        // Parse it as a List of the document model
                        Gson gson = new Gson();

                        // Use GetAllDocumentsResponseItem type instead of ScannedDocument
                        Type listType = new TypeToken<List<GetAllDocumentsResponseItem>>() {}.getType();
                        List<GetAllDocumentsResponseItem> searchResults = gson.fromJson(responseBody, listType);

                        // Filter to only show favorite documents in search results
                        if (searchResults != null) {
                            List<GetAllDocumentsResponseItem> favoriteResults = new ArrayList<>();
                            for (GetAllDocumentsResponseItem item : searchResults) {
                                if (item.isFavorite()) {
                                    favoriteResults.add(item);
                                }
                            }
                            searchResults = favoriteResults;
                        }

                        if (searchResults == null || searchResults.isEmpty()) {
                            includeNoHistory.setVisibility(View.VISIBLE);
                            recyclerView.setVisibility(View.GONE);
                            Toast.makeText(getContext(), "No favorite results found", Toast.LENGTH_SHORT).show();
                        } else {
                            includeNoHistory.setVisibility(View.GONE);
                            recyclerView.setVisibility(View.VISIBLE);
                            originalList = searchResults;

                            // Convert ResponseItem to a format compatible with your adapter
                            faviouriteAdapter = new FaviouriteAdapter(getContext(), searchResults, FaviouriteFragment.this::launchDetailActivity);
                            faviouriteAdapter.setOnDeleteClickListener(FaviouriteFragment.this::showDeleteConfirmationDialog);
                            faviouriteAdapter.setOnFavoriteClickListener(FaviouriteFragment.this::toggleFavoriteStatus);
                            faviouriteAdapter.setOnSaveClickListener(FaviouriteFragment.this::showSaveBottomSheet);
                            faviouriteAdapter.setOnRenameClickListener(FaviouriteFragment.this::renameBottomSheet);
                            recyclerView.setAdapter(faviouriteAdapter);
                        }
                    } catch (Exception e) {
                        Log.e("API_PARSE_ERROR", "Failed to parse search response", e);
                        Toast.makeText(getContext(), "Failed to load search results", Toast.LENGTH_SHORT).show();
                        includeNoHistory.setVisibility(View.VISIBLE);
                        recyclerView.setVisibility(View.GONE);
                    }
                } else {
                    handleErrorResponse(response);
                }
            }

            @Override
            public void onFailure(@NonNull Call<ResponseBody> call, @NonNull Throwable t) {
                handleApiFailure(t);
            }
        });
    }

    /**
     * Handle error responses from API calls
     *
     * @param response The error response from the API
     */
    private void handleErrorResponse(Response<?> response) {
        try {
            LoaderHelper.hideLoader();
            if (getContext() == null) return;

            if (response.errorBody() != null) {
                String errorBodyString = response.errorBody().string();
                Log.e("API_RESPONSE", "Error body: " + errorBodyString);

                Gson gson = new Gson();
                ErrorResponse errorResponse = gson.fromJson(errorBodyString, ErrorResponse.class);
                String errorMessage = errorResponse != null && errorResponse.getDetail() != null ? errorResponse.getDetail() : getString(R.string.default_error_message);

                Toast.makeText(getContext(), errorMessage, Toast.LENGTH_LONG).show();
            } else {
                Toast.makeText(getContext(), getString(R.string.default_error_message), Toast.LENGTH_LONG).show();
            }
            includeNoHistory.setVisibility(View.VISIBLE);
            recyclerView.setVisibility(View.GONE);
        } catch (Exception e) {
            Log.e("API_RESPONSE", "Exception in error handling: " + e.getMessage());
            if (getContext() != null) {
                Toast.makeText(getContext(), getString(R.string.default_error_message), Toast.LENGTH_LONG).show();
                includeNoHistory.setVisibility(View.VISIBLE);
                recyclerView.setVisibility(View.GONE);
            }
        }
    }

    /**
     * Handle API call failures
     *
     * @param t The throwable containing error details
     */
    private void handleApiFailure(Throwable t) {
        LoaderHelper.hideLoader();
        if (getContext() == null) return;

        Log.e("API_RESPONSE", "API call failed: " + t.getMessage());
        Toast.makeText(getContext(), getString(R.string.network_error), Toast.LENGTH_LONG).show();
        includeNoHistory.setVisibility(View.VISIBLE);
        recyclerView.setVisibility(View.GONE);
    }

    /**
     * Refreshes the scan history when the fragment is resumed.
     */
    @Override
    public void onResume() {
        super.onResume();
        loadFavoriteDocuments();
    }

    /**
     * Load favorite documents and apply current filter
     */
    private void loadFavoriteDocuments() {
        if (!isAdded() || getContext() == null) return;


        if (!NetworkUtils.isInternetAvailable(requireContext())) {
            Toast.makeText(requireContext(), getString(R.string.no_internet), Toast.LENGTH_SHORT).show();
            return;
        }

        LoaderHelper.showLoader(requireActivity(), true);

        apiService.getFavoriteDocuments().enqueue(new Callback<>() {
            @Override
            public void onResponse(@NonNull Call<List<GetAllDocumentsResponseItem>> call, @NonNull Response<List<GetAllDocumentsResponseItem>> response) {
                try {
                    LoaderHelper.hideLoader();
                } catch (Exception e) {
                    Log.e("API_CALL", "Error hiding loader", e);
                }
                Log.d("API_CALL", "Calling getFavoriteDocuments");
                if (getContext() == null) return; // Check if fragment is still attached

                if (response.isSuccessful() && response.body() != null) {
                    List<GetAllDocumentsResponseItem> favoriteItems = response.body();
                    if (favoriteItems.isEmpty()) {
                        includeNoHistory.setVisibility(View.VISIBLE);
                        recyclerView.setVisibility(View.GONE);
                    } else {
                        // Convert GetallfavoriteItem list to ScannedDocument list
                        originalList = favoriteItems;

                        // Apply current scan type filter if not "all"
                        applyCurrentFilter();
                    }
                } else {
                    // Error handling
                    try {
                        if (response.errorBody() != null) {
                            Gson gson = new Gson();
                            ErrorResponse errorResponse = gson.fromJson(response.errorBody().charStream(), ErrorResponse.class);
                            String errorMessage = errorResponse != null && errorResponse.getDetail() != null ? errorResponse.getDetail() : getString(R.string.default_error_message);

                            Toast.makeText(getContext(), errorMessage, Toast.LENGTH_LONG).show();
                        }
                    } catch (Exception e) {
                        Toast.makeText(getContext(), getString(R.string.default_error_message), Toast.LENGTH_LONG).show();
                    }
                    includeNoHistory.setVisibility(View.VISIBLE);
                    recyclerView.setVisibility(View.GONE);
                }
            }

            @Override
            public void onFailure(@NonNull Call<List<GetAllDocumentsResponseItem>> call, @NonNull Throwable t) {
                try {
                    LoaderHelper.hideLoader();
                } catch (Exception e) {
                    Log.e("API_CALL", "Error hiding loader", e);
                }
                if (getContext() == null) return;
                Toast.makeText(getContext(), getString(R.string.network_error), Toast.LENGTH_LONG).show();
                includeNoHistory.setVisibility(View.VISIBLE);
                recyclerView.setVisibility(View.GONE);
            }
        });
    }

    /**
     * Apply the current filter to the list of favorite documents
     */
    private void applyCurrentFilter() {
        if (originalList == null || originalList.isEmpty()) {
            includeNoHistory.setVisibility(View.VISIBLE);
            recyclerView.setVisibility(View.GONE);
            return;
        }

        // If filter is "all", show all favorites
        if ("all".equalsIgnoreCase(currentScanTypeFilter)) {
            filteredList = new ArrayList<>(originalList);
        } else {
            // Filter favorites by scan type
            filteredList = originalList.stream().filter(doc -> currentScanTypeFilter.equalsIgnoreCase(doc.getScanType())).collect(Collectors.toList());
        }

        // Update UI based on filtered results
        if (filteredList.isEmpty()) {
            includeNoHistory.setVisibility(View.VISIBLE);
            recyclerView.setVisibility(View.GONE);
        } else {
            includeNoHistory.setVisibility(View.GONE);
            recyclerView.setVisibility(View.VISIBLE);

            faviouriteAdapter = new FaviouriteAdapter(getContext(), filteredList, FaviouriteFragment.this::launchDetailActivity);
            faviouriteAdapter.setOnDeleteClickListener(FaviouriteFragment.this::showDeleteConfirmationDialog);
            faviouriteAdapter.setOnFavoriteClickListener(FaviouriteFragment.this::toggleFavoriteStatus);
            faviouriteAdapter.setOnSaveClickListener(FaviouriteFragment.this::showSaveBottomSheet);
            faviouriteAdapter.setOnRenameClickListener(FaviouriteFragment.this::renameBottomSheet);
            recyclerView.setAdapter(faviouriteAdapter);
        }
    }

    /**
     * Toggles the favorite status of a document
     *
     * @param documentId The ID of the document to update
     * @param isFavorite The new favorite status
     */
    public void toggleFavoriteStatus(String documentId, boolean isFavorite) {
        Log.d("FavoriteUpdate", "Starting toggle favorite status. DocumentId: " + documentId + ", Setting favorite to: " + isFavorite);

        if (!NetworkUtils.isInternetAvailable(requireContext())) {
            Log.e("FavoriteUpdate", "No internet connection available");
            Toast.makeText(requireContext(), getString(R.string.no_internet), Toast.LENGTH_SHORT).show();
            return;
        }

        LoaderHelper.showLoader(requireActivity(), true);
        Log.d("FavoriteUpdate", "Loader displayed, preparing API request");

        // Create form data for the is_favorite field
        RequestBody isFavoriteBody = RequestBody.create(MediaType.parse("text/plain"), String.valueOf(isFavorite));

        // Make the API call with form data
        Log.d("FavoriteUpdate", "Sending API request to update document: " + documentId);
        Call<UpdateDocumentResponse> call = apiService.updateDocumentWithFormData(documentId, null,  // document_name
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

        call.enqueue(new Callback<UpdateDocumentResponse>() {
            @Override
            public void onResponse(@NonNull Call<UpdateDocumentResponse> call, @NonNull Response<UpdateDocumentResponse> response) {
                LoaderHelper.hideLoader();
                Log.d("FavoriteUpdate", "Received API response. Status code: " + response.code());

                if (getContext() == null) {
                    Log.w("FavoriteUpdate", "Fragment no longer attached, aborting update");
                    return;
                }

                if (response.isSuccessful() && response.body() != null) {
                    String successMessage = isFavorite ? "Document added to favorites" : "Document removed from favorites";
                    Log.d("FavoriteUpdate", "Update successful: " + successMessage);
                    Toast.makeText(getContext(), successMessage, Toast.LENGTH_SHORT).show();

                    // If unfavoriting in favorites view - remove locally and update adapter
                    if (!isFavorite) {
                        removeDocumentFromLists(documentId);
                        updateAdapterWithCurrentList();
                    } else {
                        // For favoriting, reload the full list to ensure we have latest data
                        loadFavoriteDocuments();
                    }
                } else {
                    Log.e("FavoriteUpdate", "API error. Response code: " + response.code());
                    try {
                        if (response.errorBody() != null) {
                            String errorBodyString = response.errorBody().string();
                            Log.e("FavoriteUpdate", "Error body: " + errorBodyString);

                            Gson gson = new Gson();
                            ErrorResponse errorResponse = gson.fromJson(errorBodyString, ErrorResponse.class);
                            String errorMessage = errorResponse != null && errorResponse.getDetail() != null ? errorResponse.getDetail() : getString(R.string.default_error_message);

                            Log.e("FavoriteUpdate", "Error message: " + errorMessage);
                            Toast.makeText(getContext(), errorMessage, Toast.LENGTH_LONG).show();
                        }
                    } catch (Exception e) {
                        Log.e("FavoriteUpdate", "Exception parsing error response", e);
                        Toast.makeText(getContext(), getString(R.string.default_error_message), Toast.LENGTH_LONG).show();
                    }
                }
            }

            @Override
            public void onFailure(@NonNull Call<UpdateDocumentResponse> call, @NonNull Throwable t) {
                LoaderHelper.hideLoader();
                Log.e("FavoriteUpdate", "API call failed", t);

                if (getContext() == null) {
                    Log.w("FavoriteUpdate", "Fragment no longer attached, can't display error");
                    return;
                }

                Toast.makeText(getContext(), getString(R.string.network_error), Toast.LENGTH_LONG).show();
            }
        });
    }

    /**
     * Remove a document from both original and filtered lists
     *
     * @param documentId The ID of the document to remove
     */
    private void removeDocumentFromLists(String documentId) {
        // Remove from original list
        originalList.removeIf(doc -> doc.getId().equals(documentId));

        // Remove from filtered list
        filteredList.removeIf(doc -> doc.getId().equals(documentId));
    }

    /**
     * Update the adapter with the current filtered list
     */
    private void updateAdapterWithCurrentList() {
        if (filteredList.isEmpty()) {
            includeNoHistory.setVisibility(View.VISIBLE);
            recyclerView.setVisibility(View.GONE);
        } else {
            includeNoHistory.setVisibility(View.GONE);
            recyclerView.setVisibility(View.VISIBLE);

            faviouriteAdapter = new FaviouriteAdapter(getContext(), filteredList, FaviouriteFragment.this::launchDetailActivity);
            faviouriteAdapter.setOnDeleteClickListener(FaviouriteFragment.this::showDeleteConfirmationDialog);
            faviouriteAdapter.setOnFavoriteClickListener(FaviouriteFragment.this::toggleFavoriteStatus);
            faviouriteAdapter.setOnSaveClickListener(FaviouriteFragment.this::showSaveBottomSheet);
            faviouriteAdapter.setOnRenameClickListener(FaviouriteFragment.this::renameBottomSheet);
            recyclerView.setAdapter(faviouriteAdapter);
        }
    }

    /**
     * Initializes the ChipGroup using the ChipUtils helper method.
     * It handles filtering scan history based on the selected chip.
     */
    private void setupChips() {
        ChipUtils.setupChipGroup(requireContext(), chipGroup, chipLabels, selectedText -> {
            // Update current filter type
            currentScanTypeFilter = getScanTypeFromDisplayText(selectedText);

            // Apply filter to existing list instead of making new API call
            applyCurrentFilter();
        });
    }

    /**
     * Converts display text from chips to scan type parameter for API
     *
     * @param displayText The text displayed on the chip
     * @return The corresponding scan type for the API
     */
    private String getScanTypeFromDisplayText(String displayText) {
        if (displayText.equalsIgnoreCase(getString(R.string.all))) {
            return "all";
        }

        return switch (displayText.toLowerCase()) {
            case "id scan" -> "id";
            case "business card" -> "business";
            case "book" -> "book";
            case "document" -> "document";
            default -> "all"; // Default case
        };
    }

    /**
     * Deletes a document with the provided ID
     *
     * @param documentId The ID of the document to delete
     */
    public void deleteDocument(String documentId) {
        Log.d("DeleteDebug", "deleteDocument method called with ID: " + documentId);

        if (documentId == null || documentId.isEmpty()) {
            Log.e("DeleteDebug", "Invalid documentId: " + documentId);
            Toast.makeText(getContext(), "Invalid document ID", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!NetworkUtils.isInternetAvailable(requireContext())) {
            Log.e("DeleteDebug", "No internet connection");
            Toast.makeText(requireContext(), getString(R.string.no_internet), Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            LoaderHelper.showLoader(requireActivity(), true);
            Log.d("DeleteDebug", "Making API call to delete document: " + documentId);

            apiService.deleteDocument(documentId).enqueue(new Callback<BaseResponse>() {
                @Override
                public void onResponse(@NonNull Call<BaseResponse> call, @NonNull Response<BaseResponse> response) {
                    LoaderHelper.hideLoader();
                    Log.d("DeleteDebug", "Delete API response code: " + response.code());

                    if (getContext() == null) {
                        Log.e("DeleteDebug", "Context is null in API response");
                        return;
                    }

                    // 204 No Content is a success for delete operations, even with null body
                    if (response.isSuccessful()) {
                        Log.d("DeleteDebug", "Document deleted successfully");
                        Toast.makeText(getContext(), "Document deleted successfully", Toast.LENGTH_SHORT).show();

                        // Remove item locally
                        removeDocumentFromLists(documentId);
                        updateAdapterWithCurrentList();
                    } else {
                        try {
                            String errorMsg = "Failed to delete document";
                            if (response.errorBody() != null) {
                                errorMsg += ": " + response.errorBody().string();
                            }
                            Log.e("DeleteDebug", errorMsg);
                            Toast.makeText(getContext(), errorMsg, Toast.LENGTH_SHORT).show();
                        } catch (Exception e) {
                            Log.e("DeleteDebug", "Error parsing error response", e);
                            Toast.makeText(getContext(), "Failed to delete document", Toast.LENGTH_SHORT).show();
                        }
                    }
                }

                @Override
                public void onFailure(@NonNull Call<BaseResponse> call, @NonNull Throwable t) {
                    LoaderHelper.hideLoader();
                    Log.e("DeleteDebug", "API call failed", t);

                    if (getContext() == null) return;
                    Toast.makeText(getContext(), "Error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                }
            });
        } catch (Exception e) {
            Log.e("DeleteDebug", "Exception making delete API call", e);
            LoaderHelper.hideLoader();
            if (getContext() != null) {
                Toast.makeText(getContext(), "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        }
    }

    public void showDeleteConfirmationDialog(String documentId) {
        Log.d("DeleteDebug", "showDeleteConfirmationDialog called with ID: " + documentId);

        if (getContext() == null) {
            Log.e("DeleteDebug", "Context is null, cannot show dialog");
            return;
        }

        try {
            // Create a dialog using your custom layout
            AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
            View dialogView = getLayoutInflater().inflate(R.layout.layout_delete_popup, null);
            builder.setView(dialogView);

            // Create the dialog
            AlertDialog dialog = builder.create();
            dialog.setCancelable(true);

            // Set button click listeners
            MaterialButton cancelButton = dialogView.findViewById(R.id.btn_cancel);
            MaterialButton deleteButton = dialogView.findViewById(R.id.btn_delete);

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


    private void bookNameUpdateApiCall(GetAllDocumentsResponseItem documentsResponseItem) {
        // Show loading indicator
        LoaderHelper.showLoader(getActivity(), true);

        // Check network connectivity
        if (!NetworkUtils.isInternetAvailable(getActivity())) {
            LoaderHelper.hideLoader();
            Toast.makeText(getActivity(), getString(R.string.no_internet), Toast.LENGTH_SHORT).show();
            return;
        }
        RequestBody isbnBody = RequestBody.create(okhttp3.MediaType.parse("text/plain"), "" + documentsResponseItem.getIsbnNo());
        RequestBody documentNameBody = RequestBody.create(okhttp3.MediaType.parse("text/plain"), documentsResponseItem.getDocumentName());
        RequestBody authorNameBody = RequestBody.create(okhttp3.MediaType.parse("text/plain"), documentsResponseItem.getAuthorName());
        RequestBody scanTypeBody = RequestBody.create(okhttp3.MediaType.parse("text/plain"), DocumentType.BOOK.getDisplayName());
        RequestBody fileUrlBody = RequestBody.create(okhttp3.MediaType.parse("text/plain"), documentsResponseItem.getFileUrl());
        RequestBody numberOfPagesBody = RequestBody.create(okhttp3.MediaType.parse("text/plain"), documentsResponseItem.getNumberOfPages());  // Use "0" instead of empty string
        // Create numeric fields with valid values since the error is related to number_of_pages
        RequestBody publicationBody = RequestBody.create(okhttp3.MediaType.parse("text/plain"), documentsResponseItem.getPublication());  // Use "0" instead of empty string
        RequestBody subjectBody = RequestBody.create(okhttp3.MediaType.parse("text/plain"), documentsResponseItem.getSubject());  // Use "0" instead of empty string

        // Make the API call using multipart
        apiService.updateDocumentWithFormData(documentsResponseItem.getId(), documentNameBody, documentNameBody, scanTypeBody, fileUrlBody, isbnBody, authorNameBody, publicationBody, numberOfPagesBody, subjectBody).enqueue(new Callback<>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                LoaderHelper.hideLoader();
                if (response.isSuccessful() && response.body() != null) {
                    loadFavoriteDocuments();
                } else {
                    handleErrorResponse(response);
                }
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                LoaderHelper.hideLoader();
                Toast.makeText(getActivity(), "Error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }


    private void documentNameUpdateApiCall(GetAllDocumentsResponseItem documentsResponseItem) {
        // Show loading indicator
        LoaderHelper.showLoader(getActivity(), true);

        // Check network connectivity
        if (!NetworkUtils.isInternetAvailable(getActivity())) {
            LoaderHelper.hideLoader();
            Toast.makeText(getActivity(), getString(R.string.no_internet), Toast.LENGTH_SHORT).show();
            return;
        }

        RequestBody documentNameBody = RequestBody.create(okhttp3.MediaType.parse("text/plain"), documentsResponseItem.getDocumentName());

        // Use current file URL or empty if not available
        RequestBody fileUrlBody = RequestBody.create(okhttp3.MediaType.parse("text/plain"), documentsResponseItem.getFileUrl());

        // For scan type - assuming "document" for business cards
        RequestBody scanTypeBody = RequestBody.create(okhttp3.MediaType.parse("text/plain"), DocumentType.DOCUMENT.getDisplayName());

        // For description/summary - using the description from updateRequest
        RequestBody summaryBody = RequestBody.create(okhttp3.MediaType.parse("text/plain"), documentsResponseItem.getSummary());


        // Make the API call using multipart
        apiService.updateDocumentWithFormData(documentsResponseItem.getId(), documentNameBody,     // document_name
                scanTypeBody,         // scan_type
                fileUrlBody,          // file_url
                summaryBody          // summary
        ).enqueue(new Callback<>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                LoaderHelper.hideLoader();
                if (response.isSuccessful() && response.body() != null) {
                    loadFavoriteDocuments();
                } else {
                    handleErrorResponse(response);
                }
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                LoaderHelper.hideLoader();
                Toast.makeText(getActivity(), "Error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }


    private void idBusinessNameUpdateApiCall(GetAllDocumentsResponseItem documentsResponseItem) {
        // Show loading indicator
        LoaderHelper.showLoader(getActivity(), true);

        // Check network connectivity
        if (!NetworkUtils.isInternetAvailable(getActivity())) {
            LoaderHelper.hideLoader();
            Toast.makeText(getActivity(), getString(R.string.no_internet), Toast.LENGTH_SHORT).show();
            return;
        }

        // Create RequestBody objects for each field
        RequestBody nameBody = RequestBody.create(okhttp3.MediaType.parse("text/plain"), documentsResponseItem.getName());

        RequestBody professionBody = RequestBody.create(okhttp3.MediaType.parse("text/plain"), documentsResponseItem.getProfession());

        RequestBody emailBody = RequestBody.create(okhttp3.MediaType.parse("text/plain"), documentsResponseItem.getEmail());

        RequestBody mobileNumberBody = RequestBody.create(okhttp3.MediaType.parse("text/plain"), documentsResponseItem.getMobileNumber());

        RequestBody addressBody = RequestBody.create(okhttp3.MediaType.parse("text/plain"), documentsResponseItem.getAddress());

        RequestBody companyNameBody = RequestBody.create(okhttp3.MediaType.parse("text/plain"), documentsResponseItem.getCompanyName());

        RequestBody websiteBody = RequestBody.create(okhttp3.MediaType.parse("text/plain"), documentsResponseItem.getWebsite());

        RequestBody documentNameBody = RequestBody.create(okhttp3.MediaType.parse("text/plain"), documentsResponseItem.getDocumentName());

        final boolean favorite = documentsResponseItem.isFavorite();

        // For boolean values
        RequestBody isFavoriteBody = RequestBody.create(
                okhttp3.MediaType.parse("text/plain"), favorite ? "true" : "false");

        // Use current file URL or empty if not available
        RequestBody fileUrlBody = RequestBody.create(okhttp3.MediaType.parse("text/plain"), documentsResponseItem.getFileUrl());

        // For scan type - assuming "id" for business cards
        RequestBody scanTypeBody = RequestBody.create(okhttp3.MediaType.parse("text/plain"), documentsResponseItem.getScanType());

        // For description/summary - using the description from updateRequest
        RequestBody summaryBody = RequestBody.create(okhttp3.MediaType.parse("text/plain"), "");

        // Create numeric fields with valid values
        RequestBody numberOfPagesBody = RequestBody.create(okhttp3.MediaType.parse("text/plain"), "0");

        // Other empty fields that may need valid defaults
        RequestBody emptyStringBody = RequestBody.create(okhttp3.MediaType.parse("text/plain"), "");
        RequestBody emptyContentBody = RequestBody.create(okhttp3.MediaType.parse("text/plain"), "");

        // Make the API call using multipart
        apiService.updateDocumentWithFormData(documentsResponseItem.getId(), documentNameBody,     // document_name
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
            public void onResponse(Call<UpdateDocumentResponse> call, Response<UpdateDocumentResponse> response) {
                LoaderHelper.hideLoader();
                if (response.isSuccessful() && response.body() != null) {
                    loadFavoriteDocuments();
                } else {
                    handleErrorResponse(response);
                }
            }

            @Override
            public void onFailure(Call<UpdateDocumentResponse> call, Throwable t) {
                LoaderHelper.hideLoader();
                Toast.makeText(getActivity(), "Error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }


    private void renameBottomSheet(GetAllDocumentsResponseItem document) {
        @SuppressLint("InflateParams") View view = LayoutInflater.from(getActivity()).inflate(R.layout.layout_rename_popup, null);

        BottomSheetDialog dialog = new BottomSheetDialog(getActivity());
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
            InputMethodManager imm = (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
            if (imm != null) {
                imm.showSoftInput(edRename, InputMethodManager.SHOW_IMPLICIT);
            }
        }, 200);
        dialog.show();
    }

    String finalDesc = "";

    private void showSaveBottomSheet(GetAllDocumentsResponseItem document) {
        @SuppressLint("InflateParams") View view = LayoutInflater.from(getActivity()).inflate(R.layout.layout_export_popup, null);

        BottomSheetDialog dialog = new BottomSheetDialog(getActivity());
        dialog.setContentView(view);

        ImageView btnClose = view.findViewById(R.id.btnClose);
        CardView btnExportPNG = view.findViewById(R.id.btnExportPNG);
        CardView btnExportPDF = view.findViewById(R.id.btnExportPDF);

        if (document.getScanType().equalsIgnoreCase(DocumentType.BOOK.getDisplayName())) {
            finalDesc = document.getBookName();
        } else if (document.getScanType().equalsIgnoreCase(DocumentType.DOCUMENT.getDisplayName())) {
            finalDesc = document.getSummary();
        } else {
            finalDesc = document.getName();
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
            Uri imageUri = DocumentUtil.generateImage(documentData, getActivity());

            if (imageUri != null) {
                Toast.makeText(getActivity(), "Document exported as image", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(getActivity(), "Failed to export document as image", Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            Toast.makeText(getActivity(), "Error occurred while exporting as image", Toast.LENGTH_SHORT).show();
        }
    }

    public void onExportPDFClick(String desc) {
        try {
            // Get document content
            ArrayList<String> documentData = getFormattedDocumentData(desc);
            Uri pdfUri = DocumentUtil.generatePDF(documentData, getActivity());

            if (pdfUri != null) {
                Toast.makeText(getActivity(), "Document exported as PDF", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(getActivity(), "Failed to export document as PDF", Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            Toast.makeText(getActivity(), "Error occurred while exporting as PDF", Toast.LENGTH_SHORT).show();
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


    public void launchDetailActivity(GetAllDocumentsResponseItem document) {
        Intent intent;
        try {
            DocumentType type = DocumentType.fromDisplayName(document.getScanType());

            intent = switch (type) {
                case BUSINESS -> new Intent(getActivity(), BusinessDetailsActivity.class);
                case BOOK -> new Intent(getActivity(), BookDetailsActivity.class);
                case DOCUMENT -> new Intent(getActivity(), DocumentDetailsActivity.class);
                default -> new Intent(getActivity(), IDDetailsActivity.class);
            };
        } catch (IllegalArgumentException e) {
            intent = new Intent(getActivity(), DocumentDetailsActivity.class);
        }

        intent.putExtra(IntentKeys.DOCUMENT_ID, document.getId());
        intent.putExtra(IntentKeys.OCR_TEXT, document.getSummary());
        intent.putExtra(IntentKeys.DOCUMENT_TYPE, document.getScanType());
        detailLauncher.launch(intent);
    }
}
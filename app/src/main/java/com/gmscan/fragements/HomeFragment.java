package com.gmscan.fragements;

import static android.app.Activity.RESULT_OK;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
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
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.RelativeLayout;
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
import com.gmscan.activity.BookScannerActivity;
import com.gmscan.activity.BusinessDetailsActivity;
import com.gmscan.activity.BusinessScanActivity;
import com.gmscan.activity.IDDetailsActivity;
import com.gmscan.activity.DocumentDetailsActivity;
import com.gmscan.activity.DocumentScanningActivity;
import com.gmscan.activity.IDScanningActivity;
import com.gmscan.activity.ScanHistoryActivity;
import com.gmscan.adapter.ScanHistoryAdapter;
import com.gmscan.model.BaseResponse;
import com.gmscan.model.CreateDocuments.CreateDoc;
import com.gmscan.model.ErrorResponse;
import com.gmscan.model.UpdateDocumentResponse;
import com.gmscan.model.documentUpdate.DocumentUpdateRequest;
import com.gmscan.model.getAllDocuments.GetAllDocumentsResponseItem;
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
import com.google.common.reflect.TypeToken;
import com.google.gson.Gson;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import okhttp3.MediaType;
import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * HomeFragment represents the main dashboard of the application,
 * displaying options for scanning different types of documents and managing scan history.
 */
public class HomeFragment extends Fragment {

    private View includeNoHistory;
    private ImageView imgArrow;
    private RecyclerView recyclerView;
    private RelativeLayout btnIdScan, btnBookScan, btnBusinessScan, btnDocumentScan;
    private ChipGroup chipGroup;

    // Search functionality
    private ImageButton searchButton;
    private SearchView searchView;
    private boolean isSearchViewVisible = false;

    // API Service
    private RestApiService apiService;

    // List of chip labels for filtering scan history
    private final List<String> chipLabels = Arrays.asList("All", "ID Scan", "Business Card", "Book", "Document");
    SwipeRefreshLayout swipeRefreshLayout;
    private ActivityResultLauncher<Intent> scanActivityLauncher;
    private ActivityResultLauncher<Intent> detailLauncher;

    /**
     * Inflates the fragment layout and initializes UI components.
     */
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_home, container, false);
        initializeViews(view);
        setClickListeners();
        setupSearchFunctionality();

        // Initialize API service
        apiService = RestApiBuilder.getService();
        initializeScanHistoryView();
        return view;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Register the launcher
        scanActivityLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
            if (result.getResultCode() == RESULT_OK) {
                initializeScanHistoryView();
            }
        });

        // Register result launcher
        detailLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
            if (result.getResultCode() == RESULT_OK) {
                initializeScanHistoryView();
            }
        });
    }

    /**
     * Factory method to create a new instance of HomeFragment.
     */
    public static HomeFragment newInstance() {
        return new HomeFragment();
    }

    /**
     * Initializes UI components and sets up the chip group.
     *
     * @param view The root view of the fragment layout.
     */
    private void initializeViews(View view) {
        imgArrow = view.findViewById(R.id.imgArrow);
        btnIdScan = view.findViewById(R.id.btn_id_scan);
        btnBookScan = view.findViewById(R.id.btn_book_scan);
        btnDocumentScan = view.findViewById(R.id.btn_document_scan);
        btnBusinessScan = view.findViewById(R.id.btn_business_scan);
        recyclerView = view.findViewById(R.id.scanHistoryRecyclerView);
        includeNoHistory = view.findViewById(R.id.includeNoHistory);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        chipGroup = view.findViewById(R.id.chipGroup);
        swipeRefreshLayout = view.findViewById(R.id.swipeRefreshLayout);

        // Initialize search views
        searchButton = view.findViewById(R.id.searchButton);
        searchView = view.findViewById(R.id.searchView);

        // Setting up the ChipGroup dynamically
        setupChips();
    }

    /**
     * Sets up search functionality with animations
     */
    private void setupSearchFunctionality() {
        searchButton.setOnClickListener(v -> toggleSearchView());

        // Set the search icon to your custom drawable
        ImageView searchIcon;
        searchIcon = searchView.findViewById(androidx.appcompat.R.id.search_mag_icon);
        if (searchIcon != null) {
            searchIcon.setImageResource(R.drawable.search);
        }

        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                performSearch(query.trim());
                return true;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                if (newText.isEmpty()) {
                    hideSearchView();
                } else {
                    filterHistoryByQuery(); // Optional filtering
                }
                return true;
            }
        });
    }

     /**
     * Toggles the search view visibility with animation
     */
    private void toggleSearchView() {
        if (isSearchViewVisible) {
            hideSearchView();
        } else {
            showSearchView();
        }
    }

    /**
     * Shows the search view with animation from left to right
     */
    private void showSearchView() {
        // First make the view visible with width 0
        searchView.setVisibility(View.VISIBLE);
        searchView.measure(RelativeLayout.LayoutParams.MATCH_PARENT, searchView.getHeight());
        int width = searchView.getMeasuredWidth();

        // Start animation
        ValueAnimator animator = ValueAnimator.ofInt(0, width);
        animator.addUpdateListener(animation -> {
            int value = (int) animation.getAnimatedValue();
            ViewGroup.LayoutParams params = searchView.getLayoutParams();
            params.width = value;
            searchView.setLayoutParams(params);
        });

        animator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                // Focus on search view when animation ends
                searchView.requestFocus();
            }
        });

        animator.setDuration(300);
        animator.start();
        isSearchViewVisible = true;
    }

    /**
     * Hides the search view with animation from right to left
     */
    private void hideSearchView() {
        int width = searchView.getWidth();

        // Animate from current width to 0
        ValueAnimator animator = ValueAnimator.ofInt(width, 0);
        animator.addUpdateListener(animation -> {
            int value = (int) animation.getAnimatedValue();
            ViewGroup.LayoutParams params = searchView.getLayoutParams();
            params.width = value;
            searchView.setLayoutParams(params);
        });

        animator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                searchView.setVisibility(View.GONE);
                searchView.setQuery("", false);

                // Clear focus
                searchView.clearFocus();

                // Reset to original data
                initializeScanHistoryView();
            }
        });

        animator.setDuration(300);
        animator.start();
        isSearchViewVisible = false;
    }

    /**
     * Performs search on the scan history
     */
    private void performSearch(String query) {
        if (query.isEmpty()) {
            initializeScanHistoryView();
            return;
        }

        // Use API search instead of local filtering
        searchDocumentsFromApi(query);
    }

    /**
     * Filters scan history based on search query
     */
    private void filterHistoryByQuery() {
    }

    /**
     * Sets click listeners for the scanning buttons and history arrow.
     */
    private void setClickListeners() {
        btnIdScan.setOnClickListener(v -> openScanActivity(IDScanningActivity.class));
        btnBookScan.setOnClickListener(v -> openScanActivity(BookScannerActivity.class));
        btnBusinessScan.setOnClickListener(v -> openScanActivity(BusinessScanActivity.class));
        btnDocumentScan.setOnClickListener(v -> openScanActivity(DocumentScanningActivity.class));
        imgArrow.setOnClickListener(v -> openScanActivity(ScanHistoryActivity.class));
        swipeRefreshLayout.setOnRefreshListener(() -> {
            setupChips();
            initializeScanHistoryView();
            swipeRefreshLayout.setRefreshing(false);
        });
    }

    /**
     * Opens the selected scanning activity.
     *
     * @param activityClass The class of the activity to be opened.
     */
    private void openScanActivity(Class<?> activityClass) {
        if (getActivity() != null) {
            Intent intent = new Intent(getActivity(), activityClass);
            scanActivityLauncher.launch(intent);
        }
    }

    /**
     * Refreshes the scan history when the fragment is resumed.
     */
    @Override
    public void onResume() {
        super.onResume();
    }

    /**
     * Fetches and displays the recent scan history by calling the API.
     */
    // get all document api is working
    public void initializeScanHistoryView() {
        if (!isAdded() || getContext() == null) return;

        if (!NetworkUtils.isInternetAvailable(requireContext())) {
            Log.e("API_DEBUG", "initializeScanHistoryView: No internet connection available");
            Toast.makeText(requireContext(), getString(R.string.no_internet), Toast.LENGTH_SHORT).show();
            return;
        }

        Log.d("API_DEBUG", "initializeScanHistoryView: Showing loader");
        LoaderHelper.showLoader(requireActivity(), true);

        Log.d("API_DEBUG", "initializeScanHistoryView: Creating API call to getAllDocuments");
        Call<List<GetAllDocumentsResponseItem>> call = apiService.getAllDocuments();

        Log.d("API_DEBUG", "initializeScanHistoryView: Executing API call");
        call.enqueue(new Callback<>() {
            @Override
            public void onResponse(@NonNull Call<List<GetAllDocumentsResponseItem>> call, @NonNull Response<List<GetAllDocumentsResponseItem>> response) {
                Log.d("API_DEBUG", "getAllDocuments onResponse: Received response with code: " + response.code());

                try {
                    LoaderHelper.hideLoader();
                    Log.d("API_DEBUG", "getAllDocuments onResponse: Loader hidden");
                } catch (Exception e) {
                    Log.e("API_DEBUG", "getAllDocuments onResponse: Error hiding loader", e);
                }

                if (getContext() == null) {
                    Log.w("API_DEBUG", "getAllDocuments onResponse: Context is null, fragment likely detached");
                    return;
                }
                Log.d("API_CALL", "Calling getAllDocuments");
                if (getContext() == null) return;

                if (response.isSuccessful()) {
                    Log.d("API_DEBUG", "getAllDocuments onResponse: Response successful");
                    List<GetAllDocumentsResponseItem> documents = response.body();
                    if (documents != null) {
                        Log.d("API_DEBUG", "getAllDocuments onResponse: Got " + documents.size() + " documents");

                        if (documents.isEmpty()) {
                            Log.d("API_DEBUG", "getAllDocuments onResponse: No documents found, showing empty state");
                            includeNoHistory.setVisibility(View.VISIBLE);
                            recyclerView.setVisibility(View.GONE);
                        } else {
                            Log.d("API_DEBUG", "getAllDocuments onResponse: Documents found, showing recycler view");
                            includeNoHistory.setVisibility(View.GONE);
                            recyclerView.setVisibility(View.VISIBLE);

                            Log.d("API_DEBUG", "getAllDocuments onResponse: Creating adapter");
                            final ScanHistoryAdapter adapter = getAdapter(documents);
                            Log.d("API_DEBUG", "getAllDocuments onResponse: Setting adapter to recycler view");
                            recyclerView.setAdapter(adapter);
                        }
                    } else {
                        Log.e("API_DEBUG", "getAllDocuments onResponse: Response body is null despite successful response");
                        Toast.makeText(getContext(), getString(R.string.default_error_message), Toast.LENGTH_LONG).show();
                    }
                } else {
                    Log.e("API_DEBUG", "getAllDocuments onResponse: Response unsuccessful with code: " + response.code());

                    try {
                        if (response.errorBody() != null) {
                            String errorBodyString = response.errorBody().string();
                            Log.e("API_DEBUG", "getAllDocuments onResponse: Error body: " + errorBodyString);

                            Gson gson = new Gson();
                            ErrorResponse errorResponse = gson.fromJson(errorBodyString, ErrorResponse.class);
                            String errorMessage = errorResponse != null && errorResponse.getDetail() != null ? errorResponse.getDetail() : getString(R.string.default_error_message);

                            Log.e("API_DEBUG", "getAllDocuments onResponse: Parsed error message: " + errorMessage);
                            Toast.makeText(getContext(), errorMessage, Toast.LENGTH_LONG).show();
                        } else {
                            Log.e("API_DEBUG", "getAllDocuments onResponse: Error body is null");
                            Toast.makeText(getContext(), getString(R.string.default_error_message), Toast.LENGTH_LONG).show();
                        }
                    } catch (Exception e) {
                        Log.e("API_DEBUG", "getAllDocuments onResponse: Exception parsing error", e);
                        Toast.makeText(getContext(), getString(R.string.default_error_message), Toast.LENGTH_LONG).show();
                    }
                }
            }

            @Override
            public void onFailure(@NonNull Call<List<GetAllDocumentsResponseItem>> call, @NonNull Throwable t) {
                Log.e("API_DEBUG", "getAllDocuments onFailure: API call failed", t);

                try {
                    LoaderHelper.hideLoader();
                    Log.d("API_DEBUG", "getAllDocuments onFailure: Loader hidden");
                } catch (Exception e) {
                    Log.e("API_DEBUG", "getAllDocuments onFailure: Error hiding loader", e);
                }

                if (getContext() == null) {
                    Log.w("API_DEBUG", "getAllDocuments onFailure: Context is null, fragment likely detached");
                    return;
                }

                Log.e("API_DEBUG", "getAllDocuments onFailure: Error message: " + t.getMessage());
                Toast.makeText(getContext(), getString(R.string.network_error), Toast.LENGTH_LONG).show();
            }
        });
    }

    @NonNull
    private ScanHistoryAdapter getAdapter(List<GetAllDocumentsResponseItem> documents) {
        ScanHistoryAdapter adapter = new ScanHistoryAdapter(getContext(), documents, HomeFragment.this::launchDetailActivity);
        adapter.setOnFavoriteClickListener(HomeFragment.this::toggleFavoriteStatus);
        adapter.setOnDeleteClickListener(this::showDeleteConfirmationDialog);
        adapter.setOnSaveClickListener(this::showSaveBottomSheet);
        adapter.setOnRenameClickListener(this::renameBottomSheet);
        return adapter;
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

    /**
     * Initializes the ChipGroup using the ChipUtils helper method.
     * It handles filtering scan history based on the selected chip.
     */
    private void setupChips() {
        ChipUtils.setupChipGroup(requireContext(), chipGroup, chipLabels, selectedText -> {
            Log.d("FILTER_DEBUG", "Selected chip: " + selectedText);

            // Fetch history based on selected category
            if (selectedText.equalsIgnoreCase("All")) {
                initializeScanHistoryView();
            } else {
                // Convert the display text to API scan type parameter
                String scanType = getScanTypeFromDisplayText(selectedText);
                Log.d("FILTER_DEBUG", "Fetching documents for scan type: " + scanType);
                manageDocumentsByScanTypeApiCall(scanType);
            }
        });
    }

    /**
     * Converts display text from chips to scan type parameter for API
     *
     * @param displayText The text displayed on the chip
     * @return The corresponding scan type for the API
     */
    private String getScanTypeFromDisplayText(String displayText) {
        return switch (displayText.toLowerCase()) {
            case "business card" -> "business";
            case "book" -> "book";
            case "document" -> "document";
            default -> "id";
        };
    }

    /**
     * Manages document retrieval by scan type with structured error handling
     *
     * @param scanType The type of scan to filter by (e.g., "id", "book", "document", "business")
     */
    private void manageDocumentsByScanTypeApiCall(String scanType) {
        if (!NetworkUtils.isInternetAvailable(requireContext())) {
            Toast.makeText(requireContext(), getString(R.string.no_internet), Toast.LENGTH_SHORT).show();
            return;
        }

        LoaderHelper.showLoader(requireActivity(), true);
        Log.d("API_CALL", "Calling getDocumentsByScanType for type: " + scanType);

        Call<List<GetAllDocumentsResponseItem>> call = apiService.getDocumentsByScanType(scanType);

        call.enqueue(new Callback<>() {
            @Override
            public void onResponse(@NonNull Call<List<GetAllDocumentsResponseItem>> call, @NonNull retrofit2.Response<List<GetAllDocumentsResponseItem>> response) {
                LoaderHelper.hideLoader();

                if (getContext() == null) return; // Check if fragment is stillfavo attached

                Log.d("API_RESPONSE", "HTTP Status: " + response.code());

                if (response.isSuccessful() && response.body() != null) {
                    List<GetAllDocumentsResponseItem> documents = response.body();

                    // Log the response size and first item if available
                    Log.d("API_RESPONSE", "Documents found: " + documents.size());
                    if (!documents.isEmpty()) {
                        GetAllDocumentsResponseItem firstItem = documents.get(0);
                        Log.d("API_RESPONSE", "First item - ID: " + firstItem.getId() + ", Name: " + firstItem.getDocumentName() + ", Type: " + firstItem.getScanType());
                    }

                    handleSuccessfulResponse(documents);
                } else {
                    try {
                        Log.e("API_RESPONSE", "Unsuccessful response code: " + response.code());
                        if (response.errorBody() != null) {
                            String errorBodyString = response.errorBody().string();
                            Log.e("API_RESPONSE", "Error body: " + errorBodyString);

                            Gson gson = new Gson();
                            ErrorResponse errorResponse = gson.fromJson(errorBodyString, ErrorResponse.class);
                            String errorMessage = errorResponse != null && errorResponse.getDetail() != null ? errorResponse.getDetail() : getString(R.string.default_error_message);

                            Toast.makeText(getContext(), errorMessage, Toast.LENGTH_LONG).show();
                        } else {
                            Log.e("API_RESPONSE", "Error body is null");
                            Toast.makeText(getContext(), getString(R.string.default_error_message), Toast.LENGTH_LONG).show();
                        }

                        includeNoHistory.setVisibility(View.VISIBLE);
                        recyclerView.setVisibility(View.GONE);
                    } catch (Exception e) {
                        Log.e("API_RESPONSE", "Exception in error handling: " + e.getMessage());
                        Toast.makeText(getContext(), getString(R.string.default_error_message), Toast.LENGTH_LONG).show();
                        includeNoHistory.setVisibility(View.VISIBLE);
                        recyclerView.setVisibility(View.GONE);
                    }
                }
            }

            @Override
            public void onFailure(@NonNull Call<List<GetAllDocumentsResponseItem>> call, @NonNull Throwable t) {
                LoaderHelper.hideLoader();
                if (getContext() == null) return;

                Log.e("API_RESPONSE", "API call failed: " + t.getMessage());
                Toast.makeText(getContext(), getString(R.string.network_error), Toast.LENGTH_LONG).show();
                includeNoHistory.setVisibility(View.VISIBLE);
                recyclerView.setVisibility(View.GONE);
            }
        });
    }

    /**
     * Handles successful response from the documents API call
     *
     * @param documents List of ResponseItem objects from filter API
     */
    private void handleSuccessfulResponse(List<GetAllDocumentsResponseItem> documents) {
        if (documents == null || documents.isEmpty()) {
            includeNoHistory.setVisibility(View.VISIBLE);
            recyclerView.setVisibility(View.GONE);
        } else {
            includeNoHistory.setVisibility(View.GONE);
            recyclerView.setVisibility(View.VISIBLE);

            // Convert ResponseItem to a format compatible with your adapter
            ScanHistoryAdapter adapter = new ScanHistoryAdapter(getContext(), documents, this::launchDetailActivity);
            adapter.setOnDeleteClickListener(HomeFragment.this::showDeleteConfirmationDialog);
            adapter.setOnFavoriteClickListener(HomeFragment.this::toggleFavoriteStatus);
            adapter.setOnSaveClickListener(this::showSaveBottomSheet);
            adapter.setOnRenameClickListener(this::renameBottomSheet);
            recyclerView.setAdapter(adapter);
        }
    }

    private void searchDocumentsFromApi(String query) {
        if (query == null || query.trim().isEmpty()) {
            Toast.makeText(getContext(), "Search query cannot be empty", Toast.LENGTH_SHORT).show();
            initializeScanHistoryView();
            return;
        }

        LoaderHelper.showLoader(requireActivity(), true);
        Log.d("API_CALL", "Calling searchDocuments with query: " + query);

        apiService.searchDocuments(query).enqueue(new Callback<>() {
            @Override
            public void onResponse(@NonNull Call<ResponseBody> call, @NonNull Response<ResponseBody> response) {
                LoaderHelper.hideLoader();

                if (getContext() == null) return;

                if (response.isSuccessful() && response.body() != null) {
                    try {
                        String responseBody = response.body().string(); // Consume the response
                        Log.d("API_RESPONSE", "Raw response: " + responseBody);

                        // 🔁 If you want to parse it as a List of your model:
                        Gson gson = new Gson();
                        Type listType = new TypeToken<List<GetAllDocumentsResponseItem>>() {
                        }.getType();
                        List<GetAllDocumentsResponseItem> searchResults = gson.fromJson(responseBody, listType);

                        if (searchResults.isEmpty()) {
                            includeNoHistory.setVisibility(View.VISIBLE);
                            recyclerView.setVisibility(View.GONE);
                            Toast.makeText(getContext(), "No results found", Toast.LENGTH_SHORT).show();
                        } else {
                            includeNoHistory.setVisibility(View.GONE);
                            recyclerView.setVisibility(View.VISIBLE);

                            ScanHistoryAdapter adapter = new ScanHistoryAdapter(getContext(), searchResults, HomeFragment.this::launchDetailActivity);
                            adapter.setOnDeleteClickListener(HomeFragment.this::showDeleteConfirmationDialog);
                            adapter.setOnFavoriteClickListener(HomeFragment.this::toggleFavoriteStatus);
                            adapter.setOnSaveClickListener(HomeFragment.this::showSaveBottomSheet);
                            adapter.setOnRenameClickListener(HomeFragment.this::renameBottomSheet);
                            recyclerView.setAdapter(adapter);
                        }
                    } catch (Exception e) {
                        Log.e("API_PARSE_ERROR", "Failed to parse search response", e);
                        Toast.makeText(getContext(), "Failed to load search results", Toast.LENGTH_SHORT).show();
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

    private void handleErrorResponse(Response<?> response) {
        try {
            Log.e("API_SEARCH", "Search failed: " + response.message());
            if (response.errorBody() != null) {
                String errorBodyString = response.errorBody().string();
                Log.e("API_SEARCH", "Error body: " + errorBodyString);

                try {
                    Gson gson = new Gson();
                    ErrorResponse errorResponse = gson.fromJson(errorBodyString, ErrorResponse.class);
                    String errorMessage = errorResponse != null && errorResponse.getDetail() != null ? errorResponse.getDetail() : getString(R.string.default_error_message);

                    Log.e("API_SEARCH", "Parsed error message: " + errorMessage);
                    Toast.makeText(getContext(), errorMessage, Toast.LENGTH_LONG).show();
                } catch (Exception jsonEx) {
                    Log.e("API_SEARCH", "Error parsing JSON response", jsonEx);
                    Toast.makeText(getContext(), getString(R.string.default_error_message), Toast.LENGTH_LONG).show();
                }
            } else {
                Toast.makeText(getContext(), "Search failed: " + response.message(), Toast.LENGTH_SHORT).show();
            }

            includeNoHistory.setVisibility(View.VISIBLE);
            recyclerView.setVisibility(View.GONE);
        } catch (Exception e) {
            Log.e("API_SEARCH", "Exception in error handling", e);
            Toast.makeText(getContext(), getString(R.string.default_error_message), Toast.LENGTH_LONG).show();
            includeNoHistory.setVisibility(View.VISIBLE);
            recyclerView.setVisibility(View.GONE);
        }
    }

    private void handleApiFailure(Throwable t) {
        LoaderHelper.hideLoader();

        if (getContext() == null) return;

        Log.e("API_SEARCH", "Search API call failed", t);
        Toast.makeText(getContext(), getString(R.string.network_error), Toast.LENGTH_LONG).show();
        includeNoHistory.setVisibility(View.VISIBLE);
        recyclerView.setVisibility(View.GONE);
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

            apiService.deleteDocument(documentId).enqueue(new Callback<>() {
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
                        // Refresh document list to show updated data
                        initializeScanHistoryView();
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

    /**
     * Shows a custom delete confirmation dialog
     *
     * @param documentId The ID of the document to delete
     */
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
                // Don't use String.valueOf here as documentId is already a String
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

    /**
     * Toggles the favorite status of a document and updates it through the API
     *
     * @param documentId The ID of the document to update
     * @param isFavorite The new favorite status to set
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

        call.enqueue(new Callback<>() {
            @Override
            public void onResponse(@NonNull Call<UpdateDocumentResponse> call, @NonNull Response<UpdateDocumentResponse> response) {
                LoaderHelper.hideLoader();
                Log.d("FavoriteUpdate", "Received API response. Status code: " + response.code());

                if (getContext() == null) {
                    Log.w("FavoriteUpdate", "Fragment no longer attached, aborting update");
                    // Check if fragment is still attached
                } else {
                    if (response.isSuccessful() && response.body() != null) {
                        String successMessage = isFavorite ? "Document added to favorites" : "Document removed from favorites";
                        Log.d("FavoriteUpdate", "Update successful: " + successMessage);
                        Toast.makeText(getContext(), successMessage, Toast.LENGTH_SHORT).show();

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

    private void bookNameUpdateApiCall(GetAllDocumentsResponseItem documentsResponseItem) {
        // Show loading indicator
        LoaderHelper.showLoader(getActivity(), true);

        // Check network connectivity
        if (!NetworkUtils.isInternetAvailable(getActivity())) {
            LoaderHelper.hideLoader();
            Toast.makeText(getActivity(), getString(R.string.no_internet), Toast.LENGTH_SHORT).show();
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
                Toast.makeText(getActivity(),
                        "Error: " + t.getMessage(),
                        Toast.LENGTH_SHORT).show();
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
                Toast.makeText(getActivity(),
                        "Error: " + t.getMessage(),
                        Toast.LENGTH_SHORT).show();
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

        final boolean favorite = documentsResponseItem.isFavorite();

        // For boolean values
        RequestBody isFavoriteBody = RequestBody.create(
                okhttp3.MediaType.parse("text/plain"), favorite ? "true" : "false");

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
                Toast.makeText(getActivity(),
                        "Error: " + t.getMessage(),
                        Toast.LENGTH_SHORT).show();
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

    String finalDesc = "" ;
    private void showSaveBottomSheet(GetAllDocumentsResponseItem document) {
        @SuppressLint("InflateParams") View view = LayoutInflater.from(getActivity()).inflate(R.layout.layout_export_popup, null);

        BottomSheetDialog dialog = new BottomSheetDialog(getActivity());
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
}
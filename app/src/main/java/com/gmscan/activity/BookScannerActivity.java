package com.gmscan.activity;

import static com.gmscan.service.RestApiBuilder.BASE_URL_OPEN_LIB;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.util.Log;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.bumptech.glide.Glide;
import com.gmscan.R;
import com.gmscan.model.CreateDocuments.CreateDoc;
import com.gmscan.model.getAllDocuments.ScannedDocument;
import com.gmscan.model.openbooksresponse.AuthorsItem;
import com.gmscan.model.openbooksresponse.BooksResponse;
import com.gmscan.model.openbooksresponse.SubjectsItem;
import com.gmscan.service.RestApiBuilder;
import com.gmscan.service.RestApiService;
import com.gmscan.utility.BarcodeScannerUtil;
import com.gmscan.utility.DocumentType;
import com.gmscan.utility.LoaderHelper;
import com.gmscan.utility.NetworkUtils;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.gson.Gson;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

import okhttp3.MediaType;
import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Main activity for scanning books using the device's camera.
 * Handles barcode scanning, book information retrieval, and UI updates.
 */
public class BookScannerActivity extends AppCompatActivity implements BarcodeScannerUtil.BarcodeDetectionCallback {

    private static final String TAG = "BookScannerActivity";
    private static final int DETECTION_DELAY_MS = 100;

    // Camera and processing related fields
    private ProcessCameraProvider cameraProvider;
    private PreviewView previewView;
    private ImageCapture imageCapture;
    private ExecutorService cameraExecutor;
    private Handler delayHandler;

    private BarcodeScannerUtil barcodeScannerUtil;

    // UI elements
    private EditText bookTitleEditText;
    private EditText authorEditText;
    private EditText publishDateEditText;
    private EditText pagesEditText;
    private EditText subjectEditText;
    private EditText isbnEditText;
    private ImageView bookCoverPreviewImageView;

    // State management
    private boolean isCameraFrozen = false;

    // Permissions
    private static final String[] REQUIRED_PERMISSIONS;
    private final ActivityResultLauncher<String[]> permissionLauncher;

    static {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            REQUIRED_PERMISSIONS = new String[]{Manifest.permission.CAMERA, Manifest.permission.READ_MEDIA_IMAGES};
        } else {
            REQUIRED_PERMISSIONS = new String[]{Manifest.permission.CAMERA, Manifest.permission.READ_EXTERNAL_STORAGE};
        }
    }

    public BookScannerActivity() {
        // Register permission launcher
        permissionLauncher = registerForActivityResult(new ActivityResultContracts.RequestMultiplePermissions(), this::handlePermissionResult);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.layout_activity_book_scanner);

        // Initialize handlers and executors
        delayHandler = new Handler(Looper.getMainLooper());
        cameraExecutor = Executors.newSingleThreadExecutor();

        // Initialize services and utilities
        // Utility and service classes
        barcodeScannerUtil = new BarcodeScannerUtil(this, this);

        // Setup UI and request permissions
        initializeViews();
        checkAndRequestPermissions();
    }

    /**
     * Initializes all UI elements and sets up listeners
     */
    private void initializeViews() {
        // Find UI elements
        previewView = findViewById(R.id.cameraPreview);
        bookTitleEditText = findViewById(R.id.bookTitleEditText);
        authorEditText = findViewById(R.id.authorEditText);
        publishDateEditText = findViewById(R.id.publishDateEditText);
        pagesEditText = findViewById(R.id.pagesEditText);
        subjectEditText = findViewById(R.id.subjectEditText);
        isbnEditText = findViewById(R.id.isbnEditText);
        bookCoverPreviewImageView = findViewById(R.id.bookCoverImageView);

        // Setup action bar
        TextView txtTitle = findViewById(R.id.txtTitle);
        ImageView imgBack = findViewById(R.id.imgBack);
        txtTitle.setText(getString(R.string.book_scan));
        imgBack.setOnClickListener(v -> finish());
    }

    /**
     * Check and request camera permissions
     */
    private void checkAndRequestPermissions() {
        if (!allPermissionsGranted()) {
            permissionLauncher.launch(REQUIRED_PERMISSIONS);
        } else {
            startCamera();
        }
    }

    /**
     * Start the camera and set up image analysis
     */
    private void startCamera() {
        if (isCameraFrozen) return;

        ListenableFuture<ProcessCameraProvider> cameraProviderFuture = ProcessCameraProvider.getInstance(this);

        cameraProviderFuture.addListener(() -> {
            try {
                cameraProvider = cameraProviderFuture.get();

                // Setup camera preview
                Preview preview = new Preview.Builder().build();
                preview.setSurfaceProvider(previewView.getSurfaceProvider());

                // Setup image capture
                imageCapture = new ImageCapture.Builder().setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY).build();

                // Setup image analysis
                ImageAnalysis imageAnalysis = new ImageAnalysis.Builder().setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST).build();

                imageAnalysis.setAnalyzer(cameraExecutor, barcodeScannerUtil::processImageProxy);

                // Bind use cases to camera
                cameraProvider.unbindAll();
                cameraProvider.bindToLifecycle(this, CameraSelector.DEFAULT_BACK_CAMERA, preview, imageCapture, imageAnalysis);

            } catch (Exception e) {
                Log.e(TAG, "Camera initialization failed", e);
                Toast.makeText(this, "Camera initialization failed", Toast.LENGTH_SHORT).show();
            }
        }, ContextCompat.getMainExecutor(this));
    }

    /**
     * Freeze the camera to prevent continuous scanning
     */
    private void freezeCamera() {
        isCameraFrozen = true;
        if (cameraProvider != null) {
            cameraProvider.unbindAll();
        }
    }

    @Override
    public void onBarcodeDetected(String barcodeValue) {
        searchBookByIsbn(barcodeValue);
    }

    @Override
    public void onScanningFailed(Exception e) {
        Log.e(TAG, "Barcode scanning failed", e);
        runOnUiThread(() -> Toast.makeText(this, "Scanning failed", Toast.LENGTH_SHORT).show());
    }

    /**
     * Load image using Glide library
     */
    private void loadImageWithGlide(String imageUrl, ImageView imageView) {
        try {
            Glide.with(this).load(imageUrl).placeholder(R.drawable.ic_placeholderbook).error(R.drawable.ic_placeholderbook).into(imageView);
        } catch (Exception e) {
            Log.e(TAG, "Error loading image: " + e.getMessage());
        }
    }

    /**
     * Handle permission request results
     */
    private void handlePermissionResult(Map<String, Boolean> result) {
        boolean allGranted = true;
        List<String> deniedPermissions = new ArrayList<>();

        for (Map.Entry<String, Boolean> entry : result.entrySet()) {
            if (!entry.getValue()) {
                allGranted = false;
                deniedPermissions.add(entry.getKey());
            }
        }

        if (allGranted) {
            startCamera();
        } else {
            if (shouldShowRequestPermissionRationale(deniedPermissions)) {
                showPermissionExplanationDialog(deniedPermissions);
            } else {
                showSettingsDialog();
            }
        }
    }

    /**
     * Check if permission rationale should be shown
     */
    private boolean shouldShowRequestPermissionRationale(List<String> permissions) {
        for (String permission : permissions) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(this, permission)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Show dialog explaining why permissions are needed
     */
    private void showPermissionExplanationDialog(List<String> permissions) {
        new AlertDialog.Builder(this).setTitle("Permissions Required").setMessage("This app needs camera and storage permissions to scan books and access gallery images.").setPositiveButton("Grant", (dialog, which) -> permissionLauncher.launch(permissions.toArray(new String[0]))).setNegativeButton("Cancel", (dialog, which) -> finish()).create().show();
    }

    /**
     * Show dialog to direct user to app settings for permissions
     */
    private void showSettingsDialog() {
        new AlertDialog.Builder(this).setTitle("Permissions Required").setMessage("Required permissions have been denied. Please enable them in app settings.").setPositiveButton("Settings", (dialog, which) -> {
            Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
            Uri uri = Uri.fromParts("package", getPackageName(), null);
            intent.setData(uri);
            startActivity(intent);
        }).setNegativeButton("Cancel", (dialog, which) -> finish()).create().show();
    }

    /**
     * Check if all required permissions are granted
     */
    private boolean allPermissionsGranted() {
        for (String permission : REQUIRED_PERMISSIONS) {
            if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
        }
        return true;
    }

    @Override
    protected void onResume() {
        super.onResume();
        isCameraFrozen = false;
        if (allPermissionsGranted()) {
            startCamera();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (cameraProvider != null) {
            cameraProvider.unbindAll();
        }
        // Remove any pending delay callbacks
        if (delayHandler != null) {
            delayHandler.removeCallbacksAndMessages(null);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Shutdown executors and remove handlers
        if (cameraExecutor != null) {
            cameraExecutor.shutdown();
        }
        if (delayHandler != null) {
            delayHandler.removeCallbacksAndMessages(null);
            delayHandler = null;
        }
    }

    public void searchBookByIsbn(String isbn) {
        String isbnKey = "ISBN:" + isbn;
        String url = BASE_URL_OPEN_LIB + "api/books?format=json&jscmd=data&bibkeys=" + isbnKey;

        Log.d(TAG, "Request URL: " + url);

        RequestQueue requestQueue = Volley.newRequestQueue(BookScannerActivity.this);

        StringRequest request = new StringRequest(Request.Method.GET, url, response -> {
            try {
                JSONObject jsonObject = new JSONObject(response);
                Log.d(TAG, "jsonObject: " + jsonObject);
                JSONObject bookObject = jsonObject.optJSONObject(isbnKey);

                if (bookObject != null) {
                    Gson gson = new Gson();
                    BooksResponse book = gson.fromJson(bookObject.toString(), BooksResponse.class);
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        processIsbnBookDetails(book, isbn);
                    }
                } else {
                    Log.w(TAG, "Book not found. Fallback to title search.");
                }
            } catch (JSONException e) {
                Log.e(TAG, "JSON Parsing error", e);
            }
        }, error -> Log.e(TAG, "Volley Error: " + error.getMessage(), error));

        requestQueue.add(request);
    }


    @RequiresApi(api = Build.VERSION_CODES.O)
    private void processIsbnBookDetails(BooksResponse bookDetails, String isbn) {
        ScannedDocument scannedDocument = new ScannedDocument();
        scannedDocument.setName(bookDetails.getTitle());
        scannedDocument.setOcrText(formatBookDetails(bookDetails, isbn));
        scannedDocument.setIsbn(isbn);
        scannedDocument.setScanType(DocumentType.BOOK.getDisplayName());
        if (bookDetails.getCover() != null) {
            scannedDocument.setFileUrl(bookDetails.getCover().getLarge());
        }
        runOnUiThread(() -> {
            // Update UI with book details
            isbnEditText.setText(scannedDocument.getIsbn());
            bookTitleEditText.setText(scannedDocument.getName());

            // Parse and set other details from OCR text
            String[] details = scannedDocument.getOcrText().split("\n");
            for (String detail : details) {
                if (detail.startsWith("Author Name:")) {
                    authorEditText.setText(detail.replace("Author Name:", "").trim());
                } else if (detail.startsWith("Publish")) {
                    publishDateEditText.setText(detail.split(":")[1].trim());
                } else if (detail.startsWith("Number of Pages:")) {
                    pagesEditText.setText(detail.replace("Number of Pages:", "").trim());
                } else if (detail.startsWith("Subject:")) {
                    subjectEditText.setText(detail.replace("Subject:", "").trim());
                }
            }

            loadImageWithGlide(scannedDocument.getFileUrl(), bookCoverPreviewImageView);

            // Start delay to freeze camera
            delayHandler.postDelayed(() -> {
                freezeCamera();
                runOnUiThread(() -> Toast.makeText(this, "Scan complete!", Toast.LENGTH_SHORT).show());
                createBookDocumentApiCall(scannedDocument);
            }, DETECTION_DELAY_MS);
        });
    }

    /**
     * Makes an API call to create a book document on the server
     * Similar to the implementation in DocumentScanningActivity
     */
    private void createBookDocumentApiCall(ScannedDocument scannedDocument) {
        if (!NetworkUtils.isInternetAvailable(this)) {
            Toast.makeText(this, getString(R.string.no_internet), Toast.LENGTH_SHORT).show();
            return;
        }
        LoaderHelper.showLoader(this, true);

        RestApiService apiService = RestApiBuilder.getService();

        // Create CreateDoc object with appropriate fields
        CreateDoc createDocRequest = new CreateDoc();

        // Set book-specific fields
        String bookName = scannedDocument.getName() != null ? scannedDocument.getName() : "Unknown";
        createDocRequest.setDocumentName("Book - " + bookName);
        createDocRequest.setBookName(bookName);
        createDocRequest.setAuthorName(authorEditText.getText().toString());
        createDocRequest.setIsbnNo(scannedDocument.getIsbn());
        createDocRequest.setNumberOfPages(pagesEditText.getText().toString());
        createDocRequest.setSubject(subjectEditText.getText().toString());
        createDocRequest.setScanType("book");
        createDocRequest.setFileUrl(scannedDocument.getFileUrl());

        String isbnStr = scannedDocument.getIsbn();
        if (isbnStr == null || isbnStr.trim().isEmpty()) {
            isbnStr = "N/A";
        }
        RequestBody isbnNumber = RequestBody.create(isbnStr, MediaType.parse("text/plain"));
        RequestBody documentName = RequestBody.create(bookName, MediaType.parse("text/plain"));
        RequestBody bookNameRq = RequestBody.create(scannedDocument.getOcrText(), MediaType.parse("text/plain"));
        RequestBody scanType = RequestBody.create("book", MediaType.parse("text/plain"));
        RequestBody fileUrl;
        if (createDocRequest.getFileUrl() != null) {
            fileUrl = RequestBody.create(createDocRequest.getFileUrl(), MediaType.parse("text/plain"));
        } else {
            fileUrl = RequestBody.create(getString(R.string.bookDummyUrl), MediaType.parse("text/plain"));
        }

        String publication = publishDateEditText.getText().toString().trim();
        String authorName = authorEditText.getText().toString().trim();
        String numberOfPages = pagesEditText.getText().toString().trim();

        if (numberOfPages.isEmpty()) {
            numberOfPages = "0";
        }

        String subject = subjectEditText.getText().toString().trim();
        if (subject.isEmpty()) {
            subject = "General";
        }

        RequestBody numberOfPagesReq = RequestBody.create(numberOfPages, MediaType.parse("text/plain"));
        RequestBody authorNameReq = RequestBody.create(authorName, MediaType.parse("text/plain"));
        RequestBody publicationReq = RequestBody.create(publication, MediaType.parse("text/plain"));
        RequestBody subjectReq = RequestBody.create(subject, MediaType.parse("text/plain"));


        // Make API call with multipart data
        Call<ResponseBody> call = apiService.createBook(isbnNumber, bookNameRq, documentName, scanType, numberOfPagesReq, subjectReq, authorNameReq, publicationReq, fileUrl);

        call.enqueue(new Callback<>() {
            @Override
            public void onResponse(@NonNull Call<ResponseBody> call, @NonNull Response<ResponseBody> response) {

                LoaderHelper.hideLoader();

                if (response.isSuccessful()) {
                    Toast.makeText(BookScannerActivity.this, getString(R.string.document_created_successfully), Toast.LENGTH_SHORT).show();
                    // Navigate or finish as needed
                    Intent resultIntent = new Intent();
                    setResult(RESULT_OK, resultIntent);
                    finish();
                } else {
                    Log.e(TAG, "Error creating book document: " + response.code());
                    try {
                        if (response.errorBody() != null) {
                            Log.e(TAG, "Error body: " + response.errorBody().string());
                        }
                    } catch (IOException e) {
                        Log.e(TAG, "Error reading error body", e);
                    }
                    Toast.makeText(BookScannerActivity.this, getString(R.string.error_creating_document), Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(@NonNull Call<ResponseBody> call, @NonNull Throwable t) {
                Log.e(TAG, "API call failed", t);
                Toast.makeText(BookScannerActivity.this, getString(R.string.error_creating_document), Toast.LENGTH_SHORT).show();
                LoaderHelper.hideLoader();
            }
        });
    }

    private String formatBookDetails(BooksResponse bookDetails, String isbn) {
        List<AuthorsItem> authorsItems = bookDetails.getAuthors();
        List<SubjectsItem> subjectsItems = bookDetails.getSubjects();

        String authors = (authorsItems != null && !authorsItems.isEmpty()) ? authorsItems.stream().map(AuthorsItem::getName).collect(Collectors.joining(", ")) : "Unknown Author";

        String subjects = (subjectsItems != null && !subjectsItems.isEmpty()) ? subjectsItems.stream().map(SubjectsItem::getName).collect(Collectors.joining(", ")) : "No subjects available";

        return "Book Name: " + bookDetails.getTitle() + "\n" + "Author Name: " + authors + "\n" + "ISBN: " + isbn + "\n" + "Publish Date: " + bookDetails.getPublishDate() + "\n" + "Number of Pages: " + bookDetails.getNumberOfPages() + "\n" + "Subject: " + subjects;
    }

}
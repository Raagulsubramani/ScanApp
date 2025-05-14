package com.gmscan.utility;

import android.content.Context;
import android.util.Log;

import androidx.annotation.OptIn;
import androidx.camera.core.ExperimentalGetImage;
import androidx.camera.core.ImageProxy;

import com.google.mlkit.vision.barcode.BarcodeScanner;
import com.google.mlkit.vision.barcode.BarcodeScannerOptions;
import com.google.mlkit.vision.barcode.BarcodeScanning;
import com.google.mlkit.vision.barcode.common.Barcode;
import com.google.mlkit.vision.common.InputImage;

/**
 * Utility class for scanning barcodes using ML Kit
 */
public class BarcodeScannerUtil {
    private static final String TAG = "BarcodeScannerUtil";
    private final BarcodeScanner scanner;
    private final BarcodeDetectionCallback callback;
    private boolean barcodeProcessed = false; // Flag to ensure barcode is processed only once

    public interface BarcodeDetectionCallback {
        void onBarcodeDetected(String barcodeValue);
        void onScanningFailed(Exception e);
    }

    public BarcodeScannerUtil(Context context, BarcodeDetectionCallback callback) {
        BarcodeScannerOptions options = new BarcodeScannerOptions.Builder()
                .setBarcodeFormats(Barcode.FORMAT_EAN_13, Barcode.FORMAT_EAN_8, Barcode.TYPE_ISBN)
                .build();

        this.scanner = BarcodeScanning.getClient(options);
        this.callback = callback;
    }

    @OptIn(markerClass = ExperimentalGetImage.class)
    public void processImageProxy(ImageProxy imageProxy) {
        if (imageProxy == null) {
            Log.e(TAG, "Null ImageProxy");
            return;
        }

        if (barcodeProcessed) {
            // If barcode was already processed, return early to prevent further processing
            imageProxy.close();
            return;
        }

        android.media.Image mediaImage = imageProxy.getImage();
        if (mediaImage != null) {
            InputImage image = InputImage.fromMediaImage(
                    mediaImage,
                    imageProxy.getImageInfo().getRotationDegrees()
            );

            scanner.process(image)
                    .addOnSuccessListener(barcodes -> {
                        for (Barcode barcode : barcodes) {
                            String rawValue = barcode.getRawValue();
                            if (rawValue != null && rawValue.matches("^\\d{10}|\\d{13}$")) {
                                Log.d(TAG, "ISBN found: " + rawValue);
                                callback.onBarcodeDetected(rawValue);
                                barcodeProcessed = true; // Mark barcode as processed
                                break; // Exit loop after processing first barcode
                            }
                        }
                    })
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "Barcode scanning failed", e);
                        callback.onScanningFailed(e);
                    })
                    .addOnCompleteListener(task -> {
                        // Close the ImageProxy after processing
                        imageProxy.close();
                    });
        } else {
            imageProxy.close();
        }
    }
}
package com.gmscan.utility;

import android.content.Context;
import android.content.Intent;

public class ShareUtils {

    // Share a document link with description based on document type
    public static void shareDocumentUrl(Context context, String url, String docType) {
        String description = getDescriptionForType(docType);

        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setType("text/plain");
        shareIntent.putExtra(Intent.EXTRA_SUBJECT, "Shared via GMScan");
        shareIntent.putExtra(Intent.EXTRA_TEXT, description + "\n\n" + url);
        context.startActivity(Intent.createChooser(shareIntent, "Share " + docType));
    }

    // Get description text based on type
    private static String getDescriptionForType(String docType) {
        return switch (docType.toLowerCase()) {
            case "id" -> "Here's a scanned ID card from GMScan.";
            case "business" -> "Here's a business card I scanned using GMScan.";
            case "book" -> "Scanned pages from a book using GMScan.";
            case "document" -> "Scanned document shared via GMScan.";
            default -> "Shared via GMScan.";
        };
    }
}


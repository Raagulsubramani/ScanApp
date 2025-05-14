package com.gmscan.utility;

import android.content.ContentValues;
import android.content.Context;
import android.graphics.pdf.PdfDocument;
import android.net.Uri;
import android.os.Environment;
import android.provider.MediaStore;
import android.widget.Toast;

import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class PdfUtil {

    /**
     * Creates a PDF document from the extracted texts.
     *
     * @param extractedTexts List of extracted text blocks
     * @return PdfDocument containing all extracted texts
     */
    public static PdfDocument createPdfDocument(List<String> extractedTexts) {
        PdfDocument pdfDocument = new PdfDocument();

        // Create a page for each extracted text
        for (int i = 0; i < extractedTexts.size(); i++) {
            PdfDocument.PageInfo pageInfo = new PdfDocument.PageInfo.Builder(595, 842, i + 1).create();
            PdfDocument.Page page = pdfDocument.startPage(pageInfo);

            android.graphics.Canvas canvas = page.getCanvas();
            android.graphics.Paint paint = new android.graphics.Paint();
            paint.setTextSize(12);

            // Write text lines on PDF page
            String[] lines = extractedTexts.get(i).split("\n");
            int y = 50;
            for (String line : lines) {
                canvas.drawText(line, 50, y, paint);
                y += 20;
            }
            pdfDocument.finishPage(page);
        }

        return pdfDocument;
    }

    /**
     * Generate a PDF from all extracted texts.
     * Saves PDF in the device's Downloads directory and returns the URI.
     *
     * @param extractedTexts List of extracted text blocks
     * @param context Context to show Toast
     * @return Uri of the saved PDF
     */
    public static Uri generatePDF(List<String> extractedTexts, Context context) {
        PdfDocument pdfDocument = createPdfDocument(extractedTexts);
        Uri pdfUri = null;

        try {
            // Prepare to insert into the MediaStore
            ContentValues contentValues = new ContentValues();
            contentValues.put(MediaStore.Images.Media.DISPLAY_NAME, "GM_Scan_" + new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date()) + ".pdf");
            contentValues.put(MediaStore.Images.Media.MIME_TYPE, "application/pdf");
            contentValues.put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS);

            // Insert the PDF into the MediaStore
            Uri pdfCollection = MediaStore.Files.getContentUri("external");
            pdfUri = context.getContentResolver().insert(pdfCollection, contentValues);

            if (pdfUri != null) {
                // Write the PDF to the content resolver
                try (FileOutputStream fos = (FileOutputStream) context.getContentResolver().openOutputStream(pdfUri)) {
                    pdfDocument.writeTo(fos);
                    Toast.makeText(context, "PDF saved to: " + pdfUri, Toast.LENGTH_LONG).show();
                }
            } else {
                throw new IOException("Failed to create PDF URI.");
            }

        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(context, "Failed to generate PDF", Toast.LENGTH_SHORT).show();
        }

        pdfDocument.close();
        return pdfUri;
    }
}

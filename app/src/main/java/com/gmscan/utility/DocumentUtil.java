package com.gmscan.utility;

import android.content.ContentValues;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.pdf.PdfDocument;
import android.net.Uri;
import android.os.Environment;
import android.provider.MediaStore;
import android.widget.Toast;

import java.io.IOException;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class DocumentUtil {

    /**
     * Creates a Bitmap image from the extracted texts.
     *
     * @param extractedTexts List of extracted text blocks
     * @return Bitmap containing all extracted texts
     */
    public static Bitmap createImage(List<String> extractedTexts) {
        int width = 800; // Define the width of the image
        int height = 1200; // Define the height of the image
        Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);

        // Set background color
        canvas.drawColor(Color.WHITE);

        Paint paint = new Paint();
        paint.setColor(Color.BLACK); // Text color
        paint.setAntiAlias(true);

        // Set initial text size
        int textSize = 20;
        paint.setTextSize(textSize);

        // Calculate the maximum number of lines that fit vertically
        int maxWidth = width - 100; // 50 padding on each side
        int lineHeight = 30; // Line spacing

        int y = 50; // Starting position for text
        boolean textExceeds = false;

        for (String text : extractedTexts) {
            String[] lines = text.split("\n");

            for (String line : lines) {
                // Check if the line exceeds the available width
                if (paint.measureText(line) > maxWidth) {
                    // If line is too long, split it into words and break lines dynamically
                    String[] words = line.split(" ");
                    StringBuilder tempLine = new StringBuilder();

                    for (String word : words) {
                        if (paint.measureText(tempLine + " " + word) <= maxWidth) {
                            tempLine.append(" ").append(word);
                        } else {
                            // Draw the current line and move to the next line
                            canvas.drawText(tempLine.toString().trim(), 50, y, paint);
                            y += lineHeight;
                            tempLine = new StringBuilder(word); // Start new line with the current word
                        }
                    }

                    // Draw the remaining text for the line
                    if (tempLine.length() > 0) {
                        canvas.drawText(tempLine.toString(), 50, y, paint);
                        y += lineHeight;
                    }
                } else {
                    // If the line fits within the width, just draw it
                    canvas.drawText(line, 50, y, paint);
                    y += lineHeight;
                }

                // Check if we've exceeded the height of the image
                if (y > height - 50) {
                    textExceeds = true;
                    break;
                }
            }

            if (textExceeds) {
                break; // Exit if text exceeds available space
            }
        }

        return bitmap;
    }

    /**
     * Generate an image from extracted texts and save it.
     * Saves the image in the device's Pictures directory and returns the URI.
     *
     * @param extractedTexts List of extracted text blocks
     * @param context Context to show Toast
     * @return Uri of the saved image
     */
    public static Uri generateImage(List<String> extractedTexts, Context context) {
        Bitmap bitmap = createImage(extractedTexts);

        // Prepare to insert into MediaStore
        ContentValues contentValues = new ContentValues();
        contentValues.put(MediaStore.Images.Media.DISPLAY_NAME, "GM_Scan_" + new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date()) + ".png");
        contentValues.put(MediaStore.Images.Media.MIME_TYPE, "image/png");
        contentValues.put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES);

        // Insert the image into MediaStore
        Uri imageCollection = MediaStore.Images.Media.getContentUri("external");
        Uri imageUri = context.getContentResolver().insert(imageCollection, contentValues);

        if (imageUri != null) {
            try (OutputStream fos = context.getContentResolver().openOutputStream(imageUri)) {
                if (fos != null) {
                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, fos);
                    fos.flush();
                    Toast.makeText(context, "Image saved to: " + imageUri, Toast.LENGTH_LONG).show();
                }
            } catch (IOException e) {
                e.printStackTrace();
                Toast.makeText(context, "Failed to generate image", Toast.LENGTH_SHORT).show();
            }
        } else {
            Toast.makeText(context, "Failed to create image URI.", Toast.LENGTH_SHORT).show();
        }

        return imageUri;
    }

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

            Canvas canvas = page.getCanvas();
            Paint paint = new Paint();
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
            contentValues.put(MediaStore.Files.FileColumns.DISPLAY_NAME, "GM_Scan_" + new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date()) + ".pdf");
            contentValues.put(MediaStore.Files.FileColumns.MIME_TYPE, "application/pdf");
            contentValues.put(MediaStore.Files.FileColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS);

            // Insert the PDF into the MediaStore
            Uri pdfCollection = MediaStore.Files.getContentUri("external");
            pdfUri = context.getContentResolver().insert(pdfCollection, contentValues);

            if (pdfUri != null) {
                // Write the PDF to the content resolver
                try (OutputStream fos = context.getContentResolver().openOutputStream(pdfUri)) {
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
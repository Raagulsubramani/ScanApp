package com.gmscan.utility;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.RequestOptions;
import com.bumptech.glide.request.target.Target;
import com.gmscan.R;

import android.content.ContentValues;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Environment;
import android.provider.MediaStore;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.annotation.Nullable;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

public class ImageUtil {

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


    public static void load(Context context,
                            String imageUrl,
                            ImageView imageView,
                            @Nullable ProgressBar progressBar) {      // Error image resource ID

        if (progressBar != null) progressBar.setVisibility(View.VISIBLE);

        // RequestOptions for caching
        RequestOptions options = new RequestOptions()
                .diskCacheStrategy(DiskCacheStrategy.ALL)
                .placeholder(R.drawable.ic_placeholder)  // Set the placeholder image
                .error(R.drawable.ic_placeholder);             // Set the error image

        Glide.with(context)
                .load(imageUrl)
                .apply(options)  // Apply the request options with placeholder and error
                .listener(new RequestListener<Drawable>() {
                    @Override
                    public boolean onLoadFailed(@Nullable GlideException e,
                                                Object model,
                                                Target<Drawable> target,
                                                boolean isFirstResource) {
                        if (progressBar != null) progressBar.setVisibility(View.GONE);
                        return false;
                    }

                    @Override
                    public boolean onResourceReady(Drawable resource,
                                                   Object model,
                                                   Target<Drawable> target,
                                                   DataSource dataSource,
                                                   boolean isFirstResource) {
                        if (progressBar != null) progressBar.setVisibility(View.GONE);
                        return false;
                    }
                })
                .into(imageView);
    }


    public static byte[] readBytesFromUri(Context context, Uri uri) throws IOException {
        try (InputStream inputStream = context.getContentResolver().openInputStream(uri);
             ByteArrayOutputStream buffer = new ByteArrayOutputStream()) {
            byte[] data = new byte[8192];
            int bytesRead;
            while ((bytesRead = inputStream.read(data)) != -1) {
                buffer.write(data, 0, bytesRead);
            }
            return buffer.toByteArray();
        }
    }
}

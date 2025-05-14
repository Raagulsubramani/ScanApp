package com.gmscan.utility;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageFormat;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.graphics.YuvImage;

import androidx.annotation.NonNull;
import androidx.camera.core.ImageProxy;

import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;

public class ImageConverter {

    /**
     * Converts an ImageProxy to a Bitmap, handling YUV_420_888 to NV21 conversion
     * and applying the correct rotation.
     *
     * @param image The ImageProxy to be converted.
     * @return A Bitmap representation of the ImageProxy, or null if conversion fails.
     */
    public Bitmap convertImageProxyToBitmap(ImageProxy image) {
        if (image.getFormat() != ImageFormat.YUV_420_888) return null;

        // Convert ImageProxy to YuvImage
        final YuvImage yuvImage = getYuvImage(image);

        // Compress YuvImage to JPEG
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        yuvImage.compressToJpeg(new Rect(0, 0, image.getWidth(), image.getHeight()), 100, out);
        byte[] jpegData = out.toByteArray();

        // Decode the JPEG data into a Bitmap
        Bitmap bitmap = BitmapFactory.decodeByteArray(jpegData, 0, jpegData.length);

        // Rotate the Bitmap based on the image's rotation metadata
        int rotation = image.getImageInfo().getRotationDegrees();
        if (rotation != 0) {
            Matrix matrix = new Matrix();
            matrix.postRotate(rotation);
            bitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
        }

        return bitmap;
    }

    /**
     * Converts an ImageProxy to a YuvImage in NV21 format.
     *
     * @param image The ImageProxy to be converted.
     * @return A YuvImage in NV21 format.
     */
    @NonNull
    private static YuvImage getYuvImage(ImageProxy image) {
        int width = image.getWidth();
        int height = image.getHeight();

        ImageProxy.PlaneProxy[] planes = image.getPlanes();
        ByteBuffer yBuffer = planes[0].getBuffer();
        ByteBuffer uBuffer = planes[1].getBuffer();
        ByteBuffer vBuffer = planes[2].getBuffer();

        int yRowStride = planes[0].getRowStride();
        int uvRowStride = planes[1].getRowStride();
        int uvPixelStride = planes[1].getPixelStride();

        // NV21 buffer size
        byte[] nv21 = new byte[width * height * 3 / 2];

        // Copy Y plane
        int pos = 0;
        for (int row = 0; row < height; row++) {
            yBuffer.position(row * yRowStride);
            yBuffer.get(nv21, pos, width);
            pos += width;
        }

        // Copy UV planes: Interleave V and U
        int uvHeight = height / 2;
        for (int row = 0; row < uvHeight; row++) {
            vBuffer.position(row * uvRowStride);
            uBuffer.position(row * uvRowStride);

            for (int col = 0; col < width / 2; col++) {
                nv21[pos++] = vBuffer.get(col * uvPixelStride); // V
                nv21[pos++] = uBuffer.get(col * uvPixelStride); // U
            }
        }

        // Convert NV21 byte array to YuvImage
        return new YuvImage(nv21, ImageFormat.NV21, width, height, null);
    }
}

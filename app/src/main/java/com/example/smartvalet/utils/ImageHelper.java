package com.example.smartvalet.utils;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.net.Uri;
import android.util.Base64;
import androidx.exifinterface.media.ExifInterface;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;

public class ImageHelper {
    
    private static final int MAX_IMAGE_SIZE = 200; // Max width/height in pixels
    private static final int COMPRESSION_QUALITY = 80; // JPEG quality 0-100
    
    /**
     * Compress image from Uri and convert to Base64 string
     * @param context Application context
     * @param imageUri Uri of the image
     * @return Base64 encoded string of compressed image, or null if error
     */
    public static String compressAndEncode(Context context, Uri imageUri) {
        try {
            // Load bitmap from Uri
            InputStream inputStream = context.getContentResolver().openInputStream(imageUri);
            Bitmap originalBitmap = BitmapFactory.decodeStream(inputStream);
            if (inputStream != null) inputStream.close();
            
            if (originalBitmap == null) {
                return null;
            }
            
            // Fix image rotation based on EXIF data
            Bitmap rotatedBitmap = fixImageRotation(context, imageUri, originalBitmap);
            
            // Resize bitmap to max size while maintaining aspect ratio
            Bitmap resizedBitmap = resizeBitmap(rotatedBitmap, MAX_IMAGE_SIZE);
            
            // Compress to JPEG
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            resizedBitmap.compress(Bitmap.CompressFormat.JPEG, COMPRESSION_QUALITY, byteArrayOutputStream);
            byte[] imageBytes = byteArrayOutputStream.toByteArray();
            
            // Convert to Base64
            String base64Image = Base64.encodeToString(imageBytes, Base64.DEFAULT);
            
            // Clean up
            if (originalBitmap != rotatedBitmap) {
                originalBitmap.recycle();
            }
            resizedBitmap.recycle();
            byteArrayOutputStream.close();
            
            return base64Image;
            
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
    
    /**
     * Decode Base64 string to Bitmap
     * @param base64String Base64 encoded image string
     * @return Bitmap or null if error
     */
    public static Bitmap decodeBase64(String base64String) {
        try {
            if (base64String == null || base64String.isEmpty()) {
                return null;
            }
            
            byte[] decodedBytes = Base64.decode(base64String, Base64.DEFAULT);
            return BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.length);
            
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
    
    /**
     * Resize bitmap while maintaining aspect ratio
     * @param bitmap Original bitmap
     * @param maxSize Maximum width or height
     * @return Resized bitmap
     */
    private static Bitmap resizeBitmap(Bitmap bitmap, int maxSize) {
        int width = bitmap.getWidth();
        int height = bitmap.getHeight();
        
        // Calculate scaling factor
        float scale = Math.min(
            (float) maxSize / width,
            (float) maxSize / height
        );
        
        // If image is already smaller than max size, return original
        if (scale >= 1.0f) {
            return bitmap;
        }
        
        int newWidth = Math.round(width * scale);
        int newHeight = Math.round(height * scale);
        
        return Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true);
    }
    
    /**
     * Fix image rotation based on EXIF orientation data
     * @param context Application context
     * @param imageUri Uri of the image
     * @param bitmap Original bitmap
     * @return Rotated bitmap if needed, or original bitmap
     */
    private static Bitmap fixImageRotation(Context context, Uri imageUri, Bitmap bitmap) {
        try {
            InputStream inputStream = context.getContentResolver().openInputStream(imageUri);
            if (inputStream == null) return bitmap;
            
            ExifInterface exif = new ExifInterface(inputStream);
            int orientation = exif.getAttributeInt(
                ExifInterface.TAG_ORIENTATION,
                ExifInterface.ORIENTATION_NORMAL
            );
            inputStream.close();
            
            Matrix matrix = new Matrix();
            switch (orientation) {
                case ExifInterface.ORIENTATION_ROTATE_90:
                    matrix.postRotate(90);
                    break;
                case ExifInterface.ORIENTATION_ROTATE_180:
                    matrix.postRotate(180);
                    break;
                case ExifInterface.ORIENTATION_ROTATE_270:
                    matrix.postRotate(270);
                    break;
                case ExifInterface.ORIENTATION_FLIP_HORIZONTAL:
                    matrix.setScale(-1, 1);
                    break;
                case ExifInterface.ORIENTATION_FLIP_VERTICAL:
                    matrix.setScale(1, -1);
                    break;
                default:
                    return bitmap;
            }
            
            return Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
            
        } catch (Exception e) {
            e.printStackTrace();
            return bitmap;
        }
    }
}

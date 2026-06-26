package com.example.smartvalet.utils;

import android.graphics.Bitmap;
import android.graphics.Color;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;

/**
 * QR Code Generator using ZXing library
 */
public class QRCodeGenerator {
    
    public static Bitmap generateQRCode(String data, int width, int height) {
        try {
            QRCodeWriter writer = new QRCodeWriter();
            BitMatrix bitMatrix = writer.encode(data, BarcodeFormat.QR_CODE, width, height);
            
            Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565);
            
            for (int x = 0; x < width; x++) {
                for (int y = 0; y < height; y++) {
                    bitmap.setPixel(x, y, bitMatrix.get(x, y) ? Color.BLACK : Color.WHITE);
                }
            }
            
            return bitmap;
            
        } catch (WriterException e) {
            e.printStackTrace();
            System.err.println("QRCodeGenerator: Error generating QR code: " + e.getMessage());
            
            // Return a simple error bitmap
            Bitmap errorBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565);
            errorBitmap.eraseColor(Color.LTGRAY);
            return errorBitmap;
        }
    }
}


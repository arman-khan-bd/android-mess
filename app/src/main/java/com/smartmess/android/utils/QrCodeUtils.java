package com.smartmess.android.utils;

import android.graphics.Bitmap;
import android.graphics.Color;
import android.util.Log;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;

import java.util.EnumMap;
import java.util.Map;

public class QrCodeUtils {

    private static final String TAG = "QrCodeUtils";

    /**
     * Generates a high-contrast QR Code Bitmap for the specified content string.
     *
     * @param content URL or text to encode
     * @param width Width in pixels (e.g. 512)
     * @param height Height in pixels (e.g. 512)
     * @return Bitmap of QR Code or null on error
     */
    public static Bitmap generateQrCode(String content, int width, int height) {
        if (content == null || content.trim().isEmpty()) {
            return null;
        }

        try {
            QRCodeWriter writer = new QRCodeWriter();
            Map<EncodeHintType, Object> hints = new EnumMap<>(EncodeHintType.class);
            hints.put(EncodeHintType.CHARACTER_SET, "UTF-8");
            hints.put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.H);
            hints.put(EncodeHintType.MARGIN, 1); // Compact padding

            BitMatrix bitMatrix = writer.encode(content, BarcodeFormat.QR_CODE, width, height, hints);

            int matrixWidth = bitMatrix.getWidth();
            int matrixHeight = bitMatrix.getHeight();
            Bitmap bitmap = Bitmap.createBitmap(matrixWidth, matrixHeight, Bitmap.Config.ARGB_8888);

            int darkColor = 0xFF1E293B; // Deep slate dark tone matching theme
            int lightColor = 0xFFFFFFFF; // Pure white background

            for (int x = 0; x < matrixWidth; x++) {
                for (int y = 0; y < matrixHeight; y++) {
                    bitmap.setPixel(x, y, bitMatrix.get(x, y) ? darkColor : lightColor);
                }
            }

            return bitmap;
        } catch (Exception e) {
            Log.e(TAG, "Error generating QR code bitmap: " + e.getMessage(), e);
            return null;
        }
    }
}

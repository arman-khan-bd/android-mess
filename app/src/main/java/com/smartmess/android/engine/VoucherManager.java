package com.smartmess.android.engine;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.UUID;

public class VoucherManager {

    private static final int MAX_WIDTH = 1080;
    private static final int QUALITY = 75;

    public static File saveAndCompressVoucher(Context context, Uri imageUri, String messUuid) {
        try {
            InputStream inputStream = context.getContentResolver().openInputStream(imageUri);
            Bitmap original = BitmapFactory.decodeStream(inputStream);
            if (inputStream != null) inputStream.close();

            if (original == null) return null;

            // Scale down if width > 1080
            Bitmap scaled;
            if (original.getWidth() > MAX_WIDTH) {
                int targetHeight = (int) (((double) original.getHeight() / original.getWidth()) * MAX_WIDTH);
                scaled = Bitmap.createScaledBitmap(original, MAX_WIDTH, targetHeight, true);
            } else {
                scaled = original;
            }

            File dir = new File(context.getFilesDir(), "vouchers/" + messUuid);
            if (!dir.exists()) {
                dir.mkdirs();
            }

            String filename = "voucher_" + UUID.randomUUID().toString().substring(0, 8) + ".webp";
            File outputFile = new File(dir, filename);
            FileOutputStream fos = new FileOutputStream(outputFile);

            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
                scaled.compress(Bitmap.CompressFormat.WEBP_LOSSY, QUALITY, fos);
            } else {
                @SuppressWarnings("deprecation")
                Bitmap.CompressFormat webpFormat = Bitmap.CompressFormat.WEBP;
                scaled.compress(webpFormat, QUALITY, fos);
            }
            fos.flush();
            fos.close();

            if (scaled != original) {
                scaled.recycle();
            }
            original.recycle();

            return outputFile;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}

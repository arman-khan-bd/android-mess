package com.smartmess.android.utils;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.provider.MediaStore;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.FileProvider;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.smartmess.android.R;
import com.smartmess.android.data.remote.ApiClient;
import com.smartmess.android.data.remote.ApiConfig;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Locale;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;

public class FileDownloadHelper {

    private static final String TAG = "FileDownloadHelper";
    private static final Handler mainHandler = new Handler(Looper.getMainLooper());

    public interface DownloadCallback {
        void onSuccess(Uri fileUri, String fileName, String mimeType);
        void onError(String errorMessage);
    }

    /**
     * Downloads PDF or Excel statements with progress tracking, MediaStore / FileProvider storage,
     * and immediate viewing intent.
     *
     * @param activity Hosting Activity
     * @param format "pdf" or "excel"
     * @param startDate ISO start date (YYYY-MM-DD)
     * @param endDate ISO end date (YYYY-MM-DD)
     * @param callback Optional completion listener
     */
    public static void downloadReport(@NonNull Activity activity,
                                      @NonNull String format,
                                      @NonNull String startDate,
                                      @NonNull String endDate,
                                      DownloadCallback callback) {

        final boolean isPdf = "pdf".equalsIgnoreCase(format);
        final String mimeType = isPdf ? "application/pdf"
                : "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
        final String fileExt = isPdf ? ".pdf" : ".xlsx";
        final String fileName = "SmartMess_Statement_" + startDate + "_to_" + endDate + fileExt;

        // Base URL from ApiConfig
        String endpoint = isPdf ? "reports/pdf" : "reports/excel";
        String downloadUrl = ApiConfig.BASE_URL + endpoint + "?start_date=" + startDate + "&end_date=" + endDate;

        // 1. Inflate progress dialog view
        LayoutInflater inflater = LayoutInflater.from(activity);
        View dialogView = inflater.inflate(android.R.layout.simple_list_item_1, null);

        // Build customized progress layout programmatically
        android.widget.LinearLayout layout = new android.widget.LinearLayout(activity);
        layout.setOrientation(android.widget.LinearLayout.VERTICAL);
        layout.setPadding(dpToPx(activity, 24), dpToPx(activity, 16), dpToPx(activity, 24), dpToPx(activity, 16));

        TextView tvStatus = new TextView(activity);
        tvStatus.setText("Connecting to secure server...");
        tvStatus.setTextColor(ContextCompatColor(activity, R.color.text_secondary));
        tvStatus.setTextSize(13);
        layout.addView(tvStatus);

        ProgressBar progressBar = new ProgressBar(activity, null, android.R.attr.progressBarStyleHorizontal);
        progressBar.setMax(100);
        progressBar.setProgress(0);
        progressBar.setIndeterminate(false);
        android.widget.LinearLayout.LayoutParams pbLp = new android.widget.LinearLayout.LayoutParams(
                android.widget.LinearLayout.LayoutParams.MATCH_PARENT, dpToPx(activity, 18));
        pbLp.topMargin = dpToPx(activity, 12);
        pbLp.bottomMargin = dpToPx(activity, 8);
        progressBar.setLayoutParams(pbLp);
        layout.addView(progressBar);

        TextView tvPercent = new TextView(activity);
        tvPercent.setText("0%");
        tvPercent.setTextColor(ContextCompatColor(activity, R.color.primary));
        tvPercent.setTextSize(13);
        tvPercent.setTypeface(null, android.graphics.Typeface.BOLD);
        layout.addView(tvPercent);

        AlertDialog progressDialog = new MaterialAlertDialogBuilder(activity)
                .setTitle("Exporting " + (isPdf ? "PDF Statement" : "Excel Workbook"))
                .setView(layout)
                .setCancelable(false)
                .setNegativeButton("বাতিল", null)
                .create();

        progressDialog.show();

        // 2. Prepare HTTP Call using ApiClient
        OkHttpClient client = ApiClient.getOkHttpClient(activity.getApplicationContext());
        Request request = new Request.Builder()
                .url(downloadUrl)
                .header("Accept", mimeType)
                .get()
                .build();

        Call call = client.newCall(request);

        progressDialog.getButton(DialogInterface.BUTTON_NEGATIVE).setOnClickListener(v -> {
            call.cancel();
            progressDialog.dismiss();
            if (callback != null) callback.onError("Download cancelled by user");
        });

        // 3. Execute Async Network Request
        call.enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                if (call.isCanceled()) return;
                mainHandler.post(() -> {
                    progressDialog.dismiss();
                    String msg = "Download failed: " + e.getMessage();
                    Toast.makeText(activity, msg, Toast.LENGTH_LONG).show();
                    if (callback != null) callback.onError(msg);
                });
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) {
                if (!response.isSuccessful()) {
                    mainHandler.post(() -> {
                        progressDialog.dismiss();
                        String errorMsg = "Export error (HTTP " + response.code() + ")";
                        if (response.code() == 403) {
                            errorMsg = "PDF & Excel export is a Pro feature. Please upgrade your mess plan.";
                        }
                        Toast.makeText(activity, errorMsg, Toast.LENGTH_LONG).show();
                        if (callback != null) callback.onError(errorMsg);
                    });
                    return;
                }

                ResponseBody body = response.body();
                if (body == null) {
                    mainHandler.post(() -> {
                        progressDialog.dismiss();
                        Toast.makeText(activity, "সার্ভার থেকে কোনো তথ্য পাওয়া যায়নি", Toast.LENGTH_SHORT).show();
                        if (callback != null) callback.onError("সার্ভার থেকে কোনো তথ্য পাওয়া যায়নি");
                    });
                    return;
                }

                long totalBytes = body.contentLength();

                try {
                    Uri savedUri;
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        // MediaStore API (Android 10+)
                        savedUri = saveViaMediaStore(activity, body, fileName, mimeType, totalBytes,
                                (bytesRead, percent) -> updateProgress(progressBar, tvPercent, tvStatus, bytesRead, totalBytes, percent));
                    } else {
                        // Public Downloads or app storage with FileProvider (Android 9 and below)
                        savedUri = saveViaFileStream(activity, body, fileName, mimeType, totalBytes,
                                (bytesRead, percent) -> updateProgress(progressBar, tvPercent, tvStatus, bytesRead, totalBytes, percent));
                    }

                    mainHandler.post(() -> {
                        progressDialog.dismiss();
                        Toast.makeText(activity, "ফোনের ডাউনলোড ফোল্ডারে সংরক্ষিত হয়েছে: " + fileName, Toast.LENGTH_LONG).show();
                        if (savedUri != null) {
                            openFile(activity, savedUri, mimeType);
                            if (callback != null) callback.onSuccess(savedUri, fileName, mimeType);
                        }
                    });

                } catch (Exception e) {
                    Log.e(TAG, "Error writing downloaded file: " + e.getMessage(), e);
                    mainHandler.post(() -> {
                        progressDialog.dismiss();
                        String err = "Failed to save file: " + e.getMessage();
                        Toast.makeText(activity, err, Toast.LENGTH_LONG).show();
                        if (callback != null) callback.onError(err);
                    });
                }
            }
        });
    }

    private interface ProgressListener {
        void onProgress(long bytesRead, int percent);
    }

    private static void updateProgress(ProgressBar progressBar, TextView tvPercent, TextView tvStatus,
                                       long bytesRead, long totalBytes, int percent) {
        mainHandler.post(() -> {
            progressBar.setProgress(percent);
            tvPercent.setText(percent + "%");
            if (totalBytes > 0) {
                long kbRead = bytesRead / 1024;
                long kbTotal = totalBytes / 1024;
                tvStatus.setText(String.format(Locale.US, "Downloading: %d KB / %d KB", kbRead, kbTotal));
            } else {
                tvStatus.setText("Downloading... " + (bytesRead / 1024) + " KB");
            }
        });
    }

    /**
     * Android 10+ (API 29+) MediaStore API implementation
     * Saves directly into public Downloads folder so it appears immediately in file manager & Downloads app.
     */
    private static Uri saveViaMediaStore(Context context, ResponseBody body, String fileName,
                                         String mimeType, long totalBytes, ProgressListener listener) throws IOException {
        ContentResolver resolver = context.getContentResolver();
        ContentValues values = new ContentValues();
        values.put(MediaStore.Downloads.DISPLAY_NAME, fileName);
        values.put(MediaStore.Downloads.MIME_TYPE, mimeType);
        // Save directly to the public phone storage Downloads folder
        values.put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS);
        values.put(MediaStore.Downloads.IS_PENDING, 1);

        Uri uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values);
        if (uri == null) {
            throw new IOException("Failed to create MediaStore record");
        }

        try (OutputStream os = resolver.openOutputStream(uri);
             InputStream is = body.byteStream()) {
            if (os == null) throw new IOException("Failed to open MediaStore output stream");

            byte[] buffer = new byte[8192];
            long bytesReadTotal = 0;
            int read;

            while ((read = is.read(buffer)) != -1) {
                os.write(buffer, 0, read);
                bytesReadTotal += read;
                if (listener != null) {
                    int percent = totalBytes > 0 ? (int) ((bytesReadTotal * 100) / totalBytes) : 50;
                    listener.onProgress(bytesReadTotal, Math.min(percent, 100));
                }
            }
            os.flush();
        }

        values.clear();
        values.put(MediaStore.Downloads.IS_PENDING, 0);
        resolver.update(uri, values, null, null);
        return uri;
    }

    /**
     * Android 9 and below (API 21-28) File Stream implementation
     * Saves directly into public phone storage Downloads folder and registers with MediaScanner & DownloadManager.
     */
    private static Uri saveViaFileStream(Context context, ResponseBody body, String fileName,
                                         String mimeType, long totalBytes, ProgressListener listener) throws IOException {
        File downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
        if (!downloadsDir.exists()) {
            downloadsDir.mkdirs();
        }

        File destFile = new File(downloadsDir, fileName);
        if (!destFile.getParentFile().canWrite()) {
            destFile = new File(context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), fileName);
        }

        try (OutputStream os = new FileOutputStream(destFile);
             InputStream is = body.byteStream()) {

            byte[] buffer = new byte[8192];
            long bytesReadTotal = 0;
            int read;

            while ((read = is.read(buffer)) != -1) {
                os.write(buffer, 0, read);
                bytesReadTotal += read;
                if (listener != null) {
                    int percent = totalBytes > 0 ? (int) ((bytesReadTotal * 100) / totalBytes) : 50;
                    listener.onProgress(bytesReadTotal, Math.min(percent, 100));
                }
            }
            os.flush();
        }

        // Notify MediaScanner so the system file manager immediately detects the file
        try {
            android.media.MediaScannerConnection.scanFile(
                    context,
                    new String[]{ destFile.getAbsolutePath() },
                    new String[]{ mimeType },
                    null
            );
        } catch (Throwable ignored) {}

        // Add completed download to the Android system DownloadManager
        try {
            android.app.DownloadManager dm = (android.app.DownloadManager) context.getSystemService(Context.DOWNLOAD_SERVICE);
            if (dm != null) {
                dm.addCompletedDownload(
                        fileName,
                        "SmartMess Statement",
                        true,
                        mimeType,
                        destFile.getAbsolutePath(),
                        destFile.length(),
                        true
                );
            }
        } catch (Throwable ignored) {}

        return FileProvider.getUriForFile(context, context.getPackageName() + ".fileprovider", destFile);
    }

    /**
     * Dispatches an ACTION_VIEW intent with FLAG_GRANT_READ_URI_PERMISSION
     */
    public static void openFile(@NonNull Activity activity, @NonNull Uri fileUri, @NonNull String mimeType) {
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setDataAndType(fileUri, mimeType);
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

        try {
            activity.startActivity(Intent.createChooser(intent, "Open Statement"));
        } catch (Exception ex) {
            Toast.makeText(activity, "স্টেটমেন্ট ডাউনলোড সম্পন্ন হয়েছে। দেখতে পিডিএফ বা এক্সেল রিডার অ্যাপ খুলুন।", Toast.LENGTH_LONG).show();
        }
    }

    private static int dpToPx(Context context, int dp) {
        return Math.round(dp * context.getResources().getDisplayMetrics().density);
    }

    private static int ContextCompatColor(Context context, int resId) {
        return androidx.core.content.ContextCompat.getColor(context, resId);
    }
}

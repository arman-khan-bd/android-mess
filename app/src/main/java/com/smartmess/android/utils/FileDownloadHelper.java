package com.smartmess.android.utils;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.graphics.pdf.PdfDocument;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.FileProvider;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.smartmess.android.R;
import com.smartmess.android.data.local.DatabaseHelper;
import com.smartmess.android.data.local.dao.ExpenseDao;
import com.smartmess.android.data.local.dao.MessDao;
import com.smartmess.android.data.local.dao.UserDao;
import com.smartmess.android.engine.AccountingEngine;
import com.smartmess.android.engine.CalculationModels.CycleSummary;
import com.smartmess.android.engine.CalculationModels.MemberBalanceSheet;
import com.smartmess.android.model.Expense;
import com.smartmess.android.model.Mess;
import com.smartmess.android.model.User;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.Executors;

/**
 * Robust Client-Side Native PDF and Excel (UTF-8 BOM CSV) Statement Exporter.
 * Generates valid, non-corrupted statements directly on-device using local SQLite data,
 * with full offline support, public phone Downloads directory saving, and automatic viewer intent.
 */
public class FileDownloadHelper {

    private static final String TAG = "FileDownloadHelper";
    private static final Handler mainHandler = new Handler(Looper.getMainLooper());

    public interface DownloadCallback {
        void onSuccess(Uri fileUri, String fileName, String mimeType);
        void onError(String errorMessage);
    }

    /**
     * Entry point to export mess statements (PDF or Excel).
     * Runs natively on-device to guarantee instant, 100% offline-compatible, non-corrupt exports.
     */
    public static void downloadReport(@NonNull Activity activity,
                                      @NonNull String format,
                                      @NonNull String startDate,
                                      @NonNull String endDate,
                                      DownloadCallback callback) {

        final boolean isPdf = "pdf".equalsIgnoreCase(format);
        final String formatTitle = isPdf ? "PDF বিবরণী" : "Excel স্প্রেডশিট";

        // Build elegant Material progress dialog
        android.widget.LinearLayout layout = new android.widget.LinearLayout(activity);
        layout.setOrientation(android.widget.LinearLayout.VERTICAL);
        layout.setPadding(dpToPx(activity, 24), dpToPx(activity, 16), dpToPx(activity, 24), dpToPx(activity, 16));

        TextView tvStatus = new TextView(activity);
        tvStatus.setText(formatTitle + " তৈরি হচ্ছে... অনুগ্রহ করে অপেক্ষা করুন");
        tvStatus.setTextColor(ContextCompatColor(activity, R.color.text_secondary));
        tvStatus.setTextSize(13);
        layout.addView(tvStatus);

        ProgressBar progressBar = new ProgressBar(activity, null, android.R.attr.progressBarStyleHorizontal);
        progressBar.setMax(100);
        progressBar.setProgress(35);
        progressBar.setIndeterminate(true);
        android.widget.LinearLayout.LayoutParams pbLp = new android.widget.LinearLayout.LayoutParams(
                android.widget.LinearLayout.LayoutParams.MATCH_PARENT, dpToPx(activity, 16));
        pbLp.topMargin = dpToPx(activity, 12);
        pbLp.bottomMargin = dpToPx(activity, 8);
        progressBar.setLayoutParams(pbLp);
        layout.addView(progressBar);

        AlertDialog progressDialog = new MaterialAlertDialogBuilder(activity)
                .setTitle(formatTitle + " ডাউনলোড")
                .setView(layout)
                .setCancelable(false)
                .create();

        progressDialog.show();

        Executors.newSingleThreadExecutor().execute(() -> {
            try {
                if (isPdf) {
                    generateNativePdf(activity, startDate, endDate, progressDialog, callback);
                } else {
                    generateNativeExcel(activity, startDate, endDate, progressDialog, callback);
                }
            } catch (Exception e) {
                Log.e(TAG, "Export error: " + e.getMessage(), e);
                mainHandler.post(() -> {
                    if (progressDialog.isShowing()) progressDialog.dismiss();
                    String err = "স্টেটমেন্ট তৈরি করতে সমস্যা হয়েছে: " + e.getMessage();
                    Toast.makeText(activity, err, Toast.LENGTH_LONG).show();
                    if (callback != null) callback.onError(err);
                });
            }
        });
    }

    // =========================================================================
    // 1. NATIVE PDF STATEMENT GENERATOR (android.graphics.pdf.PdfDocument)
    // =========================================================================
    private static void generateNativePdf(Activity activity,
                                          String startDate,
                                          String endDate,
                                          AlertDialog progressDialog,
                                          DownloadCallback callback) throws Exception {

        SessionManager sessionManager = new SessionManager(activity);
        long messId = sessionManager.getMessId();

        DatabaseHelper helper = DatabaseHelper.getInstance(activity);
        MessDao messDao = new MessDao(helper);
        Mess mess = messDao.getById(messId);
        String messName = (mess != null && mess.getName() != null) ? mess.getName() : "স্মার্টমেস";

        AccountingEngine accountingEngine = new AccountingEngine(activity);
        CycleSummary summary = accountingEngine.calculateCycleSummary(messId, startDate, endDate);

        List<MemberBalanceSheet> memberBalances = summary.getMemberBalances();

        // A4 Paper Dimensions: 595 x 842 points (72 pt/inch)
        final int pageWidth = 595;
        final int pageHeight = 842;
        PdfDocument document = new PdfDocument();

        Paint paint = new Paint();
        paint.setAntiAlias(true);

        int currentPageNumber = 1;
        PdfDocument.PageInfo pageInfo = new PdfDocument.PageInfo.Builder(pageWidth, pageHeight, currentPageNumber).create();
        PdfDocument.Page page = document.startPage(pageInfo);
        Canvas canvas = page.getCanvas();

        // Draw Header Banner
        paint.setColor(Color.parseColor("#065F46")); // Brand Emerald Dark
        canvas.drawRect(0, 0, pageWidth, 90, paint);

        paint.setColor(Color.WHITE);
        paint.setTextSize(16);
        paint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
        canvas.drawText("SmartMess • মাসিক মেস অডিট ও হিসাব বিবরণী", 30, 36, paint);

        paint.setTextSize(11);
        paint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.NORMAL));
        canvas.drawText("মেসের নাম: " + messName, 30, 56, paint);
        canvas.drawText("হিসাব সময়কাল: " + startDate + "  থেকে  " + endDate + "   |   মুদ্রণ তারিখ: " + DateTimeUtils.getCurrentDate(), 30, 74, paint);

        // Summary Metric Cards (2x2 Grid)
        int cardTop = 105;
        int cardWidth = 255;
        int cardHeight = 44;

        drawMetricCard(canvas, paint, 30, cardTop, cardWidth, cardHeight,
                "মিল রেট (Meal Rate)", CurrencyUtils.format(summary.getMealRate()) + " / মিল", "#F0FDF4", "#15803D");
        drawMetricCard(canvas, paint, 310, cardTop, cardWidth, cardHeight,
                "মোট মেস মিল (Total Meals)", String.format(Locale.US, "%.1f", summary.getTotalMessMeals()) + " টি", "#EFF6FF", "#1D4ED8");

        drawMetricCard(canvas, paint, 30, cardTop + 52, cardWidth, cardHeight,
                "মোট মেস খরচ (All Expenses)", CurrencyUtils.format(summary.getTotalAllExpenses()), "#FEF2F2", "#B91C1C");
        drawMetricCard(canvas, paint, 310, cardTop + 52, cardWidth, cardHeight,
                "মোট জমা (Total Deposits)", CurrencyUtils.format(summary.getTotalMessDeposits()), "#FAF5FF", "#7E22CE");

        // Expense Pool Breakdown
        int poolTop = cardTop + 110;
        paint.setColor(Color.parseColor("#F1F5F9"));
        canvas.drawRoundRect(30, poolTop, pageWidth - 30, poolTop + 48, 6, 6, paint);

        paint.setColor(Color.parseColor("#334155"));
        paint.setTextSize(9);
        paint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.NORMAL));
        canvas.drawText("• কাঁচামাল বাজার: " + CurrencyUtils.format(summary.getTotalRawMealExpense()), 42, poolTop + 18, paint);
        canvas.drawText("• শেয়ার্ড খাদ্যদ্রব্য: " + CurrencyUtils.format(summary.getTotalSharedFoodExpense()), 210, poolTop + 18, paint);
        canvas.drawText("• ফিক্সড/ইউটিলিটি: " + CurrencyUtils.format(summary.getTotalUtilityAssetExpense()), 380, poolTop + 18, paint);

        canvas.drawText("• এসএমএস খরচ: " + CurrencyUtils.format(summary.getTotalSmsChargeExpense()), 42, poolTop + 36, paint);
        canvas.drawText("• ক্যাশ ইন হ্যান্ড (অবশিষ্ট জমা): " + CurrencyUtils.format(summary.getMessCashInHand()), 210, poolTop + 36, paint);
        canvas.drawText("• মোট সদস্য সংখ্যা: " + memberBalances.size() + " জন", 380, poolTop + 36, paint);

        // Member Balances Table Header
        int tableTop = poolTop + 64;
        paint.setColor(Color.parseColor("#0F172A"));
        canvas.drawRect(30, tableTop, pageWidth - 30, tableTop + 24, paint);

        paint.setColor(Color.WHITE);
        paint.setTextSize(9);
        paint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
        canvas.drawText("নং", 38, tableTop + 16, paint);
        canvas.drawText("সদস্যের নাম", 75, tableTop + 16, paint);
        canvas.drawText("মিল", 225, tableTop + 16, paint);
        canvas.drawText("মিল খরচ", 285, tableTop + 16, paint);
        canvas.drawText("মোট জমা", 365, tableTop + 16, paint);
        canvas.drawText("ব্যালেন্স", 445, tableTop + 16, paint);
        canvas.drawText("অবস্থা", 515, tableTop + 16, paint);

        // Member Rows
        int y = tableTop + 24;
        int rowHeight = 22;
        int sl = 1;

        for (int i = 0; i < memberBalances.size(); i++) {
            MemberBalanceSheet m = memberBalances.get(i);

            // Check if page overflow
            if (y + rowHeight > pageHeight - 50) {
                // Finish current page and start a new one
                document.finishPage(page);
                currentPageNumber++;
                pageInfo = new PdfDocument.PageInfo.Builder(pageWidth, pageHeight, currentPageNumber).create();
                page = document.startPage(pageInfo);
                canvas = page.getCanvas();

                // Repeat table header on continuation page
                y = 40;
                paint.setColor(Color.parseColor("#0F172A"));
                canvas.drawRect(30, y, pageWidth - 30, y + 24, paint);

                paint.setColor(Color.WHITE);
                paint.setTextSize(9);
                paint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
                canvas.drawText("নং", 38, y + 16, paint);
                canvas.drawText("সদস্যের নাম", 75, y + 16, paint);
                canvas.drawText("মিল", 225, y + 16, paint);
                canvas.drawText("মিল খরচ", 285, y + 16, paint);
                canvas.drawText("মোট জমা", 365, y + 16, paint);
                canvas.drawText("ব্যালেন্স", 445, y + 16, paint);
                canvas.drawText("অবস্থা", 515, y + 16, paint);
                y += 24;
            }

            // Alternating Row Background
            if (i % 2 == 0) {
                paint.setColor(Color.parseColor("#F8FAFC"));
                canvas.drawRect(30, y, pageWidth - 30, y + rowHeight, paint);
            }

            // Bottom border
            paint.setColor(Color.parseColor("#E2E8F0"));
            canvas.drawLine(30, y + rowHeight, pageWidth - 30, y + rowHeight, paint);

            // Row Data
            paint.setColor(Color.parseColor("#1E293B"));
            paint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.NORMAL));
            paint.setTextSize(9);

            canvas.drawText(String.valueOf(sl++), 38, y + 15, paint);
            canvas.drawText(truncateText(m.getUserName(), 20), 75, y + 15, paint);
            canvas.drawText(String.format(Locale.US, "%.1f", m.getTotalMeals()), 225, y + 15, paint);
            canvas.drawText(CurrencyUtils.format(m.getMealsCost()), 285, y + 15, paint);
            canvas.drawText(CurrencyUtils.format(m.getTotalDeposits()), 365, y + 15, paint);

            double balance = m.getNetBalance();
            if (balance >= 0) {
                paint.setColor(Color.parseColor("#15803D"));
                canvas.drawText("+ " + CurrencyUtils.format(balance), 445, y + 15, paint);
                canvas.drawText("পাবে", 515, y + 15, paint);
            } else {
                paint.setColor(Color.parseColor("#DC2626"));
                canvas.drawText("- " + CurrencyUtils.format(Math.abs(balance)), 445, y + 15, paint);
                canvas.drawText("দেনা", 515, y + 15, paint);
            }

            y += rowHeight;
        }

        // Footer Note
        paint.setColor(Color.parseColor("#64748B"));
        paint.setTextSize(8);
        paint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.NORMAL));
        canvas.drawText("স্মার্টমেস অ্যান্ড্রয়েড অ্যাপ দ্বারা স্বয়ংক্রিয়ভাবে তৈরি • এটি একটি বৈধ ডিজিটাল অডিট স্টেটমেন্ট • কোনো স্বাক্ষরের প্রয়োজন নেই",
                30, pageHeight - 25, paint);

        document.finishPage(page);

        // Convert to byte stream
        ByteArrayOutputStream pdfByteStream = new ByteArrayOutputStream();
        document.writeTo(pdfByteStream);
        document.close();

        byte[] pdfBytes = pdfByteStream.toByteArray();
        final String fileName = "SmartMess_Statement_" + startDate + "_to_" + endDate + ".pdf";
        final String mimeType = "application/pdf";

        Uri savedUri = saveFileBytes(activity, pdfBytes, fileName, mimeType);

        mainHandler.post(() -> {
            if (progressDialog.isShowing()) progressDialog.dismiss();
            Toast.makeText(activity, "পিডিএফ স্টেটমেন্ট সংরক্ষিত হয়েছে: " + fileName, Toast.LENGTH_LONG).show();
            if (savedUri != null) {
                openFile(activity, savedUri, mimeType);
                if (callback != null) callback.onSuccess(savedUri, fileName, mimeType);
            }
        });
    }

    private static void drawMetricCard(Canvas canvas, Paint paint, int x, int y, int w, int h,
                                       String title, String val, String bgColor, String textColor) {
        paint.setColor(Color.parseColor(bgColor));
        canvas.drawRoundRect(x, y, x + w, y + h, 6, 6, paint);

        paint.setColor(Color.parseColor("#64748B"));
        paint.setTextSize(8);
        paint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.NORMAL));
        canvas.drawText(title, x + 12, y + 16, paint);

        paint.setColor(Color.parseColor(textColor));
        paint.setTextSize(12);
        paint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
        canvas.drawText(val, x + 12, y + 34, paint);
    }

    private static String truncateText(String text, int maxLength) {
        if (text == null) return "N/A";
        if (text.length() <= maxLength) return text;
        return text.substring(0, maxLength - 2) + "..";
    }

    // =========================================================================
    // 2. NATIVE EXCEL STATEMENT GENERATOR (UTF-8 BOM CSV SPREADSHEET)
    // =========================================================================
    private static void generateNativeExcel(Activity activity,
                                            String startDate,
                                            String endDate,
                                            AlertDialog progressDialog,
                                            DownloadCallback callback) throws Exception {

        SessionManager sessionManager = new SessionManager(activity);
        long messId = sessionManager.getMessId();

        DatabaseHelper helper = DatabaseHelper.getInstance(activity);
        MessDao messDao = new MessDao(helper);
        Mess mess = messDao.getById(messId);
        String messName = (mess != null && mess.getName() != null) ? mess.getName() : "স্মার্টমেস";

        AccountingEngine accountingEngine = new AccountingEngine(activity);
        CycleSummary summary = accountingEngine.calculateCycleSummary(messId, startDate, endDate);

        List<MemberBalanceSheet> memberBalances = summary.getMemberBalances();

        UserDao userDao = new UserDao(helper);
        List<User> allUsers = userDao.getAllMembersByMess(messId);
        Map<Long, User> userMap = new HashMap<>();
        if (allUsers != null) {
            for (User u : allUsers) {
                userMap.put(u.getId(), u);
            }
        }

        ExpenseDao expenseDao = new ExpenseDao(helper);
        List<Expense> expenses = expenseDao.getExpensesByMessAndDateRange(messId, startDate, endDate);

        StringBuilder csv = new StringBuilder();

        // 1. Header Information
        csv.append("\"SmartMess - মাসিক মেস অডিট ও হিসাব বিবরণী\"\n");
        csv.append("\"মেসের নাম:\",\"").append(escapeCsv(messName)).append("\"\n");
        csv.append("\"হিসাব সময়কাল:\",\"").append(startDate).append(" থেকে ").append(endDate).append("\"\n");
        csv.append("\"রিপোর্ট তৈরির সময়:\",\"").append(DateTimeUtils.getCurrentDate()).append("\"\n\n");

        // 2. Financial Overview Section
        csv.append("\"আর্থিক সারসংক্ষেপ (FINANCIAL OVERVIEW)\"\n");
        csv.append("\"বিবরণ (Metric)\",\"পরিমাণ (BDT)\"\n");
        csv.append("\"মিল রেট (Meal Rate)\",\"").append(String.format(Locale.US, "%.4f", summary.getMealRate())).append("\"\n");
        csv.append("\"মোট মেস মিল (Total Meals)\",\"").append(String.format(Locale.US, "%.1f", summary.getTotalMessMeals())).append("\"\n");
        csv.append("\"কাঁচামাল বাজার খরচ (Raw Bazar)\",\"").append(String.format(Locale.US, "%.2f", summary.getTotalRawMealExpense())).append("\"\n");
        csv.append("\"শেয়ার্ড খাদ্যদ্রব্য (Shared Food)\",\"").append(String.format(Locale.US, "%.2f", summary.getTotalSharedFoodExpense())).append("\"\n");
        csv.append("\"ইউটিলিটি ও ফিক্সড খরচ (Utility & Fixed)\",\"").append(String.format(Locale.US, "%.2f", summary.getTotalUtilityAssetExpense())).append("\"\n");
        csv.append("\"মোট মেস খরচ (All Expenses)\",\"").append(String.format(Locale.US, "%.2f", summary.getTotalAllExpenses())).append("\"\n");
        csv.append("\"মোট মেস জমা (Total Deposits)\",\"").append(String.format(Locale.US, "%.2f", summary.getTotalMessDeposits())).append("\"\n");
        csv.append("\"ক্যাশ ইন হ্যান্ড (Cash in Hand)\",\"").append(String.format(Locale.US, "%.2f", summary.getMessCashInHand())).append("\"\n\n");

        // 3. Member Balances Grid
        csv.append("\"সদস্যদের ব্যালেন্স শিট (MEMBER BALANCES)\"\n");
        csv.append("\"ক্রমিক\",\"সদস্যের নাম\",\"ফোন নম্বর\",\"ভূমিকা\",\"মোট মিল\",\"মিল খরচ (BDT)\",\"মোট জমা (BDT)\",\"নেট ব্যালেন্স (BDT)\",\"অবস্থা\"\n");

        int sl = 1;
        for (MemberBalanceSheet m : memberBalances) {
            User u = userMap.get(m.getUserId());
            String phone = u != null ? u.getPhone() : "N/A";
            String role = u != null && "manager".equalsIgnoreCase(u.getRole()) ? "ম্যানেজার" : "সদস্য";
            double balance = m.getNetBalance();
            String status = balance >= 0 ? "পাবে (Refund)" : "দেনা (Due)";
            String balanceFormatted = (balance >= 0 ? "+" : "") + String.format(Locale.US, "%.2f", balance);

            csv.append("\"").append(sl++).append("\",")
                    .append("\"").append(escapeCsv(m.getUserName())).append("\",")
                    .append("\"").append(escapeCsv(phone)).append("\",")
                    .append("\"").append(role).append("\",")
                    .append("\"").append(String.format(Locale.US, "%.1f", m.getTotalMeals())).append("\",")
                    .append("\"").append(String.format(Locale.US, "%.2f", m.getMealsCost())).append("\",")
                    .append("\"").append(String.format(Locale.US, "%.2f", m.getTotalDeposits())).append("\",")
                    .append("\"").append(balanceFormatted).append("\",")
                    .append("\"").append(status).append("\"\n");
        }
        csv.append("\n");

        // 4. Daily Bazar & Expense Ledger
        csv.append("\"দৈনিক বাজার ও খরচের তালিকা (BAZAR & EXPENSE LOGS)\"\n");
        csv.append("\"তারিখ\",\"খরচের বিবরণ\",\"ক্যাটাগরি\",\"ক্রেতার নাম\",\"বণ্টন পদ্ধতি\",\"টাকার পরিমাণ (BDT)\"\n");

        if (expenses != null && !expenses.isEmpty()) {
            for (Expense exp : expenses) {
                User buyer = userMap.get(exp.getBuyerUserId());
                String buyerName = buyer != null ? buyer.getName() : "সদস্য #" + exp.getBuyerUserId();

                String cat = exp.getExpenseCategory();
                if ("raw_meal".equalsIgnoreCase(cat)) cat = "কাঁচামাল বাজার";
                else if ("shared_food".equalsIgnoreCase(cat)) cat = "শেয়ার্ড খাদ্যদ্রব্য";
                else if ("utility_asset".equalsIgnoreCase(cat)) cat = "ইউটিলিটি/ফিক্সড";

                csv.append("\"").append(exp.getExpenseDate()).append("\",")
                        .append("\"").append(escapeCsv(exp.getTitle())).append("\",")
                        .append("\"").append(cat).append("\",")
                        .append("\"").append(escapeCsv(buyerName)).append("\",")
                        .append("\"").append(exp.getSplitType() != null ? exp.getSplitType() : "সমান").append("\",")
                        .append("\"").append(String.format(Locale.US, "%.2f", exp.getAmount())).append("\"\n");
            }
        } else {
            csv.append("\"-\",\"কোনো বাজার খরচ রেকর্ড নেই\",\"-\",\"-\",\"-\",\"0.00\"\n");
        }

        // Prepend UTF-8 BOM so Microsoft Excel & Google Sheets correctly decode Bengali text
        byte[] bom = new byte[]{(byte) 0xEF, (byte) 0xBB, (byte) 0xBF};
        byte[] csvBytes = csv.toString().getBytes(StandardCharsets.UTF_8);

        byte[] finalBytes = new byte[bom.length + csvBytes.length];
        System.arraycopy(bom, 0, finalBytes, 0, bom.length);
        System.arraycopy(csvBytes, 0, finalBytes, bom.length, csvBytes.length);

        final String fileName = "SmartMess_Statement_" + startDate + "_to_" + endDate + ".csv";
        final String mimeType = "text/csv";

        Uri savedUri = saveFileBytes(activity, finalBytes, fileName, mimeType);

        mainHandler.post(() -> {
            if (progressDialog.isShowing()) progressDialog.dismiss();
            Toast.makeText(activity, "এক্সেল স্টেটমেন্ট সংরক্ষিত হয়েছে: " + fileName, Toast.LENGTH_LONG).show();
            if (savedUri != null) {
                openFile(activity, savedUri, mimeType);
                if (callback != null) callback.onSuccess(savedUri, fileName, mimeType);
            }
        });
    }

    private static String escapeCsv(String str) {
        if (str == null) return "";
        return str.replace("\"", "\"\"");
    }

    // =========================================================================
    // 3. STORAGE DISPATCH: MEDIASTORE (Android 10+) vs FILESTREAM (Android 9-)
    // =========================================================================
    private static Uri saveFileBytes(Context context, byte[] bytes, String fileName, String mimeType) throws IOException {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            return saveViaMediaStoreBytes(context, bytes, fileName, mimeType);
        } else {
            return saveViaFileStreamBytes(context, bytes, fileName, mimeType);
        }
    }

    private static Uri saveViaMediaStoreBytes(Context context, byte[] bytes, String fileName, String mimeType) throws IOException {
        ContentResolver resolver = context.getContentResolver();
        ContentValues values = new ContentValues();
        values.put(MediaStore.Downloads.DISPLAY_NAME, fileName);
        values.put(MediaStore.Downloads.MIME_TYPE, mimeType);
        values.put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS);
        values.put(MediaStore.Downloads.IS_PENDING, 1);

        Uri uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values);
        if (uri == null) {
            throw new IOException("Failed to create MediaStore entry for " + fileName);
        }

        try (OutputStream os = resolver.openOutputStream(uri)) {
            if (os == null) throw new IOException("Cannot open MediaStore output stream");
            os.write(bytes);
            os.flush();
        }

        values.clear();
        values.put(MediaStore.Downloads.IS_PENDING, 0);
        resolver.update(uri, values, null, null);
        return uri;
    }

    private static Uri saveViaFileStreamBytes(Context context, byte[] bytes, String fileName, String mimeType) throws IOException {
        File downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
        if (!downloadsDir.exists()) {
            downloadsDir.mkdirs();
        }

        File destFile = new File(downloadsDir, fileName);
        if (!destFile.getParentFile().canWrite()) {
            destFile = new File(context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), fileName);
        }

        try (FileOutputStream fos = new FileOutputStream(destFile)) {
            fos.write(bytes);
            fos.flush();
        }

        // Register with MediaScanner
        try {
            android.media.MediaScannerConnection.scanFile(
                    context,
                    new String[]{ destFile.getAbsolutePath() },
                    new String[]{ mimeType },
                    null
            );
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
            activity.startActivity(Intent.createChooser(intent, "স্টেটমেন্ট খুলুন"));
        } catch (Exception ex) {
            Toast.makeText(activity, "স্টেটমেন্ট ফোনের ডাউনলোড ফোল্ডারে সংরক্ষিত হয়েছে।", Toast.LENGTH_LONG).show();
        }
    }

    private static int dpToPx(Context context, int dp) {
        return Math.round(dp * context.getResources().getDisplayMetrics().density);
    }

    private static int ContextCompatColor(Context context, int resId) {
        return androidx.core.content.ContextCompat.getColor(context, resId);
    }
}

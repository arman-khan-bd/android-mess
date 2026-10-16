package com.smartmess.android.utils;

import android.content.Context;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.DrawableRes;
import androidx.annotation.Nullable;

import com.bumptech.glide.Glide;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.smartmess.android.R;
import com.smartmess.android.data.remote.ApiConfig;
import com.smartmess.android.model.Expense;

import java.io.File;

/**
 * Robust image loading and voucher inspection helper for SmartMess Android client.
 * Handles:
 * 1. Absolute HTTP / HTTPS URLs (e.g. https://mess.e-bd.shop/uploads/vouchers/...)
 * 2. Relative server paths (e.g. uploads/vouchers/... or storage/vouchers/...)
 * 3. Local offline file system paths (e.g. /data/user/0/... or /storage/emulated/...)
 * 4. file:// URIs
 */
public final class ImageLoaderHelper {

    private ImageLoaderHelper() {}

    /**
     * Resolves an image path/URL into an absolute HTTP URL or valid local path.
     */
    @Nullable
    public static String resolveImageUrl(@Nullable String path) {
        if (path == null) return null;
        String trimmed = path.trim();
        if (trimmed.isEmpty()) return null;

        if (trimmed.startsWith("http://") || trimmed.startsWith("https://")) {
            return trimmed;
        }

        if (trimmed.startsWith("file://")) {
            return trimmed;
        }

        // Local device file path
        if (trimmed.startsWith("/data/") || trimmed.startsWith("/storage/emulated/")) {
            return trimmed;
        }

        // Relative path on server: e.g. "uploads/..." or "storage/..." or "/uploads/..."
        String cleanRelative = trimmed.startsWith("/") ? trimmed.substring(1) : trimmed;
        String baseUrl = ApiConfig.BASE_URL;
        if (baseUrl.contains("/api/v1/")) {
            baseUrl = baseUrl.replace("/api/v1/", "/");
        }
        if (!baseUrl.endsWith("/")) {
            baseUrl += "/";
        }
        return baseUrl + cleanRelative;
    }

    /**
     * Loads a voucher image into an ImageView with appropriate placeholder and fallback.
     */
    public static void loadVoucher(Context context, @Nullable String path, ImageView imageView) {
        loadVoucher(context, path, imageView, R.drawable.ic_lucide_cart);
    }

    public static void loadVoucher(Context context, @Nullable String path, ImageView imageView, @DrawableRes int fallbackRes) {
        if (context == null || imageView == null) return;

        if (path == null || path.trim().isEmpty()) {
            imageView.setImageResource(fallbackRes);
            return;
        }

        String raw = path.trim();
        if (raw.startsWith("file://")) {
            raw = raw.substring(7);
        }

        File localFile = new File(raw);
        if (localFile.exists() && localFile.isFile()) {
            Glide.with(context)
                    .load(localFile)
                    .placeholder(fallbackRes)
                    .error(fallbackRes)
                    .into(imageView);
            return;
        }

        String fullUrl = resolveImageUrl(path);
        Glide.with(context)
                .load(fullUrl)
                .placeholder(fallbackRes)
                .error(fallbackRes)
                .into(imageView);
    }

    /**
     * Loads a user avatar with circle crop into an ImageView.
     */
    public static void loadAvatar(Context context, @Nullable String path, ImageView imageView) {
        if (context == null || imageView == null) return;

        if (path == null || path.trim().isEmpty()) {
            imageView.setImageResource(R.drawable.ic_member);
            return;
        }

        String raw = path.trim();
        if (raw.startsWith("file://")) {
            raw = raw.substring(7);
        }

        File localFile = new File(raw);
        if (localFile.exists() && localFile.isFile()) {
            Glide.with(context)
                    .load(localFile)
                    .circleCrop()
                    .placeholder(R.drawable.ic_member)
                    .error(R.drawable.ic_member)
                    .into(imageView);
            return;
        }

        String fullUrl = resolveImageUrl(path);
        Glide.with(context)
                .load(fullUrl)
                .circleCrop()
                .placeholder(R.drawable.ic_member)
                .error(R.drawable.ic_member)
                .into(imageView);
    }

    /**
     * Opens a rich full-view dialog for voucher inspection.
     */
    public static void showVoucherDialog(Context context, @Nullable Expense expense, @Nullable String fallbackUrl) {
        if (context == null) return;

        String title = (expense != null && expense.getTitle() != null) ? expense.getTitle() : "বাজার ভাউচার / রসিদ";
        String details = "";
        String url = (expense != null && expense.getVoucherImageUrl() != null) ? expense.getVoucherImageUrl() : fallbackUrl;

        if (expense != null) {
            String date = expense.getExpenseDate() != null ? expense.getExpenseDate() : "";
            details = "পরিমাণ: " + CurrencyUtils.format(expense.getAmount()) + (!date.isEmpty() ? (" • " + date) : "");
        }

        View dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_voucher_preview, null, false);
        if (dialogView != null) {
            TextView tvTitle = dialogView.findViewById(R.id.tvVoucherTitle);
            TextView tvDetails = dialogView.findViewById(R.id.tvVoucherDetails);
            ImageView ivFull = dialogView.findViewById(R.id.ivFullVoucher);

            if (tvTitle != null) tvTitle.setText(title);
            if (tvDetails != null) tvDetails.setText(details);
            if (ivFull != null) {
                loadVoucher(context, url, ivFull);
            }

            new MaterialAlertDialogBuilder(context)
                    .setView(dialogView)
                    .setPositiveButton("বন্ধ করুন", null)
                    .show();
        } else {
            ImageView iv = new ImageView(context);
            iv.setAdjustViewBounds(true);
            loadVoucher(context, url, iv);

            new MaterialAlertDialogBuilder(context)
                    .setTitle(title)
                    .setMessage(details)
                    .setView(iv)
                    .setPositiveButton("বন্ধ করুন", null)
                    .show();
        }
    }
}

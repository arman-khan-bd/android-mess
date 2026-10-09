package com.smartmess.android.ui.expenses;

import android.app.DatePickerDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.bumptech.glide.Glide;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.Chip;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.smartmess.android.R;
import com.smartmess.android.data.local.DatabaseHelper;
import com.smartmess.android.data.local.dao.ExpenseDao;
import com.smartmess.android.engine.PlanGateManager;
import com.smartmess.android.model.Expense;
import com.smartmess.android.utils.CurrencyUtils;
import com.smartmess.android.utils.DateTimeUtils;
import com.smartmess.android.utils.FileDownloadHelper;
import com.smartmess.android.utils.SessionManager;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class ActivityBazarLedger extends AppCompatActivity {

    private View btnBack;
    private MaterialButton btnExportPdf;
    private MaterialButton btnExportExcel;

    private Chip chipThisWeek;
    private Chip chipThisMonth;
    private Chip chipLastMonth;
    private Chip chipCustomRange;

    private TextView tvDateRangeDisplay;
    private TextView tvLedgerTotalCost;
    private TextView tvLedgerTotalCount;
    private TextView tvRawSubtotal;
    private TextView tvSharedSubtotal;
    private TextView tvUtilitySubtotal;

    private LinearLayout layoutLedgerTableRows;
    private TextView tvEmptyLedger;

    private ExpenseDao expenseDao;
    private SessionManager sessionManager;
    private PlanGateManager planGateManager;

    private String currentStartDate;
    private String currentEndDate;
    private final SimpleDateFormat isoFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
    private final SimpleDateFormat displayFormat = new SimpleDateFormat("dd MMM yyyy", Locale.US);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bazar_ledger);

        DatabaseHelper helper = DatabaseHelper.getInstance(this);
        expenseDao = new ExpenseDao(helper);
        sessionManager = new SessionManager(this);
        planGateManager = new PlanGateManager(this);

        initViews();
        setupFilterChips();
        setupExportButtons();

        // Default to This Month
        selectThisMonth();
    }

    private void initViews() {
        btnBack = findViewById(R.id.btnBack);
        btnExportPdf = findViewById(R.id.btnExportPdf);
        btnExportExcel = findViewById(R.id.btnExportExcel);

        chipThisWeek = findViewById(R.id.chipThisWeek);
        chipThisMonth = findViewById(R.id.chipThisMonth);
        chipLastMonth = findViewById(R.id.chipLastMonth);
        chipCustomRange = findViewById(R.id.chipCustomRange);

        tvDateRangeDisplay = findViewById(R.id.tvDateRangeDisplay);
        tvLedgerTotalCost = findViewById(R.id.tvLedgerTotalCost);
        tvLedgerTotalCount = findViewById(R.id.tvLedgerTotalCount);
        tvRawSubtotal = findViewById(R.id.tvRawSubtotal);
        tvSharedSubtotal = findViewById(R.id.tvSharedSubtotal);
        tvUtilitySubtotal = findViewById(R.id.tvUtilitySubtotal);

        layoutLedgerTableRows = findViewById(R.id.layoutLedgerTableRows);
        tvEmptyLedger = findViewById(R.id.tvEmptyLedger);

        if (btnBack != null) {
            btnBack.setOnClickListener(v -> finish());
        }
    }

    private void setupFilterChips() {
        chipThisWeek.setOnClickListener(v -> selectThisWeek());
        chipThisMonth.setOnClickListener(v -> selectThisMonth());
        chipLastMonth.setOnClickListener(v -> selectLastMonth());
        chipCustomRange.setOnClickListener(v -> showCustomDateRangePicker());
    }

    private void setupExportButtons() {
        btnExportPdf.setOnClickListener(v -> {
            if (!planGateManager.canExportPdf()) {
                planGateManager.showUpgradeBottomSheet(getSupportFragmentManager(), "Branded PDF Export",
                        "Watermark-free branded PDF audit statements are available on SmartMess Pro.");
                return;
            }
            FileDownloadHelper.downloadReport(this, "pdf", currentStartDate, currentEndDate, null);
        });

        btnExportExcel.setOnClickListener(v -> {
            if (!planGateManager.canExportPdf()) {
                planGateManager.showUpgradeBottomSheet(getSupportFragmentManager(), "Multi-Tab Excel Export",
                        "Comprehensive Excel workbooks (.xlsx) with daily market logs and settlement matrices require SmartMess Pro.");
                return;
            }
            FileDownloadHelper.downloadReport(this, "excel", currentStartDate, currentEndDate, null);
        });
    }

    // ==========================================
    // PRE-SET DATE FILTERS
    // ==========================================
    private void selectThisWeek() {
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.DAY_OF_WEEK, cal.getFirstDayOfWeek());
        currentStartDate = isoFormat.format(cal.getTime());

        Calendar endCal = Calendar.getInstance();
        endCal.setTime(cal.getTime());
        endCal.add(Calendar.DAY_OF_WEEK, 6);
        currentEndDate = isoFormat.format(endCal.getTime());

        updateFilterLabel("This Week");
        loadLedgerData();
    }

    private void selectThisMonth() {
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.DAY_OF_MONTH, 1);
        currentStartDate = isoFormat.format(cal.getTime());

        cal.set(Calendar.DAY_OF_MONTH, cal.getActualMaximum(Calendar.DAY_OF_MONTH));
        currentEndDate = isoFormat.format(cal.getTime());

        updateFilterLabel("This Month");
        loadLedgerData();
    }

    private void selectLastMonth() {
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.MONTH, -1);
        cal.set(Calendar.DAY_OF_MONTH, 1);
        currentStartDate = isoFormat.format(cal.getTime());

        cal.set(Calendar.DAY_OF_MONTH, cal.getActualMaximum(Calendar.DAY_OF_MONTH));
        currentEndDate = isoFormat.format(cal.getTime());

        updateFilterLabel("Last Month");
        loadLedgerData();
    }

    private void showCustomDateRangePicker() {
        Calendar cal = Calendar.getInstance();
        DatePickerDialog startDialog = new DatePickerDialog(this, (view, year, month, dayOfMonth) -> {
            Calendar chosenStart = Calendar.getInstance();
            chosenStart.set(year, month, dayOfMonth);
            currentStartDate = isoFormat.format(chosenStart.getTime());

            // Next pick end date
            DatePickerDialog endDialog = new DatePickerDialog(this, (view2, endYear, endMonth, endDay) -> {
                Calendar chosenEnd = Calendar.getInstance();
                chosenEnd.set(endYear, endMonth, endDay);
                currentEndDate = isoFormat.format(chosenEnd.getTime());

                updateFilterLabel("Custom Range");
                loadLedgerData();
            }, year, month, dayOfMonth);

            endDialog.setTitle("শেষ তারিখ নির্বাচন করুন");
            endDialog.show();

        }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH));

        startDialog.setTitle("শুরুর তারিখ নির্বাচন করুন");
        startDialog.show();
    }

    private void updateFilterLabel(String periodName) {
        try {
            Date start = isoFormat.parse(currentStartDate);
            Date end = isoFormat.parse(currentEndDate);
            if (start != null && end != null) {
                tvDateRangeDisplay.setText(periodName + ": " + displayFormat.format(start) + " - " + displayFormat.format(end));
            }
        } catch (Exception e) {
            tvDateRangeDisplay.setText(currentStartDate + " to " + currentEndDate);
        }
    }

    // ==========================================
    // DATA TABLE POPULATION
    // ==========================================
    private void loadLedgerData() {
        layoutLedgerTableRows.removeAllViews();

        long messId = sessionManager.getMessId();
        List<Expense> expenses = expenseDao.getExpensesInDateRange(messId, currentStartDate, currentEndDate);

        if (expenses == null || expenses.isEmpty()) {
            tvEmptyLedger.setVisibility(View.VISIBLE);
            tvLedgerTotalCost.setText("৳ 0.00");
            tvLedgerTotalCount.setText("0 Entries");
            tvRawSubtotal.setText("Raw: ৳0.00");
            tvSharedSubtotal.setText("Shared: ৳0.00");
            tvUtilitySubtotal.setText("Utility: ৳0.00");
            return;
        }

        tvEmptyLedger.setVisibility(View.GONE);

        double totalAmount = 0.0;
        double rawAmount = 0.0;
        double sharedAmount = 0.0;
        double utilAmount = 0.0;

        LayoutInflater inflater = LayoutInflater.from(this);

        for (int i = 0; i < expenses.size(); i++) {
            Expense e = expenses.get(i);
            totalAmount += e.getAmount();

            if (e.isRawMeal()) rawAmount += e.getAmount();
            else if (e.isSharedFood()) sharedAmount += e.getAmount();
            else if (e.isUtilityAsset()) utilAmount += e.getAmount();

            View rowView = inflater.inflate(R.layout.item_ledger_table_row, layoutLedgerTableRows, false);

            TextView tvDate = rowView.findViewById(R.id.tvRowDate);
            TextView tvTitle = rowView.findViewById(R.id.tvRowTitle);
            TextView tvCategory = rowView.findViewById(R.id.tvRowCategory);
            TextView tvAmount = rowView.findViewById(R.id.tvRowAmount);
            TextView tvBuyer = rowView.findViewById(R.id.tvRowBuyer);
            TextView tvVoucher = rowView.findViewById(R.id.tvRowVoucher);

            tvDate.setText(e.getExpenseDate());
            tvTitle.setText(e.getTitle() != null ? e.getTitle() : "Bazar Expense");
            tvAmount.setText(CurrencyUtils.format(e.getAmount()));

            String buyer = e.getBuyerName() != null ? e.getBuyerName() : "Buyer #" + e.getBuyerUserId();
            tvBuyer.setText(buyer);

            // Category styling
            if (e.isRawMeal()) {
                tvCategory.setText("Raw Meal");
                tvCategory.setTextColor(ContextCompat.getColor(this, R.color.credit_green));
                tvCategory.setBackgroundResource(R.drawable.badge_credit);
            } else if (e.isSharedFood()) {
                tvCategory.setText("Shared Food");
                tvCategory.setTextColor(ContextCompat.getColor(this, R.color.accent));
                tvCategory.setBackgroundResource(R.drawable.badge_credit);
            } else if (e.isUtilityAsset()) {
                tvCategory.setText("Utility/Asset");
                tvCategory.setTextColor(ContextCompat.getColor(this, R.color.info_blue));
                tvCategory.setBackgroundResource(R.drawable.badge_credit);
            } else {
                tvCategory.setText("General");
                tvCategory.setTextColor(ContextCompat.getColor(this, R.color.warning_amber));
                tvCategory.setBackgroundResource(R.drawable.badge_credit);
            }

            // Voucher thumbnail or modal preview
            final String voucherUrl = e.getVoucherImageUrl();
            if (voucherUrl != null && !voucherUrl.trim().isEmpty()) {
                tvVoucher.setText("📎 View");
                tvVoucher.setTextColor(ContextCompat.getColor(this, R.color.accent));
                tvVoucher.setOnClickListener(v -> showVoucherPreviewDialog(e, voucherUrl.trim()));
            } else {
                tvVoucher.setText("—");
                tvVoucher.setTextColor(ContextCompat.getColor(this, R.color.text_muted));
                tvVoucher.setOnClickListener(null);
            }

            // Zebra striping for table readability
            if (i % 2 == 1) {
                rowView.setBackgroundColor(ContextCompat.getColor(this, R.color.surface_variant));
            } else {
                rowView.setBackgroundColor(ContextCompat.getColor(this, R.color.surface));
            }

            layoutLedgerTableRows.addView(rowView);

            // 1dp horizontal divider
            View divider = new View(this);
            divider.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 1));
            divider.setBackgroundColor(ContextCompat.getColor(this, R.color.border_stroke));
            layoutLedgerTableRows.addView(divider);
        }

        tvLedgerTotalCost.setText(CurrencyUtils.format(totalAmount));
        tvLedgerTotalCount.setText(expenses.size() + (expenses.size() == 1 ? " Entry" : " Entries"));
        tvRawSubtotal.setText("Raw: " + CurrencyUtils.format(rawAmount));
        tvSharedSubtotal.setText("Shared: " + CurrencyUtils.format(sharedAmount));
        tvUtilitySubtotal.setText("Utility: " + CurrencyUtils.format(utilAmount));
    }

    private void showVoucherPreviewDialog(Expense expense, String voucherUrl) {
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_voucher_preview, null, false);
        if (dialogView == null) {
            // Fallback: simple dialog
            ImageView iv = new ImageView(this);
            iv.setAdjustViewBounds(true);
            Glide.with(this).load(voucherUrl).into(iv);
            new MaterialAlertDialogBuilder(this)
                    .setTitle(expense.getTitle() + " (৳" + expense.getAmount() + ")")
                    .setView(iv)
                    .setPositiveButton("বন্ধ করুন", null)
                    .show();
            return;
        }

        ImageView ivPreview = dialogView.findViewById(R.id.ivFullVoucher);
        TextView tvTitle = dialogView.findViewById(R.id.tvVoucherTitle);
        TextView tvDetails = dialogView.findViewById(R.id.tvVoucherDetails);

        if (tvTitle != null) tvTitle.setText(expense.getTitle());
        if (tvDetails != null) {
            tvDetails.setText("Amount: " + CurrencyUtils.format(expense.getAmount()) + " • Date: " + expense.getExpenseDate());
        }
        if (ivPreview != null) {
            Glide.with(this).load(voucherUrl).into(ivPreview);
        }

        new MaterialAlertDialogBuilder(this)
                .setView(dialogView)
                .setPositiveButton("বন্ধ করুন", null)
                .show();
    }
}

package com.smartmess.android.ui.reports;

import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.smartmess.android.R;
import com.smartmess.android.data.local.DatabaseHelper;
import com.smartmess.android.data.local.dao.MessDao;
import com.smartmess.android.engine.AccountingEngine;
import com.smartmess.android.engine.CalculationModels.CycleSummary;
import com.smartmess.android.engine.PlanGateManager;
import com.smartmess.android.model.Mess;
import com.smartmess.android.utils.CurrencyUtils;
import com.smartmess.android.utils.DateTimeUtils;
import com.smartmess.android.utils.SessionManager;

public class SummaryReportActivity extends AppCompatActivity {

    private TextView tvReportDates;
    private TextView tvReportMealRate;
    private TextView tvReportTotalMeals;
    private TextView tvReportMessCash;
    private TextView tvPoolBreakdownText;
    private RecyclerView rvMemberBalances;
    private MaterialButton btnExportReport;

    private View cardFlexibleMealsStatement;
    private MaterialButton btnViewFlexibleDetails;
    private TextView tvFlexibleOverviewSummary;
    private android.widget.LinearLayout layoutFlexibleReportList;

    private AccountingEngine accountingEngine;
    private MessDao messDao;
    private com.smartmess.android.data.local.dao.MealDao mealDao;
    private com.smartmess.android.data.local.dao.UserDao userDao;
    private SessionManager sessionManager;
    private PlanGateManager planGateManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_summary_report);

        tvReportDates = findViewById(R.id.tvReportDates);
        tvReportMealRate = findViewById(R.id.tvReportMealRate);
        tvReportTotalMeals = findViewById(R.id.tvReportTotalMeals);
        tvReportMessCash = findViewById(R.id.tvReportMessCash);
        tvPoolBreakdownText = findViewById(R.id.tvPoolBreakdownText);
        rvMemberBalances = findViewById(R.id.rvMemberBalances);
        btnExportReport = findViewById(R.id.btnExportReport);

        cardFlexibleMealsStatement = findViewById(R.id.cardFlexibleMealsStatement);
        btnViewFlexibleDetails = findViewById(R.id.btnViewFlexibleDetails);
        tvFlexibleOverviewSummary = findViewById(R.id.tvFlexibleOverviewSummary);
        layoutFlexibleReportList = findViewById(R.id.layoutFlexibleReportList);

        rvMemberBalances.setLayoutManager(new LinearLayoutManager(this));

        DatabaseHelper helper = DatabaseHelper.getInstance(this);
        messDao = new MessDao(helper);
        mealDao = new com.smartmess.android.data.local.dao.MealDao(helper);
        userDao = new com.smartmess.android.data.local.dao.UserDao(helper);
        sessionManager = new SessionManager(this);
        accountingEngine = new AccountingEngine(this);
        planGateManager = new PlanGateManager(this);

        loadCycleReport();

        btnExportReport.setOnClickListener(v -> {
            if (!planGateManager.canExportPdf()) {
                planGateManager.showUpgradeBottomSheet(getSupportFragmentManager(), "Branded PDF & Excel Export",
                        "Watermark-free branded PDF and Excel statement exports are a Pro feature.");
            } else {
                long messId = sessionManager.getMessId();
                Mess mess = messDao.getById(messId);
                int startDay = mess != null ? mess.getCycleStartDay() : 1;
                String start = DateTimeUtils.currentMonthStart(startDay);
                String end = DateTimeUtils.currentMonthEnd(startDay);

                new com.google.android.material.dialog.MaterialAlertDialogBuilder(this)
                        .setTitle("ডাউনলোডের ফরম্যাট নির্বাচন করুন")
                        .setItems(new CharSequence[]{"PDF বিবরণী - PDF Statement (.pdf)", "Excel স্প্রেডশিট - Excel Workbook (.csv)"}, (dialog, which) -> {
                            if (which == 0) {
                                com.smartmess.android.utils.FileDownloadHelper.downloadReport(this, "pdf", start, end, null);
                            } else {
                                com.smartmess.android.utils.FileDownloadHelper.downloadReport(this, "excel", start, end, null);
                            }
                        })
                        .setNegativeButton("বাতিল", null)
                        .show();
            }
        });
    }

    private void loadCycleReport() {
        long messId = sessionManager.getMessId();
        Mess mess = messDao.getById(messId);
        int startDay = mess != null ? mess.getCycleStartDay() : 1;
        String start = DateTimeUtils.currentMonthStart(startDay);
        String end = DateTimeUtils.currentMonthEnd(startDay);

        tvReportDates.setText(DateTimeUtils.formatDisplayDate(start) + " - " + DateTimeUtils.formatDisplayDate(end));

        CycleSummary summary = accountingEngine.calculateCycleSummary(messId, start, end);

        tvReportMealRate.setText(CurrencyUtils.format(summary.getMealRate()));
        tvReportTotalMeals.setText(summary.getTotalMessMeals() + " Meals");
        tvReportMessCash.setText(CurrencyUtils.format(summary.getMessCashInHand()));

        double flexTotal = summary.getTotalFlexibleMealExpense();
        StringBuilder breakdown = new StringBuilder();
        breakdown.append("• Billed Meal Budget: ").append(CurrencyUtils.format(summary.getBillingMealRate())).append(" / meal\n");
        breakdown.append("• Actual Working Meal Rate: ").append(CurrencyUtils.format(summary.getWorkingMealRate())).append(" / meal\n");
        breakdown.append("• Meals Total Cost: ").append(CurrencyUtils.format(summary.getTotalMealsCost()))
                .append(" (").append(summary.getTotalMessMeals()).append(" meals @ ").append(CurrencyUtils.format(summary.getBillingMealRate())).append(")\n");
        if (flexTotal > 0) {
            breakdown.append("• নমনীয় মিলের মোট খরচ (কাঁচামাল ভিত্তিক): ").append(CurrencyUtils.format(flexTotal)).append("\n");
        }
        breakdown.append("• Total Mess Expenses: ").append(CurrencyUtils.format(summary.getTotalAllExpenses())).append("\n");
        breakdown.append("  - Variable Meal Pool (Bazar): ").append(CurrencyUtils.format(summary.getTotalRawMealExpense())).append("\n");
        breakdown.append("  - Shared Fixed Pool (Oil, salt, gas): ").append(CurrencyUtils.format(summary.getTotalSharedFoodExpense())).append("\n");
        breakdown.append("  - Asset & Utility Pool (Rent, maid, Wi-Fi): ").append(CurrencyUtils.format(summary.getTotalUtilityAssetExpense())).append("\n");
        breakdown.append("  - Total SMS Charges: ").append(CurrencyUtils.format(summary.getTotalSmsChargeExpense())).append("\n");
        breakdown.append("• Budget Surplus / Savings: ").append(CurrencyUtils.format(summary.getBudgetSurplusDeficit())).append("\n");
        breakdown.append("• Total Mess Deposits Received: ").append(CurrencyUtils.format(summary.getTotalMessDeposits()));

        tvPoolBreakdownText.setText(breakdown.toString());

        // Load users cache for name resolution
        java.util.Map<Long, String> userNameMap = new java.util.HashMap<>();
        java.util.List<com.smartmess.android.model.User> allUsers = userDao.getAllMembersByMess(messId);
        if (allUsers != null) {
            for (com.smartmess.android.model.User u : allUsers) {
                userNameMap.put(u.getId(), u.getName());
            }
        }

        // Setup Flexible Meal Card
        java.util.List<com.smartmess.android.model.Meal> flexibleMeals = mealDao.getAllFlexibleMealsForMess(messId, start, end);
        if (cardFlexibleMealsStatement != null) {
            if (flexibleMeals != null && !flexibleMeals.isEmpty()) {
                cardFlexibleMealsStatement.setVisibility(android.view.View.VISIBLE);
                tvFlexibleOverviewSummary.setText("নমনীয় মিল বাবদ মোট খরচ: " + CurrencyUtils.format(flexTotal)
                        + " (" + flexibleMeals.size() + "টি এন্ট্রি লিপিবদ্ধ)");

                layoutFlexibleReportList.removeAllViews();
                int previewCount = Math.min(flexibleMeals.size(), 4);
                for (int i = 0; i < previewCount; i++) {
                    com.smartmess.android.model.Meal fm = flexibleMeals.get(i);
                    String uName = userNameMap.containsKey(fm.getUserId()) ? userNameMap.get(fm.getUserId()) : "সদস্য #" + fm.getUserId();
                    layoutFlexibleReportList.addView(createFlexibleRowView(fm, uName));
                }

                btnViewFlexibleDetails.setOnClickListener(v -> showFlexibleMealsStatementDialog(null, null, start, end, userNameMap));
            } else {
                cardFlexibleMealsStatement.setVisibility(android.view.View.GONE);
            }
        }

        if (summary.getMemberBalances() != null) {
            MemberSummaryAdapter adapter = new MemberSummaryAdapter(summary.getMemberBalances(), member -> {
                showFlexibleMealsStatementDialog(member.getUserName(), member.getUserId(), start, end, userNameMap);
            });
            rvMemberBalances.setAdapter(adapter);
        }
    }

    private android.view.View createFlexibleRowView(com.smartmess.android.model.Meal fm, String memberName) {
        android.widget.LinearLayout row = new android.widget.LinearLayout(this);
        row.setOrientation(android.widget.LinearLayout.HORIZONTAL);
        row.setPadding(0, 8, 0, 8);
        row.setGravity(android.view.Gravity.CENTER_VERTICAL);

        android.widget.LinearLayout leftLayout = new android.widget.LinearLayout(this);
        leftLayout.setOrientation(android.widget.LinearLayout.VERTICAL);
        android.widget.LinearLayout.LayoutParams lp = new android.widget.LinearLayout.LayoutParams(0, android.widget.LinearLayout.LayoutParams.WRAP_CONTENT, 1.0f);
        leftLayout.setLayoutParams(lp);

        String rawItem = (fm.getRawBazarName() != null && !fm.getRawBazarName().isEmpty()) ? fm.getRawBazarName() : "কাঁচামাল";
        String mealTypeBn = formatMealTypeBangla(fm.getMealType());

        TextView tvTitle = new TextView(this);
        tvTitle.setText(memberName + " • " + rawItem + " (" + mealTypeBn + ")");
        tvTitle.setTextColor(getResources().getColor(R.color.text_primary));
        tvTitle.setTextSize(13);
        tvTitle.setTypeface(null, android.graphics.Typeface.BOLD);
        leftLayout.addView(tvTitle);

        TextView tvSub = new TextView(this);
        tvSub.setText(DateTimeUtils.formatDisplayDate(fm.getMealDate()) + (fm.getRawBazarAmount() > 0 ? " • মোট বাজার: " + CurrencyUtils.format(fm.getRawBazarAmount()) : ""));
        tvSub.setTextColor(getResources().getColor(R.color.text_secondary));
        tvSub.setTextSize(11);
        leftLayout.addView(tvSub);

        row.addView(leftLayout);

        TextView tvPrice = new TextView(this);
        tvPrice.setText(CurrencyUtils.format(fm.getSharedPrice()));
        tvPrice.setTextColor(getResources().getColor(R.color.primary));
        tvPrice.setTextSize(13);
        tvPrice.setTypeface(null, android.graphics.Typeface.BOLD);
        row.addView(tvPrice);

        return row;
    }

    private void showFlexibleMealsStatementDialog(String filterMemberName, Long filterUserId, String start, String end, java.util.Map<Long, String> userNameMap) {
        long messId = sessionManager.getMessId();
        java.util.List<com.smartmess.android.model.Meal> list;
        if (filterUserId != null && filterUserId > 0) {
            list = mealDao.getFlexibleMealsForUser(messId, filterUserId, start, end);
        } else {
            list = mealDao.getAllFlexibleMealsForMess(messId, start, end);
        }

        com.google.android.material.bottomsheet.BottomSheetDialog dialog =
                new com.google.android.material.bottomsheet.BottomSheetDialog(this, R.style.CustomBottomSheetDialogTheme);
        android.view.View sheetView = getLayoutInflater().inflate(R.layout.activity_user_profile, null);

        // Build dialog layout programmatically for clean table/list presentation
        android.widget.LinearLayout root = new android.widget.LinearLayout(this);
        root.setOrientation(android.widget.LinearLayout.VERTICAL);
        root.setPadding(32, 28, 32, 32);

        // Title
        TextView tvDialogTitle = new TextView(this);
        String titleStr = (filterMemberName != null && !filterMemberName.isEmpty())
                ? filterMemberName + " - নমনীয় মিলের তারিখভিত্তিক স্টেটমেন্ট"
                : "মেসের নমনীয় মিলের সামগ্রিক স্টেটমেন্ট";
        tvDialogTitle.setText(titleStr);
        tvDialogTitle.setTextSize(17);
        tvDialogTitle.setTypeface(null, android.graphics.Typeface.BOLD);
        tvDialogTitle.setTextColor(getResources().getColor(R.color.text_primary));
        root.addView(tvDialogTitle);

        // Date range subtitle
        double sumCost = 0.0;
        if (list != null) {
            for (com.smartmess.android.model.Meal m : list) {
                sumCost += m.getSharedPrice();
            }
        }

        TextView tvSub = new TextView(this);
        tvSub.setText(DateTimeUtils.formatDisplayDate(start) + " হতে " + DateTimeUtils.formatDisplayDate(end)
                + " • মোট খরচ: " + CurrencyUtils.format(sumCost) + " (" + (list != null ? list.size() : 0) + "টি মিল)");
        tvSub.setTextColor(getResources().getColor(R.color.text_secondary));
        tvSub.setTextSize(12);
        tvSub.setPadding(0, 6, 0, 16);
        root.addView(tvSub);

        // Divider
        android.view.View div = new android.view.View(this);
        div.setLayoutParams(new android.widget.LinearLayout.LayoutParams(android.widget.LinearLayout.LayoutParams.MATCH_PARENT, 2));
        div.setBackgroundColor(getResources().getColor(R.color.border_stroke));
        root.addView(div);

        // Scrollable list container
        android.widget.ScrollView scrollView = new android.widget.ScrollView(this);
        android.widget.LinearLayout.LayoutParams svParams = new android.widget.LinearLayout.LayoutParams(
                android.widget.LinearLayout.LayoutParams.MATCH_PARENT, 0, 1.0f);
        svParams.topMargin = 12;
        scrollView.setLayoutParams(svParams);

        android.widget.LinearLayout itemsLayout = new android.widget.LinearLayout(this);
        itemsLayout.setOrientation(android.widget.LinearLayout.VERTICAL);

        if (list == null || list.isEmpty()) {
            TextView emptyTv = new TextView(this);
            emptyTv.setText("এই সময়ের মধ্যে কোনো নমনীয় মিল এন্ট্রি পাওয়া যায়নি।");
            emptyTv.setTextColor(getResources().getColor(R.color.text_secondary));
            emptyTv.setTextSize(13);
            emptyTv.setPadding(0, 32, 0, 32);
            emptyTv.setGravity(android.view.Gravity.CENTER);
            itemsLayout.addView(emptyTv);
        } else {
            for (com.smartmess.android.model.Meal fm : list) {
                String uName = userNameMap.containsKey(fm.getUserId()) ? userNameMap.get(fm.getUserId()) : "সদস্য #" + fm.getUserId();
                itemsLayout.addView(createFlexibleRowView(fm, uName));

                android.view.View itemDiv = new android.view.View(this);
                itemDiv.setLayoutParams(new android.widget.LinearLayout.LayoutParams(android.widget.LinearLayout.LayoutParams.MATCH_PARENT, 1));
                itemDiv.setBackgroundColor(getResources().getColor(R.color.border_stroke));
                itemsLayout.addView(itemDiv);
            }
        }

        scrollView.addView(itemsLayout);
        root.addView(scrollView);

        // Close button
        MaterialButton btnClose = new MaterialButton(this);
        btnClose.setText("বন্ধ করুন");
        android.widget.LinearLayout.LayoutParams btnLp = new android.widget.LinearLayout.LayoutParams(
                android.widget.LinearLayout.LayoutParams.MATCH_PARENT, android.widget.LinearLayout.LayoutParams.WRAP_CONTENT);
        btnLp.topMargin = 16;
        btnClose.setLayoutParams(btnLp);
        btnClose.setOnClickListener(v -> dialog.dismiss());
        root.addView(btnClose);

        dialog.setContentView(root);
        dialog.show();
    }

    private String formatMealTypeBangla(String type) {
        if (type == null) return "সাধারণ";
        switch (type.toLowerCase()) {
            case "lunch": return "দুপুর";
            case "dinner": return "রাত";
            case "breakfast": return "সকাল";
            case "special": return "বিশেষ";
            default: return type;
        }
    }
}

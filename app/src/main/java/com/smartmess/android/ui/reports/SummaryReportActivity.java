package com.smartmess.android.ui.reports;

import android.os.Bundle;
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

    private AccountingEngine accountingEngine;
    private MessDao messDao;
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

        rvMemberBalances.setLayoutManager(new LinearLayoutManager(this));

        DatabaseHelper helper = DatabaseHelper.getInstance(this);
        messDao = new MessDao(helper);
        sessionManager = new SessionManager(this);
        accountingEngine = new AccountingEngine(this);
        planGateManager = new PlanGateManager(this);

        loadCycleReport();

        btnExportReport.setOnClickListener(v -> {
            long messId = sessionManager.getMessId();
            if (!planGateManager.canExportReports(messId)) {
                planGateManager.showUpgradeDialog(this, "Branded PDF Export",
                        "Branded PDF/Excel reports are an Enterprise tier feature. Upgrade to export.");
            } else {
                Toast.makeText(this, "Generating settlement report...", Toast.LENGTH_SHORT).show();
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

        String breakdown = "• Variable Meal Pool (Bazar): " + CurrencyUtils.format(summary.getTotalRawMealExpense()) + "\n"
                + "• Shared Fixed Pool (Oil, salt, gas): " + CurrencyUtils.format(summary.getTotalSharedFoodExpense()) + "\n"
                + "• Asset & Utility Pool (Rent, maid, Wi-Fi): " + CurrencyUtils.format(summary.getTotalUtilityAssetExpense()) + "\n"
                + "• Total SMS Charges: " + CurrencyUtils.format(summary.getTotalSmsChargeExpense()) + "\n"
                + "• Total Mess Deposits Received: " + CurrencyUtils.format(summary.getTotalMessDeposits());
        tvPoolBreakdownText.setText(breakdown);

        if (summary.getMemberBalances() != null) {
            MemberSummaryAdapter adapter = new MemberSummaryAdapter(summary.getMemberBalances());
            rvMemberBalances.setAdapter(adapter);
        }
    }
}

package com.smartmess.android.ui.expenses;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.smartmess.android.R;
import com.smartmess.android.data.local.DatabaseHelper;
import com.smartmess.android.data.local.dao.ExpenseDao;
import com.smartmess.android.engine.PlanGateManager;
import com.smartmess.android.model.Expense;
import com.smartmess.android.utils.DateTimeUtils;
import com.smartmess.android.utils.SessionManager;

import java.util.List;

public class ExpenseListActivity extends AppCompatActivity {

    private RecyclerView rvExpenses;
    private MaterialButton btnAddExpense;
    private TextView tvHistoryGateBanner;
    private ExpenseDao expenseDao;
    private SessionManager sessionManager;
    private PlanGateManager planGateManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_expense_list);

        rvExpenses = findViewById(R.id.rvExpenses);
        btnAddExpense = findViewById(R.id.btnAddExpense);
        tvHistoryGateBanner = findViewById(R.id.tvHistoryGateBanner);

        rvExpenses.setLayoutManager(new LinearLayoutManager(this));
        DatabaseHelper helper = DatabaseHelper.getInstance(this);
        expenseDao = new ExpenseDao(helper);
        sessionManager = new SessionManager(this);
        planGateManager = new PlanGateManager(this);

        btnAddExpense.setOnClickListener(v -> startActivity(new Intent(this, AddExpenseActivity.class)));

        tvHistoryGateBanner.setOnClickListener(v -> {
            planGateManager.showUpgradeBottomSheet(getSupportFragmentManager(), "Lifetime Ledger History",
                    "Access lifetime expense ledgers, voucher attachments, and audit trail with SmartMess Pro.");
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadExpenses();
    }

    private void loadExpenses() {
        long messId = sessionManager.getMessId();
        boolean canFull = planGateManager.canViewFullHistory();
        List<Expense> expenses;
        if (canFull) {
            tvHistoryGateBanner.setVisibility(View.GONE);
            expenses = expenseDao.getRecentExpenses(messId, 500);
        } else {
            tvHistoryGateBanner.setVisibility(View.VISIBLE);
            String thirtyDaysAgo = DateTimeUtils.daysAgo(30);
            expenses = expenseDao.getExpensesSince(messId, thirtyDaysAgo);
        }
        ExpenseAdapter adapter = new ExpenseAdapter(expenses);
        rvExpenses.setAdapter(adapter);
    }
}

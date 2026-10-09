package com.smartmess.android.ui.expenses;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
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

public class FragmentExpenseList extends Fragment {

    private RecyclerView rvExpenses;
    private MaterialButton btnAddExpense;
    private TextView tvHistoryGateBanner;
    private ExpenseDao expenseDao;
    private SessionManager sessionManager;
    private PlanGateManager planGateManager;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_expense_list, container, false);

        rvExpenses = v.findViewById(R.id.rvExpenses);
        btnAddExpense = v.findViewById(R.id.btnAddExpense);
        tvHistoryGateBanner = v.findViewById(R.id.tvHistoryGateBanner);

        rvExpenses.setLayoutManager(new LinearLayoutManager(requireContext()));
        DatabaseHelper helper = DatabaseHelper.getInstance(requireContext());
        expenseDao = new ExpenseDao(helper);
        sessionManager = new SessionManager(requireContext());
        planGateManager = new PlanGateManager(requireContext());

        btnAddExpense.setOnClickListener(view -> startActivity(new Intent(requireContext(), ActivityAddExpense.class)));

        tvHistoryGateBanner.setOnClickListener(view -> {
            planGateManager.showUpgradeBottomSheet(getParentFragmentManager(), "Lifetime Ledger History",
                    "Access lifetime expense ledgers, multi-category splits, and voucher attachments with SmartMess Pro.");
        });

        loadExpenses();
        return v;
    }

    @Override
    public void onResume() {
        super.onResume();
        loadExpenses();
    }

    private void loadExpenses() {
        if (!isAdded()) return;
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

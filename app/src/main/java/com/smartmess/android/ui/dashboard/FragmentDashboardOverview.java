package com.smartmess.android.ui.dashboard;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.android.material.button.MaterialButton;
import com.smartmess.android.R;
import com.smartmess.android.engine.AccountingEngine;
import com.smartmess.android.engine.CalculationModels.CycleSummary;
import com.smartmess.android.ui.expenses.ActivityAddExpense;
import com.smartmess.android.ui.sms.ActivitySmsDispatch;
import com.smartmess.android.utils.CurrencyUtils;
import com.smartmess.android.utils.SessionManager;

import java.util.Locale;

public class FragmentDashboardOverview extends Fragment {

    private TextView tvLiveMealRate;
    private TextView tvTotalMessMeals;
    private TextView tvCashInHand;
    private TextView tvActiveMembers;
    private TextView tvRawMealCost;
    private TextView tvSharedFoodCost;
    private TextView tvUtilityCost;
    private MaterialButton btnQuickAddExpense;
    private MaterialButton btnQuickSmsDispatch;

    private AccountingEngine accountingEngine;
    private SessionManager sessionManager;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_dashboard_overview, container, false);

        accountingEngine = new AccountingEngine(requireContext());
        sessionManager = new SessionManager(requireContext());

        tvLiveMealRate = v.findViewById(R.id.tvLiveMealRate);
        tvTotalMessMeals = v.findViewById(R.id.tvTotalMessMeals);
        tvCashInHand = v.findViewById(R.id.tvCashInHand);
        tvActiveMembers = v.findViewById(R.id.tvActiveMembers);
        tvRawMealCost = v.findViewById(R.id.tvRawMealCost);
        tvSharedFoodCost = v.findViewById(R.id.tvSharedFoodCost);
        tvUtilityCost = v.findViewById(R.id.tvUtilityCost);
        btnQuickAddExpense = v.findViewById(R.id.btnQuickAddExpense);
        btnQuickSmsDispatch = v.findViewById(R.id.btnQuickSmsDispatch);

        btnQuickAddExpense.setOnClickListener(view -> startActivity(new Intent(requireContext(), ActivityAddExpense.class)));
        btnQuickSmsDispatch.setOnClickListener(view -> startActivity(new Intent(requireContext(), ActivitySmsDispatch.class)));

        loadStats();
        return v;
    }

    @Override
    public void onResume() {
        super.onResume();
        loadStats();
    }

    private void loadStats() {
        long messId = sessionManager.getMessId();
        CycleSummary summary = accountingEngine.calculateCurrentCycleSummary(messId);

        tvLiveMealRate.setText(CurrencyUtils.format(summary.getLiveMealRate()));
        tvTotalMessMeals.setText(String.format(Locale.US, "%.1f Meals Logged", summary.getTotalMeals()));
        tvCashInHand.setText(CurrencyUtils.format(summary.getCashInHand()));
        tvActiveMembers.setText(summary.getActiveMemberCount() + " Active Members");

        tvRawMealCost.setText(CurrencyUtils.format(summary.getRawMealCost()));
        tvSharedFoodCost.setText(CurrencyUtils.format(summary.getSharedFoodCost()));
        tvUtilityCost.setText(CurrencyUtils.format(summary.getUtilityCost()));
    }
}

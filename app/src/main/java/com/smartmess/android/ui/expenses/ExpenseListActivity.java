package com.smartmess.android.ui.expenses;

import android.content.Intent;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.smartmess.android.R;
import com.smartmess.android.data.local.DatabaseHelper;
import com.smartmess.android.data.local.dao.ExpenseDao;
import com.smartmess.android.model.Expense;
import com.smartmess.android.utils.SessionManager;

import java.util.List;

public class ExpenseListActivity extends AppCompatActivity {

    private RecyclerView rvExpenses;
    private MaterialButton btnAddExpense;
    private ExpenseDao expenseDao;
    private SessionManager sessionManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_expense_list);

        rvExpenses = findViewById(R.id.rvExpenses);
        btnAddExpense = findViewById(R.id.btnAddExpense);

        rvExpenses.setLayoutManager(new LinearLayoutManager(this));
        DatabaseHelper helper = DatabaseHelper.getInstance(this);
        expenseDao = new ExpenseDao(helper);
        sessionManager = new SessionManager(this);

        btnAddExpense.setOnClickListener(v -> startActivity(new Intent(this, AddExpenseActivity.class)));
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadExpenses();
    }

    private void loadExpenses() {
        long messId = sessionManager.getMessId();
        List<Expense> expenses = expenseDao.getRecentExpenses(messId, 60);
        ExpenseAdapter adapter = new ExpenseAdapter(expenses);
        rvExpenses.setAdapter(adapter);
    }
}

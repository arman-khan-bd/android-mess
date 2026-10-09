package com.smartmess.android.ui.deposits;

import android.content.Intent;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.smartmess.android.R;
import com.smartmess.android.data.local.DatabaseHelper;
import com.smartmess.android.data.local.dao.DepositDao;
import com.smartmess.android.model.Deposit;
import com.smartmess.android.utils.SessionManager;

import java.util.List;

public class DepositListActivity extends AppCompatActivity {

    private RecyclerView rvDeposits;
    private MaterialButton btnAddDeposit;
    private DepositDao depositDao;
    private SessionManager sessionManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_deposit_list);

        rvDeposits = findViewById(R.id.rvDeposits);
        btnAddDeposit = findViewById(R.id.btnAddDeposit);

        rvDeposits.setLayoutManager(new LinearLayoutManager(this));
        DatabaseHelper helper = DatabaseHelper.getInstance(this);
        depositDao = new DepositDao(helper);
        sessionManager = new SessionManager(this);

        btnAddDeposit.setOnClickListener(v -> startActivity(new Intent(this, AddDepositActivity.class)));
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadDeposits();
    }

    private void loadDeposits() {
        long messId = sessionManager.getMessId();
        List<Deposit> list = depositDao.getRecentDeposits(messId, 60);
        DepositAdapter adapter = new DepositAdapter(list);
        rvDeposits.setAdapter(adapter);
    }
}

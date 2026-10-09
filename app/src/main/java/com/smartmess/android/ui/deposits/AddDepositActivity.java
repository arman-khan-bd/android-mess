package com.smartmess.android.ui.deposits;

import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.smartmess.android.R;
import com.smartmess.android.data.local.DatabaseHelper;
import com.smartmess.android.data.local.dao.DepositDao;
import com.smartmess.android.data.local.dao.UserDao;
import com.smartmess.android.model.Deposit;
import com.smartmess.android.model.User;
import com.smartmess.android.utils.DateTimeUtils;
import com.smartmess.android.utils.SessionManager;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class AddDepositActivity extends AppCompatActivity {

    private Spinner spDepositMember;
    private EditText etDepositAmount;
    private EditText etDepositDate;
    private EditText etDepositNote;
    private MaterialButton btnSaveDeposit;

    private DepositDao depositDao;
    private UserDao userDao;
    private SessionManager sessionManager;
    private List<User> members = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_deposit);

        spDepositMember = findViewById(R.id.spDepositMember);
        etDepositAmount = findViewById(R.id.etDepositAmount);
        etDepositDate = findViewById(R.id.etDepositDate);
        etDepositNote = findViewById(R.id.etDepositNote);
        btnSaveDeposit = findViewById(R.id.btnSaveDeposit);

        DatabaseHelper helper = DatabaseHelper.getInstance(this);
        depositDao = new DepositDao(helper);
        userDao = new UserDao(helper);
        sessionManager = new SessionManager(this);

        etDepositDate.setText(DateTimeUtils.currentDate());
        loadMembers();

        btnSaveDeposit.setOnClickListener(v -> saveDeposit());
    }

    private void loadMembers() {
        long messId = sessionManager.getMessId();
        members = userDao.getActiveMembersByMess(messId);
        List<String> names = new ArrayList<>();
        for (User u : members) {
            names.add(u.getName());
        }

        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, names);
        spDepositMember.setAdapter(adapter);
    }

    private void saveDeposit() {
        if (members.isEmpty()) {
            Toast.makeText(this, "কোনো সক্রিয় সদস্য পাওয়া যায়নি", Toast.LENGTH_SHORT).show();
            return;
        }

        String amountStr = etDepositAmount.getText().toString().trim();
        String date = etDepositDate.getText().toString().trim();
        String note = etDepositNote.getText().toString().trim();

        if (amountStr.isEmpty()) {
            Toast.makeText(this, "দয়া করে জমার পরিমাণ লিখুন", Toast.LENGTH_SHORT).show();
            return;
        }

        double amount;
        try {
            amount = Double.parseDouble(amountStr);
        } catch (NumberFormatException e) {
            Toast.makeText(this, "টাকার পরিমাণ সঠিক নয়", Toast.LENGTH_SHORT).show();
            return;
        }

        if (date.isEmpty()) date = DateTimeUtils.currentDate();

        User selected = members.get(spDepositMember.getSelectedItemPosition());
        String now = DateTimeUtils.nowIso();

        Deposit deposit = new Deposit(
                UUID.randomUUID().toString(),
                sessionManager.getMessId(),
                selected.getId(),
                amount,
                date,
                note
        );
        deposit.setCreatedAt(now);
        deposit.setUpdatedAt(now);

        depositDao.insertOrUpdate(deposit);

        com.smartmess.android.utils.NotificationCenterHelper.postNotification(
                getApplicationContext(),
                sessionManager.getMessId(),
                "জমা রেকর্ড করা হয়েছে",
                "৳" + String.format(java.util.Locale.US, "%.2f", amount) + " টাকা জমা নেওয়া হয়েছে: " + selected.getName(),
                com.smartmess.android.utils.NotificationCenterHelper.TYPE_DEPOSIT
        );

        Toast.makeText(this, selected.getName() + " এর জন্য ৳" + amount + " সফলভাবে জমা হয়েছে!", Toast.LENGTH_SHORT).show();
        finish();
    }
}

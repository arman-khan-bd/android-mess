package com.smartmess.android.ui.members;

import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.smartmess.android.R;
import com.smartmess.android.data.local.DatabaseHelper;
import com.smartmess.android.data.local.dao.UserDao;
import com.smartmess.android.engine.PlanGateManager;
import com.smartmess.android.model.User;
import com.smartmess.android.utils.DateTimeUtils;
import com.smartmess.android.utils.SessionManager;

import java.util.UUID;

public class AddMemberActivity extends AppCompatActivity {

    private EditText etMemberName;
    private EditText etMemberPhone;
    private Spinner spMemberRole;
    private EditText etMemberPassword;
    private MaterialButton btnSaveMember;

    private UserDao userDao;
    private SessionManager sessionManager;
    private PlanGateManager planGateManager;

    private final String[] roles = {"General Member", "Assistant Manager / Bazar Boy", "Manager"};
    private final String[] roleKeys = {"member", "assistant", "manager"};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_member);

        etMemberName = findViewById(R.id.etMemberName);
        etMemberPhone = findViewById(R.id.etMemberPhone);
        spMemberRole = findViewById(R.id.spMemberRole);
        etMemberPassword = findViewById(R.id.etMemberPassword);
        btnSaveMember = findViewById(R.id.btnSaveMember);

        DatabaseHelper helper = DatabaseHelper.getInstance(this);
        userDao = new UserDao(helper);
        sessionManager = new SessionManager(this);
        planGateManager = new PlanGateManager(this);

        ArrayAdapter<String> roleAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, roles);
        spMemberRole.setAdapter(roleAdapter);

        btnSaveMember.setOnClickListener(v -> saveMember());
    }

    private void saveMember() {
        long messId = sessionManager.getMessId();
        if (!planGateManager.canAddMember(messId)) {
            planGateManager.showUpgradeBottomSheet(getSupportFragmentManager(), "Unlimited Members",
                    "Free accounts are limited to 6 members. Upgrade to Pro for unlimited member capacity.");
            return;
        }

        String name = etMemberName.getText().toString().trim();
        String phone = etMemberPhone.getText().toString().trim();
        String password = etMemberPassword.getText().toString().trim();

        if (name.isEmpty() || phone.isEmpty()) {
            Toast.makeText(this, "Please enter member name and phone", Toast.LENGTH_SHORT).show();
            return;
        }

        if (password.isEmpty()) password = "123456";

        int rolePos = spMemberRole.getSelectedItemPosition();
        String roleKey = roleKeys[rolePos];

        String now = DateTimeUtils.nowIso();
        User user = new User(
                UUID.randomUUID().toString(),
                messId,
                name,
                phone,
                roleKey,
                "active"
        );
        user.setPassword(password);
        user.setCreatedAt(now);
        user.setUpdatedAt(now);

        userDao.insertOrUpdate(user);
        Toast.makeText(this, "Member " + name + " added successfully!", Toast.LENGTH_SHORT).show();
        finish();
    }
}

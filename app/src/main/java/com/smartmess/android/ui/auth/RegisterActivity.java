package com.smartmess.android.ui.auth;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.smartmess.android.R;
import com.smartmess.android.data.local.DatabaseHelper;
import com.smartmess.android.data.local.dao.MessDao;
import com.smartmess.android.data.local.dao.UserDao;
import com.smartmess.android.data.remote.ApiClient;
import com.smartmess.android.data.remote.dto.RegisterResponse;
import com.smartmess.android.model.Mess;
import com.smartmess.android.model.User;
import com.smartmess.android.ui.dashboard.DashboardActivity;
import com.smartmess.android.utils.DateTimeUtils;
import com.smartmess.android.utils.SessionManager;

import java.util.Random;
import java.util.UUID;
import java.util.concurrent.Executors;

public class RegisterActivity extends AppCompatActivity {

    private EditText etName;
    private EditText etRegisterPhone;
    private EditText etMessName;
    private EditText etInviteCode;
    private EditText etRegisterPassword;
    private MaterialButton btnRegister;
    private ProgressBar pbRegister;
    private TextView tvGoToLogin;

    private UserDao userDao;
    private MessDao messDao;
    private SessionManager sessionManager;
    private ApiClient apiClient;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        etName = findViewById(R.id.etName);
        etRegisterPhone = findViewById(R.id.etRegisterPhone);
        etMessName = findViewById(R.id.etMessName);
        etInviteCode = findViewById(R.id.etInviteCode);
        etRegisterPassword = findViewById(R.id.etRegisterPassword);
        btnRegister = findViewById(R.id.btnRegister);
        pbRegister = findViewById(R.id.pbRegister);
        tvGoToLogin = findViewById(R.id.tvGoToLogin);

        DatabaseHelper helper = DatabaseHelper.getInstance(this);
        userDao = new UserDao(helper);
        messDao = new MessDao(helper);
        sessionManager = new SessionManager(this);
        apiClient = new ApiClient();

        btnRegister.setOnClickListener(v -> performRegistration());
        tvGoToLogin.setOnClickListener(v -> finish());
    }

    private void performRegistration() {
        String name = etName.getText().toString().trim();
        String phone = etRegisterPhone.getText().toString().trim();
        String messName = etMessName.getText().toString().trim();
        String inviteCode = etInviteCode.getText().toString().trim();
        String password = etRegisterPassword.getText().toString().trim();

        if (name.isEmpty() || phone.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Please fill in all required fields", Toast.LENGTH_SHORT).show();
            return;
        }

        if (messName.isEmpty()) {
            messName = name + "'s Mess";
        }

        pbRegister.setVisibility(View.VISIBLE);
        btnRegister.setEnabled(false);

        final String finalMessName = messName;

        // 1. Create locally in SQLite first (offline-first capability)
        String messUuid = UUID.randomUUID().toString();
        String userUuid = UUID.randomUUID().toString();
        String code = inviteCode.isEmpty() ? "MESS" + (100 + new Random().nextInt(900)) : inviteCode;

        Mess newMess = new Mess(messUuid, finalMessName, code, "monthly", 1, "22:00:00", 0.50);
        newMess.setCreatedAt(DateTimeUtils.nowIso());
        newMess.setUpdatedAt(DateTimeUtils.nowIso());
        long messId = messDao.insertOrUpdate(newMess);

        User newUser = new User(userUuid, messId, name, phone, "manager", "active");
        newUser.setPassword(password);
        newUser.setCreatedAt(DateTimeUtils.nowIso());
        newUser.setUpdatedAt(DateTimeUtils.nowIso());
        long userId = userDao.insertOrUpdate(newUser);

        // Try registering in cloud asynchronously
        Executors.newSingleThreadExecutor().execute(() -> {
            try {
                RegisterResponse resp = apiClient.register(name, phone, password, finalMessName, inviteCode);
                // If cloud gives token, update session
                String token = resp != null && resp.getToken() != null ? resp.getToken() : "offline_token";
                runOnUiThread(() -> {
                    sessionManager.createSession(userId, userUuid, name, phone, "manager", messId, messUuid, finalMessName, token);
                    if (resp != null && resp.getPlanCapabilities() != null) {
                        sessionManager.updatePlanCapabilities(resp.getPlanCapabilities());
                    }
                    pbRegister.setVisibility(View.GONE);
                    Toast.makeText(RegisterActivity.this, "Mess created successfully! Invite code: " + code, Toast.LENGTH_LONG).show();
                    startActivity(new Intent(RegisterActivity.this, com.smartmess.android.ui.MainActivity.class));
                    finish();
                });
            } catch (Exception e) {
                runOnUiThread(() -> {
                    // Saved offline successfully
                    sessionManager.createSession(userId, userUuid, name, phone, "manager", messId, messUuid, finalMessName, "offline_token");
                    pbRegister.setVisibility(View.GONE);
                    Toast.makeText(RegisterActivity.this, "Saved offline! Invite code: " + code, Toast.LENGTH_LONG).show();
                    startActivity(new Intent(RegisterActivity.this, com.smartmess.android.ui.MainActivity.class));
                    finish();
                });
            }
        });
    }
}

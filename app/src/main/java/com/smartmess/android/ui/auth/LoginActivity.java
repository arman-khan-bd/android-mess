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
import com.smartmess.android.data.remote.dto.LoginResponse;
import com.smartmess.android.model.Mess;
import com.smartmess.android.model.User;
import com.smartmess.android.ui.dashboard.DashboardActivity;
import com.smartmess.android.utils.SessionManager;

import java.util.concurrent.Executors;

public class LoginActivity extends AppCompatActivity {

    private EditText etPhone;
    private EditText etPassword;
    private MaterialButton btnLogin;
    private ProgressBar pbLogin;
    private TextView tvGoToRegister;

    private UserDao userDao;
    private MessDao messDao;
    private SessionManager sessionManager;
    private ApiClient apiClient;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        etPhone = findViewById(R.id.etPhone);
        etPassword = findViewById(R.id.etPassword);
        btnLogin = findViewById(R.id.btnLogin);
        pbLogin = findViewById(R.id.pbLogin);
        tvGoToRegister = findViewById(R.id.tvGoToRegister);

        DatabaseHelper helper = DatabaseHelper.getInstance(this);
        userDao = new UserDao(helper);
        messDao = new MessDao(helper);
        sessionManager = new SessionManager(this);
        apiClient = new ApiClient(this);

        btnLogin.setOnClickListener(v -> performLogin());

        tvGoToRegister.setOnClickListener(v -> {
            startActivity(new Intent(LoginActivity.this, RegisterActivity.class));
        });
    }

    private void performLogin() {
        String phone = etPhone.getText().toString().trim();
        String password = etPassword.getText().toString().trim();

        if (phone.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Please enter both phone and password", Toast.LENGTH_SHORT).show();
            return;
        }

        pbLogin.setVisibility(View.VISIBLE);
        btnLogin.setEnabled(false);

        // 1. Try Local SQLite Offline Login First
        User localUser = userDao.getByPhone(phone);
        if (localUser != null && (localUser.getPassword() == null || localUser.getPassword().equals(password))) {
            Mess mess = messDao.getById(localUser.getMessId());
            String messName = mess != null ? mess.getName() : "My Mess";
            String messUuid = mess != null ? mess.getUuid() : "";

            sessionManager.createSession(
                    localUser.getId(),
                    localUser.getUuid(),
                    localUser.getName(),
                    localUser.getPhone(),
                    localUser.getRole(),
                    localUser.getMessId(),
                    messUuid,
                    messName,
                    "offline_session_token"
            );

            pbLogin.setVisibility(View.GONE);
            startActivity(new Intent(LoginActivity.this, com.smartmess.android.ui.MainActivity.class));
            finish();
            return;
        }

        // 2. Fallback to Cloud Authentication
        Executors.newSingleThreadExecutor().execute(() -> {
            try {
                LoginResponse resp = apiClient.login(phone, password);
                runOnUiThread(() -> {
                    pbLogin.setVisibility(View.GONE);
                    btnLogin.setEnabled(true);

                    if (resp != null && resp.isSuccess() && resp.getUser() != null) {
                        User u = resp.getUser();
                        Mess m = resp.getMess();
                        long messId = m != null ? messDao.insertOrUpdate(m) : 1;
                        u.setMessId(messId);
                        long userId = userDao.insertOrUpdate(u);

                        sessionManager.createSession(
                                userId,
                                u.getUuid(),
                                u.getName(),
                                u.getPhone(),
                                u.getRole(),
                                messId,
                                m != null ? m.getUuid() : "",
                                m != null ? m.getName() : "Smart Mess",
                                resp.getToken()
                        );

                        if (resp.getPlanCapabilities() != null) {
                            sessionManager.updatePlanCapabilities(resp.getPlanCapabilities());
                        }

                        startActivity(new Intent(LoginActivity.this, com.smartmess.android.ui.MainActivity.class));
                        finish();
                    } else {
                        Toast.makeText(LoginActivity.this, "Invalid credentials or user not found offline", Toast.LENGTH_LONG).show();
                    }
                });
            } catch (Exception e) {
                runOnUiThread(() -> {
                    pbLogin.setVisibility(View.GONE);
                    btnLogin.setEnabled(true);
                    Toast.makeText(LoginActivity.this, "Network error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
            }
        });
    }
}

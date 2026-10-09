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
            Toast.makeText(this, "মোবাইল নম্বর ও পাসওয়ার্ড লিখুন", Toast.LENGTH_SHORT).show();
            return;
        }

        pbLogin.setVisibility(View.VISIBLE);
        btnLogin.setEnabled(false);

        // Online-First Authentication: Attempt cloud login to obtain fresh Sanctum Bearer token
        boolean isOnline = com.smartmess.android.utils.NetworkUtils.isNetworkAvailable(this);

        if (isOnline) {
            Executors.newSingleThreadExecutor().execute(() -> {
                try {
                    LoginResponse resp = apiClient.login(phone, password);
                    runOnUiThread(() -> {
                        if (resp != null && resp.isSuccess() && resp.getUser() != null) {
                            pbLogin.setVisibility(View.GONE);
                            btnLogin.setEnabled(true);

                            User u = resp.getUser();
                            Mess m = resp.getMess();
                            long messId = m != null ? messDao.insertOrUpdate(m) : 1;
                            u.setMessId(messId);
                            u.setPassword(password);
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
                            if (m != null && m.getInviteCode() != null) {
                                sessionManager.setInviteCode(m.getInviteCode());
                            }

                            if (resp.getPlanCapabilities() != null) {
                                sessionManager.updatePlanCapabilities(resp.getPlanCapabilities());
                            }

                            // Trigger cloud sync to pull latest mess meals, bazar & expenses
                            com.smartmess.android.data.sync.SyncManager.triggerSync(getApplicationContext());

                            Toast.makeText(LoginActivity.this, "লগইন সফল হয়েছে!", Toast.LENGTH_SHORT).show();
                            startActivity(new Intent(LoginActivity.this, com.smartmess.android.ui.MainActivity.class));
                            finish();
                        } else {
                            // Check local SQLite credentials as fallback
                            if (!attemptLocalOfflineLogin(phone, password, false)) {
                                pbLogin.setVisibility(View.GONE);
                                btnLogin.setEnabled(true);
                                String errMsg = (resp != null && resp.getMessage() != null)
                                        ? resp.getMessage()
                                        : "মোবাইল নম্বর বা পাসওয়ার্ড ভুল হয়েছে।";
                                Toast.makeText(LoginActivity.this, errMsg, Toast.LENGTH_LONG).show();
                            }
                        }
                    });
                } catch (Exception e) {
                    runOnUiThread(() -> {
                        // Network/server failure: fallback to local SQLite offline login
                        if (!attemptLocalOfflineLogin(phone, password, true)) {
                            pbLogin.setVisibility(View.GONE);
                            btnLogin.setEnabled(true);
                            Toast.makeText(LoginActivity.this, "লগইন ব্যর্থ হয়েছে: " + e.getMessage(), Toast.LENGTH_LONG).show();
                        }
                    });
                }
            });
        } else {
            // Offline mode: authenticating via local SQLite database
            if (!attemptLocalOfflineLogin(phone, password, true)) {
                pbLogin.setVisibility(View.GONE);
                btnLogin.setEnabled(true);
                Toast.makeText(this, "ইন্টারনেট সংযোগ নেই এবং কোনো সংরক্ষিত অ্যাকাউন্ট পাওয়া যায়নি।", Toast.LENGTH_LONG).show();
            }
        }
    }

    private boolean attemptLocalOfflineLogin(String phone, String password, boolean isOfflineMode) {
        User localUser = userDao.getByPhone(phone);
        if (localUser != null && (localUser.getPassword() == null || localUser.getPassword().equals(password))) {
            Mess mess = messDao.getById(localUser.getMessId());
            String messName = mess != null ? mess.getName() : "My Mess";
            String messUuid = mess != null ? mess.getUuid() : "";

            // Retain existing token if present
            String existingToken = sessionManager.getAuthToken();
            String tokenToUse = (existingToken != null && !existingToken.trim().isEmpty())
                    ? existingToken : "offline_session_token";

            sessionManager.createSession(
                    localUser.getId(),
                    localUser.getUuid(),
                    localUser.getName(),
                    localUser.getPhone(),
                    localUser.getRole(),
                    localUser.getMessId(),
                    messUuid,
                    messName,
                    tokenToUse
            );
            if (mess != null && mess.getInviteCode() != null) {
                sessionManager.setInviteCode(mess.getInviteCode());
            }

            pbLogin.setVisibility(View.GONE);
            if (isOfflineMode) {
                Toast.makeText(this, "অফলাইন মোডে লগইন করা হয়েছে।", Toast.LENGTH_SHORT).show();
            }
            startActivity(new Intent(LoginActivity.this, com.smartmess.android.ui.MainActivity.class));
            finish();
            return true;
        }
        return false;
    }
}

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
                        try {
                            if (resp != null && resp.isSuccess() && resp.getUser() != null) {
                                pbLogin.setVisibility(View.GONE);
                                btnLogin.setEnabled(true);

                                User u = resp.getUser();
                                Mess m = resp.getMess();
                                long messId = m != null ? messDao.insertOrUpdate(m) : 1;
                                u.setMessId(messId);
                                u.setPassword(password);
                                long userId = userDao.insertOrUpdate(u);

                                String uUuid = u.getUuid() != null ? u.getUuid() : java.util.UUID.randomUUID().toString();
                                String uName = u.getName() != null && !u.getName().trim().isEmpty() ? u.getName().trim() : "Member";
                                String uPhone = u.getPhone() != null && !u.getPhone().trim().isEmpty() ? u.getPhone().trim() : phone;
                                String uRole = u.getRole() != null ? u.getRole() : "member";
                                String mUuid = (m != null && m.getUuid() != null) ? m.getUuid() : "";
                                String mName = (m != null && m.getName() != null) ? m.getName() : "Smart Mess";
                                String token = resp.getToken() != null ? resp.getToken() : "session_token";

                                sessionManager.createSession(
                                        userId,
                                        uUuid,
                                        uName,
                                        uPhone,
                                        uRole,
                                        messId,
                                        mUuid,
                                        mName,
                                        token
                                );
                                if (m != null && m.getInviteCode() != null) {
                                    sessionManager.setInviteCode(m.getInviteCode());
                                }

                                if (resp.getPlanCapabilities() != null) {
                                    sessionManager.updatePlanCapabilities(resp.getPlanCapabilities());
                                }

                                // Trigger cloud sync in background safely
                                try {
                                    com.smartmess.android.data.sync.SyncManager.triggerSync(getApplicationContext());
                                } catch (Throwable t) {
                                    android.util.Log.e("LoginActivity", "Sync trigger notice: " + t.getMessage());
                                }

                                Toast.makeText(LoginActivity.this, "লগইন সফল হয়েছে!", Toast.LENGTH_SHORT).show();
                                Intent intent = new Intent(LoginActivity.this, com.smartmess.android.ui.MainActivity.class);
                                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
                                startActivity(intent);
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
                        } catch (Throwable t) {
                            android.util.Log.e("LoginActivity", "Error handling login response: " + t.getMessage(), t);
                            if (!attemptLocalOfflineLogin(phone, password, false)) {
                                pbLogin.setVisibility(View.GONE);
                                btnLogin.setEnabled(true);
                                Toast.makeText(LoginActivity.this, "লগইনে সমস্যা হয়েছে: " + t.getMessage(), Toast.LENGTH_LONG).show();
                            }
                        }
                    });
                } catch (Exception e) {
                    runOnUiThread(() -> {
                        try {
                            // Network/server failure: fallback to local SQLite offline login
                            if (!attemptLocalOfflineLogin(phone, password, true)) {
                                pbLogin.setVisibility(View.GONE);
                                btnLogin.setEnabled(true);
                                Toast.makeText(LoginActivity.this, "লগইন ব্যর্থ হয়েছে: " + e.getMessage(), Toast.LENGTH_LONG).show();
                            }
                        } catch (Throwable t) {
                            pbLogin.setVisibility(View.GONE);
                            btnLogin.setEnabled(true);
                            Toast.makeText(LoginActivity.this, "সংযোগ ব্যর্থ হয়েছে। আবার চেষ্টা করুন।", Toast.LENGTH_LONG).show();
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
        try {
            User localUser = userDao.getByPhone(phone);
            if (localUser != null && (localUser.getPassword() == null || localUser.getPassword().equals(password))) {
                Mess mess = messDao.getById(localUser.getMessId());
                String messName = mess != null && mess.getName() != null ? mess.getName() : "My Mess";
                String messUuid = mess != null && mess.getUuid() != null ? mess.getUuid() : "";

                // Retain existing token if present
                String existingToken = sessionManager.getAuthToken();
                String tokenToUse = (existingToken != null && !existingToken.trim().isEmpty())
                        ? existingToken : "offline_session_token";

                sessionManager.createSession(
                        localUser.getId(),
                        localUser.getUuid() != null ? localUser.getUuid() : "",
                        localUser.getName() != null ? localUser.getName() : "Member",
                        localUser.getPhone() != null ? localUser.getPhone() : phone,
                        localUser.getRole() != null ? localUser.getRole() : "member",
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
                Intent intent = new Intent(LoginActivity.this, com.smartmess.android.ui.MainActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivity(intent);
                finish();
                return true;
            }
        } catch (Throwable t) {
            android.util.Log.e("LoginActivity", "Offline login error: " + t.getMessage(), t);
        }
        return false;
    }
}

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
import com.smartmess.android.data.remote.dto.RegisterRequest;
import com.smartmess.android.data.remote.dto.RegisterResponse;
import com.smartmess.android.model.Mess;
import com.smartmess.android.model.User;
import com.smartmess.android.ui.MainActivity;
import com.smartmess.android.utils.DateTimeUtils;
import com.smartmess.android.utils.NetworkUtils;
import com.smartmess.android.utils.SessionManager;

import java.util.Random;
import java.util.UUID;
import java.util.concurrent.Executors;

public class RegisterActivity extends AppCompatActivity {

    private MaterialButton btnPathCreateMess;
    private MaterialButton btnPathJoinMess;
    private View layoutPathCreateFields;
    private View layoutPathJoinFields;

    private EditText etName;
    private EditText etEmail;
    private EditText etRegisterPhone;
    private EditText etRegisterPassword;
    private EditText etMessName;
    private EditText etLocation;
    private EditText etInviteCode;
    private TextView tvNetworkStatusNotice;

    private MaterialButton btnRegister;
    private ProgressBar pbRegister;
    private TextView tvGoToLogin;

    private UserDao userDao;
    private MessDao messDao;
    private SessionManager sessionManager;
    private ApiClient apiClient;

    private boolean isPathCreateMess = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        btnPathCreateMess = findViewById(R.id.btnPathCreateMess);
        btnPathJoinMess = findViewById(R.id.btnPathJoinMess);
        layoutPathCreateFields = findViewById(R.id.layoutPathCreateFields);
        layoutPathJoinFields = findViewById(R.id.layoutPathJoinFields);

        etName = findViewById(R.id.etName);
        etEmail = findViewById(R.id.etEmail);
        etRegisterPhone = findViewById(R.id.etRegisterPhone);
        etRegisterPassword = findViewById(R.id.etRegisterPassword);
        etMessName = findViewById(R.id.etMessName);
        etLocation = findViewById(R.id.etLocation);
        etInviteCode = findViewById(R.id.etInviteCode);
        tvNetworkStatusNotice = findViewById(R.id.tvNetworkStatusNotice);

        btnRegister = findViewById(R.id.btnRegister);
        pbRegister = findViewById(R.id.pbRegister);
        tvGoToLogin = findViewById(R.id.tvGoToLogin);

        DatabaseHelper helper = DatabaseHelper.getInstance(this);
        userDao = new UserDao(helper);
        messDao = new MessDao(helper);
        sessionManager = new SessionManager(this);
        apiClient = new ApiClient(this);

        setupPathSelectors();

        String incomingCode = getIntent().getStringExtra("EXTRA_INVITE_CODE");
        if (incomingCode != null && !incomingCode.trim().isEmpty()) {
            selectPath(false);
            etInviteCode.setText(incomingCode.trim().toUpperCase());
        }

        btnRegister.setOnClickListener(v -> performRegistration());
        tvGoToLogin.setOnClickListener(v -> finish());
    }

    private void setupPathSelectors() {
        btnPathCreateMess.setOnClickListener(v -> selectPath(true));
        btnPathJoinMess.setOnClickListener(v -> selectPath(false));
    }

    private void selectPath(boolean createMess) {
        isPathCreateMess = createMess;
        if (createMess) {
            btnPathCreateMess.setBackgroundColor(getResources().getColor(R.color.primary));
            btnPathCreateMess.setTextColor(getResources().getColor(R.color.text_on_primary));
            btnPathJoinMess.setBackgroundColor(android.graphics.Color.TRANSPARENT);
            btnPathJoinMess.setTextColor(getResources().getColor(R.color.text_secondary));

            layoutPathCreateFields.setVisibility(View.VISIBLE);
            layoutPathJoinFields.setVisibility(View.GONE);
            btnRegister.setText("Create Mess & Register");
        } else {
            btnPathJoinMess.setBackgroundColor(getResources().getColor(R.color.primary));
            btnPathJoinMess.setTextColor(getResources().getColor(R.color.text_on_primary));
            btnPathCreateMess.setBackgroundColor(android.graphics.Color.TRANSPARENT);
            btnPathCreateMess.setTextColor(getResources().getColor(R.color.text_secondary));

            layoutPathCreateFields.setVisibility(View.GONE);
            layoutPathJoinFields.setVisibility(View.VISIBLE);
            btnRegister.setText("Join Mess & Register");
        }
    }

    private void performRegistration() {
        String name = etName.getText().toString().trim();
        String email = etEmail.getText().toString().trim();
        String phone = etRegisterPhone.getText().toString().trim();
        String password = etRegisterPassword.getText().toString().trim();

        if (name.isEmpty() || phone.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "দয়া করে * চিহ্নিত সকল তথ্য পূরণ করুন", Toast.LENGTH_SHORT).show();
            return;
        }

        if (password.length() < 6) {
            Toast.makeText(this, "পাসওয়ার্ড কমপক্ষে ৬ অক্ষরের হতে হবে", Toast.LENGTH_SHORT).show();
            return;
        }

        String messName = etMessName.getText().toString().trim();
        String location = etLocation.getText().toString().trim();
        String inviteCode = etInviteCode.getText().toString().trim();

        if (isPathCreateMess) {
            if (messName.isEmpty()) {
                messName = name + "'s Mess";
            }
        } else {
            if (inviteCode.isEmpty()) {
                Toast.makeText(this, "দয়া করে ৬ অক্ষরের মেস ইনভাইট কোড লিখুন", Toast.LENGTH_SHORT).show();
                return;
            }
        }

        // 1. Network Verification: App verifies active Mobile Data or Wi-Fi connectivity
        boolean hasNetwork = NetworkUtils.isNetworkAvailable(this);
        if (!hasNetwork) {
            tvNetworkStatusNotice.setVisibility(View.VISIBLE);
            tvNetworkStatusNotice.setText("মেস তৈরি বা যোগ দিতে ইন্টারনেট সংযোগ প্রয়োজন। মোবাইল ডাটা বা ওয়াইফাই চালু করুন।");
            Toast.makeText(this, "অ্যাকাউন্ট খুলতে ইন্টারনেট সংযোগ প্রয়োজন", Toast.LENGTH_LONG).show();
            return;
        } else {
            tvNetworkStatusNotice.setVisibility(View.GONE);
        }

        pbRegister.setVisibility(View.VISIBLE);
        btnRegister.setEnabled(false);

        final String finalMessName = messName;
        final String finalInviteCode = inviteCode;
        final String finalLocation = location;

        // Build Payload
        RegisterRequest requestPayload;
        if (isPathCreateMess) {
            requestPayload = RegisterRequest.createMess(name, email, phone, password, finalMessName, finalLocation, "monthly");
        } else {
            requestPayload = RegisterRequest.joinMess(name, email, phone, password, finalInviteCode);
        }

        Executors.newSingleThreadExecutor().execute(() -> {
            try {
                RegisterResponse resp = apiClient.register(requestPayload);

                if (resp != null && resp.isSuccess()) {
                    String token = resp.getToken() != null ? resp.getToken() : "sanctum_token";
                    String role = isPathCreateMess ? "manager" : "member";

                    // Remote tenant & user IDs
                    long tenantId = (resp.getMess() != null && resp.getMess().getId() > 0) ? resp.getMess().getId() : (1000 + new Random().nextInt(9000));
                    String tenantUuid = (resp.getMess() != null && resp.getMess().getUuid() != null) ? resp.getMess().getUuid() : UUID.randomUUID().toString();
                    String tenantName = (resp.getMess() != null && resp.getMess().getName() != null) ? resp.getMess().getName() : finalMessName;
                    String tenantInviteCode = (resp.getMess() != null && resp.getMess().getInviteCode() != null) ? resp.getMess().getInviteCode() : (isPathCreateMess ? "SM" + (1000 + new Random().nextInt(9000)) : finalInviteCode);

                    long uId = (resp.getUser() != null && resp.getUser().getId() > 0) ? resp.getUser().getId() : (100 + new Random().nextInt(900));
                    String uUuid = (resp.getUser() != null && resp.getUser().getUuid() != null) ? resp.getUser().getUuid() : UUID.randomUUID().toString();
                    if (resp.getUser() != null && resp.getUser().getRole() != null) {
                        role = resp.getUser().getRole();
                    }

                    // Local Offline Seed: Seed into SQLite
                    Mess localMess = new Mess(tenantUuid, tenantName, tenantInviteCode, "monthly", 1, "22:00:00", 0.50);
                    localMess.setId(tenantId);
                    localMess.setCreatedAt(DateTimeUtils.nowIso());
                    localMess.setUpdatedAt(DateTimeUtils.nowIso());
                    messDao.insertOrUpdate(localMess);

                    User localUser = new User(uUuid, tenantId, name, phone, role, "active");
                    localUser.setId(uId);
                    localUser.setPassword(password);
                    localUser.setCreatedAt(DateTimeUtils.nowIso());
                    localUser.setUpdatedAt(DateTimeUtils.nowIso());
                    userDao.insertOrUpdate(localUser);

                    final String finalRole = role;

                    runOnUiThread(() -> {
                        // Seed into encrypted SharedPreferences
                        sessionManager.createSession(uId, uUuid, name, phone, finalRole, tenantId, tenantUuid, tenantName, token);
                        sessionManager.setInviteCode(tenantInviteCode);
                        sessionManager.setLastSyncTimestamp("1970-01-01 00:00:00");
                        if (resp.getPlanCapabilities() != null) {
                            sessionManager.updatePlanCapabilities(resp.getPlanCapabilities());
                        }

                        // Automatically trigger initial cloud sync to download all mess data (members, meals, bazar, expenses)
                        com.smartmess.android.data.sync.SyncManager.triggerSync(getApplicationContext());

                        pbRegister.setVisibility(View.GONE);
                        btnRegister.setEnabled(true);
                        String successMsg = isPathCreateMess
                                ? "মেস তৈরি সফল হয়েছে! ইনভাইট কোড: " + tenantInviteCode
                                : "মেসে সফলভাবে যোগ দিয়েছেন! তথ্য সিঙ্ক হচ্ছে...";
                        Toast.makeText(RegisterActivity.this, successMsg, Toast.LENGTH_LONG).show();

                        Intent intent = new Intent(RegisterActivity.this, MainActivity.class);
                        intent.putExtra("EXTRA_INITIAL_SYNC", true);
                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                        startActivity(intent);
                        finish();
                    });
                } else {
                    String errorMsg = (resp != null && resp.getMessage() != null)
                            ? resp.getMessage()
                            : "Registration failed. Please check your details and try again.";
                    runOnUiThread(() -> {
                        pbRegister.setVisibility(View.GONE);
                        btnRegister.setEnabled(true);
                        Toast.makeText(RegisterActivity.this, errorMsg, Toast.LENGTH_LONG).show();
                    });
                }
            } catch (Exception e) {
                runOnUiThread(() -> {
                    pbRegister.setVisibility(View.GONE);
                    btnRegister.setEnabled(true);
                    Toast.makeText(RegisterActivity.this, "নেটওয়ার্ক সমস্যা: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
            }
        });
    }
}

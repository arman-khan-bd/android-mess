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
import com.smartmess.android.data.remote.ApiClient;
import com.smartmess.android.data.remote.ApiService;
import com.smartmess.android.data.remote.dto.CreateMemberRequest;
import com.smartmess.android.data.remote.dto.CreateMemberResponse;
import com.smartmess.android.data.sync.SyncManager;
import com.smartmess.android.engine.PlanGateManager;
import com.smartmess.android.model.User;
import com.smartmess.android.utils.DateTimeUtils;
import com.smartmess.android.utils.NetworkUtils;
import com.smartmess.android.utils.SessionManager;

import java.util.UUID;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class AddMemberActivity extends AppCompatActivity {

    private EditText etMemberName;
    private EditText etMemberPhone;
    private Spinner spMemberRole;
    private EditText etMemberPassword;
    private MaterialButton btnSaveMember;

    private UserDao userDao;
    private SessionManager sessionManager;
    private PlanGateManager planGateManager;
    private ApiService apiService;

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
        apiService = ApiClient.getApiService(this);

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
            Toast.makeText(this, "দয়া করে সদস্যের নাম ও মোবাইল নম্বর লিখুন", Toast.LENGTH_SHORT).show();
            return;
        }

        if (password.isEmpty()) password = "123456";

        int rolePos = spMemberRole.getSelectedItemPosition();
        String roleKey = roleKeys[rolePos];

        String userUuid = UUID.randomUUID().toString();
        String now = DateTimeUtils.nowIso();

        final User localUser = new User(
                userUuid,
                messId,
                name,
                phone,
                roleKey,
                "active"
        );
        localUser.setPassword(password);
        localUser.setCreatedAt(now);
        localUser.setUpdatedAt(now);

        // Check internet connection for direct server database creation
        if (NetworkUtils.isNetworkAvailable(this)) {
            btnSaveMember.setEnabled(false);
            btnSaveMember.setText("সার্ভারে সংরক্ষণ হচ্ছে...");

            CreateMemberRequest request = new CreateMemberRequest(
                    userUuid,
                    name,
                    phone,
                    roleKey,
                    password
            );

            apiService.addMember(request).enqueue(new Callback<CreateMemberResponse>() {
                @Override
                public void onResponse(Call<CreateMemberResponse> call, Response<CreateMemberResponse> response) {
                    if (isFinishing() || (android.os.Build.VERSION.SDK_INT >= 17 && isDestroyed())) return;

                    if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                        User created = response.body().getUser();
                        if (created != null) {
                            localUser.setId(created.getId());
                        }
                        userDao.insertOrUpdate(localUser);
                        Toast.makeText(AddMemberActivity.this, "সদস্য " + name + " সফলভাবে ডেটাবেজে যুক্ত হয়েছে!", Toast.LENGTH_SHORT).show();
                        SyncManager.triggerSync(getApplicationContext());
                        finish();
                    } else {
                        String errMsg = "সার্ভারে ত্রুটি";
                        try {
                            if (response.body() != null && response.body().getMessage() != null) {
                                errMsg = response.body().getMessage();
                            } else if (response.errorBody() != null) {
                                errMsg = response.errorBody().string();
                            }
                        } catch (Exception ignored) {}

                        // If plan limit or validation error from server, notify user
                        if (response.code() == 403 || response.code() == 422) {
                            btnSaveMember.setEnabled(true);
                            btnSaveMember.setText("সংরক্ষণ করুন");
                            Toast.makeText(AddMemberActivity.this, errMsg, Toast.LENGTH_LONG).show();
                        } else {
                            // Offline fallback save
                            saveLocallyAndFinish(localUser, name);
                        }
                    }
                }

                @Override
                public void onFailure(Call<CreateMemberResponse> call, Throwable t) {
                    if (isFinishing() || (android.os.Build.VERSION.SDK_INT >= 17 && isDestroyed())) return;
                    // Network connection dropped during request, save locally
                    saveLocallyAndFinish(localUser, name);
                }
            });
        } else {
            // No internet connection: save direct to mobile local database
            saveLocallyAndFinish(localUser, name);
        }
    }

    private void saveLocallyAndFinish(User user, String name) {
        userDao.insertOrUpdate(user);
        Toast.makeText(this, "সদস্য " + name + " লোকাল মেমোরিতে যুক্ত হয়েছে (ইন্টারনেট পেলে সিঙ্ক হবে)।", Toast.LENGTH_SHORT).show();
        finish();
    }
}

package com.smartmess.android.ui.members;

import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.bumptech.glide.Glide;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.smartmess.android.R;
import com.smartmess.android.data.local.DatabaseHelper;
import com.smartmess.android.data.local.dao.MealDao;
import com.smartmess.android.data.local.dao.UserDao;
import com.smartmess.android.data.remote.ApiClient;
import com.smartmess.android.data.remote.ApiConfig;
import com.smartmess.android.model.Meal;
import com.smartmess.android.model.User;
import com.smartmess.android.utils.DateTimeUtils;
import com.smartmess.android.utils.SessionManager;

import java.io.IOException;
import java.util.UUID;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.FormBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class ActivityMemberManage extends AppCompatActivity {

    public static final String EXTRA_USER_ID = "EXTRA_USER_ID";

    private View btnBack;
    private ImageView ivMemberAvatar;
    private TextView tvMemberName;
    private TextView tvMemberPhone;
    private TextView tvCurrentRoleBadge;
    private TextView tvCurrentStatusBadge;

    private RadioGroup rgRoleSelector;
    private RadioButton rbRoleMember;
    private RadioButton rbRoleAssistant;
    private RadioButton rbRoleManager;

    private RadioGroup rgStatusSelector;
    private RadioButton rbStatusActive;
    private RadioButton rbStatusOnLeave;
    private RadioButton rbStatusLeft;

    private MaterialButton btnSavePermissions;

    private UserDao userDao;
    private MealDao mealDao;
    private SessionManager sessionManager;

    private User targetUser;
    private long targetUserId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_member_manage);

        DatabaseHelper helper = DatabaseHelper.getInstance(this);
        userDao = new UserDao(helper);
        mealDao = new MealDao(helper);
        sessionManager = new SessionManager(this);

        targetUserId = getIntent().getLongExtra(EXTRA_USER_ID, -1);
        if (targetUserId == -1) {
            Toast.makeText(this, "Invalid member selected", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        initViews();
        loadMemberDetails();
    }

    private void initViews() {
        btnBack = findViewById(R.id.btnBack);
        ivMemberAvatar = findViewById(R.id.ivMemberAvatar);
        tvMemberName = findViewById(R.id.tvMemberName);
        tvMemberPhone = findViewById(R.id.tvMemberPhone);
        tvCurrentRoleBadge = findViewById(R.id.tvCurrentRoleBadge);
        tvCurrentStatusBadge = findViewById(R.id.tvCurrentStatusBadge);

        rgRoleSelector = findViewById(R.id.rgRoleSelector);
        rbRoleMember = findViewById(R.id.rbRoleMember);
        rbRoleAssistant = findViewById(R.id.rbRoleAssistant);
        rbRoleManager = findViewById(R.id.rbRoleManager);

        rgStatusSelector = findViewById(R.id.rgStatusSelector);
        rbStatusActive = findViewById(R.id.rbStatusActive);
        rbStatusOnLeave = findViewById(R.id.rbStatusOnLeave);
        rbStatusLeft = findViewById(R.id.rbStatusLeft);

        btnSavePermissions = findViewById(R.id.btnSavePermissions);

        if (btnBack != null) {
            btnBack.setOnClickListener(v -> finish());
        }

        btnSavePermissions.setOnClickListener(v -> handleSavePermissions());
    }

    private void loadMemberDetails() {
        targetUser = userDao.getById(targetUserId);
        if (targetUser == null) {
            Toast.makeText(this, "Member not found in database", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        tvMemberName.setText(targetUser.getName());
        tvMemberPhone.setText(targetUser.getPhone());

        // Avatar
        if (targetUser.getAvatarUrl() != null && !targetUser.getAvatarUrl().trim().isEmpty()) {
            Glide.with(this).load(targetUser.getAvatarUrl()).circleCrop().into(ivMemberAvatar);
        } else {
            ivMemberAvatar.setImageResource(R.drawable.ic_member);
        }

        // Current Role badge & Radio Button
        String role = targetUser.getRole() != null ? targetUser.getRole().toLowerCase() : User.ROLE_MEMBER;
        if (User.ROLE_MANAGER.equals(role) || "superadmin".equals(role)) {
            tvCurrentRoleBadge.setText("Manager");
            tvCurrentRoleBadge.setTextColor(ContextCompat.getColor(this, R.color.primary));
            rbRoleManager.setChecked(true);
        } else if (User.ROLE_ASSISTANT.equals(role)) {
            tvCurrentRoleBadge.setText("Bazar Boy");
            tvCurrentRoleBadge.setTextColor(ContextCompat.getColor(this, R.color.accent));
            rbRoleAssistant.setChecked(true);
        } else {
            tvCurrentRoleBadge.setText("Member");
            tvCurrentRoleBadge.setTextColor(ContextCompat.getColor(this, R.color.text_secondary));
            rbRoleMember.setChecked(true);
        }

        // Current Status badge & Radio Button
        String status = targetUser.getStatus() != null ? targetUser.getStatus().toLowerCase() : User.STATUS_ACTIVE;
        if (User.STATUS_ON_LEAVE.equals(status) || "vacation".equals(status)) {
            tvCurrentStatusBadge.setText("On Leave");
            tvCurrentStatusBadge.setTextColor(ContextCompat.getColor(this, R.color.warning_amber));
            rbStatusOnLeave.setChecked(true);
        } else if (User.STATUS_LEFT.equals(status) || "inactive".equals(status)) {
            tvCurrentStatusBadge.setText("Left Mess");
            tvCurrentStatusBadge.setTextColor(ContextCompat.getColor(this, R.color.error_red));
            rbStatusLeft.setChecked(true);
        } else {
            tvCurrentStatusBadge.setText("Active");
            tvCurrentStatusBadge.setTextColor(ContextCompat.getColor(this, R.color.credit_green));
            rbStatusActive.setChecked(true);
        }
    }

    private void handleSavePermissions() {
        if (!sessionManager.isManager()) {
            Toast.makeText(this, "Only mess managers can modify member permissions", Toast.LENGTH_LONG).show();
            return;
        }

        final String newRole;
        if (rbRoleManager.isChecked()) {
            newRole = User.ROLE_MANAGER;
        } else if (rbRoleAssistant.isChecked()) {
            newRole = User.ROLE_ASSISTANT;
        } else {
            newRole = User.ROLE_MEMBER;
        }

        final String newStatus;
        if (rbStatusOnLeave.isChecked()) {
            newStatus = User.STATUS_ON_LEAVE;
        } else if (rbStatusLeft.isChecked()) {
            newStatus = User.STATUS_LEFT;
        } else {
            newStatus = User.STATUS_ACTIVE;
        }

        // Check if transferring ownership
        boolean isTransferringManager = User.ROLE_MANAGER.equals(newRole)
                && targetUserId != sessionManager.getUserId()
                && !targetUser.isManager();

        if (isTransferringManager) {
            new MaterialAlertDialogBuilder(this)
                    .setTitle("Handover Mess Ownership?")
                    .setMessage("Promoting " + targetUser.getName() + " to Manager will transfer overall administrative control of this mess. You will revert to a regular member.\n\nDo you want to proceed?")
                    .setPositiveButton("Confirm Handover", (dialog, which) -> executeSave(newRole, newStatus))
                    .setNegativeButton("Cancel", null)
                    .show();
        } else {
            executeSave(newRole, newStatus);
        }
    }

    private void executeSave(String role, String status) {
        // 1. Update SQLite locally
        userDao.updateRoleAndStatus(targetUserId, role, status);

        // 2. Auto-mute meal entries if On Leave or Left Mess
        if (User.STATUS_ON_LEAVE.equals(status) || User.STATUS_LEFT.equals(status)) {
            autoMuteMealsForMember(targetUserId);
        }

        // 3. If caller transferred manager role away from themselves
        if (User.ROLE_MANAGER.equals(role) && targetUserId != sessionManager.getUserId()) {
            userDao.updateRole(sessionManager.getUserId(), User.ROLE_MEMBER);
            sessionManager.setUserRole(User.ROLE_MEMBER);
        }

        // 4. Synchronize with remote Laravel API in background
        syncRoleAndStatusToServer(targetUserId, role, status);

        // 5. Post notification
        com.smartmess.android.utils.NotificationCenterHelper.postNotification(
                getApplicationContext(),
                sessionManager.getMessId(),
                "Member Permissions Updated",
                (targetUser != null ? targetUser.getName() : "Member") + " updated to " + role + " (" + status + ")",
                com.smartmess.android.utils.NotificationCenterHelper.TYPE_ROLE
        );

        Toast.makeText(this, "Member role & status updated successfully!", Toast.LENGTH_SHORT).show();
        setResult(RESULT_OK);
        finish();
    }

    private void autoMuteMealsForMember(long userId) {
        String today = DateTimeUtils.getCurrentDate();
        Meal todayMeal = mealDao.getUserMealForDate(sessionManager.getMessId(), userId, today);
        if (todayMeal != null) {
            todayMeal.setBreakfastCount(0.0);
            todayMeal.setLunchCount(0.0);
            todayMeal.setDinnerCount(0.0);
            todayMeal.setIsLocked(1);
            mealDao.insertOrUpdate(todayMeal);
        } else {
            Meal muteMeal = new Meal();
            muteMeal.setUuid(UUID.randomUUID().toString());
            muteMeal.setMessId(sessionManager.getMessId());
            muteMeal.setUserId(userId);
            muteMeal.setMealDate(today);
            muteMeal.setBreakfastCount(0.0);
            muteMeal.setLunchCount(0.0);
            muteMeal.setDinnerCount(0.0);
            muteMeal.setGuestMealCount(0.0);
            muteMeal.setIsLocked(1);
            mealDao.insertOrUpdate(muteMeal);
        }
    }

    private void syncRoleAndStatusToServer(long userId, String role, String status) {
        OkHttpClient client = ApiClient.getOkHttpClient(getApplicationContext());
        String url = ApiConfig.BASE_URL + "members/" + userId + "/update";

        RequestBody body = new FormBody.Builder()
                .add("role", role)
                .add("status", status)
                .build();

        Request request = new Request.Builder()
                .url(url)
                .post(body)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                // Silently fails for offline mode; local changes already saved
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) {
                // Server successfully synced role & status
                response.close();
            }
        });
    }
}

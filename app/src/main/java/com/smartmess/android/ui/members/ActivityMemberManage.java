package com.smartmess.android.ui.members;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.MediaStore;
import android.view.View;
import android.widget.ImageView;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.bumptech.glide.Glide;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputEditText;
import com.smartmess.android.R;
import com.smartmess.android.data.local.DatabaseHelper;
import com.smartmess.android.data.local.dao.MealDao;
import com.smartmess.android.data.local.dao.UserDao;
import com.smartmess.android.data.remote.ApiClient;
import com.smartmess.android.data.remote.ApiConfig;
import com.smartmess.android.model.Meal;
import com.smartmess.android.model.User;
import com.smartmess.android.utils.DateTimeUtils;
import com.smartmess.android.utils.PermissionHelper;
import com.smartmess.android.utils.SessionManager;

import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.UUID;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.FormBody;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class ActivityMemberManage extends AppCompatActivity {

    public static final String EXTRA_USER_ID = "EXTRA_USER_ID";
    private static final int REQ_CAMERA = 2001;
    private static final int REQ_GALLERY = 2002;

    private View btnBack;
    private ImageView ivMemberAvatar;
    private View btnChangeMemberPhoto;
    private TextView tvMemberName;
    private TextView tvMemberPhone;
    private TextView tvCurrentRoleBadge;
    private TextView tvCurrentStatusBadge;

    private TextInputEditText etMemberName;
    private TextInputEditText etMemberPhone;

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
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

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
            Toast.makeText(this, "সদস্য নির্বাচন সঠিক নয়", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        initViews();
        loadMemberDetails();
    }

    private void initViews() {
        btnBack = findViewById(R.id.btnBack);
        ivMemberAvatar = findViewById(R.id.ivMemberAvatar);
        btnChangeMemberPhoto = findViewById(R.id.btnChangeMemberPhoto);
        tvMemberName = findViewById(R.id.tvMemberName);
        tvMemberPhone = findViewById(R.id.tvMemberPhone);
        tvCurrentRoleBadge = findViewById(R.id.tvCurrentRoleBadge);
        tvCurrentStatusBadge = findViewById(R.id.tvCurrentStatusBadge);

        etMemberName = findViewById(R.id.etMemberName);
        etMemberPhone = findViewById(R.id.etMemberPhone);

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

        View.OnClickListener photoPickerListener = v -> showPhotoChoiceDialog();
        if (btnChangeMemberPhoto != null) btnChangeMemberPhoto.setOnClickListener(photoPickerListener);
        if (ivMemberAvatar != null) ivMemberAvatar.setOnClickListener(photoPickerListener);

        btnSavePermissions.setOnClickListener(v -> handleSavePermissions());
    }

    private void loadMemberDetails() {
        targetUser = userDao.getById(targetUserId);
        if (targetUser == null) {
            Toast.makeText(this, "ডাটাবেজে সদস্যকে পাওয়া যায়নি", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        tvMemberName.setText(targetUser.getName());
        tvMemberPhone.setText(targetUser.getPhone());
        if (etMemberName != null) etMemberName.setText(targetUser.getName());
        if (etMemberPhone != null) etMemberPhone.setText(targetUser.getPhone());

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

    private void showPhotoChoiceDialog() {
        new MaterialAlertDialogBuilder(this)
                .setTitle("সদস্যের ছবি পরিবর্তন")
                .setItems(new CharSequence[]{"Take Photo with Camera", "Choose from Gallery"}, (dialog, which) -> {
                    if (which == 0) {
                        launchCamera();
                    } else {
                        launchGallery();
                    }
                })
                .setNegativeButton("বাতিল", null)
                .show();
    }

    private void launchCamera() {
        if (!PermissionHelper.hasCameraPermission(this)) {
            PermissionHelper.requestCameraPermission(this);
            return;
        }
        Intent cameraIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (cameraIntent.resolveActivity(getPackageManager()) != null) {
            startActivityForResult(cameraIntent, REQ_CAMERA);
        } else {
            Toast.makeText(this, "ক্যামেরা পাওয়া যায়নি", Toast.LENGTH_SHORT).show();
        }
    }

    private void launchGallery() {
        if (!PermissionHelper.hasStoragePermission(this)) {
            PermissionHelper.requestStoragePermission(this);
            return;
        }
        Intent pickIntent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        startActivityForResult(pickIntent, REQ_GALLERY);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (grantResults.length > 0 && grantResults[0] == android.content.pm.PackageManager.PERMISSION_GRANTED) {
            if (requestCode == PermissionHelper.REQ_CAMERA) {
                launchCamera();
            } else if (requestCode == PermissionHelper.REQ_STORAGE) {
                launchGallery();
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode == Activity.RESULT_OK && data != null) {
            Bitmap avatarBitmap = null;
            if (requestCode == REQ_CAMERA) {
                Bundle extras = data.getExtras();
                if (extras != null && extras.get("data") != null) {
                    avatarBitmap = (Bitmap) extras.get("data");
                }
            } else if (requestCode == REQ_GALLERY) {
                Uri imageUri = data.getData();
                if (imageUri != null) {
                    try (InputStream is = getContentResolver().openInputStream(imageUri)) {
                        avatarBitmap = BitmapFactory.decodeStream(is);
                    } catch (Exception e) {
                        Toast.makeText(this, "ছবি লোড করা যায়নি", Toast.LENGTH_SHORT).show();
                    }
                }
            }

            if (avatarBitmap != null) {
                processAndUploadAvatar(avatarBitmap);
            }
        }
    }

    private void processAndUploadAvatar(Bitmap originalBitmap) {
        Toast.makeText(this, "ছবি অপ্টিমাইজ করা হচ্ছে...", Toast.LENGTH_SHORT).show();

        int size = Math.min(originalBitmap.getWidth(), originalBitmap.getHeight());
        int x = (originalBitmap.getWidth() - size) / 2;
        int y = (originalBitmap.getHeight() - size) / 2;
        Bitmap cropped = Bitmap.createBitmap(originalBitmap, x, y, size, size);
        Bitmap scaled = Bitmap.createScaledBitmap(cropped, 300, 300, true);

        File cacheFile = new File(getCacheDir(), "avatar_" + targetUserId + "_" + System.currentTimeMillis() + ".webp");
        try (FileOutputStream fos = new FileOutputStream(cacheFile)) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                scaled.compress(Bitmap.CompressFormat.WEBP_LOSSY, 80, fos);
            } else {
                @SuppressWarnings("deprecation")
                Bitmap.CompressFormat format = Bitmap.CompressFormat.WEBP;
                scaled.compress(format, 80, fos);
            }
            fos.flush();
        } catch (Exception e) {
            Toast.makeText(this, "ছবি প্রসেস করতে ত্রুটি: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            return;
        }

        Glide.with(this).load(scaled).circleCrop().into(ivMemberAvatar);
        uploadAvatarToServer(cacheFile);
    }

    private void uploadAvatarToServer(File webpFile) {
        OkHttpClient client = ApiClient.getOkHttpClient(getApplicationContext());
        String url = ApiConfig.BASE_URL + "uploads/avatar";

        RequestBody fileBody = RequestBody.create(webpFile, MediaType.parse("image/webp"));
        String userUuid = targetUser.getUuid() != null ? targetUser.getUuid() : "user_" + targetUserId;

        MultipartBody requestBody = new MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("avatar_image", webpFile.getName(), fileBody)
                .addFormDataPart("user_uuid", userUuid)
                .addFormDataPart("user_id", String.valueOf(targetUserId))
                .build();

        Request request = new Request.Builder()
                .url(url)
                .post(requestBody)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                mainHandler.post(() -> {
                    userDao.updateAvatar(targetUserId, webpFile.getAbsolutePath());
                    Toast.makeText(ActivityMemberManage.this, "ছবি অফলাইনে সংরক্ষিত।", Toast.LENGTH_SHORT).show();
                });
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) {
                try {
                    String respStr = response.body() != null ? response.body().string() : "";
                    if (response.isSuccessful()) {
                        JSONObject json = new JSONObject(respStr);
                        String avatarUrl = json.optString("avatar_url", webpFile.getAbsolutePath());
                        mainHandler.post(() -> {
                            userDao.updateAvatar(targetUserId, avatarUrl);
                            if (targetUserId == sessionManager.getUserId()) {
                                sessionManager.setAvatarUrl(avatarUrl);
                            }
                            Glide.with(ActivityMemberManage.this).load(avatarUrl).circleCrop().into(ivMemberAvatar);
                            Toast.makeText(ActivityMemberManage.this, "ছবি সফলভাবে আপডেট হয়েছে!", Toast.LENGTH_SHORT).show();
                        });
                    } else {
                        mainHandler.post(() -> userDao.updateAvatar(targetUserId, webpFile.getAbsolutePath()));
                    }
                } catch (Exception ex) {
                    mainHandler.post(() -> userDao.updateAvatar(targetUserId, webpFile.getAbsolutePath()));
                }
            }
        });
    }

    private void handleSavePermissions() {
        if (!sessionManager.isManager()) {
            Toast.makeText(this, "শুধুমাত্র মেস ম্যানেজার সদস্যদের তথ্য পরিবর্তন করতে পারবেন", Toast.LENGTH_LONG).show();
            return;
        }

        final String newName = (etMemberName != null && etMemberName.getText() != null)
                ? etMemberName.getText().toString().trim() : (targetUser != null ? targetUser.getName() : "");
        final String newPhone = (etMemberPhone != null && etMemberPhone.getText() != null)
                ? etMemberPhone.getText().toString().trim() : (targetUser != null ? targetUser.getPhone() : "");

        if (newName.isEmpty()) {
            if (etMemberName != null) etMemberName.setError("নাম প্রয়োজন");
            Toast.makeText(this, "সদস্যের নাম খালি রাখা যাবে না", Toast.LENGTH_SHORT).show();
            return;
        }

        if (newPhone.isEmpty()) {
            if (etMemberPhone != null) etMemberPhone.setError("ফোন নম্বর প্রয়োজন");
            Toast.makeText(this, "ফোন নম্বর খালি রাখা যাবে না", Toast.LENGTH_SHORT).show();
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
                    .setTitle("মেস পরিচালনার দায়িত্ব হস্তান্তর করবেন?")
                    .setMessage("Promoting " + newName + " to Manager will transfer overall administrative control of this mess. You will revert to a regular member.\n\nDo you want to proceed?")
                    .setPositiveButton("হস্তান্তর নিশ্চিত করুন", (dialog, which) -> executeSave(newRole, newStatus, newName, newPhone))
                    .setNegativeButton("বাতিল", null)
                    .show();
        } else {
            executeSave(newRole, newStatus, newName, newPhone);
        }
    }

    private void executeSave(String role, String status, String name, String phone) {
        // 1. Update personal info and role/status locally
        userDao.updateUserProfile(targetUserId, name, phone, targetUser.getAvatarUrl());
        userDao.updateRoleAndStatus(targetUserId, role, status);

        if (targetUser != null) {
            targetUser.setName(name);
            targetUser.setPhone(phone);
            targetUser.setRole(role);
            targetUser.setStatus(status);
        }

        // If manager editing their own info
        if (targetUserId == sessionManager.getUserId()) {
            sessionManager.setUserName(name);
            sessionManager.setUserPhone(phone);
        }

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
        syncMemberToServer(targetUserId, role, status, name, phone);

        // 5. Post notification
        com.smartmess.android.utils.NotificationCenterHelper.postNotification(
                getApplicationContext(),
                sessionManager.getMessId(),
                "সদস্যের তথ্য ও পদবী আপডেট",
                name + " এর তথ্য ও পদবী সফলভাবে আপডেট করা হয়েছে",
                com.smartmess.android.utils.NotificationCenterHelper.TYPE_ROLE
        );

        Toast.makeText(this, "সদস্যের তথ্য সফলভাবে আপডেট করা হয়েছে!", Toast.LENGTH_SHORT).show();
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

    private void syncMemberToServer(long userId, String role, String status, String name, String phone) {
        OkHttpClient client = ApiClient.getOkHttpClient(getApplicationContext());
        String url = ApiConfig.BASE_URL + "members/" + userId + "/update";

        RequestBody body = new FormBody.Builder()
                .add("role", role)
                .add("status", status)
                .add("name", name)
                .add("phone", phone)
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
                // Server successfully synced role, status, name, phone
                response.close();
            }
        });
    }
}

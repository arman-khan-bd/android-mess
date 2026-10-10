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
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.bumptech.glide.Glide;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.smartmess.android.R;
import com.smartmess.android.data.local.DatabaseHelper;
import com.smartmess.android.data.local.dao.DepositDao;
import com.smartmess.android.data.local.dao.ExpenseDao;
import com.smartmess.android.data.local.dao.MessDao;
import com.smartmess.android.data.local.dao.UserDao;
import com.smartmess.android.data.remote.ApiClient;
import com.smartmess.android.data.remote.ApiConfig;
import com.smartmess.android.engine.AccountingEngine;
import com.smartmess.android.engine.CalculationModels.CycleSummary;
import com.smartmess.android.engine.CalculationModels.MemberBalanceSheet;
import com.smartmess.android.model.Deposit;
import com.smartmess.android.model.Expense;
import com.smartmess.android.model.Mess;
import com.smartmess.android.model.User;
import com.smartmess.android.ui.sms.DueReminderActivity;
import com.smartmess.android.utils.CurrencyUtils;
import com.smartmess.android.utils.DateTimeUtils;
import com.smartmess.android.utils.PermissionHelper;
import com.smartmess.android.utils.SessionManager;

import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Locale;
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

public class ActivityUserProfile extends AppCompatActivity {

    public static final String EXTRA_USER_ID = "EXTRA_USER_ID";
    private static final int REQ_CAMERA = 1001;
    private static final int REQ_GALLERY = 1002;
    private static final int REQ_MANAGE_ROLE = 1003;

    private View btnBack;
    private MaterialButton btnManageMemberTop;
    private MaterialButton btnEditProfileInfo;
    private ImageView ivProfileAvatar;
    private View btnEditAvatar;
    private TextView tvProfileName;
    private TextView tvProfilePhone;
    private TextView tvJoinedDate;
    private TextView tvRoleBadge;
    private TextView tvStatusBadge;

    private TextView tvCycleDates;
    private TextView tvTotalDeposits;
    private TextView tvMealDetailsFormula;
    private TextView tvConsumedMealCost;
    private TextView tvSharedFoodShare;
    private TextView tvUtilityShare;
    private TextView tvSmsCharges;
    private TextView tvBalanceStatusLabel;
    private TextView tvNetBalance;

    private LinearLayout layoutActivityHistoryContainer;
    private TextView tvEmptyHistory;

    private View layoutManagerActions;
    private MaterialButton btnManageMemberPermissions;
    private MaterialButton btnSendDueReminderSms;

    private UserDao userDao;
    private ExpenseDao expenseDao;
    private DepositDao depositDao;
    private MessDao messDao;
    private AccountingEngine accountingEngine;
    private SessionManager sessionManager;

    private long targetUserId;
    private User targetUser;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_profile);

        DatabaseHelper helper = DatabaseHelper.getInstance(this);
        userDao = new UserDao(helper);
        expenseDao = new ExpenseDao(helper);
        depositDao = new DepositDao(helper);
        messDao = new MessDao(helper);
        accountingEngine = new AccountingEngine(this);
        sessionManager = new SessionManager(this);

        targetUserId = getIntent().getLongExtra(EXTRA_USER_ID, sessionManager.getUserId());

        initViews();
        loadUserProfile();
    }

    private void initViews() {
        btnBack = findViewById(R.id.btnBack);
        btnManageMemberTop = findViewById(R.id.btnManageMemberTop);
        ivProfileAvatar = findViewById(R.id.ivProfileAvatar);
        btnEditAvatar = findViewById(R.id.btnEditAvatar);
        tvProfileName = findViewById(R.id.tvProfileName);
        tvProfilePhone = findViewById(R.id.tvProfilePhone);
        tvJoinedDate = findViewById(R.id.tvJoinedDate);
        tvRoleBadge = findViewById(R.id.tvRoleBadge);
        tvStatusBadge = findViewById(R.id.tvStatusBadge);

        tvCycleDates = findViewById(R.id.tvCycleDates);
        tvTotalDeposits = findViewById(R.id.tvTotalDeposits);
        tvMealDetailsFormula = findViewById(R.id.tvMealDetailsFormula);
        tvConsumedMealCost = findViewById(R.id.tvConsumedMealCost);
        tvSharedFoodShare = findViewById(R.id.tvSharedFoodShare);
        tvUtilityShare = findViewById(R.id.tvUtilityShare);
        tvSmsCharges = findViewById(R.id.tvSmsCharges);
        tvBalanceStatusLabel = findViewById(R.id.tvBalanceStatusLabel);
        tvNetBalance = findViewById(R.id.tvNetBalance);

        layoutActivityHistoryContainer = findViewById(R.id.layoutActivityHistoryContainer);
        tvEmptyHistory = findViewById(R.id.tvEmptyHistory);

        layoutManagerActions = findViewById(R.id.layoutManagerActions);
        btnManageMemberPermissions = findViewById(R.id.btnManageMemberPermissions);
        btnSendDueReminderSms = findViewById(R.id.btnSendDueReminderSms);
        btnEditProfileInfo = findViewById(R.id.btnEditProfileInfo);

        if (btnBack != null) btnBack.setOnClickListener(v -> finish());

        // Edit Profile Details Button
        if (btnEditProfileInfo != null) {
            btnEditProfileInfo.setOnClickListener(v -> showEditProfileDialog());
        }

        // Avatar Click / Edit Button
        View.OnClickListener avatarPickerListener = v -> {
            boolean canEdit = (sessionManager.getUserId() == targetUserId) || sessionManager.isManager();
            if (canEdit) {
                showAvatarChoiceDialog();
            } else {
                Toast.makeText(this, "শুধুমাত্র মেস ম্যানেজার অন্য সদস্যদের ছবি পরিবর্তন করতে পারবেন", Toast.LENGTH_SHORT).show();
            }
        };

        if (btnEditAvatar != null) btnEditAvatar.setOnClickListener(avatarPickerListener);
        if (ivProfileAvatar != null) ivProfileAvatar.setOnClickListener(avatarPickerListener);

        // Manager Actions
        View.OnClickListener openManageListener = v -> {
            Intent intent = new Intent(this, ActivityMemberManage.class);
            intent.putExtra(ActivityMemberManage.EXTRA_USER_ID, targetUserId);
            startActivityForResult(intent, REQ_MANAGE_ROLE);
        };

        if (btnManageMemberTop != null) btnManageMemberTop.setOnClickListener(openManageListener);
        if (btnManageMemberPermissions != null) btnManageMemberPermissions.setOnClickListener(openManageListener);

        if (btnSendDueReminderSms != null) {
            btnSendDueReminderSms.setOnClickListener(v -> {
                Intent intent = new Intent(this, DueReminderActivity.class);
                intent.putExtra("TARGET_USER_ID", targetUserId);
                startActivity(intent);
            });
        }
    }

    private void loadUserProfile() {
        targetUser = userDao.getById(targetUserId);

        // Fallback: If not found by targetUserId, lookup by phone or synthesize from SessionManager
        if (targetUser == null) {
            if (sessionManager.getUserPhone() != null && !sessionManager.getUserPhone().isEmpty()) {
                targetUser = userDao.getByPhone(sessionManager.getUserPhone());
                if (targetUser != null) {
                    targetUserId = targetUser.getId();
                }
            }
            if (targetUser == null && (sessionManager.getUserId() == targetUserId || targetUserId <= 0)) {
                // Synthesize from SessionManager so logged-in user profile always loads!
                targetUser = new User();
                long sUid = sessionManager.getUserId() > 0 ? sessionManager.getUserId() : 1;
                targetUser.setId(sUid);
                targetUser.setMessId(sessionManager.getMessId());
                targetUser.setName(sessionManager.getUserName() != null && !sessionManager.getUserName().isEmpty() ? sessionManager.getUserName() : "আমার প্রোফাইল");
                targetUser.setPhone(sessionManager.getUserPhone() != null ? sessionManager.getUserPhone() : "");
                targetUser.setRole(sessionManager.getUserRole() != null ? sessionManager.getUserRole() : User.ROLE_MEMBER);
                targetUser.setStatus(User.STATUS_ACTIVE);
                targetUser.setCreatedAt(DateTimeUtils.getCurrentDateTime());
                targetUser.setUpdatedAt(DateTimeUtils.getCurrentDateTime());
                targetUserId = sUid;
                userDao.insertOrUpdate(targetUser);
            }
        }

        if (targetUser == null) {
            Toast.makeText(this, "সদস্য পাওয়া যায়নি", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // 1. Header Details
        tvProfileName.setText(targetUser.getName());

        // Mask phone for non-managers
        boolean isManager = sessionManager.isManager();
        boolean isSelf = (sessionManager.getUserId() == targetUserId);
        if (isManager || isSelf) {
            tvProfilePhone.setText(targetUser.getPhone());
        } else {
            tvProfilePhone.setText(maskPhoneNumber(targetUser.getPhone()));
        }

        if (targetUser.getCreatedAt() != null && targetUser.getCreatedAt().length() >= 10) {
            tvJoinedDate.setText("Member since " + targetUser.getCreatedAt().substring(0, 10));
        } else {
            tvJoinedDate.setText("Active Member");
        }

        // Role & Status Badges
        String role = targetUser.getRole() != null ? targetUser.getRole() : User.ROLE_MEMBER;
        if (targetUser.isManager()) {
            tvRoleBadge.setText("Manager");
            tvRoleBadge.setTextColor(ContextCompat.getColor(this, R.color.primary));
        } else if (targetUser.isAssistant()) {
            tvRoleBadge.setText("Bazar Boy");
            tvRoleBadge.setTextColor(ContextCompat.getColor(this, R.color.accent));
        } else {
            tvRoleBadge.setText("Member");
            tvRoleBadge.setTextColor(ContextCompat.getColor(this, R.color.text_secondary));
        }

        String status = targetUser.getStatus() != null ? targetUser.getStatus() : User.STATUS_ACTIVE;
        if (targetUser.isOnLeave()) {
            tvStatusBadge.setText("On Leave");
            tvStatusBadge.setTextColor(ContextCompat.getColor(this, R.color.warning_amber));
        } else if (targetUser.hasLeft()) {
            tvStatusBadge.setText("Left Mess");
            tvStatusBadge.setTextColor(ContextCompat.getColor(this, R.color.error_red));
        } else {
            tvStatusBadge.setText("Active");
            tvStatusBadge.setTextColor(ContextCompat.getColor(this, R.color.credit_green));
        }

        // Avatar loading
        if (targetUser.getAvatarUrl() != null && !targetUser.getAvatarUrl().trim().isEmpty()) {
            Glide.with(this).load(targetUser.getAvatarUrl()).circleCrop().into(ivProfileAvatar);
        } else {
            ivProfileAvatar.setImageResource(R.drawable.ic_member);
        }

        // Permissions for Manager UI
        if (isManager) {
            btnManageMemberTop.setVisibility(View.VISIBLE);
            layoutManagerActions.setVisibility(View.VISIBLE);
        } else {
            btnManageMemberTop.setVisibility(View.GONE);
            layoutManagerActions.setVisibility(View.GONE);
        }

        if (!isSelf && !isManager) {
            btnEditAvatar.setVisibility(View.GONE);
            if (btnEditProfileInfo != null) btnEditProfileInfo.setVisibility(View.GONE);
        } else {
            btnEditAvatar.setVisibility(View.VISIBLE);
            if (btnEditProfileInfo != null) btnEditProfileInfo.setVisibility(View.VISIBLE);
        }

        // 2. Financial Ledger Card
        loadFinancialStatement();

        // 3. Activity History
        loadActivityHistory();
    }

    private void showEditProfileDialog() {
        if (targetUser == null) return;

        android.widget.LinearLayout layout = new android.widget.LinearLayout(this);
        layout.setOrientation(android.widget.LinearLayout.VERTICAL);
        layout.setPadding(48, 24, 48, 16);

        final com.google.android.material.textfield.TextInputLayout tilName = new com.google.android.material.textfield.TextInputLayout(this);
        tilName.setHint("নাম");
        final com.google.android.material.textfield.TextInputEditText etName = new com.google.android.material.textfield.TextInputEditText(this);
        etName.setText(targetUser.getName());
        tilName.addView(etName);
        layout.addView(tilName);

        final com.google.android.material.textfield.TextInputLayout tilPhone = new com.google.android.material.textfield.TextInputLayout(this);
        tilPhone.setHint("ফোন নম্বর");
        android.widget.LinearLayout.LayoutParams lp = new android.widget.LinearLayout.LayoutParams(
                android.widget.LinearLayout.LayoutParams.MATCH_PARENT,
                android.widget.LinearLayout.LayoutParams.WRAP_CONTENT
        );
        lp.topMargin = 24;
        tilPhone.setLayoutParams(lp);
        final com.google.android.material.textfield.TextInputEditText etPhone = new com.google.android.material.textfield.TextInputEditText(this);
        etPhone.setText(targetUser.getPhone());
        etPhone.setInputType(android.text.InputType.TYPE_CLASS_PHONE);
        tilPhone.addView(etPhone);
        layout.addView(tilPhone);

        new MaterialAlertDialogBuilder(this)
                .setTitle("প্রোফাইল তথ্য পরিবর্তন")
                .setView(layout)
                .setPositiveButton("সংরক্ষণ করুন", (dialog, which) -> {
                    String newName = etName.getText() != null ? etName.getText().toString().trim() : "";
                    String newPhone = etPhone.getText() != null ? etPhone.getText().toString().trim() : "";
                    if (newName.isEmpty()) {
                        Toast.makeText(this, "নাম খালি রাখা যাবে না", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    if (newPhone.isEmpty()) {
                        Toast.makeText(this, "ফোন নম্বর খালি রাখা যাবে না", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    saveProfileChanges(newName, newPhone);
                })
                .setNegativeButton("বাতিল", null)
                .show();
    }

    private void saveProfileChanges(String newName, String newPhone) {
        targetUser.setName(newName);
        targetUser.setPhone(newPhone);
        userDao.updateUserProfile(targetUserId, newName, newPhone, targetUser.getAvatarUrl());

        if (sessionManager.getUserId() == targetUserId) {
            sessionManager.setUserName(newName);
            sessionManager.setUserPhone(newPhone);
        }

        tvProfileName.setText(newName);
        boolean isManager = sessionManager.isManager();
        boolean isSelf = (sessionManager.getUserId() == targetUserId);
        if (isManager || isSelf) {
            tvProfilePhone.setText(newPhone);
        } else {
            tvProfilePhone.setText(maskPhoneNumber(newPhone));
        }

        Toast.makeText(this, "প্রোফাইল তথ্য সফলভাবে আপডেট হয়েছে!", Toast.LENGTH_SHORT).show();

        // Sync to server API
        syncProfileToServer(newName, newPhone);
    }

    private void syncProfileToServer(String newName, String newPhone) {
        OkHttpClient client = ApiClient.getOkHttpClient(getApplicationContext());
        boolean isSelf = (sessionManager.getUserId() == targetUserId);

        String url = isSelf ? (ApiConfig.BASE_URL + "users/profile")
                            : (ApiConfig.BASE_URL + "members/" + targetUserId + "/update");

        FormBody.Builder formBuilder = new FormBody.Builder()
                .add("name", newName)
                .add("phone", newPhone);

        if (!isSelf) {
            if (targetUser.getRole() != null) formBuilder.add("role", targetUser.getRole());
            if (targetUser.getStatus() != null) formBuilder.add("status", targetUser.getStatus());
        }

        Request request = new Request.Builder()
                .url(url)
                .post(formBuilder.build())
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                // Offline fallback already updated in SQLite
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) {
                response.close();
            }
        });
    }

    private void loadFinancialStatement() {
        long messId = sessionManager.getMessId();
        Mess mess = messDao.getById(messId);
        int cycleStartDay = mess != null ? mess.getCycleStartDay() : 1;
        String startDate = DateTimeUtils.currentMonthStart(cycleStartDay);
        String endDate = DateTimeUtils.currentMonthEnd(cycleStartDay);

        tvCycleDates.setText("Cycle: " + DateTimeUtils.formatDisplayDate(startDate) + " - " + DateTimeUtils.formatDisplayDate(endDate));

        CycleSummary cycleSummary = accountingEngine.calculateCycleSummary(messId, startDate, endDate);
        MemberBalanceSheet memberSheet = null;

        if (cycleSummary != null && cycleSummary.getMemberBalances() != null) {
            for (MemberBalanceSheet sheet : cycleSummary.getMemberBalances()) {
                if (sheet.getUserId() == targetUserId) {
                    memberSheet = sheet;
                    break;
                }
            }
        }

        if (memberSheet != null) {
            tvTotalDeposits.setText(CurrencyUtils.format(memberSheet.getTotalDeposit()));
            tvConsumedMealCost.setText(CurrencyUtils.format(memberSheet.getMealCost()));
            tvMealDetailsFormula.setText(String.format(Locale.US, "%.1f Meals × %s rate",
                    memberSheet.getConsumedMeals(), CurrencyUtils.format(cycleSummary.getMealRate())));

            tvSharedFoodShare.setText(CurrencyUtils.format(memberSheet.getSharedFoodCost()));
            tvUtilityShare.setText(CurrencyUtils.format(memberSheet.getUtilityAssetCost()));
            tvSmsCharges.setText(CurrencyUtils.format(memberSheet.getIndividualCost()));

            double netBalance = memberSheet.getNetBalance();
            if (netBalance >= 0) {
                tvNetBalance.setText("+" + CurrencyUtils.format(netBalance));
                tvNetBalance.setTextColor(ContextCompat.getColor(this, R.color.credit_green));
                tvNetBalance.setBackgroundResource(R.drawable.badge_credit);
                tvBalanceStatusLabel.setText("Refund Due (Credit)");
                btnSendDueReminderSms.setVisibility(View.GONE);
            } else {
                tvNetBalance.setText(CurrencyUtils.format(netBalance));
                tvNetBalance.setTextColor(ContextCompat.getColor(this, R.color.error_red));
                tvNetBalance.setBackgroundResource(R.drawable.badge_due);
                tvBalanceStatusLabel.setText("Outstanding Debt (Payable)");
                if (sessionManager.isManager()) {
                    btnSendDueReminderSms.setVisibility(View.VISIBLE);
                }
            }
        } else {
            // Fallback for member not in active balances
            double userDeposits = depositDao.getTotalUserDeposits(messId, targetUserId, startDate, endDate);
            tvTotalDeposits.setText(CurrencyUtils.format(userDeposits));
            tvConsumedMealCost.setText("৳ 0.00");
            tvMealDetailsFormula.setText("0 Meals × ৳0.00 rate");
            tvSharedFoodShare.setText("৳ 0.00");
            tvUtilityShare.setText("৳ 0.00");
            tvSmsCharges.setText("৳ 0.00");
            tvNetBalance.setText(CurrencyUtils.format(userDeposits));
        }
    }

    private void loadActivityHistory() {
        layoutActivityHistoryContainer.removeAllViews();
        long messId = sessionManager.getMessId();

        List<Expense> bazarTrips = expenseDao.getUserBazarTrips(messId, targetUserId);
        List<Deposit> deposits = depositDao.getUserDeposits(messId, targetUserId);

        boolean hasEntries = (bazarTrips != null && !bazarTrips.isEmpty()) || (deposits != null && !deposits.isEmpty());

        if (!hasEntries) {
            tvEmptyHistory.setVisibility(View.VISIBLE);
            return;
        }

        tvEmptyHistory.setVisibility(View.GONE);
        LayoutInflater inflater = LayoutInflater.from(this);

        // Display recent bazar trips
        if (bazarTrips != null) {
            for (Expense exp : bazarTrips) {
                View row = inflater.inflate(R.layout.item_profile_activity_row, layoutActivityHistoryContainer, false);
                ImageView ivIcon = row.findViewById(R.id.ivActivityIcon);
                TextView tvTitle = row.findViewById(R.id.tvActivityTitle);
                TextView tvDate = row.findViewById(R.id.tvActivityDate);
                TextView tvAmount = row.findViewById(R.id.tvActivityAmount);

                ivIcon.setImageResource(R.drawable.ic_lucide_cart);
                ivIcon.setColorFilter(ContextCompat.getColor(this, R.color.primary));

                tvTitle.setText(exp.getTitle() != null ? exp.getTitle() : "Bazar Expense");
                tvDate.setText(exp.getExpenseDate() + " • " + exp.getExpenseCategory());
                tvAmount.setText(CurrencyUtils.format(exp.getAmount()));
                tvAmount.setTextColor(ContextCompat.getColor(this, R.color.primary_dark));

                layoutActivityHistoryContainer.addView(row);
            }
        }

        // Display recent deposits
        if (deposits != null) {
            for (Deposit dep : deposits) {
                View row = inflater.inflate(R.layout.item_profile_activity_row, layoutActivityHistoryContainer, false);
                ImageView ivIcon = row.findViewById(R.id.ivActivityIcon);
                TextView tvTitle = row.findViewById(R.id.tvActivityTitle);
                TextView tvDate = row.findViewById(R.id.tvActivityDate);
                TextView tvAmount = row.findViewById(R.id.tvActivityAmount);

                ivIcon.setImageResource(R.drawable.ic_deposit);
                ivIcon.setColorFilter(ContextCompat.getColor(this, R.color.credit_green));

                String note = dep.getNote() != null && !dep.getNote().trim().isEmpty() ? dep.getNote() : "Meal Advance Deposit";
                tvTitle.setText("Deposit: " + note);
                tvDate.setText(dep.getDepositDate());
                tvAmount.setText("+" + CurrencyUtils.format(dep.getAmount()));
                tvAmount.setTextColor(ContextCompat.getColor(this, R.color.credit_green));

                layoutActivityHistoryContainer.addView(row);
            }
        }
    }

    private String maskPhoneNumber(String phone) {
        if (phone == null || phone.length() < 7) return phone != null ? phone : "";
        int len = phone.length();
        String start = phone.substring(0, Math.min(4, len));
        String end = phone.substring(Math.max(len - 4, 0));
        return start + "****" + end;
    }

    // ==========================================
    // CLOUD AVATAR UPLOADER
    // ==========================================
    private void showAvatarChoiceDialog() {
        new MaterialAlertDialogBuilder(this)
                .setTitle("প্রোফাইল ছবি পরিবর্তন")
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

        if (requestCode == REQ_MANAGE_ROLE && resultCode == RESULT_OK) {
            loadUserProfile();
            return;
        }

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

        // 1. Center Crop square 300x300
        int size = Math.min(originalBitmap.getWidth(), originalBitmap.getHeight());
        int x = (originalBitmap.getWidth() - size) / 2;
        int y = (originalBitmap.getHeight() - size) / 2;
        Bitmap cropped = Bitmap.createBitmap(originalBitmap, x, y, size, size);
        Bitmap scaled = Bitmap.createScaledBitmap(cropped, 300, 300, true);

        // 2. Compress to WebP file
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

        // Preview locally immediately
        Glide.with(this).load(scaled).circleCrop().into(ivProfileAvatar);

        // 3. Upload to cPanel Laravel backend via POST /api/v1/uploads/avatar
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
                    // Save local cached file path as fallback
                    userDao.updateAvatar(targetUserId, webpFile.getAbsolutePath());
                    Toast.makeText(ActivityUserProfile.this, "ছবি অফলাইনে সংরক্ষিত। ইন্টারনেট পেলে সিঙ্ক হবে।", Toast.LENGTH_SHORT).show();
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
                            Glide.with(ActivityUserProfile.this).load(avatarUrl).circleCrop().into(ivProfileAvatar);
                            Toast.makeText(ActivityUserProfile.this, "প্রোফাইল ছবি সফলভাবে আপডেট হয়েছে!", Toast.LENGTH_SHORT).show();
                        });
                    } else {
                        mainHandler.post(() -> {
                            userDao.updateAvatar(targetUserId, webpFile.getAbsolutePath());
                            Toast.makeText(ActivityUserProfile.this, "অফলাইনে সংরক্ষিত। সার্ভার সমস্যা: " + response.code(), Toast.LENGTH_SHORT).show();
                        });
                    }
                } catch (Exception ex) {
                    mainHandler.post(() -> userDao.updateAvatar(targetUserId, webpFile.getAbsolutePath()));
                }
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadUserProfile();
    }
}

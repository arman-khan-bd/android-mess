package com.smartmess.android.ui.expenses;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.smartmess.android.R;
import com.smartmess.android.data.local.DatabaseHelper;
import com.smartmess.android.data.local.dao.ExpenseDao;
import com.smartmess.android.data.local.dao.UserDao;
import com.smartmess.android.engine.VoucherManager;
import com.smartmess.android.model.Expense;
import com.smartmess.android.model.User;
import com.smartmess.android.utils.DateTimeUtils;
import com.smartmess.android.utils.PermissionHelper;
import com.smartmess.android.utils.SessionManager;

import com.smartmess.android.engine.PlanGateManager;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class AddExpenseActivity extends AppCompatActivity {

    private static final int REQ_PICK_VOUCHER = 201;

    private EditText etExpenseTitle;
    private EditText etExpenseAmount;
    private Spinner spCategory;
    private Spinner spSplitType;
    private Spinner spBuyer;
    private View layoutTargetMember;
    private Spinner spTargetMember;
    private EditText etExpenseDate;
    private MaterialButton btnCaptureCamera;
    private MaterialButton btnAttachVoucher;
    private ImageView ivVoucherPreview;
    private TextView tvVoucherStatus;
    private MaterialButton btnSaveExpense;

    private ExpenseDao expenseDao;
    private UserDao userDao;
    private SessionManager sessionManager;
    private PlanGateManager planGateManager;
    private List<User> members = new ArrayList<>();
    private File savedVoucherFile = null;

    private final String[] categories = {
            "Variable (Raw Meal / Bazar)",
            "Shared Food (Oil, Salt, Gas)",
            "Asset & Utility (Rent, Maid, Wi-Fi)",
            "SMS Charge"
    };

    private final String[] splitTypes = {
            "Meal Dependent",
            "Split Equally (All Members)",
            "Individual Member Charge"
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_expense);

        sessionManager = new SessionManager(this);
        if (!sessionManager.canLogExpenses()) {
            Toast.makeText(this, "শুধুমাত্র ম্যানেজার এবং বাজার সদস্যরা খরচ এন্ট্রি করতে পারবেন", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        DatabaseHelper helper = DatabaseHelper.getInstance(this);
        expenseDao = new ExpenseDao(helper);
        userDao = new UserDao(helper);
        planGateManager = new PlanGateManager(this);

        initViews();
        setupSpinners();

        btnAttachVoucher.setOnClickListener(v -> pickVoucherImage());
        btnCaptureCamera.setOnClickListener(v -> {
            if (!planGateManager.canUseOcr()) {
                planGateManager.showUpgradeBottomSheet(getSupportFragmentManager(), "Smart Receipt OCR Scanner",
                        "Automated receipt OCR extraction is a Pro feature. Upgrade to scan and auto-extract bill amounts.");
                return;
            }
            pickVoucherImage();
            Toast.makeText(this, "ভাউচার স্ক্যানার সক্রিয় (প্রো)। ছবি প্রসেস করা হচ্ছে...", Toast.LENGTH_SHORT).show();
        });
        btnSaveExpense.setOnClickListener(v -> saveExpense());
    }

    private void initViews() {
        etExpenseTitle = findViewById(R.id.etExpenseTitle);
        etExpenseAmount = findViewById(R.id.etExpenseAmount);
        spCategory = findViewById(R.id.spCategory);
        spSplitType = findViewById(R.id.spSplitType);
        spBuyer = findViewById(R.id.spBuyer);
        layoutTargetMember = findViewById(R.id.layoutTargetMember);
        spTargetMember = findViewById(R.id.spTargetMember);
        etExpenseDate = findViewById(R.id.etExpenseDate);
        btnCaptureCamera = findViewById(R.id.btnCaptureCamera);
        btnAttachVoucher = findViewById(R.id.btnAttachVoucher);
        ivVoucherPreview = findViewById(R.id.ivVoucherPreview);
        tvVoucherStatus = findViewById(R.id.tvVoucherStatus);
        btnSaveExpense = findViewById(R.id.btnSaveExpense);

        etExpenseDate.setText(DateTimeUtils.currentDate());
    }

    private void setupSpinners() {
        ArrayAdapter<String> catAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, categories);
        spCategory.setAdapter(catAdapter);

        ArrayAdapter<String> splitAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, splitTypes);
        spSplitType.setAdapter(splitAdapter);

        long messId = sessionManager.getMessId();
        members = userDao.getActiveMembersByMess(messId);
        List<String> memberNames = new ArrayList<>();
        int currentBuyerIndex = 0;
        for (int i = 0; i < members.size(); i++) {
            User u = members.get(i);
            memberNames.add(u.getName());
            if (u.getId() == sessionManager.getUserId()) {
                currentBuyerIndex = i;
            }
        }

        ArrayAdapter<String> buyerAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, memberNames);
        spBuyer.setAdapter(buyerAdapter);
        spBuyer.setSelection(currentBuyerIndex);

        ArrayAdapter<String> targetAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, memberNames);
        spTargetMember.setAdapter(targetAdapter);

        // Auto-change split type when category changes
        spCategory.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (position == 0) {
                    spSplitType.setSelection(0); // Meal dependent
                } else if (position == 1 || position == 2) {
                    spSplitType.setSelection(1); // Split equally
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        spSplitType.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (position == 2) {
                    layoutTargetMember.setVisibility(View.VISIBLE);
                } else {
                    layoutTargetMember.setVisibility(View.GONE);
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });
    }

    private void pickVoucherImage() {
        if (!PermissionHelper.hasCameraStoragePermission(this)) {
            PermissionHelper.requestCameraStoragePermission(this);
            return;
        }

        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("image/*");
        startActivityForResult(Intent.createChooser(intent, "Select Voucher Bill Photo"), REQ_PICK_VOUCHER);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQ_PICK_VOUCHER && resultCode == RESULT_OK && data != null && data.getData() != null) {
            Uri imageUri = data.getData();
            savedVoucherFile = VoucherManager.saveAndCompressVoucher(this, imageUri, sessionManager.getMessUuid());
            if (savedVoucherFile != null) {
                ivVoucherPreview.setVisibility(View.VISIBLE);
                ivVoucherPreview.setImageURI(Uri.fromFile(savedVoucherFile));
                tvVoucherStatus.setText("WebP compressed (" + (savedVoucherFile.length() / 1024) + " KB)");
                tvVoucherStatus.setTextColor(getResources().getColor(R.color.credit_green));
            }
        }
    }

    private void saveExpense() {
        String title = etExpenseTitle.getText().toString().trim();
        String amountStr = etExpenseAmount.getText().toString().trim();
        String date = etExpenseDate.getText().toString().trim();

        if (title.isEmpty() || amountStr.isEmpty()) {
            Toast.makeText(this, "খরচের বিবরণ ও টাকার পরিমাণ লিখুন", Toast.LENGTH_SHORT).show();
            return;
        }

        double amount;
        try {
            amount = Double.parseDouble(amountStr);
        } catch (NumberFormatException e) {
            Toast.makeText(this, "টাকার পরিমাণ সঠিক নয়", Toast.LENGTH_SHORT).show();
            return;
        }

        if (date.isEmpty()) date = DateTimeUtils.currentDate();

        String categoryKey = Expense.CAT_RAW_MEAL;
        int catPos = spCategory.getSelectedItemPosition();
        if (catPos == 1) categoryKey = Expense.CAT_SHARED_FOOD;
        else if (catPos == 2) categoryKey = Expense.CAT_UTILITY_ASSET;
        else if (catPos == 3) categoryKey = Expense.CAT_SMS_CHARGE;

        String splitTypeKey = Expense.SPLIT_MEAL_DEPENDENT;
        int splitPos = spSplitType.getSelectedItemPosition();
        if (splitPos == 1) splitTypeKey = Expense.SPLIT_ALL_EQUAL;
        else if (splitPos == 2) splitTypeKey = Expense.SPLIT_INDIVIDUAL;

        User buyer = members.get(spBuyer.getSelectedItemPosition());
        Long targetUserId = null;
        if (Expense.SPLIT_INDIVIDUAL.equals(splitTypeKey)) {
            User target = members.get(spTargetMember.getSelectedItemPosition());
            targetUserId = target.getId();
        }

        String now = DateTimeUtils.nowIso();
        Expense expense = new Expense(
                UUID.randomUUID().toString(),
                sessionManager.getMessId(),
                buyer.getId(),
                categoryKey,
                amount,
                date,
                title,
                splitTypeKey
        );
        expense.setTargetUserId(targetUserId);
        if (savedVoucherFile != null) {
            expense.setVoucherImageUrl(savedVoucherFile.getAbsolutePath());
        }
        expense.setCreatedAt(now);
        expense.setUpdatedAt(now);

        expenseDao.insertOrUpdate(expense);

        com.smartmess.android.utils.NotificationCenterHelper.postNotification(
                getApplicationContext(),
                sessionManager.getMessId(),
                "নতুন খরচ যুক্ত হয়েছে",
                buyer.getName() + " ৳" + String.format(java.util.Locale.US, "%.2f", amount) + " খরচ যোগ করেছেন (" + title + ")",
                com.smartmess.android.utils.NotificationCenterHelper.TYPE_EXPENSE
        );

        Toast.makeText(this, "খরচের হিসাব সফলভাবে সংরক্ষিত হয়েছে!", Toast.LENGTH_SHORT).show();
        finish();
    }
}

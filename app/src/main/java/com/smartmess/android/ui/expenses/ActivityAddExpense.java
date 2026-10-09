package com.smartmess.android.ui.expenses;

import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
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
import com.smartmess.android.data.sync.SyncManager;
import com.smartmess.android.engine.VoucherManager;
import com.smartmess.android.model.Expense;
import com.smartmess.android.model.User;
import com.smartmess.android.utils.DateTimeUtils;
import com.smartmess.android.utils.PermissionHelper;
import com.smartmess.android.utils.SessionManager;

import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class ActivityAddExpense extends AppCompatActivity {

    private static final int REQ_CAPTURE_CAMERA = 101;
    private static final int REQ_PICK_GALLERY = 102;

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
    private View layoutVoucherPreview;
    private ImageView ivVoucherPreview;
    private TextView tvCompressionIndicator;
    private TextView tvVoucherStatus;
    private MaterialButton btnSaveExpense;

    private ExpenseDao expenseDao;
    private UserDao userDao;
    private SessionManager sessionManager;
    private List<User> members = new ArrayList<>();
    private File savedVoucherFile = null;

    private final String[] categoryLabels = {
            "Raw Meal (Affects meal rate)",
            "Shared Food (Onion, Garlic, Salt - equally split)",
            "Utility & Assets (Bulbs, Locks - equally split)",
            "Individual Penalty / Custom Charge"
    };

    private final String[] categoryDbKeys = {
            "raw_meal",
            "shared_food",
            "utility_asset",
            "individual_charge"
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
            Toast.makeText(this, "Only Managers and Bazar members can log expenses", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        DatabaseHelper helper = DatabaseHelper.getInstance(this);
        expenseDao = new ExpenseDao(helper);
        userDao = new UserDao(helper);

        initViews();
        setupSpinners();

        btnCaptureCamera.setOnClickListener(v -> launchCameraCapture());
        btnAttachVoucher.setOnClickListener(v -> launchGalleryPicker());
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
        layoutVoucherPreview = findViewById(R.id.layoutVoucherPreview);
        ivVoucherPreview = findViewById(R.id.ivVoucherPreview);
        tvCompressionIndicator = findViewById(R.id.tvCompressionIndicator);
        tvVoucherStatus = findViewById(R.id.tvVoucherStatus);
        btnSaveExpense = findViewById(R.id.btnSaveExpense);

        etExpenseDate.setText(DateTimeUtils.getCurrentDate());
    }

    private void setupSpinners() {
        ArrayAdapter<String> catAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, categoryLabels);
        spCategory.setAdapter(catAdapter);

        ArrayAdapter<String> splitAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, splitTypes);
        spSplitType.setAdapter(splitAdapter);

        long messId = sessionManager.getMessId();
        members = userDao.getAllMembers(messId);

        List<String> memberNames = new ArrayList<>();
        int currentBuyerIndex = 0;
        long currentUserId = sessionManager.getUserId();

        for (int i = 0; i < members.size(); i++) {
            User u = members.get(i);
            memberNames.add(u.getName() + " (" + u.getRole() + ")");
            if (u.getId() == currentUserId) {
                currentBuyerIndex = i;
            }
        }

        ArrayAdapter<String> memberAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, memberNames);
        spBuyer.setAdapter(memberAdapter);
        spBuyer.setSelection(currentBuyerIndex);

        spTargetMember.setAdapter(memberAdapter);

        spCategory.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (position == 0) {
                    spSplitType.setSelection(0); // Meal Dependent
                    layoutTargetMember.setVisibility(View.GONE);
                } else if (position == 1 || position == 2) {
                    spSplitType.setSelection(1); // Split Equally
                    layoutTargetMember.setVisibility(View.GONE);
                } else if (position == 3) {
                    spSplitType.setSelection(2); // Individual Charge
                    layoutTargetMember.setVisibility(View.VISIBLE);
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

    private void launchCameraCapture() {
        if (!PermissionHelper.hasCameraPermission(this)) {
            PermissionHelper.requestCameraPermission(this);
            return;
        }
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
            startActivityForResult(takePictureIntent, REQ_CAPTURE_CAMERA);
        } else {
            Toast.makeText(this, "Camera application not found", Toast.LENGTH_SHORT).show();
        }
    }

    private void launchGalleryPicker() {
        if (!PermissionHelper.hasStoragePermission(this)) {
            PermissionHelper.requestStoragePermission(this);
            return;
        }
        Intent pickIntent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        startActivityForResult(pickIntent, REQ_PICK_GALLERY);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode == RESULT_OK && data != null) {
            String messUuid = sessionManager.getMessUuid();
            if (messUuid == null || messUuid.isEmpty()) messUuid = "default_mess";

            if (requestCode == REQ_CAPTURE_CAMERA) {
                Bundle extras = data.getExtras();
                if (extras != null && extras.get("data") != null) {
                    Bitmap photo = (Bitmap) extras.get("data");
                    processAndDisplayBitmap(photo, messUuid);
                }
            } else if (requestCode == REQ_PICK_GALLERY) {
                Uri imageUri = data.getData();
                if (imageUri != null) {
                    savedVoucherFile = VoucherManager.saveAndCompressVoucher(this, imageUri, messUuid);
                    if (savedVoucherFile != null) {
                        layoutVoucherPreview.setVisibility(View.VISIBLE);
                        ivVoucherPreview.setImageURI(Uri.fromFile(savedVoucherFile));
                        long sizeKb = savedVoucherFile.length() / 1024;
                        tvCompressionIndicator.setText("WebP Compressed • " + sizeKb + " KB • 1080px max");
                        tvVoucherStatus.setText("Offline SQLite ready: " + savedVoucherFile.getName());
                    }
                }
            }
        }
    }

    private void processAndDisplayBitmap(Bitmap bitmap, String messUuid) {
        try {
            File dir = new File(getFilesDir(), "vouchers/" + messUuid);
            if (!dir.exists()) dir.mkdirs();

            String filename = "voucher_cam_" + UUID.randomUUID().toString().substring(0, 8) + ".webp";
            File outputFile = new File(dir, filename);
            FileOutputStream fos = new FileOutputStream(outputFile);

            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
                bitmap.compress(Bitmap.CompressFormat.WEBP_LOSSY, 75, fos);
            } else {
                @SuppressWarnings("deprecation")
                Bitmap.CompressFormat format = Bitmap.CompressFormat.WEBP;
                bitmap.compress(format, 75, fos);
            }
            fos.flush();
            fos.close();

            savedVoucherFile = outputFile;
            layoutVoucherPreview.setVisibility(View.VISIBLE);
            ivVoucherPreview.setImageBitmap(bitmap);

            long sizeKb = savedVoucherFile.length() / 1024;
            tvCompressionIndicator.setText("Camera WebP Encoded • " + sizeKb + " KB (75% quality)");
            tvVoucherStatus.setText("Saved locally: " + savedVoucherFile.getName());

        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "Failed to compress captured photo", Toast.LENGTH_SHORT).show();
        }
    }

    private void saveExpense() {
        String title = etExpenseTitle.getText().toString().trim();
        String amountStr = etExpenseAmount.getText().toString().trim();
        String date = etExpenseDate.getText().toString().trim();

        if (title.isEmpty()) {
            etExpenseTitle.setError("Expense title is required");
            return;
        }

        if (amountStr.isEmpty()) {
            etExpenseAmount.setError("Amount is required");
            return;
        }

        double amount;
        try {
            amount = Double.parseDouble(amountStr);
            if (amount <= 0) {
                etExpenseAmount.setError("Amount must be greater than 0");
                return;
            }
        } catch (NumberFormatException ex) {
            etExpenseAmount.setError("Invalid number");
            return;
        }

        int buyerPos = spBuyer.getSelectedItemPosition();
        if (buyerPos < 0 || buyerPos >= members.size()) {
            Toast.makeText(this, "Please select who paid", Toast.LENGTH_SHORT).show();
            return;
        }
        User buyer = members.get(buyerPos);

        int catPos = spCategory.getSelectedItemPosition();
        String selectedCategory = (catPos >= 0 && catPos < categoryDbKeys.length) ? categoryDbKeys[catPos] : "raw_meal";

        int splitPos = spSplitType.getSelectedItemPosition();
        String splitType = "meal_dependent";
        if (splitPos == 1) splitType = "equal";
        else if (splitPos == 2) splitType = "individual";

        Long targetUserId = null;
        if ("individual".equals(splitType)) {
            int targetPos = spTargetMember.getSelectedItemPosition();
            if (targetPos >= 0 && targetPos < members.size()) {
                targetUserId = members.get(targetPos).getId();
            }
        }

        String voucherPath = (savedVoucherFile != null) ? savedVoucherFile.getAbsolutePath() : null;

        Expense expense = new Expense();
        expense.setUuid(UUID.randomUUID().toString());
        expense.setMessId(sessionManager.getMessId());
        expense.setBuyerUserId(buyer.getId());
        expense.setExpenseCategory(selectedCategory);
        expense.setAmount(amount);
        expense.setExpenseDate(date);
        expense.setTitle(title);
        expense.setVoucherImageUrl(voucherPath);
        expense.setSplitType(splitType);
        expense.setTargetUserId(targetUserId);
        expense.setSyncStatus(0);
        expense.setCreatedAt(DateTimeUtils.getCurrentDateTime());
        expense.setUpdatedAt(DateTimeUtils.getCurrentDateTime());

        long rowId = expenseDao.insert(expense);
        if (rowId > 0) {
            Toast.makeText(this, "Expense saved to local SQLite!", Toast.LENGTH_SHORT).show();
            // Trigger background sync to cloud
            SyncManager.triggerSync(getApplicationContext());
            finish();
        } else {
            Toast.makeText(this, "Failed to save expense locally", Toast.LENGTH_SHORT).show();
        }
    }
}

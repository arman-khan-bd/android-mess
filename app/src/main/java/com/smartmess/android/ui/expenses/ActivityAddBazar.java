package com.smartmess.android.ui.expenses;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.bumptech.glide.Glide;
import com.google.android.material.button.MaterialButton;
import com.smartmess.android.R;
import com.smartmess.android.data.local.DatabaseHelper;
import com.smartmess.android.data.local.dao.ExpenseDao;
import com.smartmess.android.data.local.dao.UserDao;
import com.smartmess.android.data.sync.SyncManager;
import com.smartmess.android.engine.VoucherManager;
import com.smartmess.android.model.BazarItem;
import com.smartmess.android.model.BazarPayer;
import com.smartmess.android.model.Expense;
import com.smartmess.android.model.User;
import com.smartmess.android.utils.CurrencyUtils;
import com.smartmess.android.utils.DateTimeUtils;
import com.smartmess.android.utils.PermissionHelper;
import com.smartmess.android.utils.SessionManager;

import java.io.File;
import java.io.FileOutputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

public class ActivityAddBazar extends AppCompatActivity {

    private static final int REQ_CAPTURE_CAMERA = 201;
    private static final int REQ_PICK_GALLERY = 202;

    // Header & Info
    private View btnBack;
    private EditText etBazarDate;
    private EditText etBazarTitle;

    // Item Repeater
    private TextView tvTotalItemCost;
    private TextView tvSubtotalRawMeal;
    private TextView tvSubtotalSharedFood;
    private TextView tvSubtotalUtility;
    private LinearLayout layoutItemsContainer;
    private MaterialButton btnAddItemRow;

    // Multi-Payer Split
    private TextView tvTotalPaidAmount;
    private MaterialButton btnPaidByMe;
    private MaterialButton btnSplitEqually;
    private LinearLayout layoutPayersContainer;
    private MaterialButton btnAddPayerRow;
    private LinearLayout layoutBalanceStatus;
    private TextView tvBalanceStatusMessage;

    // Multi-Receipt
    private MaterialButton btnCaptureCamera;
    private MaterialButton btnPickGallery;
    private LinearLayout layoutReceiptsContainer;

    // Submit
    private MaterialButton btnSaveBazar;

    // Database & Session
    private ExpenseDao expenseDao;
    private UserDao userDao;
    private SessionManager sessionManager;
    private List<User> members = new ArrayList<>();
    private final Calendar calendar = Calendar.getInstance();
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.US);

    // Dynamic Row Holders
    private final List<ItemRowHolder> itemRows = new ArrayList<>();
    private final List<PayerRowHolder> payerRows = new ArrayList<>();
    private final List<File> compressedReceiptFiles = new ArrayList<>();

    // Category mappings
    private final String[] categoryNames = {
            "Raw Meal (Affects meal rate)",
            "Shared Pool (Equally split)",
            "Utility (Equally split)"
    };
    private final String[] categoryKeys = {
            Expense.CAT_RAW_MEAL,
            Expense.CAT_SHARED_FOOD,
            Expense.CAT_UTILITY_ASSET
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_bazar);

        sessionManager = new SessionManager(this);
        if (!sessionManager.canLogExpenses()) {
            Toast.makeText(this, "শুধুমাত্র ম্যানেজার এবং দায়িত্বপ্রাপ্ত বাজার সদস্য বাজার এন্ট্রি করতে পারবেন", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        DatabaseHelper helper = DatabaseHelper.getInstance(this);
        expenseDao = new ExpenseDao(helper);
        userDao = new UserDao(helper);

        initViews();
        loadMembers();
        setupDatePicker();

        // Add initial rows
        addItemRow();
        addPayerRow(sessionManager.getUserId());

        // Button listeners
        btnAddItemRow.setOnClickListener(v -> addItemRow());
        btnAddPayerRow.setOnClickListener(v -> addPayerRow(-1));

        btnPaidByMe.setOnClickListener(v -> setPaid100ByCurrentUser());
        btnSplitEqually.setOnClickListener(v -> splitBillEquallyAmongPayers());

        btnCaptureCamera.setOnClickListener(v -> launchCamera());
        btnPickGallery.setOnClickListener(v -> launchGallery());

        btnSaveBazar.setOnClickListener(v -> saveBazarSession());
        if (btnBack != null) {
            btnBack.setOnClickListener(v -> finish());
        }

        recalculateTotals();
    }

    private void initViews() {
        btnBack = findViewById(R.id.btnBack);
        etBazarDate = findViewById(R.id.etBazarDate);
        etBazarTitle = findViewById(R.id.etBazarTitle);

        tvTotalItemCost = findViewById(R.id.tvTotalItemCost);
        tvSubtotalRawMeal = findViewById(R.id.tvSubtotalRawMeal);
        tvSubtotalSharedFood = findViewById(R.id.tvSubtotalSharedFood);
        tvSubtotalUtility = findViewById(R.id.tvSubtotalUtility);
        layoutItemsContainer = findViewById(R.id.layoutItemsContainer);
        btnAddItemRow = findViewById(R.id.btnAddItemRow);

        tvTotalPaidAmount = findViewById(R.id.tvTotalPaidAmount);
        btnPaidByMe = findViewById(R.id.btnPaidByMe);
        btnSplitEqually = findViewById(R.id.btnSplitEqually);
        layoutPayersContainer = findViewById(R.id.layoutPayersContainer);
        btnAddPayerRow = findViewById(R.id.btnAddPayerRow);
        layoutBalanceStatus = findViewById(R.id.layoutBalanceStatus);
        tvBalanceStatusMessage = findViewById(R.id.tvBalanceStatusMessage);

        btnCaptureCamera = findViewById(R.id.btnCaptureCamera);
        btnPickGallery = findViewById(R.id.btnPickGallery);
        layoutReceiptsContainer = findViewById(R.id.layoutReceiptsContainer);

        btnSaveBazar = findViewById(R.id.btnSaveBazar);

        etBazarDate.setText(dateFormat.format(calendar.getTime()));
    }

    private void loadMembers() {
        long messId = sessionManager.getMessId();
        members = userDao.getActiveMembersByMess(messId);
        if (members == null || members.isEmpty()) {
            members = userDao.getAllMembers(messId);
        }
    }

    private void setupDatePicker() {
        etBazarDate.setOnClickListener(v -> {
            DatePickerDialog dialog = new DatePickerDialog(
                    this,
                    (view, year, month, dayOfMonth) -> {
                        calendar.set(Calendar.YEAR, year);
                        calendar.set(Calendar.MONTH, month);
                        calendar.set(Calendar.DAY_OF_MONTH, dayOfMonth);
                        etBazarDate.setText(dateFormat.format(calendar.getTime()));
                    },
                    calendar.get(Calendar.YEAR),
                    calendar.get(Calendar.MONTH),
                    calendar.get(Calendar.DAY_OF_MONTH)
            );
            dialog.show();
        });
    }

    // ==========================================
    // 1. DYNAMIC ITEM REPEATER ENGINE
    // ==========================================
    private void addItemRow() {
        LayoutInflater inflater = LayoutInflater.from(this);
        View rowView = inflater.inflate(R.layout.item_bazar_repeater_row, layoutItemsContainer, false);

        ItemRowHolder holder = new ItemRowHolder(rowView);
        itemRows.add(holder);
        layoutItemsContainer.addView(rowView);

        refreshItemRowIndices();
        recalculateTotals();
    }

    private void refreshItemRowIndices() {
        for (int i = 0; i < itemRows.size(); i++) {
            itemRows.get(i).tvItemRowIndex.setText("Item #" + (i + 1));
        }
    }

    private class ItemRowHolder {
        View rootView;
        TextView tvItemRowIndex;
        View btnDeleteItemRow;
        EditText etItemName;
        Spinner spItemCategory;
        EditText etItemQty;
        EditText etItemPrice;

        ItemRowHolder(View view) {
            rootView = view;
            tvItemRowIndex = view.findViewById(R.id.tvItemRowIndex);
            btnDeleteItemRow = view.findViewById(R.id.btnDeleteItemRow);
            etItemName = view.findViewById(R.id.etItemName);
            spItemCategory = view.findViewById(R.id.spItemCategory);
            etItemQty = view.findViewById(R.id.etItemQty);
            etItemPrice = view.findViewById(R.id.etItemPrice);

            ArrayAdapter<String> catAdapter = new ArrayAdapter<>(ActivityAddBazar.this,
                    android.R.layout.simple_spinner_dropdown_item, categoryNames);
            spItemCategory.setAdapter(catAdapter);

            btnDeleteItemRow.setOnClickListener(v -> {
                if (itemRows.size() > 1) {
                    layoutItemsContainer.removeView(rootView);
                    itemRows.remove(ItemRowHolder.this);
                    refreshItemRowIndices();
                    recalculateTotals();
                } else {
                    Toast.makeText(ActivityAddBazar.this, "অন্তত ১টি পণ্যের নাম প্রয়োজন", Toast.LENGTH_SHORT).show();
                }
            });

            spItemCategory.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> parent, View v, int position, long id) {
                    recalculateTotals();
                }
                @Override
                public void onNothingSelected(AdapterView<?> parent) {}
            });

            etItemPrice.addTextChangedListener(new SimpleTextWatcher() {
                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                    recalculateTotals();
                }
            });
        }

        double getPrice() {
            String str = etItemPrice.getText().toString().trim();
            if (str.isEmpty()) return 0.0;
            try {
                return Double.parseDouble(str);
            } catch (NumberFormatException e) {
                return 0.0;
            }
        }

        String getCategoryKey() {
            int pos = spItemCategory.getSelectedItemPosition();
            if (pos >= 0 && pos < categoryKeys.length) {
                return categoryKeys[pos];
            }
            return Expense.CAT_RAW_MEAL;
        }

        String getItemName() {
            return etItemName.getText().toString().trim();
        }

        String getItemQty() {
            return etItemQty.getText().toString().trim();
        }
    }

    // ==========================================
    // 2. MULTI-MEMBER SPLIT PAYMENT ENGINE
    // ==========================================
    private void addPayerRow(long preselectUserId) {
        LayoutInflater inflater = LayoutInflater.from(this);
        View rowView = inflater.inflate(R.layout.item_bazar_payer_row, layoutPayersContainer, false);

        PayerRowHolder holder = new PayerRowHolder(rowView, preselectUserId);
        payerRows.add(holder);
        layoutPayersContainer.addView(rowView);

        recalculateTotals();
    }

    private class PayerRowHolder {
        View rootView;
        Spinner spPayerMember;
        EditText etPayerAmount;
        View btnDeletePayerRow;

        PayerRowHolder(View view, long preselectUserId) {
            rootView = view;
            spPayerMember = view.findViewById(R.id.spPayerMember);
            etPayerAmount = view.findViewById(R.id.etPayerAmount);
            btnDeletePayerRow = view.findViewById(R.id.btnDeletePayerRow);

            List<String> memberNames = new ArrayList<>();
            int defaultIndex = 0;
            for (int i = 0; i < members.size(); i++) {
                User u = members.get(i);
                memberNames.add(u.getName() + " (" + (u.getRole() != null ? u.getRole() : "Member") + ")");
                if (u.getId() == preselectUserId) {
                    defaultIndex = i;
                }
            }

            ArrayAdapter<String> adapter = new ArrayAdapter<>(ActivityAddBazar.this,
                    android.R.layout.simple_spinner_dropdown_item, memberNames);
            spPayerMember.setAdapter(adapter);
            if (!memberNames.isEmpty() && defaultIndex < memberNames.size()) {
                spPayerMember.setSelection(defaultIndex);
            }

            btnDeletePayerRow.setOnClickListener(v -> {
                if (payerRows.size() > 1) {
                    layoutPayersContainer.removeView(rootView);
                    payerRows.remove(PayerRowHolder.this);
                    recalculateTotals();
                } else {
                    Toast.makeText(ActivityAddBazar.this, "টাকা প্রদানকারী অন্তত ১ জন সদস্য প্রয়োজন", Toast.LENGTH_SHORT).show();
                }
            });

            etPayerAmount.addTextChangedListener(new SimpleTextWatcher() {
                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                    recalculateTotals();
                }
            });
        }

        long getSelectedUserId() {
            int pos = spPayerMember.getSelectedItemPosition();
            if (pos >= 0 && pos < members.size()) {
                return members.get(pos).getId();
            }
            return sessionManager.getUserId();
        }

        String getSelectedUserName() {
            int pos = spPayerMember.getSelectedItemPosition();
            if (pos >= 0 && pos < members.size()) {
                return members.get(pos).getName();
            }
            return "Member";
        }

        double getAmount() {
            String str = etPayerAmount.getText().toString().trim();
            if (str.isEmpty()) return 0.0;
            try {
                return Double.parseDouble(str);
            } catch (NumberFormatException e) {
                return 0.0;
            }
        }

        void setAmount(double amount) {
            etPayerAmount.setText(String.format(Locale.US, "%.2f", amount));
        }
    }

    private void setPaid100ByCurrentUser() {
        double totalBill = calculateTotalItemCost();
        long currentUserId = sessionManager.getUserId();

        // Keep 1st payer or reset to 1 payer
        while (payerRows.size() > 1) {
            View viewToRemove = payerRows.get(payerRows.size() - 1).rootView;
            layoutPayersContainer.removeView(viewToRemove);
            payerRows.remove(payerRows.size() - 1);
        }

        if (payerRows.isEmpty()) {
            addPayerRow(currentUserId);
        }

        PayerRowHolder first = payerRows.get(0);
        // Preselect current user in spinner
        for (int i = 0; i < members.size(); i++) {
            if (members.get(i).getId() == currentUserId) {
                first.spPayerMember.setSelection(i);
                break;
            }
        }
        first.setAmount(totalBill);
        recalculateTotals();
    }

    private void splitBillEquallyAmongPayers() {
        double totalBill = calculateTotalItemCost();
        if (payerRows.isEmpty()) {
            Toast.makeText(this, "প্রথমে অন্তত ১ জন টাকা প্রদানকারী যোগ করুন", Toast.LENGTH_SHORT).show();
            return;
        }

        int count = payerRows.size();
        double splitAmount = round(totalBill / count);
        double distributed = 0.0;

        for (int i = 0; i < count; i++) {
            if (i == count - 1) {
                // Adjust rounding difference on last payer
                double lastShare = round(totalBill - distributed);
                payerRows.get(i).setAmount(lastShare);
            } else {
                payerRows.get(i).setAmount(splitAmount);
                distributed += splitAmount;
            }
        }

        recalculateTotals();
    }

    // ==========================================
    // 3. RECALCULATION & AUTO-VALIDATION
    // ==========================================
    private double calculateTotalItemCost() {
        double total = 0.0;
        for (ItemRowHolder item : itemRows) {
            total += item.getPrice();
        }
        return round(total);
    }

    private void recalculateTotals() {
        double rawSub = 0.0, sharedSub = 0.0, utilSub = 0.0;
        for (ItemRowHolder item : itemRows) {
            double p = item.getPrice();
            String cat = item.getCategoryKey();
            if (Expense.CAT_RAW_MEAL.equals(cat)) {
                rawSub += p;
            } else if (Expense.CAT_SHARED_FOOD.equals(cat)) {
                sharedSub += p;
            } else if (Expense.CAT_UTILITY_ASSET.equals(cat)) {
                utilSub += p;
            }
        }

        double totalBill = round(rawSub + sharedSub + utilSub);
        tvTotalItemCost.setText(CurrencyUtils.format(totalBill));
        tvSubtotalRawMeal.setText("Raw: " + CurrencyUtils.format(rawSub));
        tvSubtotalSharedFood.setText("Shared: " + CurrencyUtils.format(sharedSub));
        tvSubtotalUtility.setText("Utility: " + CurrencyUtils.format(utilSub));

        double totalPaid = 0.0;
        for (PayerRowHolder payer : payerRows) {
            totalPaid += payer.getAmount();
        }
        totalPaid = round(totalPaid);
        tvTotalPaidAmount.setText("Paid: " + CurrencyUtils.format(totalPaid));

        double diff = round(totalBill - totalPaid);

        if (totalBill > 0 && Math.abs(diff) < 0.01) {
            // Perfectly Balanced
            layoutBalanceStatus.setBackgroundResource(R.drawable.badge_credit);
            tvBalanceStatusMessage.setTextColor(ContextCompat.getColor(this, R.color.credit_green));
            tvBalanceStatusMessage.setText("✓ Perfectly Balanced! " + CurrencyUtils.format(totalBill)
                    + " paid across " + payerRows.size() + " contributor(s)");
        } else {
            // Mismatch
            layoutBalanceStatus.setBackgroundResource(R.drawable.badge_due);
            tvBalanceStatusMessage.setTextColor(ContextCompat.getColor(this, R.color.due_red));
            if (diff > 0) {
                tvBalanceStatusMessage.setText("⚠ Unbalanced: " + CurrencyUtils.format(diff) + " remaining to assign");
            } else {
                tvBalanceStatusMessage.setText("⚠ Overpaid: Payments exceed bill by " + CurrencyUtils.format(-diff));
            }
        }
    }

    // ==========================================
    // 4. MULTI-RECEIPT WEBP COMPRESSION PIPELINE
    // ==========================================
    private void launchCamera() {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
            startActivityForResult(takePictureIntent, REQ_CAPTURE_CAMERA);
        } else {
            Toast.makeText(this, "ক্যামেরা অ্যাপ পাওয়া যায়নি", Toast.LENGTH_SHORT).show();
        }
    }

    private void launchGallery() {
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
            if (messUuid == null || messUuid.isEmpty()) messUuid = "mess_" + sessionManager.getMessId();

            if (requestCode == REQ_CAPTURE_CAMERA) {
                Bundle extras = data.getExtras();
                if (extras != null && extras.get("data") != null) {
                    Bitmap photo = (Bitmap) extras.get("data");
                    File compressedFile = compressBitmapToWebp(photo, messUuid);
                    if (compressedFile != null) {
                        addReceiptThumbnail(compressedFile);
                    }
                }
            } else if (requestCode == REQ_PICK_GALLERY) {
                Uri imageUri = data.getData();
                if (imageUri != null) {
                    File compressedFile = VoucherManager.saveAndCompressVoucher(this, imageUri, messUuid);
                    if (compressedFile != null) {
                        addReceiptThumbnail(compressedFile);
                    }
                }
            }
        }
    }

    private File compressBitmapToWebp(Bitmap bitmap, String messUuid) {
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
                Bitmap.CompressFormat webpFormat = Bitmap.CompressFormat.WEBP;
                bitmap.compress(webpFormat, 75, fos);
            }
            fos.flush();
            fos.close();
            return outputFile;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private void addReceiptThumbnail(File file) {
        compressedReceiptFiles.add(file);
        LayoutInflater inflater = LayoutInflater.from(this);
        View thumbView = inflater.inflate(R.layout.item_receipt_thumbnail, layoutReceiptsContainer, false);

        ImageView ivThumbnail = thumbView.findViewById(R.id.ivReceiptThumbnail);
        TextView tvSize = thumbView.findViewById(R.id.tvReceiptSize);
        View btnRemove = thumbView.findViewById(R.id.btnRemoveReceipt);

        long kb = file.length() / 1024;
        tvSize.setText("WebP " + kb + "K");

        Glide.with(this).load(file).centerCrop().into(ivThumbnail);

        btnRemove.setOnClickListener(v -> {
            compressedReceiptFiles.remove(file);
            layoutReceiptsContainer.removeView(thumbView);
            try {
                file.delete();
            } catch (Exception ignored) {}
        });

        layoutReceiptsContainer.addView(thumbView);
        Toast.makeText(this, "রসিদের ছবি যুক্ত ও অপ্টিমাইজ করা হয়েছে (" + kb + " KB)", Toast.LENGTH_SHORT).show();
    }

    // ==========================================
    // 5. COMMIT MULTI-SPLIT BAZAR TO LEDGER
    // ==========================================
    private void saveBazarSession() {
        double totalBill = calculateTotalItemCost();
        if (totalBill <= 0) {
            Toast.makeText(this, "দয়া করে অন্তত ১টি পণ্য ও তার সঠিক মূল্য লিখুন", Toast.LENGTH_LONG).show();
            return;
        }

        // Verify all items have names
        for (int i = 0; i < itemRows.size(); i++) {
            if (itemRows.get(i).getItemName().isEmpty()) {
                Toast.makeText(this, "দয়া করে পণ্য নং এর নাম লিখুন: " + (i + 1), Toast.LENGTH_SHORT).show();
                return;
            }
        }

        // Verify balance
        double totalPaid = 0.0;
        for (PayerRowHolder payer : payerRows) {
            totalPaid += payer.getAmount();
        }
        totalPaid = round(totalPaid);

        if (Math.abs(totalBill - totalPaid) >= 0.01) {
            Toast.makeText(this, "সংরক্ষণ করা যায়নি: মোট খরচের সাথে প্রদানকৃত টাকা মিলতে হবে ("
                    + CurrencyUtils.format(totalBill) + " vs " + CurrencyUtils.format(totalPaid) + ")", Toast.LENGTH_LONG).show();
            return;
        }

        String date = etBazarDate.getText().toString().trim();
        if (date.isEmpty()) date = dateFormat.format(new Date());

        String sessionTag = etBazarTitle.getText().toString().trim();

        // Build item highlights summary
        StringBuilder highlights = new StringBuilder();
        for (int i = 0; i < itemRows.size(); i++) {
            ItemRowHolder it = itemRows.get(i);
            if (i > 0) highlights.append(", ");
            highlights.append(it.getItemName());
            if (!it.getItemQty().isEmpty()) {
                highlights.append(" (").append(it.getItemQty()).append(")");
            }
        }

        String baseTitle = sessionTag.isEmpty() ? highlights.toString() : (sessionTag + ": " + highlights.toString());

        // Prepare receipt voucher path
        String voucherPath = null;
        if (!compressedReceiptFiles.isEmpty()) {
            voucherPath = compressedReceiptFiles.get(0).getAbsolutePath();
        }

        // Calculate Category Subtotals
        double rawSub = 0.0, sharedSub = 0.0, utilSub = 0.0;
        for (ItemRowHolder item : itemRows) {
            double p = item.getPrice();
            String cat = item.getCategoryKey();
            if (Expense.CAT_RAW_MEAL.equals(cat)) rawSub += p;
            else if (Expense.CAT_SHARED_FOOD.equals(cat)) sharedSub += p;
            else if (Expense.CAT_UTILITY_ASSET.equals(cat)) utilSub += p;
        }
        rawSub = round(rawSub);
        sharedSub = round(sharedSub);
        utilSub = round(utilSub);

        long messId = sessionManager.getMessId();
        String now = DateTimeUtils.getCurrentDateTime();

        // Multi-Payer Proportional Ledger Assignment
        for (PayerRowHolder payer : payerRows) {
            double payerPaid = payer.getAmount();
            if (payerPaid <= 0) continue;

            double ratio = payerPaid / totalBill;
            long buyerId = payer.getSelectedUserId();
            String payerName = payer.getSelectedUserName();

            // 1. Raw Meal Portion
            if (rawSub > 0) {
                double rawPortion = round(rawSub * ratio);
                if (rawPortion > 0) {
                    Expense exp = new Expense();
                    exp.setUuid(UUID.randomUUID().toString());
                    exp.setMessId(messId);
                    exp.setBuyerUserId(buyerId);
                    exp.setExpenseCategory(Expense.CAT_RAW_MEAL);
                    exp.setAmount(rawPortion);
                    exp.setExpenseDate(date);
                    exp.setTitle(baseTitle + " [" + payerName + " - Raw Meal]");
                    exp.setVoucherImageUrl(voucherPath);
                    exp.setSplitType(Expense.SPLIT_MEAL_DEPENDENT);
                    exp.setSyncStatus(0);
                    exp.setCreatedAt(now);
                    exp.setUpdatedAt(now);
                    expenseDao.insertOrUpdate(exp);
                }
            }

            // 2. Shared Food Portion
            if (sharedSub > 0) {
                double sharedPortion = round(sharedSub * ratio);
                if (sharedPortion > 0) {
                    Expense exp = new Expense();
                    exp.setUuid(UUID.randomUUID().toString());
                    exp.setMessId(messId);
                    exp.setBuyerUserId(buyerId);
                    exp.setExpenseCategory(Expense.CAT_SHARED_FOOD);
                    exp.setAmount(sharedPortion);
                    exp.setExpenseDate(date);
                    exp.setTitle(baseTitle + " [" + payerName + " - Shared Food]");
                    exp.setVoucherImageUrl(voucherPath);
                    exp.setSplitType(Expense.SPLIT_ALL_EQUAL);
                    exp.setSyncStatus(0);
                    exp.setCreatedAt(now);
                    exp.setUpdatedAt(now);
                    expenseDao.insertOrUpdate(exp);
                }
            }

            // 3. Utility Asset Portion
            if (utilSub > 0) {
                double utilPortion = round(utilSub * ratio);
                if (utilPortion > 0) {
                    Expense exp = new Expense();
                    exp.setUuid(UUID.randomUUID().toString());
                    exp.setMessId(messId);
                    exp.setBuyerUserId(buyerId);
                    exp.setExpenseCategory(Expense.CAT_UTILITY_ASSET);
                    exp.setAmount(utilPortion);
                    exp.setExpenseDate(date);
                    exp.setTitle(baseTitle + " [" + payerName + " - Utility/Asset]");
                    exp.setVoucherImageUrl(voucherPath);
                    exp.setSplitType(Expense.SPLIT_ALL_EQUAL);
                    exp.setSyncStatus(0);
                    exp.setCreatedAt(now);
                    exp.setUpdatedAt(now);
                    expenseDao.insertOrUpdate(exp);
                }
            }
        }

        // Post in-app notification to SQLite Notification Center
        com.smartmess.android.utils.NotificationCenterHelper.postNotification(
                getApplicationContext(),
                messId,
                "নতুন বাজার খরচ যুক্ত হয়েছে",
                "৳" + String.format(java.util.Locale.US, "%.2f", totalBill) + " বাজার খরচ এন্ট্রি (" + date + "), প্রদানকারী: " + payerRows.size() + " জন",
                com.smartmess.android.utils.NotificationCenterHelper.TYPE_EXPENSE
        );

        // Trigger background sync
        SyncManager.triggerSync(getApplicationContext());

        Toast.makeText(this, "বাজারের হিসাব ও সদস্যদের খরচ সফলভাবে সংরক্ষিত হয়েছে!", Toast.LENGTH_LONG).show();
        finish();
    }

    private static double round(double val) {
        return BigDecimal.valueOf(val).setScale(2, RoundingMode.HALF_UP).doubleValue();
    }

    private static abstract class SimpleTextWatcher implements TextWatcher {
        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
        @Override
        public void afterTextChanged(Editable s) {}
    }
}

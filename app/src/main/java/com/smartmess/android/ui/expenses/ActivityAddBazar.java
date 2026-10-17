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
import android.widget.RadioButton;
import android.widget.RadioGroup;
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
import com.smartmess.android.data.local.dao.BazarSessionDao;
import com.smartmess.android.data.local.dao.ExpenseDao;
import com.smartmess.android.data.local.dao.UserDao;
import com.smartmess.android.data.sync.SyncManager;
import com.smartmess.android.engine.VoucherManager;
import com.smartmess.android.model.BazarItem;
import com.smartmess.android.model.BazarPayer;
import com.smartmess.android.model.BazarSession;
import com.smartmess.android.model.Expense;
import com.smartmess.android.model.User;
import com.smartmess.android.utils.CurrencyUtils;
import com.smartmess.android.utils.DateTimeUtils;
import com.smartmess.android.utils.NotificationCenterHelper;
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

    public static final String EXTRA_BAZAR_UUID = "extra_bazar_uuid";
    public static final String EXTRA_EXPENSE_ID = "extra_expense_id";

    private static final int REQ_CAPTURE_CAMERA = 201;
    private static final int REQ_PICK_GALLERY = 202;

    // Header & Info
    private View btnBack;
    private TextView tvHeaderTitle;
    private EditText etBazarDate;
    private EditText etBazarTitle;

    // Meal Type RadioGroup
    private RadioGroup rgMealType;
    private RadioButton rbMealLunch;
    private RadioButton rbMealDinner;
    private RadioButton rbMealBreakfast;
    private RadioButton rbMealSpecial;
    private RadioButton rbMealMixed;

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
    private BazarSessionDao bazarSessionDao;
    private UserDao userDao;
    private SessionManager sessionManager;
    private List<User> members = new ArrayList<>();
    private final Calendar calendar = Calendar.getInstance();
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.US);

    // Dynamic Row Holders
    private final List<ItemRowHolder> itemRows = new ArrayList<>();
    private final List<PayerRowHolder> payerRows = new ArrayList<>();
    private final List<File> compressedReceiptFiles = new ArrayList<>();

    // Edit Mode State
    private String editBazarUuid = null;
    private long editExpenseId = -1;
    private boolean isEditMode = false;
    private String originalCreatedAt = null;

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
        bazarSessionDao = new BazarSessionDao(this);
        userDao = new UserDao(helper);

        editBazarUuid = getIntent().getStringExtra(EXTRA_BAZAR_UUID);
        editExpenseId = getIntent().getLongExtra(EXTRA_EXPENSE_ID, -1);

        initViews();
        loadMembers();
        setupDatePicker();

        if ((editBazarUuid != null && !editBazarUuid.trim().isEmpty()) || editExpenseId > 0) {
            isEditMode = true;
            btnSaveBazar.setText("বাজারের পরিবর্তন সংরক্ষণ করুন (Update)");
            loadBazarForEditing();
        } else {
            // Add initial rows for new creation
            addItemRow();
            addPayerRow(sessionManager.getUserId());
        }

        // Button listeners
        btnAddItemRow.setOnClickListener(v -> addItemRow());
        btnAddPayerRow.setOnClickListener(v -> addPayerRow(-1));

        btnPaidByMe.setOnClickListener(v -> setPaid100ByCurrentUser());

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

        rgMealType = findViewById(R.id.rgMealType);
        rbMealLunch = findViewById(R.id.rbMealLunch);
        rbMealDinner = findViewById(R.id.rbMealDinner);
        rbMealBreakfast = findViewById(R.id.rbMealBreakfast);
        rbMealSpecial = findViewById(R.id.rbMealSpecial);
        rbMealMixed = findViewById(R.id.rbMealMixed);

        tvTotalItemCost = findViewById(R.id.tvTotalItemCost);
        tvSubtotalRawMeal = findViewById(R.id.tvSubtotalRawMeal);
        tvSubtotalSharedFood = findViewById(R.id.tvSubtotalSharedFood);
        tvSubtotalUtility = findViewById(R.id.tvSubtotalUtility);
        layoutItemsContainer = findViewById(R.id.layoutItemsContainer);
        btnAddItemRow = findViewById(R.id.btnAddItemRow);

        tvTotalPaidAmount = findViewById(R.id.tvTotalPaidAmount);
        btnPaidByMe = findViewById(R.id.btnPaidByMe);
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

    private String getSelectedMealType() {
        if (rgMealType == null) return "lunch";
        int checkedId = rgMealType.getCheckedRadioButtonId();
        if (checkedId == R.id.rbMealDinner) return "dinner";
        if (checkedId == R.id.rbMealBreakfast) return "breakfast";
        if (checkedId == R.id.rbMealSpecial) return "special";
        if (checkedId == R.id.rbMealMixed) return "mixed";
        return "lunch";
    }

    private void setSelectedMealType(String mealType) {
        if (rgMealType == null || mealType == null) return;
        if ("dinner".equalsIgnoreCase(mealType)) {
            if (rbMealDinner != null) rbMealDinner.setChecked(true);
        } else if ("breakfast".equalsIgnoreCase(mealType)) {
            if (rbMealBreakfast != null) rbMealBreakfast.setChecked(true);
        } else if ("special".equalsIgnoreCase(mealType)) {
            if (rbMealSpecial != null) rbMealSpecial.setChecked(true);
        } else if ("mixed".equalsIgnoreCase(mealType)) {
            if (rbMealMixed != null) rbMealMixed.setChecked(true);
        } else {
            if (rbMealLunch != null) rbMealLunch.setChecked(true);
        }
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

    private void loadBazarForEditing() {
        BazarSession session = null;
        if (editBazarUuid != null && !editBazarUuid.trim().isEmpty()) {
            session = bazarSessionDao.getByUuid(editBazarUuid);
        }

        if (session == null && editExpenseId > 0) {
            Expense exp = expenseDao.getById(editExpenseId);
            if (exp != null) {
                if (exp.getBazarSessionUuid() != null && !exp.getBazarSessionUuid().trim().isEmpty()) {
                    editBazarUuid = exp.getBazarSessionUuid();
                    session = bazarSessionDao.getByUuid(editBazarUuid);
                }
                if (session == null) {
                    session = new BazarSession();
                    session.setUuid(exp.getUuid());
                    session.setDate(exp.getExpenseDate());
                    session.setTitle(exp.getTitle());
                    session.setVoucherImageUrl(exp.getVoucherImageUrl());
                    originalCreatedAt = exp.getCreatedAt();

                    List<BazarItem> items = new ArrayList<>();
                    items.add(new BazarItem(exp.getTitle(), exp.getExpenseCategory(), "1 pkg", exp.getAmount()));
                    session.setItems(items);

                    List<BazarPayer> payers = new ArrayList<>();
                    payers.add(new BazarPayer(exp.getBuyerUserId(), exp.getBuyerName() != null ? exp.getBuyerName() : "Buyer", exp.getAmount()));
                    session.setPayers(payers);
                }
            }
        }

        if (session != null) {
            editBazarUuid = session.getUuid();
            originalCreatedAt = session.getCreatedAt();
            if (session.getDate() != null) etBazarDate.setText(session.getDate());
            if (session.getTitle() != null) etBazarTitle.setText(session.getTitle());
            setSelectedMealType(session.getMealType());

            layoutItemsContainer.removeAllViews();
            itemRows.clear();
            if (session.getItems() != null && !session.getItems().isEmpty()) {
                for (BazarItem it : session.getItems()) {
                    addItemRowWithData(it);
                }
            } else {
                addItemRow();
            }

            layoutPayersContainer.removeAllViews();
            payerRows.clear();
            if (session.getPayers() != null && !session.getPayers().isEmpty()) {
                for (BazarPayer p : session.getPayers()) {
                    addPayerRowWithData(p.getUserId(), p.getAmountPaid());
                }
            } else {
                addPayerRow(sessionManager.getUserId());
            }

            if (session.getVoucherImageUrl() != null && !session.getVoucherImageUrl().trim().isEmpty()) {
                File f = new File(session.getVoucherImageUrl());
                if (f.exists()) {
                    addReceiptThumbnail(f);
                }
            }
        } else {
            addItemRow();
            addPayerRow(sessionManager.getUserId());
        }
        recalculateTotals();
    }

    // ==========================================
    // 1. DYNAMIC ITEM REPEATER ENGINE
    // ==========================================
    private void addItemRow() {
        addItemRowWithData(null);
    }

    private void addItemRowWithData(@Nullable BazarItem initialItem) {
        LayoutInflater inflater = LayoutInflater.from(this);
        View rowView = inflater.inflate(R.layout.item_bazar_repeater_row, layoutItemsContainer, false);

        ItemRowHolder holder = new ItemRowHolder(rowView);
        if (initialItem != null) {
            if (initialItem.getName() != null) holder.etItemName.setText(initialItem.getName());
            if (initialItem.getQuantity() != null) holder.etItemQty.setText(initialItem.getQuantity());
            holder.setPrice(initialItem.getPrice());
            holder.setCategoryByKey(initialItem.getCategory());
        }

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
                    Toast.makeText(ActivityAddBazar.this, "বাজার তালিকায় অন্তত ১টি পণ্য থাকা আবশ্যক", Toast.LENGTH_SHORT).show();
                }
            });

            etItemPrice.addTextChangedListener(new SimpleTextWatcher() {
                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                    recalculateTotals();
                }
            });

            spItemCategory.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                    recalculateTotals();
                }

                @Override
                public void onNothingSelected(AdapterView<?> parent) {}
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

        void setPrice(double price) {
            etItemPrice.setText(String.format(Locale.US, "%.2f", price));
        }

        String getCategoryKey() {
            int pos = spItemCategory.getSelectedItemPosition();
            if (pos >= 0 && pos < categoryKeys.length) {
                return categoryKeys[pos];
            }
            return Expense.CAT_RAW_MEAL;
        }

        void setCategoryByKey(String key) {
            for (int i = 0; i < categoryKeys.length; i++) {
                if (categoryKeys[i].equalsIgnoreCase(key)) {
                    spItemCategory.setSelection(i);
                    break;
                }
            }
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
        addPayerRowWithData(preselectUserId, 0.0);
    }

    private void addPayerRowWithData(long preselectUserId, double initialAmount) {
        LayoutInflater inflater = LayoutInflater.from(this);
        View rowView = inflater.inflate(R.layout.item_bazar_payer_row, layoutPayersContainer, false);

        PayerRowHolder holder = new PayerRowHolder(rowView, preselectUserId);
        if (initialAmount > 0) {
            holder.setAmount(initialAmount);
        }
        payerRows.add(holder);
        layoutPayersContainer.addView(rowView);

        recalculateTotals();
    }

    private class PayerRowHolder {
        View rootView;
        Spinner spPayerMember;
        EditText etPayerAmount;
        MaterialButton btnFillAmount;
        View btnDeletePayerRow;

        PayerRowHolder(View view, long preselectUserId) {
            rootView = view;
            spPayerMember = view.findViewById(R.id.spPayerMember);
            etPayerAmount = view.findViewById(R.id.etPayerAmount);
            btnFillAmount = view.findViewById(R.id.btnFillAmount);
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

            // Fill Button Click: Auto-assigns remaining or total bazaar amount to this member
            if (btnFillAmount != null) {
                btnFillAmount.setOnClickListener(v -> {
                    double totalBill = calculateTotalItemCost();
                    double otherPayersTotal = 0.0;
                    for (PayerRowHolder p : payerRows) {
                        if (p != PayerRowHolder.this) {
                            otherPayersTotal += p.getAmount();
                        }
                    }
                    double remaining = round(totalBill - otherPayersTotal);
                    if (remaining > 0) {
                        setAmount(remaining);
                    } else {
                        setAmount(totalBill);
                    }
                    recalculateTotals();
                    Toast.makeText(ActivityAddBazar.this, "সম্পূর্ণ অবশিষ্ট টাকা বরাদ্দ করা হয়েছে: " + CurrencyUtils.format(getAmount()), Toast.LENGTH_SHORT).show();
                });
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

        while (payerRows.size() > 1) {
            View viewToRemove = payerRows.get(payerRows.size() - 1).rootView;
            layoutPayersContainer.removeView(viewToRemove);
            payerRows.remove(payerRows.size() - 1);
        }

        if (payerRows.isEmpty()) {
            addPayerRow(currentUserId);
        }

        PayerRowHolder first = payerRows.get(0);
        for (int i = 0; i < members.size(); i++) {
            if (members.get(i).getId() == currentUserId) {
                first.spPayerMember.setSelection(i);
                break;
            }
        }
        first.setAmount(totalBill);
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
    // 5. COMMIT MULTI-SPLIT BAZAR TO LEDGER & BROADCAST
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

        // Check if backdated entry
        String today = dateFormat.format(new Date());
        boolean isBackdated = date.compareTo(today) < 0;

        // Build item highlights summary
        StringBuilder highlights = new StringBuilder();
        List<BazarItem> collectedItems = new ArrayList<>();
        for (int i = 0; i < itemRows.size(); i++) {
            ItemRowHolder it = itemRows.get(i);
            if (i > 0) highlights.append(", ");
            highlights.append(it.getItemName());
            if (!it.getItemQty().isEmpty()) {
                highlights.append(" (").append(it.getItemQty()).append(")");
            }
            collectedItems.add(new BazarItem(it.getItemName(), it.getCategoryKey(), it.getItemQty(), it.getPrice()));
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
        String sessionUuid = (isEditMode && editBazarUuid != null && !editBazarUuid.trim().isEmpty())
                ? editBazarUuid : UUID.randomUUID().toString();

        // 1. If in edit mode, remove previous expense rows linked to this session
        if (isEditMode) {
            expenseDao.deleteByBazarSessionUuid(messId, sessionUuid);
            if (editExpenseId > 0) {
                expenseDao.delete(editExpenseId);
            }
        }

        // 2. Persist BazarSession object into bazar_sessions table
        List<BazarPayer> collectedPayers = new ArrayList<>();
        BazarSession session = new BazarSession();
        session.setUuid(sessionUuid);
        session.setMessId(messId);
        session.setDate(date);
        session.setTitle(baseTitle);
        session.setMealType(getSelectedMealType());
        session.setVoucherImageUrl(voucherPath);
        session.setCreatedBy(sessionManager.getUserId());
        session.setIsBackdated(isBackdated ? 1 : 0);
        session.setItems(collectedItems);
        session.setCreatedAt(originalCreatedAt != null ? originalCreatedAt : now);
        session.setUpdatedAt(now);

        // 3. Multi-Payer Proportional Ledger Assignment
        for (PayerRowHolder payer : payerRows) {
            double payerPaid = payer.getAmount();
            if (payerPaid <= 0) continue;

            long buyerId = payer.getSelectedUserId();
            String payerName = payer.getSelectedUserName();
            collectedPayers.add(new BazarPayer(buyerId, payerName, payerPaid));

            double ratio = payerPaid / totalBill;

            // 1. Raw Meal Portion
            if (rawSub > 0) {
                double rawPortion = round(rawSub * ratio);
                if (rawPortion > 0) {
                    Expense exp = new Expense();
                    exp.setUuid(UUID.randomUUID().toString());
                    exp.setBazarSessionUuid(sessionUuid);
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
                    exp.setBazarSessionUuid(sessionUuid);
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
                    exp.setBazarSessionUuid(sessionUuid);
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

        session.setPayers(collectedPayers);
        bazarSessionDao.insertOrUpdate(session);

        // 4. Real-time Audit Push Notification Broadcast to all Mess Members
        String notificationTitle;
        String notificationBody;
        String currentUserName = sessionManager.getUserName() != null ? sessionManager.getUserName() : "মেস সদস্য";

        if (isEditMode) {
            notificationTitle = "✏️ বাজার খরচ সংশোধন করা হয়েছে";
            notificationBody = currentUserName + " " + date + " তারিখের বাজার তথ্য পরিবর্তন করেছেন (মোট: " + CurrencyUtils.format(totalBill) + ", আইটেম: " + collectedItems.size() + " টি)।";
        } else if (isBackdated) {
            notificationTitle = "📅 পূর্ববর্তী (ব্যাকডেটেড) বাজার যুক্ত হয়েছে";
            notificationBody = currentUserName + " পূর্ববর্তী তারিখের (" + date + ") " + CurrencyUtils.format(totalBill) + " টাকার বাজার এন্ট্রি করেছেন।";
        } else {
            notificationTitle = "🛒 নতুন বাজার খরচ যুক্ত হয়েছে";
            notificationBody = "৳" + String.format(Locale.US, "%.2f", totalBill) + " বাজার খরচ এন্ট্রি (" + date + ", " + getSelectedMealType() + "), প্রদানকারী: " + payerRows.size() + " জন";
        }

        NotificationCenterHelper.postNotification(
                getApplicationContext(),
                messId,
                notificationTitle,
                notificationBody,
                NotificationCenterHelper.TYPE_EXPENSE
        );

        // 5. Trigger background sync
        SyncManager.triggerSync(getApplicationContext());

        String successMsg = isEditMode
                ? "বাজারের বিবরণ ও সদস্যদের খরচ সফলভাবে আপডেট হয়েছে!"
                : "বাজারের হিসাব ও সদস্যদের খরচ সফলভাবে সংরক্ষিত হয়েছে!";
        Toast.makeText(this, successMsg, Toast.LENGTH_LONG).show();
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

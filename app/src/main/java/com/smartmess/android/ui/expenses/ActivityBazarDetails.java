package com.smartmess.android.ui.expenses;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;
import com.smartmess.android.R;
import com.smartmess.android.data.local.DatabaseHelper;
import com.smartmess.android.data.local.dao.BazarSessionDao;
import com.smartmess.android.data.local.dao.ExpenseDao;
import com.smartmess.android.data.local.dao.ItemizedConsumptionDao;
import com.smartmess.android.data.local.dao.MealDao;
import com.smartmess.android.data.local.dao.UserDao;
import com.smartmess.android.model.BazarItem;
import com.smartmess.android.model.BazarPayer;
import com.smartmess.android.model.BazarSession;
import com.smartmess.android.model.Expense;
import com.smartmess.android.model.ItemizedConsumptionLog;
import com.smartmess.android.model.Meal;
import com.smartmess.android.model.User;
import com.smartmess.android.utils.CurrencyUtils;
import com.smartmess.android.utils.DateTimeUtils;
import com.smartmess.android.utils.ImageLoaderHelper;
import com.smartmess.android.utils.SessionManager;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class ActivityBazarDetails extends AppCompatActivity {

    public static final String EXTRA_BAZAR_UUID = "extra_bazar_uuid";
    public static final String EXTRA_EXPENSE_ID = "extra_expense_id";

    private ImageButton btnBack;
    private MaterialButton btnEditBazarTop;
    private ExtendedFloatingActionButton fabEditBazar;

    private TextView tvHeaderTitle;
    private TextView tvBazarDate;
    private TextView tvBackdatedBadge;
    private TextView tvMealTypeBadge;
    private TextView tvBazarTitle;
    private TextView tvTotalCost;
    private TextView tvTotalPaid;

    private TextView tvItemCount;
    private LinearLayout layoutItemsList;
    private TextView tvRawSubtotal;
    private TextView tvSharedSubtotal;
    private TextView tvUtilitySubtotal;

    private LinearLayout layoutPayersList;

    private TextView tvActiveDinersCount;
    private TextView tvDinerSplitFormula;
    private LinearLayout layoutDinersList;

    private View cardVoucher;
    private ImageView ivVoucherPreview;

    private BazarSessionDao bazarSessionDao;
    private ExpenseDao expenseDao;
    private MealDao mealDao;
    private ItemizedConsumptionDao consumptionDao;
    private UserDao userDao;
    private SessionManager sessionManager;

    private String bazarUuid;
    private long expenseId = -1;
    private BazarSession currentSession;
    private Expense currentExpense;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bazar_details);

        DatabaseHelper helper = DatabaseHelper.getInstance(this);
        bazarSessionDao = new BazarSessionDao(this);
        expenseDao = new ExpenseDao(helper);
        mealDao = new MealDao(helper);
        consumptionDao = new ItemizedConsumptionDao(this);
        userDao = new UserDao(helper);
        sessionManager = new SessionManager(this);

        bazarUuid = getIntent().getStringExtra(EXTRA_BAZAR_UUID);
        expenseId = getIntent().getLongExtra(EXTRA_EXPENSE_ID, -1);

        initViews();
        loadBazarDetails();
    }

    private void initViews() {
        btnBack = findViewById(R.id.btnBack);
        btnEditBazarTop = findViewById(R.id.btnEditBazarTop);
        fabEditBazar = findViewById(R.id.fabEditBazar);

        tvHeaderTitle = findViewById(R.id.tvHeaderTitle);
        tvBazarDate = findViewById(R.id.tvBazarDate);
        tvBackdatedBadge = findViewById(R.id.tvBackdatedBadge);
        tvMealTypeBadge = findViewById(R.id.tvMealTypeBadge);
        tvBazarTitle = findViewById(R.id.tvBazarTitle);
        tvTotalCost = findViewById(R.id.tvTotalCost);
        tvTotalPaid = findViewById(R.id.tvTotalPaid);

        tvItemCount = findViewById(R.id.tvItemCount);
        layoutItemsList = findViewById(R.id.layoutItemsList);
        tvRawSubtotal = findViewById(R.id.tvRawSubtotal);
        tvSharedSubtotal = findViewById(R.id.tvSharedSubtotal);
        tvUtilitySubtotal = findViewById(R.id.tvUtilitySubtotal);

        layoutPayersList = findViewById(R.id.layoutPayersList);

        tvActiveDinersCount = findViewById(R.id.tvActiveDinersCount);
        tvDinerSplitFormula = findViewById(R.id.tvDinerSplitFormula);
        layoutDinersList = findViewById(R.id.layoutDinersList);

        cardVoucher = findViewById(R.id.cardVoucher);
        ivVoucherPreview = findViewById(R.id.ivVoucherPreview);

        btnBack.setOnClickListener(v -> finish());
        btnEditBazarTop.setOnClickListener(v -> openEditBazarScreen());
        fabEditBazar.setOnClickListener(v -> openEditBazarScreen());
    }

    private void loadBazarDetails() {
        if (bazarUuid != null && !bazarUuid.trim().isEmpty()) {
            currentSession = bazarSessionDao.getByUuid(bazarUuid);
        }

        if (currentSession == null && expenseId > 0) {
            currentExpense = expenseDao.getById(expenseId);
            if (currentExpense != null) {
                if (currentExpense.getBazarSessionUuid() != null && !currentExpense.getBazarSessionUuid().trim().isEmpty()) {
                    bazarUuid = currentExpense.getBazarSessionUuid();
                    currentSession = bazarSessionDao.getByUuid(bazarUuid);
                }
            }
        }

        if (currentSession == null && currentExpense != null) {
            // Synthesize session from expense record
            currentSession = new BazarSession();
            currentSession.setUuid(currentExpense.getUuid());
            currentSession.setMessId(currentExpense.getMessId());
            currentSession.setDate(currentExpense.getExpenseDate());
            currentSession.setTitle(currentExpense.getTitle());
            currentSession.setVoucherImageUrl(currentExpense.getVoucherImageUrl());

            BazarItem item = new BazarItem(currentExpense.getTitle(), currentExpense.getExpenseCategory(), "1 pkg", currentExpense.getAmount());
            List<BazarItem> items = new ArrayList<>();
            items.add(item);
            currentSession.setItems(items);

            BazarPayer payer = new BazarPayer(currentExpense.getBuyerUserId(), currentExpense.getBuyerName() != null ? currentExpense.getBuyerName() : "Buyer", currentExpense.getAmount());
            List<BazarPayer> payers = new ArrayList<>();
            payers.add(payer);
            currentSession.setPayers(payers);
            bazarUuid = currentExpense.getUuid();
        }

        if (currentSession == null) {
            Toast.makeText(this, "বাজারের তথ্য পাওয়া যায়নি", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        renderSessionUI();
    }

    private void renderSessionUI() {
        tvHeaderTitle.setText(currentSession.getTitle() != null && !currentSession.getTitle().trim().isEmpty() ? currentSession.getTitle() : "বাজারের বিবরণ");
        tvBazarTitle.setText(currentSession.getTitle() != null && !currentSession.getTitle().trim().isEmpty() ? currentSession.getTitle() : "বাজার অধিবেশন");
        tvBazarDate.setText(DateTimeUtils.formatDisplayDate(currentSession.getDate()));

        // Backdated check
        String today = DateTimeUtils.nowIso().substring(0, 10);
        boolean isBackdated = currentSession.isBackdated() || (currentSession.getDate() != null && currentSession.getDate().compareTo(today) < 0);
        tvBackdatedBadge.setVisibility(isBackdated ? View.VISIBLE : View.GONE);

        // Meal Type
        String mType = currentSession.getMealType();
        if (mType == null || mType.isEmpty()) mType = "lunch";
        tvMealTypeBadge.setText(getMealTypeBangla(mType));

        double totalItemCost = currentSession.getTotalItemCost();
        double totalPaid = currentSession.getTotalPaid();
        tvTotalCost.setText(CurrencyUtils.format(totalItemCost));
        tvTotalPaid.setText(CurrencyUtils.format(totalPaid));

        // 1. Render Items
        layoutItemsList.removeAllViews();
        List<BazarItem> items = currentSession.getItems();
        tvItemCount.setText((items != null ? items.size() : 0) + " টি পণ্য");

        LayoutInflater inflater = LayoutInflater.from(this);
        if (items != null && !items.isEmpty()) {
            for (BazarItem item : items) {
                View row = inflater.inflate(R.layout.item_bazar_details_item_row, layoutItemsList, false);
                TextView tvName = row.findViewById(R.id.tvItemName);
                TextView tvCat = row.findViewById(R.id.tvItemCategory);
                TextView tvQty = row.findViewById(R.id.tvItemQty);
                TextView tvPrice = row.findViewById(R.id.tvItemPrice);

                tvName.setText(item.getName());
                tvCat.setText(item.getCategoryLabel());
                tvQty.setText(item.getQuantity() != null && !item.getQuantity().isEmpty() ? item.getQuantity() : "1 unit");
                tvPrice.setText(CurrencyUtils.format(item.getPrice()));

                if (BazarItem.CAT_RAW_MEAL.equals(item.getCategory())) {
                    tvCat.setTextColor(ContextCompat.getColor(this, R.color.credit_green));
                } else if (BazarItem.CAT_SHARED_FOOD.equals(item.getCategory())) {
                    tvCat.setTextColor(ContextCompat.getColor(this, R.color.accent));
                } else {
                    tvCat.setTextColor(ContextCompat.getColor(this, R.color.warning_amber));
                }
                layoutItemsList.addView(row);
            }
        }

        tvRawSubtotal.setText("কাঁচা মিল: " + CurrencyUtils.format(currentSession.getCategoryTotal(BazarItem.CAT_RAW_MEAL)));
        tvSharedSubtotal.setText("মশলা/তেল: " + CurrencyUtils.format(currentSession.getCategoryTotal(BazarItem.CAT_SHARED_FOOD)));
        tvUtilitySubtotal.setText("ইউটিলিটি: " + CurrencyUtils.format(currentSession.getCategoryTotal(BazarItem.CAT_UTILITY_ASSET)));

        // 2. Render Payers
        layoutPayersList.removeAllViews();
        List<BazarPayer> payers = currentSession.getPayers();
        if (payers != null && !payers.isEmpty()) {
            for (BazarPayer payer : payers) {
                View row = inflater.inflate(R.layout.item_bazar_details_payer_row, layoutPayersList, false);
                TextView tvPayerName = row.findViewById(R.id.tvPayerName);
                TextView tvPayerShare = row.findViewById(R.id.tvPayerShare);
                TextView tvPayerAmount = row.findViewById(R.id.tvPayerAmount);

                String name = payer.getUserName();
                if (name == null || name.isEmpty()) {
                    User u = userDao.getById(payer.getUserId());
                    name = u != null ? u.getName() : "Member #" + payer.getUserId();
                }
                tvPayerName.setText(name);

                double sharePercent = totalPaid > 0 ? (payer.getAmountPaid() / totalPaid) * 100.0 : 0.0;
                tvPayerShare.setText(String.format(Locale.US, "%.1f%% of total", sharePercent));
                tvPayerAmount.setText(CurrencyUtils.format(payer.getAmountPaid()));

                layoutPayersList.addView(row);
            }
        }

        // 3. Render Diners & Itemized Active Diner Cost Splitting
        renderLinkedMealsAndDiners(inflater);

        // 4. Voucher Preview
        String voucherUrl = currentSession.getVoucherImageUrl();
        if (voucherUrl != null && !voucherUrl.trim().isEmpty()) {
            cardVoucher.setVisibility(View.VISIBLE);
            ImageLoaderHelper.loadVoucher(this, voucherUrl, ivVoucherPreview);
            ivVoucherPreview.setOnClickListener(v -> ImageLoaderHelper.showVoucherDialog(this, null, voucherUrl));
        } else {
            cardVoucher.setVisibility(View.GONE);
        }
    }

    private void renderLinkedMealsAndDiners(LayoutInflater inflater) {
        layoutDinersList.removeAllViews();
        long messId = currentSession.getMessId();
        String sessionUuid = currentSession.getUuid();

        List<ItemizedConsumptionLog> logs = consumptionDao.getByBazarSessionUuid(sessionUuid);
        if (logs != null && !logs.isEmpty()) {
            int activeCount = 0;
            for (ItemizedConsumptionLog log : logs) {
                if (log.getIsConsumed() == 1) activeCount++;
                View row = inflater.inflate(R.layout.item_bazar_details_diner_row, layoutDinersList, false);
                TextView tvName = row.findViewById(R.id.tvDinerName);
                TextView tvMealStatus = row.findViewById(R.id.tvMealStatus);
                TextView tvCostShare = row.findViewById(R.id.tvCostShare);

                tvName.setText(log.getUserName() != null ? log.getUserName() : "Member #" + log.getUserId());

                if (log.getIsConsumed() == 1) {
                    tvMealStatus.setText("✓ মিল খেয়েছেন (" + getMealTypeBangla(log.getMealType()) + ")");
                    tvMealStatus.setTextColor(ContextCompat.getColor(this, R.color.credit_green));
                    tvCostShare.setText(CurrencyUtils.format(log.getCostShare()));
                    tvCostShare.setTextColor(ContextCompat.getColor(this, R.color.coral_primary));
                } else {
                    tvMealStatus.setText("✗ মিল নেননি (Non-Diner)");
                    tvMealStatus.setTextColor(ContextCompat.getColor(this, R.color.text_secondary));
                    tvCostShare.setText("৳ 0.00");
                    tvCostShare.setTextColor(ContextCompat.getColor(this, R.color.text_secondary));
                }
                layoutDinersList.addView(row);
            }
            tvActiveDinersCount.setText(activeCount + " জন মেম্বার খেয়েছেন");
            tvDinerSplitFormula.setText("মোট বাজার খরচ সমানভাবে " + activeCount + " জন মিল গ্রহণকারীর মধ্যে বণ্টন হয়েছে (জনপ্রতি " + (activeCount > 0 ? CurrencyUtils.format(currentSession.getTotalItemCost() / activeCount) : "৳০") + ")");
            return;
        }

        // Fallback: check linked meals from meals table
        List<Meal> linkedMeals = mealDao.getMealsByBazarSessionUuid(messId, sessionUuid);
        if (linkedMeals == null || linkedMeals.isEmpty()) {
            linkedMeals = mealDao.getMealsForDate(messId, currentSession.getDate());
        }

        if (linkedMeals != null && !linkedMeals.isEmpty()) {
            List<Meal> activeMeals = new ArrayList<>();
            for (Meal m : linkedMeals) {
                if (m.getLunchCount() > 0 || m.getDinnerCount() > 0 || m.getBreakfastCount() > 0) {
                    activeMeals.add(m);
                }
            }

            int activeCount = activeMeals.size();
            double perDinerShare = activeCount > 0 ? (currentSession.getTotalItemCost() / activeCount) : 0.0;

            for (Meal m : linkedMeals) {
                boolean ate = (m.getLunchCount() > 0 || m.getDinnerCount() > 0 || m.getBreakfastCount() > 0);
                View row = inflater.inflate(R.layout.item_bazar_details_diner_row, layoutDinersList, false);
                TextView tvName = row.findViewById(R.id.tvDinerName);
                TextView tvMealStatus = row.findViewById(R.id.tvMealStatus);
                TextView tvCostShare = row.findViewById(R.id.tvCostShare);

                tvName.setText(m.getUserName() != null ? m.getUserName() : "Member #" + m.getUserId());

                if (ate) {
                    tvMealStatus.setText("✓ মিল খেয়েছেন (" + getMealTypeBangla(currentSession.getMealType()) + ")");
                    tvMealStatus.setTextColor(ContextCompat.getColor(this, R.color.credit_green));
                    tvCostShare.setText(CurrencyUtils.format(perDinerShare));
                    tvCostShare.setTextColor(ContextCompat.getColor(this, R.color.coral_primary));
                } else {
                    tvMealStatus.setText("✗ মিল নেননি (Non-Diner)");
                    tvMealStatus.setTextColor(ContextCompat.getColor(this, R.color.text_secondary));
                    tvCostShare.setText("৳ 0.00");
                    tvCostShare.setTextColor(ContextCompat.getColor(this, R.color.text_secondary));
                }
                layoutDinersList.addView(row);
            }

            tvActiveDinersCount.setText(activeCount + " জন খেয়েছেন");
            tvDinerSplitFormula.setText(String.format(Locale.US, "মোট খরচ %s ÷ %d জন সক্রিয় সদস্য = জনপ্রতি %s",
                    CurrencyUtils.format(currentSession.getTotalItemCost()), activeCount, CurrencyUtils.format(perDinerShare)));
        } else {
            tvActiveDinersCount.setText("কোনো মিলের লিংক নেই");
            tvDinerSplitFormula.setText("এই বাজারের সাথে এখনো মিল এন্ট্রি সরাসরি যুক্ত করা হয়নি");
        }
    }

    private void openEditBazarScreen() {
        Intent intent = new Intent(this, ActivityAddBazar.class);
        if (currentSession != null) {
            intent.putExtra(ActivityAddBazar.EXTRA_BAZAR_UUID, currentSession.getUuid());
        }
        if (expenseId > 0) {
            intent.putExtra(ActivityAddBazar.EXTRA_EXPENSE_ID, expenseId);
        }
        startActivity(intent);
    }

    private String getMealTypeBangla(String type) {
        if ("lunch".equalsIgnoreCase(type)) return "দুপুর (Lunch)";
        if ("dinner".equalsIgnoreCase(type)) return "রাত (Dinner)";
        if ("breakfast".equalsIgnoreCase(type)) return "সকাল (Breakfast)";
        if ("special".equalsIgnoreCase(type)) return "বিশেষ (Special)";
        return "মিক্সড (Mixed)";
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadBazarDetails();
    }
}

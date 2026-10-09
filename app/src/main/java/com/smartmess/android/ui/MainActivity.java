package com.smartmess.android.ui;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.Chip;
import com.google.android.material.switchmaterial.SwitchMaterial;
import com.smartmess.android.R;
import com.smartmess.android.data.local.DatabaseHelper;
import com.smartmess.android.data.local.dao.MealDao;
import com.smartmess.android.data.sync.SyncManager;
import com.smartmess.android.engine.AccountingEngine;
import com.smartmess.android.engine.CalculationModels.MemberBalanceSheet;
import com.smartmess.android.model.Meal;
import com.smartmess.android.ui.dashboard.FragmentDashboardOverview;
import com.smartmess.android.ui.deposits.AddDepositActivity;
import com.smartmess.android.ui.expenses.ExpenseListActivity;
import com.smartmess.android.ui.meals.FragmentMealSheet;
import com.smartmess.android.ui.members.MemberListActivity;
import com.smartmess.android.utils.CurrencyUtils;
import com.smartmess.android.utils.DateTimeUtils;
import com.smartmess.android.utils.SessionManager;

import java.util.UUID;

public class MainActivity extends AppCompatActivity {

    public static final String EXTRA_OPEN_TAB = "extra_open_tab";

    private TextView tvMessTitle;
    private TextView tvUserRoleBadge;
    private Chip chipSyncIndicator;
    private TextView tvPersonalBalance;
    private TextView tvBalanceBadge;
    private MaterialButton btnPayAdvance;
    private TextView tvMealQuickStatus;
    private SwitchMaterial switchQuickDinner;
    private BottomNavigationView bottomNavigation;

    private SessionManager sessionManager;
    private AccountingEngine accountingEngine;
    private MealDao mealDao;
    private SyncManager syncManager;
    private com.smartmess.android.engine.PlanGateManager planGateManager;

    private final android.content.BroadcastReceiver capabilitiesReceiver = new android.content.BroadcastReceiver() {
        @Override
        public void onReceive(android.content.Context context, Intent intent) {
            if (SessionManager.ACTION_CAPABILITIES_UPDATED.equals(intent.getAction())) {
                loadHeaderData();
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        sessionManager = new SessionManager(this);
        accountingEngine = new AccountingEngine(this);
        DatabaseHelper helper = DatabaseHelper.getInstance(this);
        mealDao = new MealDao(helper);
        syncManager = SyncManager.getInstance(this);
        planGateManager = new com.smartmess.android.engine.PlanGateManager(this);

        initViews();
        setupBottomNav();
        setupQuickMealToggle();
        setupSyncIndicator();

        // Check remote kill-switch, maintenance mode, and force updates
        try {
            com.smartmess.android.engine.RemoteConfigManager.checkRemoteConfig(this, null);
        } catch (Throwable ignored) {}

        // Schedule daily 22:00 notification alarm
        try {
            com.smartmess.android.notification.NotificationScheduler.scheduleDailyReminder(this, null);
        } catch (Throwable ignored) {}

        // Check if opened from Daily Meal Status Reminder notification
        handleIncomingIntent(getIntent(), savedInstanceState);
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        handleIncomingIntent(intent, null);
    }

    private void handleIncomingIntent(Intent intent, Bundle savedInstanceState) {
        if (intent != null && "meals".equals(intent.getStringExtra(EXTRA_OPEN_TAB))) {
            bottomNavigation.setSelectedItemId(R.id.nav_meals);
            loadFragment(new FragmentMealSheet());
        } else if (savedInstanceState == null) {
            loadFragment(new FragmentDashboardOverview());
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        try {
            android.content.IntentFilter filter = new android.content.IntentFilter(SessionManager.ACTION_CAPABILITIES_UPDATED);
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                registerReceiver(capabilitiesReceiver, filter, android.content.Context.RECEIVER_NOT_EXPORTED);
            } else {
                registerReceiver(capabilitiesReceiver, filter);
            }
        } catch (Throwable ignored) {}
    }

    @Override
    protected void onStop() {
        super.onStop();
        try {
            unregisterReceiver(capabilitiesReceiver);
        } catch (Throwable ignored) {}
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadHeaderData();
    }

    private void initViews() {
        tvMessTitle = findViewById(R.id.tvMessTitle);
        tvUserRoleBadge = findViewById(R.id.tvUserRoleBadge);
        chipSyncIndicator = findViewById(R.id.chipSyncIndicator);
        tvPersonalBalance = findViewById(R.id.tvPersonalBalance);
        tvBalanceBadge = findViewById(R.id.tvBalanceBadge);
        btnPayAdvance = findViewById(R.id.btnPayAdvance);
        tvMealQuickStatus = findViewById(R.id.tvMealQuickStatus);
        switchQuickDinner = findViewById(R.id.switchQuickDinner);
        bottomNavigation = findViewById(R.id.bottom_navigation);

        btnPayAdvance.setOnClickListener(v -> {
            startActivity(new Intent(MainActivity.this, AddDepositActivity.class));
        });

        tvUserRoleBadge.setOnClickListener(v -> {
            if (!planGateManager.isPro()) {
                planGateManager.showUpgradeBottomSheet(getSupportFragmentManager(), "SmartMess Pro",
                        "Upgrade to unlock unlimited members, real-time cloud sync, and branded exports.");
            }
        });
    }

    private void loadHeaderData() {
        try {
            String messName = sessionManager.getMessName();
            if (messName == null || messName.isEmpty()) messName = "SmartMess";
            tvMessTitle.setText(messName);

            String role = sessionManager.isManager() ? "Manager" : "Member";
            boolean isPro = planGateManager.isPro();

            if (isPro) {
                tvUserRoleBadge.setText("PRO • " + role + " • " + (sessionManager.getUserPhone() != null ? sessionManager.getUserPhone() : ""));
                tvUserRoleBadge.setTextColor(androidx.core.content.ContextCompat.getColor(this, R.color.primary_light));
            } else {
                tvUserRoleBadge.setText("FREE • " + role + " • Tap to Upgrade Pro ★");
                tvUserRoleBadge.setTextColor(androidx.core.content.ContextCompat.getColor(this, R.color.warning_amber));
            }

            // Gating Cloud Sync Button in Header: Hidden on Free (Background only), Visible on Pro for Manager
            boolean showSyncButton = planGateManager.canUseCloudSync();
            chipSyncIndicator.setVisibility(showSyncButton ? View.VISIBLE : View.GONE);

            // Calculate Personal Ledger Balance
            long messId = sessionManager.getMessId();
            long userId = sessionManager.getUserId();
            MemberBalanceSheet sheet = accountingEngine.calculateMemberBalance(messId, userId);

            double balance = sheet != null ? sheet.getNetBalance() : 0.0;
            tvPersonalBalance.setText(CurrencyUtils.format(balance));

            if (sheet != null && sheet.isOverdue()) {
                tvPersonalBalance.setTextColor(androidx.core.content.ContextCompat.getColor(this, R.color.due_red));
                tvBalanceBadge.setText("Payment Due");
                tvBalanceBadge.setBackgroundResource(R.drawable.badge_due);
                tvBalanceBadge.setTextColor(androidx.core.content.ContextCompat.getColor(this, R.color.due_red));
                btnPayAdvance.setVisibility(View.VISIBLE);
            } else {
                tvPersonalBalance.setTextColor(androidx.core.content.ContextCompat.getColor(this, R.color.credit_green));
                tvBalanceBadge.setText("Credit Advance");
                tvBalanceBadge.setBackgroundResource(R.drawable.badge_credit);
                tvBalanceBadge.setTextColor(androidx.core.content.ContextCompat.getColor(this, R.color.credit_green));
                btnPayAdvance.setVisibility(View.GONE);
            }

            // Check tonight's meal state for today
            String today = DateTimeUtils.getCurrentDate();
            Meal todayMeal = mealDao.getUserMealForDate(messId, userId, today);
            boolean isDinnerOn = (todayMeal != null && todayMeal.getDinnerCount() > 0);
            boolean isLunchOn = (todayMeal != null && todayMeal.getLunchCount() > 0);

            switchQuickDinner.setChecked(isDinnerOn);
            tvMealQuickStatus.setText(String.format("Lunch: %s | Dinner: %s (Lock at 22:00)",
                    isLunchOn ? "ON" : "OFF",
                    isDinnerOn ? "ON" : "OFF"));
        } catch (Throwable t) {
            android.util.Log.e("MainActivity", "Error in loadHeaderData: " + t.getMessage(), t);
        }
    }

    private void setupQuickMealToggle() {
        switchQuickDinner.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (!buttonView.isPressed()) return; // ignore programmatic changes

            long messId = sessionManager.getMessId();
            long userId = sessionManager.getUserId();
            String today = DateTimeUtils.getCurrentDate();

            Meal meal = mealDao.getUserMealForDate(messId, userId, today);
            if (meal == null) {
                meal = new Meal();
                meal.setUuid(UUID.randomUUID().toString());
                meal.setMessId(messId);
                meal.setUserId(userId);
                meal.setMealDate(today);
                meal.setBreakfastCount(0.0);
                meal.setLunchCount(1.0);
                meal.setDinnerCount(isChecked ? 1.0 : 0.0);
                meal.setGuestMealCount(0.0);
                meal.setIsLocked(0);
                meal.setSyncStatus(0);
                meal.setCreatedAt(DateTimeUtils.getCurrentDateTime());
                meal.setUpdatedAt(DateTimeUtils.getCurrentDateTime());
            } else {
                meal.setDinnerCount(isChecked ? 1.0 : 0.0);
                meal.setSyncStatus(0);
                meal.setUpdatedAt(DateTimeUtils.getCurrentDateTime());
            }

            mealDao.insertOrUpdate(meal);
            loadHeaderData();
            Toast.makeText(this, isChecked ? "Dinner Enabled for Tonight" : "Dinner Cancelled for Tonight", Toast.LENGTH_SHORT).show();

            // Background sync
            SyncManager.triggerSync(getApplicationContext());
        });
    }

    private void setupSyncIndicator() {
        chipSyncIndicator.setOnClickListener(v -> {
            if (!planGateManager.canUseCloudSync()) {
                planGateManager.showUpgradeBottomSheet(getSupportFragmentManager(), "Cloud Sync",
                        "Multi-device instant cloud sync is a Pro tier capability. Upgrade your mess to enable.");
                return;
            }

            chipSyncIndicator.setText("Syncing...");
            syncManager.triggerTwoWaySync(new SyncManager.SyncCallback() {
                @Override
                public void onSyncStarted() {
                    chipSyncIndicator.setText("Syncing...");
                }

                @Override
                public void onSyncSuccess(String message) {
                    chipSyncIndicator.setText("Cloud Synced");
                    Toast.makeText(MainActivity.this, message, Toast.LENGTH_SHORT).show();
                    loadHeaderData();
                }

                @Override
                public void onSyncFailed(String error) {
                    chipSyncIndicator.setText("Offline Mode");
                    Toast.makeText(MainActivity.this, error, Toast.LENGTH_SHORT).show();
                }
            });
        });
    }

    private void setupBottomNav() {
        bottomNavigation.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();
            if (itemId == R.id.nav_dashboard) {
                loadFragment(new FragmentDashboardOverview());
                return true;
            } else if (itemId == R.id.nav_meals) {
                loadFragment(new FragmentMealSheet());
                return true;
            } else if (itemId == R.id.nav_expenses) {
                startActivity(new Intent(MainActivity.this, ExpenseListActivity.class));
                return false;
            } else if (itemId == R.id.nav_members) {
                startActivity(new Intent(MainActivity.this, MemberListActivity.class));
                return false;
            }
            return false;
        });
    }

    private void loadFragment(Fragment fragment) {
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragment_container, fragment)
                .commit();
    }
}

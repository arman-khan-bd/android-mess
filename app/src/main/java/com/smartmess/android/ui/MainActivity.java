package com.smartmess.android.ui;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.Fragment;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.switchmaterial.SwitchMaterial;
import com.smartmess.android.R;
import com.smartmess.android.data.local.DatabaseHelper;
import com.smartmess.android.data.local.dao.MealDao;
import com.smartmess.android.data.local.dao.UserDao;
import com.smartmess.android.data.sync.SyncManager;
import com.smartmess.android.engine.AccountingEngine;
import com.smartmess.android.engine.CalculationModels.MemberBalanceSheet;
import com.smartmess.android.engine.PlanGateManager;
import com.smartmess.android.model.Meal;
import com.smartmess.android.model.User;
import com.smartmess.android.ui.auth.LoginActivity;
import com.smartmess.android.ui.dashboard.FragmentDashboardOverview;
import com.smartmess.android.ui.expenses.FragmentExpenseList;
import com.smartmess.android.ui.meals.FragmentMealSheet;
import com.smartmess.android.ui.members.FragmentMemberList;
import com.smartmess.android.ui.reports.SummaryReportActivity;
import com.smartmess.android.ui.settings.SettingsActivity;
import com.smartmess.android.ui.sms.ActivitySmsDispatch;
import com.smartmess.android.ui.support.ActivitySupportTickets;
import com.smartmess.android.ui.telemetry.ActivityReportIssue;
import com.smartmess.android.utils.CurrencyUtils;
import com.smartmess.android.utils.DateTimeUtils;
import com.smartmess.android.utils.SessionManager;

import java.util.List;
import java.util.UUID;

public class MainActivity extends AppCompatActivity {

    public static final String EXTRA_OPEN_TAB = "extra_open_tab";

    private DrawerLayout drawerLayout;
    private TextView tvTopMessName;
    private TextView tvTopPlanPill;
    private TextView chipBalanceBadge;
    private SwitchMaterial switchQuickDinner;
    private ImageButton btnCloudSync;
    private View btnNotificationCenter;
    private TextView tvNotificationBadge;
    private View btnOpenProfileDrawer;
    private TextView tvAvatarInitials;
    private BottomNavigationView bottomNavigation;

    // Right Drawer Views
    private TextView tvDrawerAvatarInitials;
    private TextView tvDrawerUserName;
    private TextView tvDrawerUserPhone;
    private TextView tvDrawerRoleBadge;
    private TextView tvDrawerPlanPill;
    private View drawerItemProfile;
    private View drawerItemPlans;
    private View drawerItemSms;
    private View drawerItemStatements;
    private View drawerItemVacation;
    private View drawerItemNotifications;
    private View drawerItemSupport;
    private View drawerItemBugReport;
    private View tvDrawerManagerHeader;
    private View drawerItemSettings;
    private View drawerItemNotifManager;
    private View drawerItemHandover;
    private View drawerItemSignOut;

    private SessionManager sessionManager;
    private AccountingEngine accountingEngine;
    private MealDao mealDao;
    private UserDao userDao;
    private SyncManager syncManager;
    private PlanGateManager planGateManager;

    private int currentSelectedNavId = R.id.nav_dashboard;

    private final android.content.BroadcastReceiver capabilitiesReceiver = new android.content.BroadcastReceiver() {
        @Override
        public void onReceive(android.content.Context context, Intent intent) {
            if (SessionManager.ACTION_CAPABILITIES_UPDATED.equals(intent.getAction())) {
                loadHeaderData();
                populateDrawerProfile();
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
        userDao = new UserDao(helper);
        syncManager = SyncManager.getInstance(this);
        planGateManager = new PlanGateManager(this);

        initViews();
        setupBottomNav();
        setupQuickMealToggle();
        setupSyncIndicator();
        setupDrawerActions();

        // Check remote kill-switch, maintenance mode, and force updates
        try {
            com.smartmess.android.engine.RemoteConfigManager.checkRemoteConfig(this, null);
        } catch (Throwable ignored) {}

        // Schedule daily 22:00 notification alarm
        try {
            com.smartmess.android.notification.NotificationScheduler.scheduleDailyReminder(this, null);
        } catch (Throwable ignored) {}

        // Auto-sync mess data on startup if network is available
        if (com.smartmess.android.utils.NetworkUtils.isNetworkAvailable(this) && sessionManager.isLoggedIn()) {
            syncManager.triggerTwoWaySync(new SyncManager.SyncCallback() {
                @Override
                public void onSyncStarted() {}

                @Override
                public void onSyncSuccess(String message) {
                    runOnUiThread(() -> {
                        loadHeaderData();
                        populateDrawerProfile();
                        updateNotificationBadge();
                    });
                }

                @Override
                public void onSyncFailed(String error) {}
            });
        }

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
        if (intent != null && "notifications".equals(intent.getStringExtra(com.smartmess.android.notification.PushNotificationManager.EXTRA_NAV_TARGET))) {
            if (bottomNavigation.getMenu().findItem(R.id.nav_notifications) != null) {
                bottomNavigation.setSelectedItemId(R.id.nav_notifications);
                currentSelectedNavId = R.id.nav_notifications;
            }
            loadFragment(new com.smartmess.android.ui.notifications.FragmentNotifications());
            long notifId = intent.getLongExtra(com.smartmess.android.notification.PushNotificationManager.EXTRA_NOTIFICATION_ID, 0);
            if (notifId > 0) {
                showFullNotificationById(notifId);
            }
        } else if (intent != null && "meals".equals(intent.getStringExtra(EXTRA_OPEN_TAB))) {
            int mealTabId = (bottomNavigation.getMenu().findItem(R.id.nav_cycle) != null) ? R.id.nav_cycle : R.id.nav_meals;
            if (bottomNavigation.getMenu().findItem(mealTabId) != null) {
                bottomNavigation.setSelectedItemId(mealTabId);
            }
            currentSelectedNavId = mealTabId;
            loadFragment(new FragmentMealSheet());
        } else if (savedInstanceState == null) {
            bottomNavigation.setSelectedItemId(R.id.nav_dashboard);
            currentSelectedNavId = R.id.nav_dashboard;
            loadFragment(new FragmentDashboardOverview());
        }
    }

    private void showFullNotificationById(long notifId) {
        try {
            com.smartmess.android.data.local.dao.AppNotificationDao dao =
                    new com.smartmess.android.data.local.dao.AppNotificationDao(com.smartmess.android.data.local.DatabaseHelper.getInstance(this));
            com.smartmess.android.model.AppNotification notif = dao.getById(notifId);
            if (notif != null) {
                com.smartmess.android.ui.notifications.DialogNotificationFullView.show(this, notif, this::updateNotificationBadge);
            }
        } catch (Throwable ignored) {}
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
        populateDrawerProfile();
        updateNotificationBadge();
        try {
            com.smartmess.android.utils.BatteryOptimizationHelper.promptOnceIfNeeded(this);
            com.smartmess.android.notification.PushNotificationSyncWorker.runImmediateSync(this);
        } catch (Throwable ignored) {}
    }

    private void initViews() {
        drawerLayout = findViewById(R.id.drawer_layout);
        tvTopMessName = findViewById(R.id.tvTopMessName);
        tvTopPlanPill = findViewById(R.id.tvTopPlanPill);
        chipBalanceBadge = findViewById(R.id.chipBalanceBadge);
        switchQuickDinner = findViewById(R.id.switchQuickDinner);
        btnCloudSync = findViewById(R.id.btnCloudSync);
        btnNotificationCenter = findViewById(R.id.btnNotificationCenter);
        tvNotificationBadge = findViewById(R.id.tvNotificationBadge);
        btnOpenProfileDrawer = findViewById(R.id.btnOpenProfileDrawer);
        tvAvatarInitials = findViewById(R.id.tvAvatarInitials);
        bottomNavigation = findViewById(R.id.bottom_navigation);

        // Right Drawer views
        tvDrawerAvatarInitials = findViewById(R.id.tvDrawerAvatarInitials);
        tvDrawerUserName = findViewById(R.id.tvDrawerUserName);
        tvDrawerUserPhone = findViewById(R.id.tvDrawerUserPhone);
        tvDrawerRoleBadge = findViewById(R.id.tvDrawerRoleBadge);
        tvDrawerPlanPill = findViewById(R.id.tvDrawerPlanPill);
        drawerItemProfile = findViewById(R.id.drawerItemProfile);
        drawerItemPlans = findViewById(R.id.drawerItemPlans);
        drawerItemSms = findViewById(R.id.drawerItemSms);
        drawerItemStatements = findViewById(R.id.drawerItemStatements);
        drawerItemVacation = findViewById(R.id.drawerItemVacation);
        drawerItemNotifications = findViewById(R.id.drawerItemNotifications);
        drawerItemSupport = findViewById(R.id.drawerItemSupport);
        drawerItemBugReport = findViewById(R.id.drawerItemBugReport);
        tvDrawerManagerHeader = findViewById(R.id.tvDrawerManagerHeader);
        drawerItemSettings = findViewById(R.id.drawerItemSettings);
        drawerItemNotifManager = findViewById(R.id.drawerItemNotifManager);
        drawerItemHandover = findViewById(R.id.drawerItemHandover);
        drawerItemSignOut = findViewById(R.id.drawerItemSignOut);

        View btnMenu = findViewById(R.id.btnMenu);
        if (btnMenu != null) {
            btnMenu.setOnClickListener(v -> {
                if (drawerLayout != null) {
                    if (drawerLayout.isDrawerOpen(GravityCompat.END)) {
                        drawerLayout.closeDrawer(GravityCompat.END);
                    } else {
                        drawerLayout.openDrawer(GravityCompat.END);
                    }
                }
            });
        }

        if (btnNotificationCenter != null) {
            btnNotificationCenter.setOnClickListener(v ->
                    startActivity(new Intent(MainActivity.this, com.smartmess.android.ui.notifications.ActivityNotificationCenter.class)));
        }

        btnOpenProfileDrawer.setOnClickListener(v -> {
            if (drawerLayout != null) {
                if (drawerLayout.isDrawerOpen(GravityCompat.END)) {
                    drawerLayout.closeDrawer(GravityCompat.END);
                } else {
                    drawerLayout.openDrawer(GravityCompat.END);
                }
            }
        });

        chipBalanceBadge.setOnClickListener(v -> {
            startActivity(new Intent(MainActivity.this, com.smartmess.android.ui.deposits.AddDepositActivity.class));
        });

        tvTopPlanPill.setOnClickListener(v -> {
            if (!planGateManager.isPro()) {
                planGateManager.showUpgradeBottomSheet(getSupportFragmentManager(), "SmartMess Pro",
                        "Upgrade to unlock unlimited active members, multi-device cloud sync, and branded financial exports.");
            }
        });
    }

    private void loadHeaderData() {
        try {
            String messName = sessionManager.getMessName();
            if (messName == null || messName.isEmpty()) messName = "SmartMess";
            tvTopMessName.setText(messName);

            boolean isPro = planGateManager.isPro();
            tvTopPlanPill.setText(isPro ? "PRO" : "FREE");
            if (isPro) {
                tvTopPlanPill.setBackgroundResource(R.drawable.badge_credit);
                tvTopPlanPill.setTextColor(androidx.core.content.ContextCompat.getColor(this, R.color.primary_dark));
            } else {
                tvTopPlanPill.setBackgroundResource(R.drawable.badge_due);
                tvTopPlanPill.setTextColor(androidx.core.content.ContextCompat.getColor(this, R.color.warning_amber));
            }

            // Cloud Sync Button in Header: Visible for all logged in mess members
            boolean showSyncButton = sessionManager.isLoggedIn();
            btnCloudSync.setVisibility(showSyncButton ? View.VISIBLE : View.GONE);

            // User name & role in top bar
            String userName = sessionManager.getUserName();
            if (userName == null || userName.isEmpty()) userName = "User";
            TextView tvTopUserName = findViewById(R.id.tvTopUserName);
            TextView tvTopUserRole = findViewById(R.id.tvTopUserRole);
            if (tvTopUserName != null) {
                tvTopUserName.setText(userName);
            }
            if (tvTopUserRole != null) {
                String role = sessionManager.getUserRole();
                if ("manager".equalsIgnoreCase(role)) {
                    tvTopUserRole.setText("Manager");
                } else if ("assistant".equalsIgnoreCase(role)) {
                    tvTopUserRole.setText("Assistant");
                } else {
                    tvTopUserRole.setText("Member");
                }
            }

            // Initials avatar
            String[] parts = userName.trim().split("\\s+");
            String initials = parts.length > 1
                    ? ("" + parts[0].charAt(0) + parts[1].charAt(0)).toUpperCase()
                    : ("" + parts[0].charAt(0)).toUpperCase();
            tvAvatarInitials.setText(initials);

            // Calculate Personal Ledger Balance
            long messId = sessionManager.getMessId();
            long userId = sessionManager.getUserId();
            MemberBalanceSheet sheet = accountingEngine.calculateMemberBalance(messId, userId);

            double balance = sheet != null ? sheet.getNetBalance() : 0.0;
            if (balance >= 0) {
                chipBalanceBadge.setText("+ " + CurrencyUtils.format(balance));
                chipBalanceBadge.setBackgroundResource(R.drawable.badge_credit);
                chipBalanceBadge.setTextColor(androidx.core.content.ContextCompat.getColor(this, R.color.credit_green));
            } else {
                chipBalanceBadge.setText("- " + CurrencyUtils.format(Math.abs(balance)));
                chipBalanceBadge.setBackgroundResource(R.drawable.badge_due);
                chipBalanceBadge.setTextColor(androidx.core.content.ContextCompat.getColor(this, R.color.due_red));
            }

            // Check today's meal state
            String today = DateTimeUtils.getCurrentDate();
            Meal todayMeal = mealDao.getUserMealForDate(messId, userId, today);
            boolean isMealOn = (todayMeal != null && (todayMeal.getDinnerCount() > 0 || todayMeal.getLunchCount() > 0));
            switchQuickDinner.setChecked(isMealOn);

        } catch (Throwable t) {
            android.util.Log.e("MainActivity", "Error in loadHeaderData: " + t.getMessage(), t);
        }
    }

    private void populateDrawerProfile() {
        try {
            String userName = sessionManager.getUserName();
            if (userName == null || userName.isEmpty()) userName = "Member";
            tvDrawerUserName.setText(userName);

            String userPhone = sessionManager.getUserPhone();
            tvDrawerUserPhone.setText(userPhone != null && !userPhone.isEmpty() ? userPhone : "No phone registered");

            String[] parts = userName.trim().split("\\s+");
            String initials = parts.length > 1
                    ? ("" + parts[0].charAt(0) + parts[1].charAt(0)).toUpperCase()
                    : ("" + parts[0].charAt(0)).toUpperCase();
            tvDrawerAvatarInitials.setText(initials);

            boolean isManager = sessionManager.isManager();
            boolean isAssistant = sessionManager.isAssistant();
            String roleStr = isManager ? "Manager" : (isAssistant ? "Assistant" : "Member");
            tvDrawerRoleBadge.setText(roleStr);

            boolean isPro = planGateManager.isPro();
            tvDrawerPlanPill.setText(isPro ? "Pro Plan" : "Free Plan");
            tvDrawerPlanPill.setTextColor(androidx.core.content.ContextCompat.getColor(this,
                    isPro ? R.color.primary_dark : R.color.warning_amber));

            // Manager Controls visibility
            int managerVisibility = isManager ? View.VISIBLE : View.GONE;
            if (tvDrawerManagerHeader != null) tvDrawerManagerHeader.setVisibility(managerVisibility);
            if (drawerItemSettings != null) drawerItemSettings.setVisibility(managerVisibility);
            if (drawerItemNotifManager != null) drawerItemNotifManager.setVisibility(managerVisibility);
            if (drawerItemHandover != null) drawerItemHandover.setVisibility(managerVisibility);

        } catch (Throwable t) {
            android.util.Log.e("MainActivity", "Error in populateDrawerProfile: " + t.getMessage(), t);
        }
    }

    private void setupDrawerActions() {
        if (drawerItemProfile != null) {
            drawerItemProfile.setOnClickListener(v -> {
                closeDrawer();
                Intent intent = new Intent(MainActivity.this, com.smartmess.android.ui.members.ActivityUserProfile.class);
                intent.putExtra(com.smartmess.android.ui.members.ActivityUserProfile.EXTRA_USER_ID, sessionManager.getUserId());
                startActivity(intent);
            });
        }

        if (drawerItemPlans != null) {
            drawerItemPlans.setOnClickListener(v -> {
                closeDrawer();
                planGateManager.showUpgradeBottomSheet(getSupportFragmentManager(), "SmartMess SaaS Plans",
                        "Upgrade to Pro for unlimited members, branded monthly audit reports, bulk SMS dispatcher, and cloud backup.");
            });
        }

        if (drawerItemSms != null) {
            drawerItemSms.setOnClickListener(v -> {
                closeDrawer();
                startActivity(new Intent(MainActivity.this, ActivitySmsDispatch.class));
            });
        }

        if (drawerItemStatements != null) {
            drawerItemStatements.setOnClickListener(v -> {
                closeDrawer();
                startActivity(new Intent(MainActivity.this, SummaryReportActivity.class));
            });
        }

        if (drawerItemVacation != null) {
            drawerItemVacation.setOnClickListener(v -> {
                closeDrawer();
                startActivity(new Intent(MainActivity.this, com.smartmess.android.ui.meals.ActivityMealVacation.class));
            });
        }

        if (drawerItemNotifications != null) {
            drawerItemNotifications.setOnClickListener(v -> {
                closeDrawer();
                startActivity(new Intent(MainActivity.this, com.smartmess.android.ui.notifications.ActivityNotificationCenter.class));
            });
        }

        if (drawerItemSupport != null) {
            drawerItemSupport.setOnClickListener(v -> {
                closeDrawer();
                startActivity(new Intent(MainActivity.this, ActivitySupportTickets.class));
            });
        }

        if (drawerItemBugReport != null) {
            drawerItemBugReport.setOnClickListener(v -> {
                closeDrawer();
                startActivity(new Intent(MainActivity.this, ActivityReportIssue.class));
            });
        }

        if (drawerItemSettings != null) {
            drawerItemSettings.setOnClickListener(v -> {
                closeDrawer();
                startActivity(new Intent(MainActivity.this, SettingsActivity.class));
            });
        }

        if (drawerItemNotifManager != null) {
            drawerItemNotifManager.setOnClickListener(v -> {
                closeDrawer();
                startActivity(new Intent(MainActivity.this, com.smartmess.android.ui.notifications.ActivityNotificationManager.class));
            });
        }

        if (drawerItemHandover != null) {
            drawerItemHandover.setOnClickListener(v -> {
                closeDrawer();
                showHandoverDialog();
            });
        }

        if (drawerItemSignOut != null) {
            drawerItemSignOut.setOnClickListener(v -> {
                closeDrawer();
                confirmSignOut();
            });
        }
    }

    private void updateNotificationBadge() {
        try {
            com.smartmess.android.data.local.dao.AppNotificationDao notifDao =
                    new com.smartmess.android.data.local.dao.AppNotificationDao(com.smartmess.android.data.local.DatabaseHelper.getInstance(this));
            int unread = notifDao.getUnreadCount(sessionManager.getMessId());
            if (tvNotificationBadge != null) {
                if (unread > 0) {
                    tvNotificationBadge.setVisibility(View.VISIBLE);
                    tvNotificationBadge.setText(unread > 99 ? "99+" : String.valueOf(unread));
                } else {
                    tvNotificationBadge.setVisibility(View.GONE);
                }
            }

            if (bottomNavigation != null && bottomNavigation.getMenu().findItem(R.id.nav_notifications) != null) {
                com.google.android.material.badge.BadgeDrawable badge = bottomNavigation.getOrCreateBadge(R.id.nav_notifications);
                if (unread > 0) {
                    badge.setVisible(true);
                    badge.setNumber(unread);
                } else {
                    badge.setVisible(false);
                }
            }
        } catch (Throwable ignored) {}
    }

    private void closeDrawer() {
        if (drawerLayout != null && drawerLayout.isDrawerOpen(GravityCompat.END)) {
            drawerLayout.closeDrawer(GravityCompat.END);
        }
    }

    private void showProfileDialog() {
        new AlertDialog.Builder(this)
                .setTitle("আমার একাউন্ট")
                .setMessage("Name: " + sessionManager.getUserName() + "\n" +
                        "Phone: " + sessionManager.getUserPhone() + "\n" +
                        "Role: " + sessionManager.getUserRole().toUpperCase() + "\n" +
                        "Mess: " + sessionManager.getMessName() + "\n" +
                        "Plan: " + (planGateManager.isPro() ? "SmartMess PRO ★" : "Free Tier"))
                .setPositiveButton("বন্ধ করুন", null)
                .show();
    }

    private void showHandoverDialog() {
        long messId = sessionManager.getMessId();
        List<User> members = userDao.getActiveMembersByMess(messId);
        // Exclude current user
        long currentUserId = sessionManager.getUserId();
        java.util.ArrayList<User> candidates = new java.util.ArrayList<>();
        for (User u : members) {
            if (u.getId() != currentUserId) {
                candidates.add(u);
            }
        }

        if (candidates.isEmpty()) {
            Toast.makeText(this, "ম্যানেজার দায়িত্ব দেওয়ার মতো অন্য কোনো সক্রিয় সদস্য নেই।", Toast.LENGTH_LONG).show();
            return;
        }

        String[] candidateNames = new String[candidates.size()];
        for (int i = 0; i < candidates.size(); i++) {
            candidateNames[i] = candidates.get(i).getName() + " (" + candidates.get(i).getPhone() + ")";
        }

        new AlertDialog.Builder(this)
                .setTitle("ম্যানেজার দায়িত্ব হস্তান্তর")
                .setItems(candidateNames, (dialog, which) -> {
                    User selected = candidates.get(which);
                    confirmHandoverToUser(selected);
                })
                .setNegativeButton("বাতিল", null)
                .show();
    }

    private void confirmHandoverToUser(User targetUser) {
        new AlertDialog.Builder(this)
                .setTitle("দায়িত্ব হস্তান্তরের নিশ্চিতকরণ")
                .setMessage("Are you sure you want to transfer full Manager privileges to " + targetUser.getName() + "? You will become a general member.")
                .setPositiveButton("হস্তান্তর করুন", (dialog, which) -> {
                    try {
                        long currentUserId = sessionManager.getUserId();
                        // Update in local DB
                        userDao.updateRole(currentUserId, "member");
                        userDao.updateRole(targetUser.getId(), "manager");

                        sessionManager.createSession(
                                sessionManager.getUserId(),
                                sessionManager.getUserUuid(),
                                sessionManager.getUserName(),
                                sessionManager.getUserPhone(),
                                "member",
                                sessionManager.getMessId(),
                                sessionManager.getMessUuid(),
                                sessionManager.getMessName(),
                                sessionManager.getAuthToken()
                        );

                        Toast.makeText(MainActivity.this, "ম্যানেজার দায়িত্ব সফলভাবে হস্তান্তর করা হয়েছে: " + targetUser.getName(), Toast.LENGTH_LONG).show();
                        loadHeaderData();
                        populateDrawerProfile();

                        com.smartmess.android.utils.NotificationCenterHelper.postNotification(
                                getApplicationContext(),
                                sessionManager.getMessId(),
                                "ম্যানেজার দায়িত্ব হস্তান্তর",
                                targetUser.getName() + " কে ম্যানেজার দায়িত্ব হস্তান্তর করা হয়েছে",
                                com.smartmess.android.utils.NotificationCenterHelper.TYPE_ROLE
                        );
                        updateNotificationBadge();

                        // Background sync
                        SyncManager.triggerSync(getApplicationContext());
                    } catch (Exception e) {
                        Toast.makeText(MainActivity.this, "হস্তান্তরের সময় ত্রুটি: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("বাতিল", null)
                .show();
    }

    private void confirmSignOut() {
        new AlertDialog.Builder(this)
                .setTitle("লগআউট")
                .setMessage("আপনি কি স্মার্ট মেস থেকে লগআউট করতে চান? আপনার সংরক্ষিত অফলাইন ডেটা নিরাপদে থাকবে।")
                .setPositiveButton("লগআউট করুন", (dialog, which) -> {
                    sessionManager.clearSession();
                    Toast.makeText(MainActivity.this, "সফলভাবে লগআউট হয়েছে", Toast.LENGTH_SHORT).show();
                    Intent intent = new Intent(MainActivity.this, LoginActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                    finish();
                })
                .setNegativeButton("বাতিল", null)
                .show();
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
                meal.setLunchCount(isChecked ? 1.0 : 0.0);
                meal.setDinnerCount(isChecked ? 1.0 : 0.0);
                meal.setGuestMealCount(0.0);
                meal.setIsLocked(0);
                meal.setSyncStatus(0);
                meal.setCreatedAt(DateTimeUtils.getCurrentDateTime());
                meal.setUpdatedAt(DateTimeUtils.getCurrentDateTime());
            } else {
                meal.setDinnerCount(isChecked ? 1.0 : 0.0);
                meal.setLunchCount(isChecked ? 1.0 : 0.0);
                meal.setSyncStatus(0);
                meal.setUpdatedAt(DateTimeUtils.getCurrentDateTime());
            }

            mealDao.insertOrUpdate(meal);
            loadHeaderData();
            Toast.makeText(this, isChecked ? "Meals Enabled for Today" : "Meals Turned OFF for Today", Toast.LENGTH_SHORT).show();

            // Background sync
            SyncManager.triggerSync(getApplicationContext());
        });
    }

    private void setupSyncIndicator() {
        btnCloudSync.setOnClickListener(v -> {
            Toast.makeText(MainActivity.this, "ক্লাউড সিঙ্ক শুরু হচ্ছে...", Toast.LENGTH_SHORT).show();
            syncManager.triggerTwoWaySync(new SyncManager.SyncCallback() {
                @Override
                public void onSyncStarted() {}

                @Override
                public void onSyncSuccess(String message) {
                    Toast.makeText(MainActivity.this, "সিঙ্ক সম্পন্ন: " + message, Toast.LENGTH_SHORT).show();
                    loadHeaderData();
                    populateDrawerProfile();
                }

                @Override
                public void onSyncFailed(String error) {
                    Toast.makeText(MainActivity.this, "সিঙ্ক নোটিশ: " + error, Toast.LENGTH_SHORT).show();
                }
            });
        });
    }

    private void setupBottomNav() {
        bottomNavigation.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();
            if (itemId == R.id.nav_dashboard) {
                currentSelectedNavId = R.id.nav_dashboard;
                loadFragment(new FragmentDashboardOverview());
                return true;
            } else if (itemId == R.id.nav_home) {
                currentSelectedNavId = R.id.nav_home;
                loadFragment(new FragmentDashboardOverview());
                return true;
            } else if (itemId == R.id.nav_help) {
                showHelpDialog();
                return false;
            } else if (itemId == R.id.nav_cycle || itemId == R.id.nav_meals) {
                currentSelectedNavId = itemId;
                loadFragment(new FragmentMealSheet());
                return true;
            } else if (itemId == R.id.nav_profile || itemId == R.id.nav_members) {
                currentSelectedNavId = itemId;
                loadFragment(new FragmentMemberList());
                return true;
            } else if (itemId == R.id.nav_bazar) {
                currentSelectedNavId = R.id.nav_bazar;
                loadFragment(new FragmentExpenseList());
                return true;
            } else if (itemId == R.id.nav_notifications) {
                currentSelectedNavId = R.id.nav_notifications;
                loadFragment(new com.smartmess.android.ui.notifications.FragmentNotifications());
                return true;
            } else if (itemId == R.id.nav_menu) {
                if (drawerLayout != null) {
                    if (drawerLayout.isDrawerOpen(GravityCompat.END)) {
                        drawerLayout.closeDrawer(GravityCompat.END);
                    } else {
                        drawerLayout.openDrawer(GravityCompat.END);
                    }
                }
                return false;
            }
            return false;
        });
    }

    private void showHelpDialog() {
        new com.google.android.material.dialog.MaterialAlertDialogBuilder(this)
                .setTitle("স্মার্ট মেস গাইড ও সহায়তা")
                .setMessage("• ড্যাশবোর্ড: এক নজরে মেসের ব্যালেন্স, মিল রেট ও খরচের হিসাব।\n"
                        + "• কুইক অ্যাকশন: সরাসরি মিল যোগ, টাকা জমা, খরচ যোগ ও মিল ফিক্সড করুন।\n"
                        + "• বিস্তারিত হিসাব: পূর্ণাঙ্গ মেস সামারি দেখতে 'বিস্তারিত হিসাব →' লিংকে ট্যাপ করুন।\n"
                        + "• মিল শিট: সদস্যভিত্তিক দৈনিক মিল গণনার তালিকা দেখতে ৪ নম্বর ট্যাবে যান।\n"
                        + "• সহায়তা: কোনো অনুসন্ধানের জন্য ডানদিকের ড্রয়ারের 'Support' ব্যবহার করুন।")
                .setPositiveButton("বুঝেছি", null)
                .show();
    }

    private void loadFragment(Fragment fragment) {
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragment_container, fragment)
                .commit();
    }

    @Override
    public void onBackPressed() {
        if (drawerLayout != null && drawerLayout.isDrawerOpen(GravityCompat.END)) {
            drawerLayout.closeDrawer(GravityCompat.END);
            return;
        }
        if (currentSelectedNavId != R.id.nav_dashboard && currentSelectedNavId != R.id.nav_home) {
            bottomNavigation.setSelectedItemId(R.id.nav_dashboard);
            currentSelectedNavId = R.id.nav_dashboard;
            loadFragment(new FragmentDashboardOverview());
            return;
        }
        super.onBackPressed();
    }
}

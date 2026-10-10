package com.smartmess.android.ui.dashboard;

import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.bumptech.glide.Glide;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.smartmess.android.R;
import com.smartmess.android.data.local.DatabaseHelper;
import com.smartmess.android.data.local.dao.ExpenseDao;
import com.smartmess.android.data.local.dao.MealDao;
import com.smartmess.android.data.local.dao.MessDao;
import com.smartmess.android.engine.AccountingEngine;
import com.smartmess.android.engine.CalculationModels.CycleSummary;
import com.smartmess.android.model.Expense;
import com.smartmess.android.model.Meal;
import com.smartmess.android.ui.expenses.ActivityAddExpense;
import com.smartmess.android.ui.sms.ActivitySmsDispatch;
import com.smartmess.android.utils.CurrencyUtils;
import com.smartmess.android.utils.DateTimeUtils;
import com.smartmess.android.utils.SessionManager;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class FragmentDashboardOverview extends Fragment {

    private TextView tvLiveMealRate;
    private TextView tvTotalMessMeals;
    private TextView tvCashInHand;
    private TextView tvActiveMembers;
    private TextView tvRawMealCost;
    private TextView tvSharedFoodCost;
    private TextView tvUtilityCost;
    private MaterialButton btnQuickAddExpense;
    private MaterialButton btnQuickSmsDispatch;
    private MaterialButton btnQuickAddDeposit;

    // Mockup Views (3. ড্যাশবোর্ডে বিস্তারিত)
    private TextView tvDashboardDate;
    private TextView btnDetailedReportLink;
    private TextView tvDashboardMessName;
    private View btnAllMembersSummary;
    private TextView tvTodayMessMeals;
    private TextView tvMessTotalCost;

    // Personal Hero Card Views (আমার হিসাব)
    private TextView tvMyPersonalBalance;
    private TextView tvMyDepositPill;
    private TextView tvMyExpensePill;
    private TextView tvMyTotalMeals;
    private android.widget.ProgressBar progressMealRing;

    // Quick Action Buttons
    private View btnActionAddMeal;
    private View btnActionAddDeposit;
    private View btnActionAddExpense;
    private View btnActionMealRequest;
    private View btnActionBazar;
    private View btnActionMembers;

    // Point 5: Recent Bazars Activity Stream
    private TextView btnViewAllBazars;
    private TextView tvEmptyRecentBazars;
    private LinearLayout layoutRecentBazarsContainer;

    // Point 5: Today's Meal Snapshot
    private TextView btnViewMealSheet;
    private TextView tvSnapshotBfCount;
    private TextView tvSnapshotLunchCount;
    private TextView tvSnapshotDinnerCount;
    private TextView tvSnapshotTotalMeals;
    private LinearLayout layoutAvatarStack;
    private TextView tvDinerCountSummary;

    // Point 16: Daily Meal Budget Alert Engine Views
    private MaterialCardView cardBudgetWarning;
    private TextView tvBudgetWarningText;
    private TextView tvTargetBudgetBadge;

    private AccountingEngine accountingEngine;
    private ExpenseDao expenseDao;
    private MealDao mealDao;
    private MessDao messDao;
    private SessionManager sessionManager;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_dashboard_overview, container, false);

        DatabaseHelper dbHelper = DatabaseHelper.getInstance(requireContext());
        accountingEngine = new AccountingEngine(requireContext());
        expenseDao = new ExpenseDao(dbHelper);
        mealDao = new MealDao(dbHelper);
        messDao = new MessDao(dbHelper);
        sessionManager = new SessionManager(requireContext());

        // Mockup header, cards, and quick action views
        tvDashboardDate = v.findViewById(R.id.tvDashboardDate);
        btnDetailedReportLink = v.findViewById(R.id.btnDetailedReportLink);
        tvDashboardMessName = v.findViewById(R.id.tvDashboardMessName);
        btnAllMembersSummary = v.findViewById(R.id.btnAllMembersSummary);
        tvTodayMessMeals = v.findViewById(R.id.tvTodayMessMeals);
        tvMessTotalCost = v.findViewById(R.id.tvMessTotalCost);

        tvMyPersonalBalance = v.findViewById(R.id.tvMyPersonalBalance);
        tvMyDepositPill = v.findViewById(R.id.tvMyDepositPill);
        tvMyExpensePill = v.findViewById(R.id.tvMyExpensePill);
        tvMyTotalMeals = v.findViewById(R.id.tvMyTotalMeals);
        progressMealRing = v.findViewById(R.id.progressMealRing);

        btnActionAddMeal = v.findViewById(R.id.btnActionAddMeal);
        btnActionAddDeposit = v.findViewById(R.id.btnActionAddDeposit);
        btnActionAddExpense = v.findViewById(R.id.btnActionAddExpense);
        btnActionMealRequest = v.findViewById(R.id.btnActionMealRequest);
        btnActionBazar = v.findViewById(R.id.btnActionBazar);
        btnActionMembers = v.findViewById(R.id.btnActionMembers);

        // Core metric views
        tvLiveMealRate = v.findViewById(R.id.tvLiveMealRate);
        tvTotalMessMeals = v.findViewById(R.id.tvTotalMessMeals);
        tvCashInHand = v.findViewById(R.id.tvCashInHand);
        tvActiveMembers = v.findViewById(R.id.tvActiveMembers);
        tvRawMealCost = v.findViewById(R.id.tvRawMealCost);
        tvSharedFoodCost = v.findViewById(R.id.tvSharedFoodCost);
        tvUtilityCost = v.findViewById(R.id.tvUtilityCost);
        btnQuickAddExpense = v.findViewById(R.id.btnQuickAddExpense);
        btnQuickSmsDispatch = v.findViewById(R.id.btnQuickSmsDispatch);
        btnQuickAddDeposit = v.findViewById(R.id.btnQuickAddDeposit);

        // Meal Budget Warning Views
        cardBudgetWarning = v.findViewById(R.id.cardBudgetWarning);
        tvBudgetWarningText = v.findViewById(R.id.tvBudgetWarningText);
        tvTargetBudgetBadge = v.findViewById(R.id.tvTargetBudgetBadge);

        if (cardBudgetWarning != null) {
            cardBudgetWarning.setOnClickListener(view ->
                    startActivity(new Intent(requireContext(), com.smartmess.android.ui.settings.SettingsActivity.class)));
        }

        // Recent Bazars Views
        btnViewAllBazars = v.findViewById(R.id.btnViewAllBazars);
        tvEmptyRecentBazars = v.findViewById(R.id.tvEmptyRecentBazars);
        layoutRecentBazarsContainer = v.findViewById(R.id.layoutRecentBazarsContainer);

        // Meal Snapshot Views
        btnViewMealSheet = v.findViewById(R.id.btnViewMealSheet);
        tvSnapshotBfCount = v.findViewById(R.id.tvSnapshotBfCount);
        tvSnapshotLunchCount = v.findViewById(R.id.tvSnapshotLunchCount);
        tvSnapshotDinnerCount = v.findViewById(R.id.tvSnapshotDinnerCount);
        tvSnapshotTotalMeals = v.findViewById(R.id.tvSnapshotTotalMeals);
        layoutAvatarStack = v.findViewById(R.id.layoutAvatarStack);
        tvDinerCountSummary = v.findViewById(R.id.tvDinerCountSummary);

        // Click Listeners
        if (btnDetailedReportLink != null) {
            btnDetailedReportLink.setOnClickListener(view ->
                    startActivity(new Intent(requireContext(), com.smartmess.android.ui.reports.SummaryReportActivity.class)));
        }
        if (btnAllMembersSummary != null) {
            btnAllMembersSummary.setOnClickListener(view ->
                    startActivity(new Intent(requireContext(), com.smartmess.android.ui.reports.SummaryReportActivity.class)));
        }

        // Quick action circular buttons
        if (btnActionAddMeal != null) {
            btnActionAddMeal.setOnClickListener(view ->
                    startActivity(new Intent(requireContext(), com.smartmess.android.ui.meals.AddMealActivity.class)));
        }
        if (btnActionAddDeposit != null) {
            btnActionAddDeposit.setOnClickListener(view ->
                    startActivity(new Intent(requireContext(), com.smartmess.android.ui.deposits.AddDepositActivity.class)));
        }
        if (btnActionAddExpense != null) {
            btnActionAddExpense.setOnClickListener(view ->
                    startActivity(new Intent(requireContext(), com.smartmess.android.ui.expenses.ActivityAddExpense.class)));
        }
        if (btnActionMealRequest != null) {
            btnActionMealRequest.setOnClickListener(view -> {
                Intent intent = new Intent(requireContext(), com.smartmess.android.ui.meals.AddMealActivity.class);
                intent.putExtra("open_request_tab", true);
                startActivity(intent);
            });
        }
        if (btnActionBazar != null) {
            btnActionBazar.setOnClickListener(view ->
                    startActivity(new Intent(requireContext(), com.smartmess.android.ui.expenses.ActivityBazarLedger.class)));
        }
        if (btnActionMembers != null) {
            btnActionMembers.setOnClickListener(view ->
                    startActivity(new Intent(requireContext(), com.smartmess.android.ui.members.MemberListActivity.class)));
        }

        if (btnQuickAddExpense != null) {
            btnQuickAddExpense.setOnClickListener(view -> startActivity(new Intent(requireContext(), com.smartmess.android.ui.expenses.ActivityAddBazar.class)));
        }
        if (btnQuickSmsDispatch != null) {
            btnQuickSmsDispatch.setOnClickListener(view -> startActivity(new Intent(requireContext(), ActivitySmsDispatch.class)));
        }
        if (btnQuickAddDeposit != null) {
            btnQuickAddDeposit.setOnClickListener(view -> startActivity(new Intent(requireContext(), com.smartmess.android.ui.deposits.AddDepositActivity.class)));
        }

        if (btnViewAllBazars != null) {
            btnViewAllBazars.setOnClickListener(view -> startActivity(new Intent(requireContext(), com.smartmess.android.ui.expenses.ActivityBazarLedger.class)));
        }

        if (btnViewMealSheet != null) {
            btnViewMealSheet.setOnClickListener(view -> navigateToBottomTab(R.id.nav_cycle));
        }

        loadAllData();
        return v;
    }

    @Override
    public void onResume() {
        super.onResume();
        loadAllData();
    }

    private void navigateToBottomTab(int tabId) {
        if (getActivity() != null) {
            BottomNavigationView nav = getActivity().findViewById(R.id.bottom_navigation);
            if (nav == null) {
                nav = getActivity().findViewById(R.id.bottomNavigation);
            }
            if (nav != null) {
                if (tabId == R.id.nav_meals || tabId == R.id.nav_cycle) {
                    if (nav.getMenu().findItem(R.id.nav_cycle) != null) tabId = R.id.nav_cycle;
                    else if (nav.getMenu().findItem(R.id.nav_meals) != null) tabId = R.id.nav_meals;
                } else if (tabId == R.id.nav_members || tabId == R.id.nav_profile) {
                    if (nav.getMenu().findItem(R.id.nav_profile) != null) tabId = R.id.nav_profile;
                    else if (nav.getMenu().findItem(R.id.nav_members) != null) tabId = R.id.nav_members;
                } else if (tabId == R.id.nav_bazar) {
                    startActivity(new Intent(requireContext(), com.smartmess.android.ui.expenses.ActivityBazarLedger.class));
                    return;
                }
                if (nav.getMenu().findItem(tabId) != null) {
                    nav.setSelectedItemId(tabId);
                }
            }
        }
    }

    private void loadAllData() {
        loadStats();
        long messId = sessionManager.getMessId();
        loadRecentBazars(messId);
        loadTodayMealSnapshot(messId);
    }

    private void loadStats() {
        try {
            long messId = sessionManager.getMessId();
            CycleSummary summary = accountingEngine.calculateCurrentCycleSummary(messId);
            if (summary == null) return;

            // Date Pill e.g. July 26
            if (tvDashboardDate != null) {
                SimpleDateFormat sdf = new SimpleDateFormat("MMMM d", Locale.US);
                tvDashboardDate.setText(sdf.format(new Date()));
            }

            com.smartmess.android.model.Mess mess = messDao.getById(messId);
            if (tvDashboardMessName != null) {
                String mName = (mess != null && mess.getName() != null && !mess.getName().isEmpty()) ? mess.getName() : "Our Mess";
                tvDashboardMessName.setText(mName);
            }

            if (tvLiveMealRate != null) {
                tvLiveMealRate.setText(String.format(Locale.US, "%.2f ৳", summary.getLiveMealRate()));
            }
            if (tvTotalMessMeals != null) {
                tvTotalMessMeals.setText(String.format(Locale.US, "%.2f", summary.getTotalMeals()));
            }
            if (tvCashInHand != null) {
                tvCashInHand.setText(String.format(Locale.US, "%.2f ৳", summary.getCashInHand()));
            }
            if (tvMessTotalCost != null) {
                tvMessTotalCost.setText(String.format(Locale.US, "%.2f ৳", summary.getTotalAllExpenses()));
            }

            // Today's Mess Meals count
            if (tvTodayMessMeals != null) {
                String todayIso = DateTimeUtils.currentDate();
                List<Meal> todayMeals = mealDao.getMealsForDate(messId, todayIso);
                double todayCount = 0.0;
                if (todayMeals != null) {
                    for (Meal m : todayMeals) {
                        todayCount += m.getTotalMeals();
                    }
                }
                tvTodayMessMeals.setText(String.format(Locale.US, "%.2f", todayCount));
            }

            // Personal Balance & Hero Card (আমার হিসাব)
            long currentUserId = sessionManager.getUserId();
            com.smartmess.android.engine.CalculationModels.MemberBalanceSheet sheet =
                    accountingEngine.calculateMemberBalance(messId, currentUserId);

            if (sheet != null) {
                if (tvMyPersonalBalance != null) {
                    tvMyPersonalBalance.setText(String.format(Locale.US, "%.2f ৳", sheet.getNetBalance()));
                }
                if (tvMyDepositPill != null) {
                    tvMyDepositPill.setText("জমা " + String.format(Locale.US, "%.2f৳", sheet.getTotalDeposit()));
                }
                if (tvMyExpensePill != null) {
                    tvMyExpensePill.setText("খরচ " + String.format(Locale.US, "%.2f৳", sheet.getTotalCost()));
                }
                if (tvMyTotalMeals != null) {
                    tvMyTotalMeals.setText(String.format(Locale.US, "%.2f", sheet.getConsumedMeals()));
                }
                if (progressMealRing != null) {
                    int prog = (int) Math.min(100, Math.max(15, (sheet.getConsumedMeals() / Math.max(1.0, summary.getTotalMeals())) * 100));
                    progressMealRing.setProgress(prog);
                }
            }

            if (tvActiveMembers != null) {
                tvActiveMembers.setText(summary.getActiveMemberCount() + " জন সক্রিয় সদস্য");
            }
            if (tvRawMealCost != null) {
                tvRawMealCost.setText(CurrencyUtils.format(summary.getRawMealCost()));
            }
            if (tvSharedFoodCost != null) {
                tvSharedFoodCost.setText(CurrencyUtils.format(summary.getSharedFoodCost()));
            }
            if (tvUtilityCost != null) {
                tvUtilityCost.setText(CurrencyUtils.format(summary.getUtilityCost()));
            }

            // Real-time Meal Budget Engine Check
            double targetBudget = (mess != null) ? mess.getTargetMealBudget() : 70.00;
            double liveMealRate = summary.getLiveMealRate();

            if (tvTargetBudgetBadge != null) {
                tvTargetBudgetBadge.setText("বাজেট: " + CurrencyUtils.format(targetBudget) + "/মিল");
            }

            if (targetBudget > 0 && liveMealRate > targetBudget) {
                if (cardBudgetWarning != null) {
                    cardBudgetWarning.setVisibility(View.VISIBLE);
                    if (tvBudgetWarningText != null) {
                        tvBudgetWarningText.setText(String.format(Locale.US,
                                "সতর্কতা: বর্তমান মিল রেট (%s) আপনার বাজেট (%s) অতিক্রম করেছে",
                                CurrencyUtils.format(liveMealRate), CurrencyUtils.format(targetBudget)));
                    }
                }
                tvLiveMealRate.setTextColor(ContextCompat.getColor(requireContext(), R.color.due_red));
                if (tvTargetBudgetBadge != null) {
                    tvTargetBudgetBadge.setTextColor(ContextCompat.getColor(requireContext(), R.color.due_red));
                    tvTargetBudgetBadge.setText("বাজেটের বেশি (" + CurrencyUtils.format(targetBudget) + ")");
                }

                // Point 16 & Notification Center: Log budget alert notification once per day
                String today = com.smartmess.android.utils.DateTimeUtils.getCurrentDate();
                String lastBudgetAlertKey = "last_budget_alert_date_" + messId;
                android.content.SharedPreferences sp = requireContext().getSharedPreferences("smartmess_alerts", android.content.Context.MODE_PRIVATE);
                if (!today.equals(sp.getString(lastBudgetAlertKey, ""))) {
                    sp.edit().putString(lastBudgetAlertKey, today).apply();
                    com.smartmess.android.utils.NotificationCenterHelper.postNotification(
                            requireContext().getApplicationContext(),
                            messId,
                            "বাজেট অতিক্রম",
                            String.format(Locale.US, "বর্তমান মিল রেট (%s) আপনার নির্ধারিত বাজেট (%s) ছাড়িয়ে গেছে",
                                    CurrencyUtils.format(liveMealRate), CurrencyUtils.format(targetBudget)),
                            com.smartmess.android.utils.NotificationCenterHelper.TYPE_BUDGET
                    );
                }
            } else {
                if (cardBudgetWarning != null) {
                    cardBudgetWarning.setVisibility(View.GONE);
                }
                tvLiveMealRate.setTextColor(ContextCompat.getColor(requireContext(), R.color.primary_dark));
                if (tvTargetBudgetBadge != null) {
                    tvTargetBudgetBadge.setTextColor(ContextCompat.getColor(requireContext(), R.color.text_muted));
                }
            }
        } catch (Throwable t) {
            android.util.Log.e("FragmentDashboard", "Error loading stats: " + t.getMessage(), t);
        }
    }

    /**
     * Point 5 Section A: Recent Bazars Stream (Last 3 entries)
     */
    private void loadRecentBazars(long messId) {
        if (layoutRecentBazarsContainer == null) return;
        layoutRecentBazarsContainer.removeAllViews();

        try {
            List<Expense> recentBazars = expenseDao.getRecentExpenses(messId, 3);

            if (recentBazars == null || recentBazars.isEmpty()) {
                if (tvEmptyRecentBazars != null) tvEmptyRecentBazars.setVisibility(View.VISIBLE);
                return;
            }

            if (tvEmptyRecentBazars != null) tvEmptyRecentBazars.setVisibility(View.GONE);
            LayoutInflater inflater = LayoutInflater.from(requireContext());

            for (int i = 0; i < recentBazars.size(); i++) {
                Expense expense = recentBazars.get(i);
                View itemView = inflater.inflate(R.layout.item_recent_bazar, layoutRecentBazarsContainer, false);

                ImageView ivThumbnail = itemView.findViewById(R.id.ivBazarThumbnail);
                TextView tvTitle = itemView.findViewById(R.id.tvBazarTitle);
                TextView tvBuyerDate = itemView.findViewById(R.id.tvBazarBuyerDate);
                TextView tvCategory = itemView.findViewById(R.id.tvBazarCategory);
                TextView tvVoucherTag = itemView.findViewById(R.id.tvVoucherTag);
                TextView tvAmount = itemView.findViewById(R.id.tvBazarAmount);

                String title = expense.getTitle();
                tvTitle.setText(title != null && !title.trim().isEmpty() ? title : "বাজারের সদাই");

                String buyer = expense.getBuyerName() != null ? expense.getBuyerName() : "সদস্য #" + expense.getBuyerUserId();
                String dateStr = DateTimeUtils.formatDisplayDate(expense.getExpenseDate());
                tvBuyerDate.setText(buyer + " কর্তৃক • " + dateStr);

                tvAmount.setText(CurrencyUtils.format(expense.getAmount()));

                // Category pill styling
                if (expense.isRawMeal()) {
                    tvCategory.setText("বাজার খরচ");
                    tvCategory.setTextColor(ContextCompat.getColor(requireContext(), R.color.credit_green));
                } else if (expense.isSharedFood()) {
                    tvCategory.setText("মশলা ও তেল");
                    tvCategory.setTextColor(ContextCompat.getColor(requireContext(), R.color.accent));
                } else if (expense.isUtilityAsset()) {
                    tvCategory.setText("ভাড়া/অন্যান্য");
                    tvCategory.setTextColor(ContextCompat.getColor(requireContext(), R.color.info_blue));
                } else {
                    tvCategory.setText("সাধারণ");
                    tvCategory.setTextColor(ContextCompat.getColor(requireContext(), R.color.warning_amber));
                }

                // WebP voucher image thumbnail or cart icon
                String voucherUrl = expense.getVoucherImageUrl();
                if (voucherUrl != null && !voucherUrl.trim().isEmpty()) {
                    tvVoucherTag.setVisibility(View.VISIBLE);
                    ivThumbnail.setPadding(0, 0, 0, 0);
                    ivThumbnail.setImageTintList(null);
                    Glide.with(this)
                            .load(voucherUrl.trim())
                            .placeholder(R.drawable.ic_lucide_cart)
                            .error(R.drawable.ic_lucide_cart)
                            .centerCrop()
                            .into(ivThumbnail);
                } else {
                    tvVoucherTag.setVisibility(View.GONE);
                    ivThumbnail.setImageResource(R.drawable.ic_lucide_cart);
                    ivThumbnail.setPadding(dpToPx(8), dpToPx(8), dpToPx(8), dpToPx(8));
                    ivThumbnail.setImageTintList(ColorStateList.valueOf(ContextCompat.getColor(requireContext(), R.color.primary)));
                }

                itemView.setOnClickListener(v -> startActivity(new Intent(requireContext(), com.smartmess.android.ui.expenses.ActivityBazarLedger.class)));

                layoutRecentBazarsContainer.addView(itemView);

                // Add thin divider between items
                if (i < recentBazars.size() - 1) {
                    View divider = new View(requireContext());
                    LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dpToPx(1));
                    lp.setMargins(dpToPx(54), 0, 0, 0);
                    divider.setLayoutParams(lp);
                    divider.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.border_stroke));
                    layoutRecentBazarsContainer.addView(divider);
                }
            }
        } catch (Throwable t) {
            android.util.Log.e("FragmentDashboard", "Error loading recent bazars: " + t.getMessage(), t);
        }
    }

    /**
     * Point 5 Section B: Today's Meal Snapshot (Live count of BF, Lunch, Dinner & Avatar stack)
     */
    private void loadTodayMealSnapshot(long messId) {
        if (tvSnapshotBfCount == null) return;

        try {
            String today = new SimpleDateFormat("yyyy-MM-dd", Locale.US).format(new Date());
            List<Meal> todayMeals = mealDao.getMealsForDate(messId, today);

            double sumBf = 0, sumLn = 0, sumDn = 0, sumGuest = 0;
            List<String> activeDiners = new ArrayList<>();

            if (todayMeals != null) {
                for (Meal m : todayMeals) {
                    sumBf += m.getBreakfastCount();
                    sumLn += m.getLunchCount();
                    sumDn += m.getDinnerCount();
                    sumGuest += m.getGuestMealCount();

                    double total = m.getBreakfastCount() + m.getLunchCount() + m.getDinnerCount() + m.getGuestMealCount();
                    if (total > 0) {
                        String name = m.getUserName();
                        if (name != null && !name.trim().isEmpty()) {
                            activeDiners.add(name.trim());
                        } else {
                            activeDiners.add("User #" + m.getUserId());
                        }
                    }
                }
            }

            tvSnapshotBfCount.setText(String.format(Locale.US, "%.1f", sumBf));
            tvSnapshotLunchCount.setText(String.format(Locale.US, "%.1f", sumLn));
            tvSnapshotDinnerCount.setText(String.format(Locale.US, "%.1f", sumDn));
            tvSnapshotTotalMeals.setText(String.format(Locale.US, "%.1f", (sumBf + sumLn + sumDn + sumGuest)));

            // Populate Avatar Stack
            if (layoutAvatarStack != null) {
                layoutAvatarStack.removeAllViews();
                int dinerCount = activeDiners.size();
                if (tvDinerCountSummary != null) {
                    tvDinerCountSummary.setText(dinerCount + " জন খাচ্ছেন");
                }

                if (dinerCount == 0) {
                    TextView tvEmpty = new TextView(requireContext());
                    tvEmpty.setText("কোনো মিল গণনা নেই");
                    tvEmpty.setTextColor(ContextCompat.getColor(requireContext(), R.color.text_muted));
                    tvEmpty.setTextSize(11);
                    layoutAvatarStack.addView(tvEmpty);
                } else {
                    int maxAvatars = 6;
                    int showCount = Math.min(dinerCount, maxAvatars);

                    for (int i = 0; i < showCount; i++) {
                        String name = activeDiners.get(i);
                        String initial = !name.isEmpty() ? String.valueOf(name.charAt(0)).toUpperCase(Locale.ROOT) : "M";

                        TextView avatarView = new TextView(requireContext());
                        int sizePx = dpToPx(30);
                        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(sizePx, sizePx);
                        if (i > 0) {
                            lp.setMarginStart(dpToPx(-8)); // Overlapping avatar stack
                        }
                        avatarView.setLayoutParams(lp);
                        avatarView.setBackgroundResource(R.drawable.bg_avatar_stack_item);
                        avatarView.setGravity(Gravity.CENTER);
                        avatarView.setText(initial);
                        avatarView.setTextColor(ContextCompat.getColor(requireContext(), R.color.text_on_primary));
                        avatarView.setTextSize(11);
                        avatarView.setTypeface(null, Typeface.BOLD);

                        layoutAvatarStack.addView(avatarView);
                    }

                    if (dinerCount > maxAvatars) {
                        int extra = dinerCount - maxAvatars;
                        TextView extraView = new TextView(requireContext());
                        int sizePx = dpToPx(30);
                        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(sizePx, sizePx);
                        lp.setMarginStart(dpToPx(-8));
                        extraView.setLayoutParams(lp);
                        extraView.setBackgroundResource(R.drawable.bg_avatar_stack_item);
                        extraView.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(requireContext(), R.color.slate_800)));
                        extraView.setGravity(Gravity.CENTER);
                        extraView.setText("+" + extra);
                        extraView.setTextColor(ContextCompat.getColor(requireContext(), R.color.text_on_primary));
                        extraView.setTextSize(10);
                        extraView.setTypeface(null, Typeface.BOLD);
                        layoutAvatarStack.addView(extraView);
                    }
                }
            }
        } catch (Throwable t) {
            android.util.Log.e("FragmentDashboard", "Error loading today's meal snapshot: " + t.getMessage(), t);
        }
    }

    private int dpToPx(int dp) {
        return Math.round(dp * getResources().getDisplayMetrics().density);
    }
}

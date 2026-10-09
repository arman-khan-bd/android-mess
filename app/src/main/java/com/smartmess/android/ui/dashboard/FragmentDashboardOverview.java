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
import com.smartmess.android.R;
import com.smartmess.android.data.local.DatabaseHelper;
import com.smartmess.android.data.local.dao.ExpenseDao;
import com.smartmess.android.data.local.dao.MealDao;
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

    private AccountingEngine accountingEngine;
    private ExpenseDao expenseDao;
    private MealDao mealDao;
    private SessionManager sessionManager;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_dashboard_overview, container, false);

        DatabaseHelper dbHelper = DatabaseHelper.getInstance(requireContext());
        accountingEngine = new AccountingEngine(requireContext());
        expenseDao = new ExpenseDao(dbHelper);
        mealDao = new MealDao(dbHelper);
        sessionManager = new SessionManager(requireContext());

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

        // Point 5: Recent Bazars Views
        btnViewAllBazars = v.findViewById(R.id.btnViewAllBazars);
        tvEmptyRecentBazars = v.findViewById(R.id.tvEmptyRecentBazars);
        layoutRecentBazarsContainer = v.findViewById(R.id.layoutRecentBazarsContainer);

        // Point 5: Meal Snapshot Views
        btnViewMealSheet = v.findViewById(R.id.btnViewMealSheet);
        tvSnapshotBfCount = v.findViewById(R.id.tvSnapshotBfCount);
        tvSnapshotLunchCount = v.findViewById(R.id.tvSnapshotLunchCount);
        tvSnapshotDinnerCount = v.findViewById(R.id.tvSnapshotDinnerCount);
        tvSnapshotTotalMeals = v.findViewById(R.id.tvSnapshotTotalMeals);
        layoutAvatarStack = v.findViewById(R.id.layoutAvatarStack);
        tvDinerCountSummary = v.findViewById(R.id.tvDinerCountSummary);

        btnQuickAddExpense.setOnClickListener(view -> startActivity(new Intent(requireContext(), com.smartmess.android.ui.expenses.ActivityAddBazar.class)));
        btnQuickSmsDispatch.setOnClickListener(view -> startActivity(new Intent(requireContext(), ActivitySmsDispatch.class)));
        if (btnQuickAddDeposit != null) {
            btnQuickAddDeposit.setOnClickListener(view -> startActivity(new Intent(requireContext(), com.smartmess.android.ui.deposits.AddDepositActivity.class)));
        }

        // View All Bazars navigation
        if (btnViewAllBazars != null) {
            btnViewAllBazars.setOnClickListener(view -> navigateToBottomTab(R.id.nav_bazar));
        }

        // View Meal Sheet navigation
        if (btnViewMealSheet != null) {
            btnViewMealSheet.setOnClickListener(view -> navigateToBottomTab(R.id.nav_meals));
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
            BottomNavigationView nav = getActivity().findViewById(R.id.bottomNavigation);
            if (nav != null) {
                nav.setSelectedItemId(tabId);
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

            tvLiveMealRate.setText(CurrencyUtils.format(summary.getLiveMealRate()));
            tvTotalMessMeals.setText(String.format(Locale.US, "%.1f Meals Logged", summary.getTotalMeals()));
            tvCashInHand.setText(CurrencyUtils.format(summary.getCashInHand()));
            tvActiveMembers.setText(summary.getActiveMemberCount() + " Active Members");

            tvRawMealCost.setText(CurrencyUtils.format(summary.getRawMealCost()));
            tvSharedFoodCost.setText(CurrencyUtils.format(summary.getSharedFoodCost()));
            tvUtilityCost.setText(CurrencyUtils.format(summary.getUtilityCost()));
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
                tvTitle.setText(title != null && !title.trim().isEmpty() ? title : "Market Purchase");

                String buyer = expense.getBuyerName() != null ? expense.getBuyerName() : "Buyer #" + expense.getBuyerUserId();
                String dateStr = DateTimeUtils.formatDisplayDate(expense.getExpenseDate());
                tvBuyerDate.setText("By " + buyer + " • " + dateStr);

                tvAmount.setText(CurrencyUtils.format(expense.getAmount()));

                // Category pill styling
                if (expense.isRawMeal()) {
                    tvCategory.setText("Raw Meal");
                    tvCategory.setTextColor(ContextCompat.getColor(requireContext(), R.color.credit_green));
                } else if (expense.isSharedFood()) {
                    tvCategory.setText("Shared Food");
                    tvCategory.setTextColor(ContextCompat.getColor(requireContext(), R.color.accent));
                } else if (expense.isUtilityAsset()) {
                    tvCategory.setText("Asset/Utility");
                    tvCategory.setTextColor(ContextCompat.getColor(requireContext(), R.color.info_blue));
                } else {
                    tvCategory.setText("General");
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

                itemView.setOnClickListener(v -> navigateToBottomTab(R.id.nav_bazar));

                layoutRecentBazarsContainer.addView(itemView);

                // Add thin divider between items
                if (i < recentBazars.size() - 1) {
                    View divider = new View(requireContext());
                    LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.match_parent, dpToPx(1));
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
                    tvDinerCountSummary.setText(dinerCount + (dinerCount == 1 ? " diner" : " diners"));
                }

                if (dinerCount == 0) {
                    TextView tvEmpty = new TextView(requireContext());
                    tvEmpty.setText("No meals logged yet");
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

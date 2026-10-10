package com.smartmess.android.engine;

import android.content.Context;

import com.smartmess.android.data.local.DatabaseHelper;
import com.smartmess.android.data.local.dao.DepositDao;
import com.smartmess.android.data.local.dao.ExpenseDao;
import com.smartmess.android.data.local.dao.MealDao;
import com.smartmess.android.data.local.dao.UserDao;
import com.smartmess.android.engine.CalculationModels.CycleSummary;
import com.smartmess.android.engine.CalculationModels.MemberBalanceSheet;
import com.smartmess.android.model.Expense;
import com.smartmess.android.model.User;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

public class AccountingEngine {

    private final UserDao userDao;
    private final MealDao mealDao;
    private final ExpenseDao expenseDao;
    private final DepositDao depositDao;
    private final com.smartmess.android.data.local.dao.MessDao messDao;

    public AccountingEngine(Context context) {
        DatabaseHelper helper = DatabaseHelper.getInstance(context);
        this.userDao = new UserDao(helper);
        this.mealDao = new MealDao(helper);
        this.expenseDao = new ExpenseDao(helper);
        this.depositDao = new DepositDao(helper);
        this.messDao = new com.smartmess.android.data.local.dao.MessDao(helper);
    }

    public CycleSummary calculateCycleSummary(long messId, String startDate, String endDate) {
        CycleSummary summary = new CycleSummary();
        summary.setStartDate(startDate);
        summary.setEndDate(endDate);

        List<User> activeMembers = userDao.getActiveMembersByMess(messId);
        int memberCount = activeMembers.size();
        summary.setActiveMemberCount(memberCount);

        com.smartmess.android.model.Mess mess = messDao.getById(messId);
        double targetMealBudget = (mess != null && mess.getTargetMealBudget() > 0) ? mess.getTargetMealBudget() : 70.0;

        // 1. Dual Expense Pools
        double rawMealTotal = expenseDao.getTotalByCategory(messId, Expense.CAT_RAW_MEAL, startDate, endDate);
        double sharedFoodTotal = expenseDao.getTotalByCategory(messId, Expense.CAT_SHARED_FOOD, startDate, endDate);
        double utilityAssetTotal = expenseDao.getTotalByCategory(messId, Expense.CAT_UTILITY_ASSET, startDate, endDate);
        double smsChargeTotal = expenseDao.getTotalByCategory(messId, Expense.CAT_SMS_CHARGE, startDate, endDate);
        double allExpensesTotal = expenseDao.getTotalMessExpenses(messId, startDate, endDate);

        summary.setTotalRawMealExpense(round(rawMealTotal));
        summary.setTotalSharedFoodExpense(round(sharedFoodTotal));
        summary.setTotalUtilityAssetExpense(round(utilityAssetTotal));
        summary.setTotalSmsChargeExpense(round(smsChargeTotal));
        summary.setTotalAllExpenses(round(allExpensesTotal));

        // 2. Meal Counts & Working Meal Rate vs Fixed Budget Billing Rate
        double totalMessMeals = mealDao.getTotalMessMeals(messId, startDate, endDate);
        summary.setTotalMessMeals(round(totalMessMeals));

        double workingMealRate = 0.0;
        if (totalMessMeals > 0) {
            workingMealRate = rawMealTotal / totalMessMeals;
        }
        summary.setWorkingMealRate(round(workingMealRate));
        summary.setTargetMealBudget(round(targetMealBudget));

        // Effective Billing Meal Rate: if target budget is configured (> 0, e.g. 70.00 Tk), bill every consumed meal at 70 Tk
        double billingMealRate = (targetMealBudget > 0) ? targetMealBudget : workingMealRate;
        summary.setBillingMealRate(round(billingMealRate));
        summary.setMealRate(round(billingMealRate)); // primary displayed meal rate

        double totalMealsCost = totalMessMeals * billingMealRate;
        summary.setTotalMealsCost(round(totalMealsCost));
        summary.setBudgetSurplusDeficit(round(totalMealsCost - rawMealTotal));

        // 3. Shared pool cost per member
        double sharedFoodPerMember = memberCount > 0 ? (sharedFoodTotal / memberCount) : 0.0;
        // Shared utility + shared SMS pool split equally
        double sharedUtilityPerMember = memberCount > 0 ? ((utilityAssetTotal + smsChargeTotal) / memberCount) : 0.0;

        // 4. Per-member balance calculation
        List<MemberBalanceSheet> balanceSheets = new ArrayList<>();
        for (User member : activeMembers) {
            MemberBalanceSheet sheet = new MemberBalanceSheet();
            sheet.setUserId(member.getId());
            sheet.setUserName(member.getName());
            sheet.setUserPhone(member.getPhone());

            double consumedMeals = mealDao.getTotalUserMeals(messId, member.getId(), startDate, endDate);
            sheet.setConsumedMeals(round(consumedMeals));

            // Bill consumed meals at the fixed 70.00 Tk budget rate
            double mealCost = consumedMeals * billingMealRate;
            sheet.setMealCost(round(mealCost));
            sheet.setSharedFoodCost(round(sharedFoodPerMember));
            sheet.setUtilityAssetCost(round(sharedUtilityPerMember));

            double individualCost = expenseDao.getIndividualTargetExpenses(messId, member.getId(), startDate, endDate);
            sheet.setIndividualCost(round(individualCost));

            double totalCost = mealCost + sharedFoodPerMember + sharedUtilityPerMember + individualCost;
            sheet.setTotalCost(round(totalCost));

            double totalDeposit = depositDao.getTotalUserDeposits(messId, member.getId(), startDate, endDate);
            sheet.setTotalDeposit(round(totalDeposit));

            double netBalance = totalDeposit - totalCost;
            sheet.setNetBalance(round(netBalance));

            balanceSheets.add(sheet);
        }
        summary.setMemberBalances(balanceSheets);

        // 5. Total Mess Funds & Cash in Hand
        double totalMessDeposits = depositDao.getTotalMessDeposits(messId, startDate, endDate);
        summary.setTotalMessDeposits(round(totalMessDeposits));
        summary.setMessCashInHand(round(totalMessDeposits - allExpensesTotal));

        return summary;
    }

    public MemberBalanceSheet calculateSingleMemberBalance(long messId, long userId, String startDate, String endDate) {
        CycleSummary summary = calculateCycleSummary(messId, startDate, endDate);
        if (summary.getMemberBalances() != null) {
            for (MemberBalanceSheet sheet : summary.getMemberBalances()) {
                if (sheet.getUserId() == userId) {
                    return sheet;
                }
            }
        }
        MemberBalanceSheet fallback = new MemberBalanceSheet();
        fallback.setUserId(userId);
        return fallback;
    }

    public CycleSummary calculateCurrentCycleSummary(long messId) {
        String startDate = com.smartmess.android.utils.DateTimeUtils.getCurrentMonthStart();
        String endDate = com.smartmess.android.utils.DateTimeUtils.getCurrentMonthEnd();
        return calculateCycleSummary(messId, startDate, endDate);
    }

    public MemberBalanceSheet calculateMemberBalance(long messId, long userId) {
        String startDate = com.smartmess.android.utils.DateTimeUtils.getCurrentMonthStart();
        String endDate = com.smartmess.android.utils.DateTimeUtils.getCurrentMonthEnd();
        return calculateSingleMemberBalance(messId, userId, startDate, endDate);
    }

    public static double round(double value) {
        if (Double.isNaN(value) || Double.isInfinite(value)) {
            return 0.0;
        }
        return BigDecimal.valueOf(value).setScale(2, RoundingMode.HALF_UP).doubleValue();
    }
}

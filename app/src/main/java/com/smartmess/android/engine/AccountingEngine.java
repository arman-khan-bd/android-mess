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

    public AccountingEngine(Context context) {
        DatabaseHelper helper = DatabaseHelper.getInstance(context);
        this.userDao = new UserDao(helper);
        this.mealDao = new MealDao(helper);
        this.expenseDao = new ExpenseDao(helper);
        this.depositDao = new DepositDao(helper);
    }

    public CycleSummary calculateCycleSummary(long messId, String startDate, String endDate) {
        CycleSummary summary = new CycleSummary();
        summary.setStartDate(startDate);
        summary.setEndDate(endDate);

        List<User> activeMembers = userDao.getActiveMembersByMess(messId);
        int memberCount = activeMembers.size();
        summary.setActiveMemberCount(memberCount);

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

        // 2. Meal Counts & Dynamic Meal Rate
        double totalMessMeals = mealDao.getTotalMessMeals(messId, startDate, endDate);
        summary.setTotalMessMeals(round(totalMessMeals));

        double dynamicMealRate = 0.0;
        if (totalMessMeals > 0) {
            dynamicMealRate = rawMealTotal / totalMessMeals;
        }
        summary.setMealRate(round(dynamicMealRate));

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

            double mealCost = consumedMeals * dynamicMealRate;
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

    public static double round(double value) {
        return BigDecimal.valueOf(value).setScale(2, RoundingMode.HALF_UP).doubleValue();
    }
}

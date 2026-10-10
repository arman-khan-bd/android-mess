package com.smartmess.android.engine;

import java.util.List;

public class CalculationModels {

    public static class CycleSummary {
        private String startDate;
        private String endDate;
        private double totalRawMealExpense;      // Variable Meal Pool
        private double totalSharedFoodExpense;   // Shared Fixed Pool (onion, oil, salt, gas)
        private double totalUtilityAssetExpense; // Utility & Asset Pool (rent, maid, wifi)
        private double totalSmsChargeExpense;    // Shared SMS charges
        private double totalAllExpenses;
        private double totalMessMeals;           // Total meals consumed across all members
        private double mealRate;                 // Billing meal rate (e.g. 70.00 Tk budget)
        private double targetMealBudget = 70.0;  // Configured target meal budget
        private double workingMealRate;          // Actual working meal rate = totalRawMealExpense / totalMessMeals
        private double billingMealRate;          // Effective billed rate per meal
        private double totalMealsCost;           // Total cost of all meals = totalMessMeals * billingMealRate
        private double budgetSurplusDeficit;     // totalMealsCost - totalRawMealExpense
        private int activeMemberCount;
        private double totalMessDeposits;
        private double messCashInHand;           // totalMessDeposits - totalAllExpenses
        private List<MemberBalanceSheet> memberBalances;

        public String getStartDate() { return startDate; }
        public void setStartDate(String startDate) { this.startDate = startDate; }

        public String getEndDate() { return endDate; }
        public void setEndDate(String endDate) { this.endDate = endDate; }

        public double getTotalRawMealExpense() { return totalRawMealExpense; }
        public void setTotalRawMealExpense(double totalRawMealExpense) { this.totalRawMealExpense = totalRawMealExpense; }

        public double getTotalSharedFoodExpense() { return totalSharedFoodExpense; }
        public void setTotalSharedFoodExpense(double totalSharedFoodExpense) { this.totalSharedFoodExpense = totalSharedFoodExpense; }

        public double getTotalUtilityAssetExpense() { return totalUtilityAssetExpense; }
        public void setTotalUtilityAssetExpense(double totalUtilityAssetExpense) { this.totalUtilityAssetExpense = totalUtilityAssetExpense; }

        public double getTotalSmsChargeExpense() { return totalSmsChargeExpense; }
        public void setTotalSmsChargeExpense(double totalSmsChargeExpense) { this.totalSmsChargeExpense = totalSmsChargeExpense; }

        public double getTotalAllExpenses() { return totalAllExpenses; }
        public void setTotalAllExpenses(double totalAllExpenses) { this.totalAllExpenses = totalAllExpenses; }

        public double getTotalMessMeals() { return totalMessMeals; }
        public void setTotalMessMeals(double totalMessMeals) { this.totalMessMeals = totalMessMeals; }

        public double getMealRate() { return mealRate; }
        public void setMealRate(double mealRate) { this.mealRate = mealRate; }

        public double getTargetMealBudget() { return targetMealBudget; }
        public void setTargetMealBudget(double targetMealBudget) { this.targetMealBudget = targetMealBudget; }

        public double getWorkingMealRate() { return workingMealRate; }
        public void setWorkingMealRate(double workingMealRate) { this.workingMealRate = workingMealRate; }

        public double getBillingMealRate() { return billingMealRate > 0 ? billingMealRate : mealRate; }
        public void setBillingMealRate(double billingMealRate) { this.billingMealRate = billingMealRate; }

        public double getTotalMealsCost() { return totalMealsCost; }
        public void setTotalMealsCost(double totalMealsCost) { this.totalMealsCost = totalMealsCost; }

        public double getBudgetSurplusDeficit() { return budgetSurplusDeficit; }
        public void setBudgetSurplusDeficit(double budgetSurplusDeficit) { this.budgetSurplusDeficit = budgetSurplusDeficit; }

        public int getActiveMemberCount() { return activeMemberCount; }
        public void setActiveMemberCount(int activeMemberCount) { this.activeMemberCount = activeMemberCount; }

        public double getTotalMessDeposits() { return totalMessDeposits; }
        public void setTotalMessDeposits(double totalMessDeposits) { this.totalMessDeposits = totalMessDeposits; }

        public double getMessCashInHand() { return messCashInHand; }
        public void setMessCashInHand(double messCashInHand) { this.messCashInHand = messCashInHand; }

        public List<MemberBalanceSheet> getMemberBalances() { return memberBalances; }
        public void setMemberBalances(List<MemberBalanceSheet> memberBalances) { this.memberBalances = memberBalances; }

        public double getLiveMealRate() { return mealRate; }
        public double getTotalMeals() { return totalMessMeals; }
        public double getCashInHand() { return messCashInHand; }
        public double getRawMealCost() { return totalRawMealExpense; }
        public double getSharedFoodCost() { return totalSharedFoodExpense; }
        public double getUtilityCost() { return totalUtilityAssetExpense; }
    }

    public static class MemberBalanceSheet {
        private long userId;
        private String userName;
        private String userPhone;
        private double consumedMeals;
        private double mealCost;             // consumedMeals * mealRate
        private double sharedFoodCost;       // totalSharedFoodExpense / activeMemberCount
        private double utilityAssetCost;     // totalUtilityAssetExpense / activeMemberCount
        private double individualCost;       // Specific individual debits (e.g. personal SMS, fines)
        private double totalCost;            // mealCost + sharedFoodCost + utilityAssetCost + individualCost
        private double totalDeposit;
        private double netBalance;           // totalDeposit - totalCost (>=0 credit, <0 due)

        public long getUserId() { return userId; }
        public void setUserId(long userId) { this.userId = userId; }

        public String getUserName() { return userName; }
        public void setUserName(String userName) { this.userName = userName; }

        public String getUserPhone() { return userPhone; }
        public void setUserPhone(String userPhone) { this.userPhone = userPhone; }

        public double getConsumedMeals() { return consumedMeals; }
        public void setConsumedMeals(double consumedMeals) { this.consumedMeals = consumedMeals; }

        public double getMealCost() { return mealCost; }
        public void setMealCost(double mealCost) { this.mealCost = mealCost; }

        public double getSharedFoodCost() { return sharedFoodCost; }
        public void setSharedFoodCost(double sharedFoodCost) { this.sharedFoodCost = sharedFoodCost; }

        public double getUtilityAssetCost() { return utilityAssetCost; }
        public void setUtilityAssetCost(double utilityAssetCost) { this.utilityAssetCost = utilityAssetCost; }

        public double getIndividualCost() { return individualCost; }
        public void setIndividualCost(double individualCost) { this.individualCost = individualCost; }

        public double getTotalCost() { return totalCost; }
        public void setTotalCost(double totalCost) { this.totalCost = totalCost; }

        public double getTotalDeposit() { return totalDeposit; }
        public void setTotalDeposit(double totalDeposit) { this.totalDeposit = totalDeposit; }

        public double getNetBalance() { return netBalance; }
        public void setNetBalance(double netBalance) { this.netBalance = netBalance; }

        public boolean isDue() {
            return netBalance < 0;
        }

        public boolean isOverdue() {
            return isDue();
        }

        public double getDueAmount() {
            return isDue() ? Math.abs(netBalance) : 0.0;
        }
    }
}

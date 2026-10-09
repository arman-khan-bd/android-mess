package com.smartmess.android.engine;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;

import com.smartmess.android.data.local.DatabaseHelper;
import com.smartmess.android.data.local.dao.MessDao;
import com.smartmess.android.data.local.dao.SaasPlanDao;
import com.smartmess.android.data.local.dao.UserDao;
import com.smartmess.android.model.Mess;
import com.smartmess.android.model.SaasPlan;
import com.smartmess.android.ui.settings.SettingsActivity;

public class PlanGateManager {

    private final MessDao messDao;
    private final SaasPlanDao planDao;
    private final UserDao userDao;

    public PlanGateManager(Context context) {
        DatabaseHelper helper = DatabaseHelper.getInstance(context);
        this.messDao = new MessDao(helper);
        this.planDao = new SaasPlanDao(helper);
        this.userDao = new UserDao(helper);
    }

    public SaasPlan getActivePlanForMess(long messId) {
        Mess mess = messDao.getById(messId);
        if (mess != null && mess.getCurrentPlanId() != null) {
            SaasPlan plan = planDao.getById(mess.getCurrentPlanId());
            if (plan != null) return plan;
        }
        // Default Free / Basic Tier
        SaasPlan fallback = new SaasPlan();
        fallback.setName("Basic (Free)");
        fallback.setFeaturesJson("{\"max_members\": 15, \"sms_sim\": true, \"sms_cloud\": false, \"ocr_receipt\": false, \"pdf_branding\": false, \"ad_free\": false}");
        return fallback;
    }

    public boolean canAddMember(long messId) {
        SaasPlan plan = getActivePlanForMess(messId);
        int currentCount = userDao.countActiveMembers(messId);
        return currentCount < plan.getMaxMembers();
    }

    public boolean canUseSimSms(long messId) {
        SaasPlan plan = getActivePlanForMess(messId);
        return plan.isSmsSim();
    }

    public boolean canUseOcr(long messId) {
        SaasPlan plan = getActivePlanForMess(messId);
        return plan.isOcrReceipt();
    }

    public boolean canExportReports(long messId) {
        SaasPlan plan = getActivePlanForMess(messId);
        return plan.isPdfBranding();
    }

    public boolean isAdFree(long messId) {
        SaasPlan plan = getActivePlanForMess(messId);
        return plan.isAdFree();
    }

    public void showUpgradeDialog(Context context, String featureName, String explanation) {
        new AlertDialog.Builder(context)
                .setTitle("Upgrade Required: " + featureName)
                .setMessage(explanation + "\n\nUpgrade your mess plan to unlock higher limits, automated cloud features, and branded exports.")
                .setPositiveButton("View Plans", (dialog, which) -> {
                    Intent intent = new Intent(context, SettingsActivity.class);
                    context.startActivity(intent);
                })
                .setNegativeButton("Cancel", null)
                .show();
    }
}

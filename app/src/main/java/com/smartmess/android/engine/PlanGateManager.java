package com.smartmess.android.engine;

import android.content.Context;
import androidx.fragment.app.FragmentManager;

import com.smartmess.android.data.local.DatabaseHelper;
import com.smartmess.android.data.local.dao.MessDao;
import com.smartmess.android.data.local.dao.SaasPlanDao;
import com.smartmess.android.data.local.dao.UserDao;
import com.smartmess.android.model.Mess;
import com.smartmess.android.model.PlanCapabilities;
import com.smartmess.android.model.SaasPlan;
import com.smartmess.android.ui.dialogs.UpgradeProBottomSheet;
import com.smartmess.android.utils.SessionManager;

/**
 * Client-Side Free vs. Pro Feature Gating & Lock Engine
 */
public class PlanGateManager {

    private final Context context;
    private final MessDao messDao;
    private final SaasPlanDao planDao;
    private final UserDao userDao;
    private final SessionManager sessionManager;

    public PlanGateManager(Context context) {
        this.context = context.getApplicationContext();
        DatabaseHelper helper = DatabaseHelper.getInstance(context);
        this.messDao = new MessDao(helper);
        this.planDao = new SaasPlanDao(helper);
        this.userDao = new UserDao(helper);
        this.sessionManager = new SessionManager(context);
    }

    public PlanCapabilities getCapabilities() {
        return sessionManager.getPlanCapabilities();
    }

    public boolean isPro() {
        return sessionManager.isPro();
    }

    /**
     * Resolves the active SaasPlan for the given mess, falling back to Pro / Free defaults.
     */
    public SaasPlan getActivePlanForMess(long messId) {
        if (messId > 0) {
            Mess mess = messDao.getById(messId);
            if (mess != null && mess.getCurrentPlanId() != null && mess.getCurrentPlanId() > 0) {
                SaasPlan plan = planDao.getById(mess.getCurrentPlanId());
                if (plan != null) {
                    return plan;
                }
            }
        }

        if (sessionManager.isPro()) {
            SaasPlan proPlan = new SaasPlan();
            proPlan.setName("Pro");
            proPlan.setFeaturesJson("{\"max_members\":99999,\"sms_sim\":true,\"sms_cloud\":true,\"ocr_receipt\":true,\"pdf_branding\":true,\"ad_free\":true}");
            return proPlan;
        }

        SaasPlan freePlan = new SaasPlan();
        freePlan.setName("Free");
        freePlan.setFeaturesJson("{\"max_members\":6,\"sms_sim\":true,\"sms_cloud\":false,\"ocr_receipt\":false,\"pdf_branding\":false,\"ad_free\":false}");
        return freePlan;
    }

    public int getMaxAllowedMembers(long messId) {
        SaasPlan activePlan = getActivePlanForMess(messId);
        if (activePlan != null && activePlan.getMaxMembers() > 0) {
            return activePlan.getMaxMembers();
        }
        PlanCapabilities caps = getCapabilities();
        return caps.getMaxMembers();
    }

    /**
     * Enforce Max 6 Members on Free tier, Unlimited on Pro
     */
    public boolean canAddMember(long messId) {
        int currentCount = userDao.countActiveMembers(messId);
        int maxAllowed = getMaxAllowedMembers(messId);
        return currentCount < maxAllowed;
    }

    /**
     * Cloud Sync: Active for all mess members when cloud sync is enabled
     */
    public boolean canUseCloudSync() {
        return true;
    }

    /**
     * Watermark-Free Branded PDF & Excel Exports
     */
    public boolean canExportPdf() {
        return getCapabilities().isPdfExportEnabled();
    }

    /**
     * In-App Bulk Due Reminders & SIM SMS Dispatcher
     */
    public boolean canUseBulkSms() {
        return getCapabilities().isBulkSmsEnabled();
    }

    /**
     * Automated Receipt OCR Scanner
     */
    public boolean canUseOcr() {
        return getCapabilities().isOcrScannerEnabled();
    }

    /**
     * Historical Ledger & Voucher Archive beyond 30 days
     */
    public boolean canViewFullHistory() {
        return getCapabilities().isFullHistoryEnabled();
    }

    /**
     * Priority Support Badge & Queue
     */
    public boolean isPrioritySupport() {
        return getCapabilities().isPrioritySupportEnabled();
    }

    /**
     * Launch Modern Material 3 "Upgrade to Pro" BottomSheet Dialog
     */
    public void showUpgradeBottomSheet(FragmentManager fm, String featureName, String explanation) {
        if (fm != null) {
            UpgradeProBottomSheet sheet = UpgradeProBottomSheet.newInstance(featureName, explanation);
            sheet.show(fm, UpgradeProBottomSheet.TAG);
        }
    }

    public void showUpgradeDialog(Context ctx, String featureName, String explanation) {
        if (ctx instanceof androidx.fragment.app.FragmentActivity) {
            showUpgradeBottomSheet(((androidx.fragment.app.FragmentActivity) ctx).getSupportFragmentManager(), featureName, explanation);
        }
    }
}

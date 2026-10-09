package com.smartmess.android.sms;

import android.content.Context;

import com.smartmess.android.data.local.DatabaseHelper;
import com.smartmess.android.data.local.dao.DepositDao;
import com.smartmess.android.data.local.dao.ExpenseDao;
import com.smartmess.android.data.local.dao.MessDao;
import com.smartmess.android.data.local.dao.SmsLogDao;
import com.smartmess.android.data.local.dao.UserDao;
import com.smartmess.android.model.Deposit;
import com.smartmess.android.model.Expense;
import com.smartmess.android.model.Mess;
import com.smartmess.android.model.SmsLog;
import com.smartmess.android.model.User;
import com.smartmess.android.utils.DateTimeUtils;

import java.util.List;
import java.util.UUID;

public class SmsCostingManager {

    private final Context context;
    private final MessDao messDao;
    private final UserDao userDao;
    private final ExpenseDao expenseDao;
    private final DepositDao depositDao;
    private final SmsLogDao smsLogDao;

    public SmsCostingManager(Context context) {
        this.context = context.getApplicationContext();
        DatabaseHelper helper = DatabaseHelper.getInstance(this.context);
        this.messDao = new MessDao(helper);
        this.userDao = new UserDao(helper);
        this.expenseDao = new ExpenseDao(helper);
        this.depositDao = new DepositDao(helper);
        this.smsLogDao = new SmsLogDao(helper);
    }

    /**
     * Requirement:
     * If sent to a single member (due reminder), the exact configured SMS cost is debited
     * from that member's ledger (expense targeting that member) and credited to the sender's deposit.
     */
    public boolean sendSingleDueReminder(User sender, User targetMember, String messageContent) {
        Mess mess = messDao.getById(sender.getMessId());
        double perSmsCost = mess != null ? mess.getPerSmsCost() : 0.50;

        boolean sent = SmsDispatcher.sendSms(targetMember.getPhone(), messageContent);
        String status = sent ? "sent" : "failed";
        String now = DateTimeUtils.nowIso();
        String today = DateTimeUtils.currentDate();

        // 1. Log in sms_logs
        SmsLog log = new SmsLog(
                UUID.randomUUID().toString(),
                sender.getMessId(),
                sender.getId(),
                targetMember.getPhone(),
                targetMember.getId(),
                messageContent,
                perSmsCost,
                "device_sim",
                status
        );
        log.setCreatedAt(now);
        log.setUpdatedAt(now);
        smsLogDao.insert(log);

        if (sent) {
            // 2. Debit target member: Create individual expense targeting member
            Expense debitExpense = new Expense();
            debitExpense.setUuid(UUID.randomUUID().toString());
            debitExpense.setMessId(sender.getMessId());
            debitExpense.setBuyerUserId(sender.getId());
            debitExpense.setExpenseCategory(Expense.CAT_SMS_CHARGE);
            debitExpense.setAmount(perSmsCost);
            debitExpense.setExpenseDate(today);
            debitExpense.setTitle("SMS Due Reminder Charge (" + targetMember.getName() + ")");
            debitExpense.setSplitType(Expense.SPLIT_INDIVIDUAL);
            debitExpense.setTargetUserId(targetMember.getId());
            debitExpense.setSyncStatus(0);
            debitExpense.setCreatedAt(now);
            debitExpense.setUpdatedAt(now);
            expenseDao.insertOrUpdate(debitExpense);

            // 3. Credit sender's deposit so sender gets reimbursed for paying via their personal SIM
            Deposit creditDeposit = new Deposit();
            creditDeposit.setUuid(UUID.randomUUID().toString());
            creditDeposit.setMessId(sender.getMessId());
            creditDeposit.setUserId(sender.getId());
            creditDeposit.setAmount(perSmsCost);
            creditDeposit.setDepositDate(today);
            creditDeposit.setNote("Reimbursement: Due SMS sent to " + targetMember.getName());
            creditDeposit.setSyncStatus(0);
            creditDeposit.setCreatedAt(now);
            creditDeposit.setUpdatedAt(now);
            depositDao.insertOrUpdate(creditDeposit);
        }

        return sent;
    }

    /**
     * Requirement:
     * If broadcast to all members, total cost is split equally across mess members.
     */
    public int broadcastNoticeToAllMembers(User sender, String messageContent) {
        Mess mess = messDao.getById(sender.getMessId());
        double perSmsCost = mess != null ? mess.getPerSmsCost() : 0.50;
        List<User> members = userDao.getActiveMembersByMess(sender.getMessId());

        int sentCount = 0;
        String now = DateTimeUtils.nowIso();
        String today = DateTimeUtils.currentDate();

        for (User member : members) {
            boolean sent = SmsDispatcher.sendSms(member.getPhone(), messageContent);
            if (sent) sentCount++;

            SmsLog log = new SmsLog(
                    UUID.randomUUID().toString(),
                    sender.getMessId(),
                    sender.getId(),
                    member.getPhone(),
                    member.getId(),
                    messageContent,
                    perSmsCost,
                    "device_sim",
                    sent ? "sent" : "failed"
            );
            log.setCreatedAt(now);
            log.setUpdatedAt(now);
            smsLogDao.insert(log);
        }

        if (sentCount > 0) {
            double totalCost = sentCount * perSmsCost;

            // Debit Mess Pool: Create shared expense split equally among all members
            Expense broadcastExpense = new Expense();
            broadcastExpense.setUuid(UUID.randomUUID().toString());
            broadcastExpense.setMessId(sender.getMessId());
            broadcastExpense.setBuyerUserId(sender.getId());
            broadcastExpense.setExpenseCategory(Expense.CAT_SMS_CHARGE);
            broadcastExpense.setAmount(totalCost);
            broadcastExpense.setExpenseDate(today);
            broadcastExpense.setTitle("Broadcast SMS Notice (" + sentCount + " members)");
            broadcastExpense.setSplitType(Expense.SPLIT_ALL_EQUAL);
            broadcastExpense.setSyncStatus(0);
            broadcastExpense.setCreatedAt(now);
            broadcastExpense.setUpdatedAt(now);
            expenseDao.insertOrUpdate(broadcastExpense);

            // Credit sender's deposit: Sender is reimbursed for the SIM airtime used
            Deposit creditDeposit = new Deposit();
            creditDeposit.setUuid(UUID.randomUUID().toString());
            creditDeposit.setMessId(sender.getMessId());
            creditDeposit.setUserId(sender.getId());
            creditDeposit.setAmount(totalCost);
            creditDeposit.setDepositDate(today);
            creditDeposit.setNote("Reimbursement: Broadcast SMS (" + sentCount + " members)");
            creditDeposit.setSyncStatus(0);
            creditDeposit.setCreatedAt(now);
            creditDeposit.setUpdatedAt(now);
            depositDao.insertOrUpdate(creditDeposit);
        }

        return sentCount;
    }
}

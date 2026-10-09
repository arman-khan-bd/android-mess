package com.smartmess.android.ui.sms;

import android.os.Bundle;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.smartmess.android.R;
import com.smartmess.android.data.local.DatabaseHelper;
import com.smartmess.android.data.local.dao.MessDao;
import com.smartmess.android.data.local.dao.UserDao;
import com.smartmess.android.engine.PlanGateManager;
import com.smartmess.android.model.Mess;
import com.smartmess.android.model.User;
import com.smartmess.android.sms.SmsCostingManager;
import com.smartmess.android.utils.CurrencyUtils;
import com.smartmess.android.utils.PermissionHelper;
import com.smartmess.android.utils.SessionManager;

import java.util.List;

public class SmsBroadcastActivity extends AppCompatActivity {

    private TextView tvActiveMemberCount;
    private TextView tvTotalBroadcastCost;
    private EditText etBroadcastMessage;
    private MaterialButton btnDispatchBroadcast;

    private UserDao userDao;
    private MessDao messDao;
    private SessionManager sessionManager;
    private SmsCostingManager smsCostingManager;
    private PlanGateManager planGateManager;
    private List<User> activeMembers;
    private double perSmsCost = 0.50;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sms_broadcast);

        tvActiveMemberCount = findViewById(R.id.tvActiveMemberCount);
        tvTotalBroadcastCost = findViewById(R.id.tvTotalBroadcastCost);
        etBroadcastMessage = findViewById(R.id.etBroadcastMessage);
        btnDispatchBroadcast = findViewById(R.id.btnDispatchBroadcast);

        DatabaseHelper helper = DatabaseHelper.getInstance(this);
        userDao = new UserDao(helper);
        messDao = new MessDao(helper);
        sessionManager = new SessionManager(this);
        smsCostingManager = new SmsCostingManager(this);
        planGateManager = new PlanGateManager(this);

        loadBroadcastInfo();
        btnDispatchBroadcast.setOnClickListener(v -> dispatchBroadcast());
    }

    private void loadBroadcastInfo() {
        long messId = sessionManager.getMessId();
        Mess mess = messDao.getById(messId);
        perSmsCost = mess != null ? mess.getPerSmsCost() : 0.50;

        activeMembers = userDao.getActiveMembersByMess(messId);
        int count = activeMembers.size();
        double totalCost = count * perSmsCost;

        tvActiveMemberCount.setText("Target: " + count + " Active Members");
        tvTotalBroadcastCost.setText("Total SMS Cost: " + CurrencyUtils.format(totalCost) + " (Split equally among all members)");

        etBroadcastMessage.setText("Mess Notice: Please review your meal and bazar status on the SmartMess app. - " + sessionManager.getMessName());
    }

    private void dispatchBroadcast() {
        if (!planGateManager.canUseBulkSms()) {
            planGateManager.showUpgradeBottomSheet(getSupportFragmentManager(), "Bulk SMS Dispatcher",
                    "Bulk due reminders and automated ledger debits are a Pro subscription feature.");
            return;
        }

        if (!PermissionHelper.hasSmsPermission(this)) {
            PermissionHelper.requestSmsPermission(this);
            return;
        }

        String msg = etBroadcastMessage.getText().toString().trim();
        if (msg.isEmpty()) {
            Toast.makeText(this, "Message cannot be empty", Toast.LENGTH_SHORT).show();
            return;
        }

        User sender = userDao.getById(sessionManager.getUserId());
        if (sender == null) {
            sender = new User("sender", sessionManager.getMessId(), sessionManager.getUserName(), sessionManager.getUserPhone(), sessionManager.getUserRole(), "active");
            sender.setId(sessionManager.getUserId());
        }

        int sent = smsCostingManager.broadcastNoticeToAllMembers(sender, msg);
        if (sent > 0) {
            Toast.makeText(this, "Broadcast sent to " + sent + " members! Total cost split equally across mess members.", Toast.LENGTH_LONG).show();
            finish();
        } else {
            Toast.makeText(this, "Could not send broadcast. Check SIM connectivity.", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PermissionHelper.REQ_SMS && PermissionHelper.hasSmsPermission(this)) {
            dispatchBroadcast();
        }
    }
}

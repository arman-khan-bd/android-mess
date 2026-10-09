package com.smartmess.android.ui.members;

import android.content.Intent;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.smartmess.android.R;
import com.smartmess.android.data.local.DatabaseHelper;
import com.smartmess.android.data.local.dao.UserDao;
import com.smartmess.android.engine.PlanGateManager;
import com.smartmess.android.model.User;
import com.smartmess.android.utils.SessionManager;

import java.util.List;

public class MemberListActivity extends AppCompatActivity {

    private RecyclerView rvMembers;
    private MaterialButton btnAddMember;
    private UserDao userDao;
    private SessionManager sessionManager;
    private PlanGateManager planGateManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_member_list);

        rvMembers = findViewById(R.id.rvMembers);
        btnAddMember = findViewById(R.id.btnAddMember);

        rvMembers.setLayoutManager(new LinearLayoutManager(this));
        DatabaseHelper helper = DatabaseHelper.getInstance(this);
        userDao = new UserDao(helper);
        sessionManager = new SessionManager(this);
        planGateManager = new PlanGateManager(this);

        btnAddMember.setOnClickListener(v -> {
            long messId = sessionManager.getMessId();
            if (!planGateManager.canAddMember(messId)) {
                planGateManager.showUpgradeDialog(this, "Member Limit Reached",
                        "Your current plan has reached its active member limit (" + planGateManager.getActivePlanForMess(messId).getMaxMembers() + " members).");
                return;
            }
            startActivity(new Intent(this, AddMemberActivity.class));
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadMembers();
    }

    private void loadMembers() {
        long messId = sessionManager.getMessId();
        List<User> list = userDao.getActiveMembersByMess(messId);
        MemberAdapter adapter = new MemberAdapter(this, list);
        rvMembers.setAdapter(adapter);
    }
}

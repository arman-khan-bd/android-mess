package com.smartmess.android.ui.members;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
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

public class FragmentMemberList extends Fragment {

    private RecyclerView rvMembers;
    private MaterialButton btnAddMember;
    private TextView tvMemberCountHeader;
    private View bannerMemberLimit;
    private TextView tvMemberLimitText;

    private UserDao userDao;
    private SessionManager sessionManager;
    private PlanGateManager planGateManager;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_member_list, container, false);

        rvMembers = v.findViewById(R.id.rvMembers);
        btnAddMember = v.findViewById(R.id.btnAddMember);
        tvMemberCountHeader = v.findViewById(R.id.tvMemberCountHeader);
        bannerMemberLimit = v.findViewById(R.id.bannerMemberLimit);
        tvMemberLimitText = v.findViewById(R.id.tvMemberLimitText);

        rvMembers.setLayoutManager(new LinearLayoutManager(requireContext()));
        DatabaseHelper helper = DatabaseHelper.getInstance(requireContext());
        userDao = new UserDao(helper);
        sessionManager = new SessionManager(requireContext());
        planGateManager = new PlanGateManager(requireContext());

        MaterialButton btnInviteMember = v.findViewById(R.id.btnInviteMember);
        if (btnInviteMember != null) {
            btnInviteMember.setOnClickListener(view -> startActivity(new Intent(requireContext(), ActivityMessInvite.class)));
        }

        btnAddMember.setOnClickListener(view -> {
            long messId = sessionManager.getMessId();
            if (!planGateManager.canAddMember(messId)) {
                planGateManager.showUpgradeBottomSheet(getParentFragmentManager(), "Member Limit Reached",
                        "Your current plan has reached its active member limit (" + planGateManager.getActivePlanForMess(messId).getMaxMembers() + " members). Upgrade to Pro for unlimited members.");
                return;
            }
            startActivity(new Intent(requireContext(), AddMemberActivity.class));
        });

        bannerMemberLimit.setOnClickListener(view -> {
            planGateManager.showUpgradeBottomSheet(getParentFragmentManager(), "SmartMess Pro",
                    "Upgrade to unlock unlimited active members and automated SMS reminders.");
        });

        loadMembers();
        return v;
    }

    @Override
    public void onResume() {
        super.onResume();
        loadMembers();
    }

    private void loadMembers() {
        if (!isAdded()) return;
        long messId = sessionManager.getMessId();
        List<User> list = userDao.getAllMembersByMess(messId);
        MemberAdapter adapter = new MemberAdapter(requireContext(), list);
        rvMembers.setAdapter(adapter);

        int activeCount = 0;
        for (User u : list) {
            if (u.isActive()) activeCount++;
        }

        tvMemberCountHeader.setText("Mess Members (" + list.size() + " Total • " + activeCount + " Active)");

        if (!planGateManager.isPro()) {
            bannerMemberLimit.setVisibility(View.VISIBLE);
            int max = planGateManager.getActivePlanForMess(messId).getMaxMembers();
            tvMemberLimitText.setText("Free Tier (" + activeCount + "/" + max + " Active) • Tap to unlock Unlimited");
        } else {
            bannerMemberLimit.setVisibility(View.GONE);
        }
    }
}

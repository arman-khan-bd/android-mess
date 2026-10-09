package com.smartmess.android.ui.members;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.smartmess.android.R;
import com.smartmess.android.data.local.DatabaseHelper;
import com.smartmess.android.data.local.dao.MessDao;
import com.smartmess.android.model.Mess;
import com.smartmess.android.utils.QrCodeUtils;
import com.smartmess.android.utils.SessionManager;

public class ActivityMessInvite extends AppCompatActivity {

    private View btnBack;
    private TextView tvMessName;
    private ImageView ivQrCode;
    private TextView tvInviteCode;
    private TextView tvDeepLinkUrl;
    private View layoutCopyCode;
    private MaterialButton btnShareInvite;
    private MaterialButton btnCopyLink;

    private SessionManager sessionManager;
    private MessDao messDao;

    private String inviteCode = "MES123";
    private String messName = "Smart Mess";
    private String deepLink = "https://mess.e-bd.shop/join?code=MES123";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_mess_invite);

        sessionManager = new SessionManager(this);
        messDao = new MessDao(DatabaseHelper.getInstance(this));

        // Deep-link check: supports both query param (?code=ABC123) and path style (/join/ABC123)
        android.net.Uri deepData = getIntent().getData();
        String incomingCode = null;
        if (deepData != null) {
            if (deepData.getQueryParameter("code") != null) {
                incomingCode = deepData.getQueryParameter("code");
            } else if (deepData.getQueryParameter("invite_code") != null) {
                incomingCode = deepData.getQueryParameter("invite_code");
            } else {
                java.util.List<String> segments = deepData.getPathSegments();
                if (segments != null && segments.size() >= 2 && "join".equalsIgnoreCase(segments.get(0))) {
                    incomingCode = segments.get(1);
                } else if (segments != null && segments.size() == 1 && !"join".equalsIgnoreCase(segments.get(0))) {
                    incomingCode = segments.get(0);
                }
            }
        }
        if (incomingCode == null && getIntent().hasExtra("EXTRA_INVITE_CODE")) {
            incomingCode = getIntent().getStringExtra("EXTRA_INVITE_CODE");
        }

        if (incomingCode != null && !incomingCode.trim().isEmpty()) {
            final String cleanIncomingCode = incomingCode.trim().toUpperCase();
            if (!sessionManager.isLoggedIn()) {
                Intent regIntent = new Intent(this, com.smartmess.android.ui.auth.RegisterActivity.class);
                regIntent.putExtra("EXTRA_INVITE_CODE", cleanIncomingCode);
                startActivity(regIntent);
                finish();
                return;
            } else {
                String currentMessCode = sessionManager.getInviteCode();
                if (currentMessCode != null && !cleanIncomingCode.equalsIgnoreCase(currentMessCode.trim())) {
                    new androidx.appcompat.app.AlertDialog.Builder(this)
                            .setTitle("মেসে যোগদানের আমন্ত্রণ")
                            .setMessage("You have been invited to join a different mess with code: " + cleanIncomingCode + ".\n\nWould you like to register or join this new mess?")
                            .setPositiveButton("নতুন মেসে যোগ দিন", (dialog, which) -> {
                                Intent regIntent = new Intent(this, com.smartmess.android.ui.auth.RegisterActivity.class);
                                regIntent.putExtra("EXTRA_INVITE_CODE", cleanIncomingCode);
                                startActivity(regIntent);
                                finish();
                            })
                            .setNegativeButton("এখানেই থাকুন", null)
                            .show();
                }
            }
            inviteCode = cleanIncomingCode;
        }

        initViews();
        loadMessData();
        setupListeners();
    }

    private void initViews() {
        btnBack = findViewById(R.id.btnBack);
        tvMessName = findViewById(R.id.tvMessName);
        ivQrCode = findViewById(R.id.ivQrCode);
        tvInviteCode = findViewById(R.id.tvInviteCode);
        tvDeepLinkUrl = findViewById(R.id.tvDeepLinkUrl);
        layoutCopyCode = findViewById(R.id.layoutCopyCode);
        btnShareInvite = findViewById(R.id.btnShareInvite);
        btnCopyLink = findViewById(R.id.btnCopyLink);

        if (btnBack != null) {
            btnBack.setOnClickListener(v -> finish());
        }
    }

    private void loadMessData() {
        long messId = sessionManager.getMessId();
        Mess mess = messDao.getById(messId);

        if (mess != null) {
            messName = mess.getName();
            if (mess.getInviteCode() != null && !mess.getInviteCode().trim().isEmpty()) {
                inviteCode = mess.getInviteCode().toUpperCase();
            }
        } else {
            String savedCode = sessionManager.getInviteCode();
            if (savedCode != null && !savedCode.trim().isEmpty()) {
                inviteCode = savedCode.toUpperCase();
            }
            String savedName = sessionManager.getMessName();
            if (savedName != null && !savedName.trim().isEmpty()) {
                messName = savedName;
            }
        }

        deepLink = "https://mess.e-bd.shop/join?code=" + inviteCode;

        tvMessName.setText(messName);
        tvInviteCode.setText(inviteCode);
        tvDeepLinkUrl.setText(deepLink);

        // Generate dynamic QR Code (512x512)
        Bitmap qrBitmap = QrCodeUtils.generateQrCode(deepLink, 512, 512);
        if (qrBitmap != null) {
            ivQrCode.setImageBitmap(qrBitmap);
        }
    }

    private void setupListeners() {
        View.OnClickListener copyListener = v -> copyToClipboard("Invite Code", inviteCode);
        if (layoutCopyCode != null) {
            layoutCopyCode.setOnClickListener(copyListener);
        }

        if (btnCopyLink != null) {
            btnCopyLink.setOnClickListener(v -> copyToClipboard("Invite Link", deepLink));
        }

        if (btnShareInvite != null) {
            btnShareInvite.setOnClickListener(v -> shareInviteLink());
        }
    }

    private void shareInviteLink() {
        String shareMessage = "Hey! Join our mess \"" + messName + "\" on SmartMess.\n\n"
                + "👉 Tap link to join: " + deepLink + "\n"
                + "🔑 Or use 6-digit Invite Code: " + inviteCode + "\n\n"
                + "You will get instant live access to daily meals, bazar ledgers, and financial balances.";

        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setType("text/plain");
        shareIntent.putExtra(Intent.EXTRA_SUBJECT, "Join " + messName + " on SmartMess");
        shareIntent.putExtra(Intent.EXTRA_TEXT, shareMessage);

        startActivity(Intent.createChooser(shareIntent, "Share Mess Invitation"));
    }

    private void copyToClipboard(String label, String text) {
        ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        if (clipboard != null) {
            ClipData clip = ClipData.newPlainText(label, text);
            clipboard.setPrimaryClip(clip);
            Toast.makeText(this, label + " copied to clipboard!", Toast.LENGTH_SHORT).show();
        }
    }
}

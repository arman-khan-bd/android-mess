package com.smartmess.android.ui.support;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.smartmess.android.R;
import com.smartmess.android.data.remote.ApiClient;
import com.smartmess.android.data.remote.ApiService;
import com.smartmess.android.data.remote.dto.SendTicketMessageRequest;
import com.smartmess.android.data.remote.dto.TicketDetailResponse;
import com.smartmess.android.data.remote.dto.TicketMessageDto;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ActivityTicketChat extends AppCompatActivity {

    private long ticketId;
    private String ticketNumber;
    private String ticketSubject;
    private String ticketStatus;

    private ImageButton btnBackChat;
    private View btnRefreshChat;
    private ImageView ivRefreshIcon;
    private TextView tvRefreshLabel;
    private TextView tvTicketNumber;
    private TextView tvTicketStatus;
    private TextView tvTicketSubject;
    private RecyclerView rvMessages;
    private EditText etMessageInput;
    private MaterialButton btnSendMessage;

    private ChatAdapter adapter;
    private final List<TicketMessageDto> messageList = new ArrayList<>();
    private boolean isReloading = false;

    private final Handler pollHandler = new Handler(Looper.getMainLooper());
    private final Runnable pollRunnable = new Runnable() {
        @Override
        public void run() {
            if (!isFinishing() && !isDestroyedCompat()) {
                reloadMessagesWithoutLoading(false);
                pollHandler.postDelayed(this, 4000); // Live poll every 4s for instant two-way chat sync
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ticket_chat);

        ticketId = getIntent().getLongExtra("ticket_id", 0);
        ticketNumber = getIntent().getStringExtra("ticket_number");
        ticketSubject = getIntent().getStringExtra("ticket_subject");
        ticketStatus = getIntent().getStringExtra("ticket_status");

        initViews();
        setupHeader();
        setupRecyclerView();
        setupSendButton();
        reloadMessagesWithoutLoading(false);
    }

    private void initViews() {
        btnBackChat = findViewById(R.id.btnBackChat);
        btnRefreshChat = findViewById(R.id.btnRefreshChat);
        ivRefreshIcon = findViewById(R.id.ivRefreshIcon);
        tvRefreshLabel = findViewById(R.id.tvRefreshLabel);
        tvTicketNumber = findViewById(R.id.tvTicketNumber);
        tvTicketStatus = findViewById(R.id.tvTicketStatus);
        tvTicketSubject = findViewById(R.id.tvTicketSubject);
        rvMessages = findViewById(R.id.rvMessages);
        etMessageInput = findViewById(R.id.etMessageInput);
        btnSendMessage = findViewById(R.id.btnSendMessage);

        if (btnBackChat != null) {
            btnBackChat.setOnClickListener(v -> finish());
        }

        if (btnRefreshChat != null) {
            btnRefreshChat.setOnClickListener(v -> {
                if (ivRefreshIcon != null) {
                    ivRefreshIcon.animate().rotationBy(360f).setDuration(500).start();
                }
                reloadMessagesWithoutLoading(true);
            });
        }
    }

    private void setupHeader() {
        if (ticketNumber != null) tvTicketNumber.setText(ticketNumber);
        if (ticketSubject != null) tvTicketSubject.setText(ticketSubject);
        if (ticketStatus != null) updateStatusPill(ticketStatus);
    }

    private void updateStatusPill(String status) {
        if (tvTicketStatus == null || status == null) return;
        tvTicketStatus.setText(status.toUpperCase());
        if ("closed".equalsIgnoreCase(status) || "resolved".equalsIgnoreCase(status)) {
            tvTicketStatus.setBackgroundResource(R.drawable.badge_credit);
            tvTicketStatus.setTextColor(ContextCompat.getColor(this, R.color.credit_green));
        } else if ("answered".equalsIgnoreCase(status)) {
            tvTicketStatus.setBackgroundResource(R.drawable.badge_credit);
            tvTicketStatus.setTextColor(ContextCompat.getColor(this, R.color.primary));
        } else {
            tvTicketStatus.setBackgroundResource(R.drawable.badge_due);
            tvTicketStatus.setTextColor(ContextCompat.getColor(this, R.color.warning_amber));
        }
    }

    private void setupRecyclerView() {
        adapter = new ChatAdapter(messageList);
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        layoutManager.setStackFromEnd(true);
        rvMessages.setLayoutManager(layoutManager);
        rvMessages.setAdapter(adapter);
    }

    private void setupSendButton() {
        btnSendMessage.setOnClickListener(v -> sendMessage());
    }

    @Override
    protected void onResume() {
        super.onResume();
        pollHandler.removeCallbacks(pollRunnable);
        pollHandler.postDelayed(pollRunnable, 4000);
    }

    @Override
    protected void onPause() {
        super.onPause();
        pollHandler.removeCallbacks(pollRunnable);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        pollHandler.removeCallbacks(pollRunnable);
    }

    private boolean isDestroyedCompat() {
        return android.os.Build.VERSION.SDK_INT >= 17 && isDestroyed();
    }

    /**
     * Reload messages silently without screen flicker, without clearing the list,
     * and without disruptive blocking progress dialogs.
     */
    private void reloadMessagesWithoutLoading(boolean showToast) {
        if (ticketId <= 0 || isReloading) return;
        isReloading = true;

        ApiService apiService = ApiClient.getApiService(this);
        apiService.getTicketMessages(ticketId).enqueue(new Callback<TicketDetailResponse>() {
            @Override
            public void onResponse(Call<TicketDetailResponse> call, Response<TicketDetailResponse> response) {
                isReloading = false;
                if (isFinishing() || isDestroyedCompat()) return;

                if (response.isSuccessful() && response.body() != null) {
                    if (response.body().getTicket() != null) {
                        String newStatus = response.body().getTicket().getStatus();
                        if (newStatus != null && !newStatus.equalsIgnoreCase(ticketStatus)) {
                            ticketStatus = newStatus;
                            updateStatusPill(ticketStatus);
                        }
                    }

                    List<TicketMessageDto> incoming = response.body().getMessages();
                    if (incoming != null) {
                        if (messageList.isEmpty()) {
                            messageList.addAll(incoming);
                            adapter.notifyDataSetChanged();
                            if (!messageList.isEmpty()) {
                                rvMessages.scrollToPosition(messageList.size() - 1);
                            }
                            if (showToast) {
                                Toast.makeText(ActivityTicketChat.this, "বার্তা লোড সম্পন্ন", Toast.LENGTH_SHORT).show();
                            }
                        } else {
                            // Find genuinely new messages by comparing message IDs
                            List<TicketMessageDto> newMessages = new ArrayList<>();
                            for (TicketMessageDto inMsg : incoming) {
                                boolean exists = false;
                                for (TicketMessageDto existing : messageList) {
                                    if (existing.getId() != 0 && existing.getId() == inMsg.getId()) {
                                        exists = true;
                                        break;
                                    }
                                }
                                if (!exists) {
                                    newMessages.add(inMsg);
                                }
                            }

                            if (!newMessages.isEmpty()) {
                                int startPos = messageList.size();
                                messageList.addAll(newMessages);
                                adapter.notifyItemRangeInserted(startPos, newMessages.size());
                                rvMessages.smoothScrollToPosition(messageList.size() - 1);
                                if (showToast) {
                                    Toast.makeText(ActivityTicketChat.this, "নতুন " + newMessages.size() + " টি বার্তা এসেছে", Toast.LENGTH_SHORT).show();
                                }
                            } else if (showToast) {
                                Toast.makeText(ActivityTicketChat.this, "সব বার্তা আপ-টু-ডেট", Toast.LENGTH_SHORT).show();
                            }
                        }
                    }
                }
            }

            @Override
            public void onFailure(Call<TicketDetailResponse> call, Throwable t) {
                isReloading = false;
                if (isFinishing() || isDestroyedCompat()) return;
                if (showToast) {
                    Toast.makeText(ActivityTicketChat.this, "বার্তা আপডেট করা সম্ভব হয়নি", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private void sendMessage() {
        String msg = etMessageInput.getText().toString().trim();
        if (msg.isEmpty()) return;

        btnSendMessage.setEnabled(false);
        SendTicketMessageRequest req = new SendTicketMessageRequest(msg);

        // Optimistic display for ultra-smooth user experience without lag
        TicketMessageDto optMsg = new TicketMessageDto();
        optMsg.setId(System.currentTimeMillis());
        optMsg.setMessageBody(msg);
        optMsg.setSenderType("user");
        optMsg.setMe(true);
        optMsg.setSenderName("You");
        optMsg.setCreatedAt(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(new Date()));

        int optIndex = messageList.size();
        messageList.add(optMsg);
        adapter.notifyItemInserted(optIndex);
        rvMessages.smoothScrollToPosition(messageList.size() - 1);
        etMessageInput.setText("");

        ApiService apiService = ApiClient.getApiService(this);
        apiService.sendTicketMessage(ticketId, req).enqueue(new Callback<TicketDetailResponse>() {
            @Override
            public void onResponse(Call<TicketDetailResponse> call, Response<TicketDetailResponse> response) {
                btnSendMessage.setEnabled(true);
                if (response.isSuccessful()) {
                    reloadMessagesWithoutLoading(false);
                } else {
                    Toast.makeText(ActivityTicketChat.this, "বার্তা পাঠাতে সমস্যা হয়েছে", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<TicketDetailResponse> call, Throwable t) {
                btnSendMessage.setEnabled(true);
                Toast.makeText(ActivityTicketChat.this, "নেটওয়ার্ক সমস্যা: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private static class ChatAdapter extends RecyclerView.Adapter<ChatAdapter.ViewHolder> {
        private final List<TicketMessageDto> list;

        public ChatAdapter(List<TicketMessageDto> list) {
            this.list = list;
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_chat_message, parent, false);
            return new ViewHolder(v);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            TicketMessageDto item = list.get(position);
            boolean isMe = item.isMe() || "user".equalsIgnoreCase(item.getSenderType());

            holder.tvMessageBody.setText(item.getMessageBody());
            holder.tvMessageTimestamp.setText(item.getCreatedAt() != null && item.getCreatedAt().length() >= 16 
                    ? item.getCreatedAt().substring(11, 16) : "");

            LinearLayout.LayoutParams params = (LinearLayout.LayoutParams) holder.layoutContainer.getLayoutParams();
            if (isMe) {
                params.gravity = Gravity.END;
                holder.tvSenderLabel.setText("You");
                holder.tvSenderLabel.setTextColor(ContextCompat.getColor(holder.itemView.getContext(), R.color.primary));
            } else {
                params.gravity = Gravity.START;
                holder.tvSenderLabel.setText(item.getSenderName() != null ? item.getSenderName() : "Support Agent");
                holder.tvSenderLabel.setTextColor(ContextCompat.getColor(holder.itemView.getContext(), R.color.accent));
            }
            holder.layoutContainer.setLayoutParams(params);
        }

        @Override
        public int getItemCount() {
            return list.size();
        }

        static class ViewHolder extends RecyclerView.ViewHolder {
            LinearLayout layoutContainer;
            TextView tvSenderLabel, tvMessageBody, tvMessageTimestamp;

            public ViewHolder(@NonNull View itemView) {
                super(itemView);
                layoutContainer = itemView.findViewById(R.id.layoutMessageContainer);
                tvSenderLabel = itemView.findViewById(R.id.tvSenderLabel);
                tvMessageBody = itemView.findViewById(R.id.tvMessageBody);
                tvMessageTimestamp = itemView.findViewById(R.id.tvMessageTimestamp);
            }
        }
    }
}

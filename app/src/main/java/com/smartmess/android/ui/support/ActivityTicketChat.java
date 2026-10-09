package com.smartmess.android.ui.support;

import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
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

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ActivityTicketChat extends AppCompatActivity {

    private long ticketId;
    private String ticketNumber;
    private String ticketSubject;
    private String ticketStatus;

    private TextView tvTicketNumber;
    private TextView tvTicketStatus;
    private TextView tvTicketSubject;
    private RecyclerView rvMessages;
    private EditText etMessageInput;
    private MaterialButton btnSendMessage;

    private ChatAdapter adapter;
    private final List<TicketMessageDto> messageList = new ArrayList<>();

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
        loadMessages();
    }

    private void initViews() {
        tvTicketNumber = findViewById(R.id.tvTicketNumber);
        tvTicketStatus = findViewById(R.id.tvTicketStatus);
        tvTicketSubject = findViewById(R.id.tvTicketSubject);
        rvMessages = findViewById(R.id.rvMessages);
        etMessageInput = findViewById(R.id.etMessageInput);
        btnSendMessage = findViewById(R.id.btnSendMessage);
    }

    private void setupHeader() {
        if (ticketNumber != null) tvTicketNumber.setText(ticketNumber);
        if (ticketSubject != null) tvTicketSubject.setText(ticketSubject);
        if (ticketStatus != null) tvTicketStatus.setText(ticketStatus.toUpperCase());
    }

    private void setupRecyclerView() {
        adapter = new ChatAdapter(messageList);
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        layoutManager.setStackFromEnd(true);
        rvMessages.setLayoutManager(layoutManager);
        rvMessages.setAdapter(adapter);
    }

    private void setupSendButton() {
        btnSendMessage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sendMessage();
            }
        });
    }

    private void loadMessages() {
        ApiService apiService = ApiClient.getApiService(this);
        apiService.getTicketMessages(ticketId).enqueue(new Callback<TicketDetailResponse>() {
            @Override
            public void onResponse(Call<TicketDetailResponse> call, Response<TicketDetailResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    if (response.body().getTicket() != null) {
                        ticketStatus = response.body().getTicket().getStatus();
                        tvTicketStatus.setText(ticketStatus.toUpperCase());
                    }
                    if (response.body().getMessages() != null) {
                        messageList.clear();
                        messageList.addAll(response.body().getMessages());
                        adapter.notifyDataSetChanged();
                        if (!messageList.isEmpty()) {
                            rvMessages.scrollToPosition(messageList.size() - 1);
                        }
                    }
                }
            }

            @Override
            public void onFailure(Call<TicketDetailResponse> call, Throwable t) {
                Toast.makeText(ActivityTicketChat.this, "Offline: Could not sync latest replies", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void sendMessage() {
        String msg = etMessageInput.getText().toString().trim();
        if (msg.isEmpty()) return;

        btnSendMessage.setEnabled(false);
        SendTicketMessageRequest req = new SendTicketMessageRequest(msg);

        ApiService apiService = ApiClient.getApiService(this);
        apiService.sendTicketMessage(ticketId, req).enqueue(new Callback<TicketDetailResponse>() {
            @Override
            public void onResponse(Call<TicketDetailResponse> call, Response<TicketDetailResponse> response) {
                btnSendMessage.setEnabled(true);
                if (response.isSuccessful()) {
                    etMessageInput.setText("");
                    loadMessages();
                } else {
                    Toast.makeText(ActivityTicketChat.this, "Failed to send message", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<TicketDetailResponse> call, Throwable t) {
                btnSendMessage.setEnabled(true);
                Toast.makeText(ActivityTicketChat.this, "Network error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
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

package com.smartmess.android.ui.support;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;
import com.smartmess.android.R;
import com.smartmess.android.data.remote.ApiClient;
import com.smartmess.android.data.remote.ApiService;
import com.smartmess.android.data.remote.dto.CreateTicketRequest;
import com.smartmess.android.data.remote.dto.SupportTicketDto;
import com.smartmess.android.data.remote.dto.TicketDetailResponse;
import com.smartmess.android.data.remote.dto.TicketListResponse;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ActivitySupportTickets extends AppCompatActivity {

    private RecyclerView rvTickets;
    private SwipeRefreshLayout swipeRefresh;
    private View layoutEmpty;
    private ProgressBar progressBar;
    private ExtendedFloatingActionButton fabNewTicket;

    private TicketsAdapter adapter;
    private final List<SupportTicketDto> ticketList = new ArrayList<>();

    private final String[] categoryKeys = {"billing_plan", "sync_issue", "feature_request", "other"};
    private final String[] priorityKeys = {"normal", "urgent"};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_support_tickets);

        initViews();
        setupRecyclerView();
        setupListeners();
        loadTickets();
    }

    private void initViews() {
        rvTickets = findViewById(R.id.rvTickets);
        swipeRefresh = findViewById(R.id.swipeRefresh);
        layoutEmpty = findViewById(R.id.layoutEmpty);
        progressBar = findViewById(R.id.progressBar);
        fabNewTicket = findViewById(R.id.fabNewTicket);
    }

    private void setupRecyclerView() {
        adapter = new TicketsAdapter(ticketList, new OnTicketClickListener() {
            @Override
            public void onTicketClick(SupportTicketDto ticket) {
                Intent intent = new Intent(ActivitySupportTickets.this, ActivityTicketChat.class);
                intent.putExtra("ticket_id", ticket.getId());
                intent.putExtra("ticket_number", ticket.getTicketNumber());
                intent.putExtra("ticket_subject", ticket.getSubject());
                intent.putExtra("ticket_status", ticket.getStatus());
                startActivity(intent);
            }
        });
        rvTickets.setLayoutManager(new LinearLayoutManager(this));
        rvTickets.setAdapter(adapter);
    }

    private void setupListeners() {
        swipeRefresh.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                loadTickets();
            }
        });

        fabNewTicket.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showCreateTicketDialog();
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadTickets();
    }

    private void loadTickets() {
        ApiService apiService = ApiClient.getApiService(this);
        apiService.getTickets().enqueue(new Callback<TicketListResponse>() {
            @Override
            public void onResponse(Call<TicketListResponse> call, Response<TicketListResponse> response) {
                swipeRefresh.setRefreshing(false);
                progressBar.setVisibility(View.GONE);

                if (response.isSuccessful() && response.body() != null) {
                    ticketList.clear();
                    if (response.body().getTickets() != null) {
                        ticketList.addAll(response.body().getTickets());
                    }
                    adapter.notifyDataSetChanged();
                    layoutEmpty.setVisibility(ticketList.isEmpty() ? View.VISIBLE : View.GONE);
                } else {
                    layoutEmpty.setVisibility(ticketList.isEmpty() ? View.VISIBLE : View.GONE);
                }
            }

            @Override
            public void onFailure(Call<TicketListResponse> call, Throwable t) {
                swipeRefresh.setRefreshing(false);
                progressBar.setVisibility(View.GONE);
                layoutEmpty.setVisibility(ticketList.isEmpty() ? View.VISIBLE : View.GONE);
                Toast.makeText(ActivitySupportTickets.this, "Offline: Check internet connection", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void showCreateTicketDialog() {
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_create_ticket, null);
        final Spinner spCategory = dialogView.findViewById(R.id.spCategory);
        final Spinner spPriority = dialogView.findViewById(R.id.spPriority);
        final EditText etSubject = dialogView.findViewById(R.id.etSubject);
        final EditText etMessage = dialogView.findViewById(R.id.etMessage);

        String[] categoryLabels = {"Billing & Subscription", "Sync & Data Issue", "Feature Request", "General Inquiry"};
        ArrayAdapter<String> catAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, categoryLabels);
        spCategory.setAdapter(catAdapter);

        String[] priorityLabels = {"Normal Priority", "Urgent / Critical"};
        ArrayAdapter<String> priAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, priorityLabels);
        spPriority.setAdapter(priAdapter);

        new AlertDialog.Builder(this)
                .setView(dialogView)
                .setPositiveButton("Open Ticket", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        String subject = etSubject.getText().toString().trim();
                        String message = etMessage.getText().toString().trim();

                        if (subject.isEmpty() || message.isEmpty()) {
                            Toast.makeText(ActivitySupportTickets.this, "Subject and message are required", Toast.LENGTH_SHORT).show();
                            return;
                        }

                        int catIdx = spCategory.getSelectedItemPosition();
                        String category = (catIdx >= 0 && catIdx < categoryKeys.length) ? categoryKeys[catIdx] : "other";

                        int priIdx = spPriority.getSelectedItemPosition();
                        String priority = (priIdx >= 0 && priIdx < priorityKeys.length) ? priorityKeys[priIdx] : "normal";

                        CreateTicketRequest req = new CreateTicketRequest(subject, category, priority, message);
                        createTicketOnServer(req);
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void createTicketOnServer(CreateTicketRequest req) {
        progressBar.setVisibility(View.VISIBLE);
        ApiService apiService = ApiClient.getApiService(this);
        apiService.createTicket(req).enqueue(new Callback<TicketDetailResponse>() {
            @Override
            public void onResponse(Call<TicketDetailResponse> call, Response<TicketDetailResponse> response) {
                progressBar.setVisibility(View.GONE);
                if (response.isSuccessful() && response.body() != null && response.body().getTicket() != null) {
                    Toast.makeText(ActivitySupportTickets.this, "Ticket opened successfully!", Toast.LENGTH_SHORT).show();
                    loadTickets();

                    SupportTicketDto newTicket = response.body().getTicket();
                    Intent intent = new Intent(ActivitySupportTickets.this, ActivityTicketChat.class);
                    intent.putExtra("ticket_id", newTicket.getId());
                    intent.putExtra("ticket_number", newTicket.getTicketNumber());
                    intent.putExtra("ticket_subject", newTicket.getSubject());
                    intent.putExtra("ticket_status", newTicket.getStatus());
                    startActivity(intent);
                } else {
                    Toast.makeText(ActivitySupportTickets.this, "Failed to create ticket", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<TicketDetailResponse> call, Throwable t) {
                progressBar.setVisibility(View.GONE);
                Toast.makeText(ActivitySupportTickets.this, "Error connecting to support server", Toast.LENGTH_SHORT).show();
            }
        });
    }

    public interface OnTicketClickListener {
        void onTicketClick(SupportTicketDto ticket);
    }

    private static class TicketsAdapter extends RecyclerView.Adapter<TicketsAdapter.ViewHolder> {
        private final List<SupportTicketDto> list;
        private final OnTicketClickListener listener;

        public TicketsAdapter(List<SupportTicketDto> list, OnTicketClickListener listener) {
            this.list = list;
            this.listener = listener;
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_support_ticket, parent, false);
            return new ViewHolder(v);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            final SupportTicketDto item = list.get(position);
            holder.tvTicketNumber.setText(item.getTicketNumber());
            holder.tvTicketSubject.setText(item.getSubject());
            holder.tvTicketCategory.setText("Category: " + item.getCategory());
            holder.tvTicketStatus.setText(item.getStatus() != null ? item.getStatus().toUpperCase() : "OPEN");
            holder.tvTicketDate.setText(item.getCreatedAt() != null ? item.getCreatedAt().substring(0, 10) : "");

            holder.itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (listener != null) listener.onTicketClick(item);
                }
            });
        }

        @Override
        public int getItemCount() {
            return list.size();
        }

        static class ViewHolder extends RecyclerView.ViewHolder {
            TextView tvTicketNumber, tvTicketStatus, tvTicketSubject, tvTicketCategory, tvTicketDate;

            public ViewHolder(@NonNull View itemView) {
                super(itemView);
                tvTicketNumber = itemView.findViewById(R.id.tvTicketNumber);
                tvTicketStatus = itemView.findViewById(R.id.tvTicketStatus);
                tvTicketSubject = itemView.findViewById(R.id.tvTicketSubject);
                tvTicketCategory = itemView.findViewById(R.id.tvTicketCategory);
                tvTicketDate = itemView.findViewById(R.id.tvTicketDate);
            }
        }
    }
}

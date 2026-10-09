package com.smartmess.android.ui.meals;

import android.app.DatePickerDialog;
import android.content.res.ColorStateList;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.smartmess.android.R;
import com.smartmess.android.data.local.DatabaseHelper;
import com.smartmess.android.data.local.dao.MealDao;
import com.smartmess.android.data.local.dao.MealVacationDao;
import com.smartmess.android.model.MealVacation;
import com.smartmess.android.utils.DateTimeUtils;
import com.smartmess.android.utils.NotificationCenterHelper;
import com.smartmess.android.utils.SessionManager;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

public class ActivityMealVacation extends AppCompatActivity {

    private ImageButton btnBackVacation;
    private View btnSelectStartDate;
    private View btnSelectEndDate;
    private TextView tvStartDate;
    private TextView tvEndDate;
    private EditText etVacationReason;
    private MaterialButton btnScheduleVacation;
    private TextView tvEmptyVacations;
    private RecyclerView rvVacationsList;

    private MealVacationDao vacationDao;
    private MealDao mealDao;
    private SessionManager sessionManager;
    private VacationAdapter adapter;

    private String selectedStartDate;
    private String selectedEndDate;
    private final SimpleDateFormat apiFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
    private final SimpleDateFormat displayFormat = new SimpleDateFormat("dd MMM yyyy", Locale.US);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_meal_vacation);

        DatabaseHelper helper = DatabaseHelper.getInstance(this);
        vacationDao = new MealVacationDao(helper);
        mealDao = new MealDao(helper);
        sessionManager = new SessionManager(this);

        initViews();
        setupDefaultDates();
        loadVacations();
    }

    private void initViews() {
        btnBackVacation = findViewById(R.id.btnBackVacation);
        btnSelectStartDate = findViewById(R.id.btnSelectStartDate);
        btnSelectEndDate = findViewById(R.id.btnSelectEndDate);
        tvStartDate = findViewById(R.id.tvStartDate);
        tvEndDate = findViewById(R.id.tvEndDate);
        etVacationReason = findViewById(R.id.etVacationReason);
        btnScheduleVacation = findViewById(R.id.btnScheduleVacation);
        tvEmptyVacations = findViewById(R.id.tvEmptyVacations);
        rvVacationsList = findViewById(R.id.rvVacationsList);

        btnBackVacation.setOnClickListener(v -> finish());

        rvVacationsList.setLayoutManager(new LinearLayoutManager(this));
        adapter = new VacationAdapter();
        rvVacationsList.setAdapter(adapter);

        btnSelectStartDate.setOnClickListener(v -> showDatePicker(true));
        btnSelectEndDate.setOnClickListener(v -> showDatePicker(false));
        btnScheduleVacation.setOnClickListener(v -> scheduleVacation());
    }

    private void setupDefaultDates() {
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.DAY_OF_YEAR, 1); // Tomorrow
        selectedStartDate = apiFormat.format(cal.getTime());
        tvStartDate.setText(displayFormat.format(cal.getTime()));

        cal.add(Calendar.DAY_OF_YEAR, 6); // 1 week
        selectedEndDate = apiFormat.format(cal.getTime());
        tvEndDate.setText(displayFormat.format(cal.getTime()));
    }

    private void showDatePicker(boolean isStart) {
        Calendar cal = Calendar.getInstance();
        DatePickerDialog dialog = new DatePickerDialog(this, (view, year, month, dayOfMonth) -> {
            cal.set(year, month, dayOfMonth);
            String dateStr = apiFormat.format(cal.getTime());
            String dispStr = displayFormat.format(cal.getTime());

            if (isStart) {
                selectedStartDate = dateStr;
                tvStartDate.setText(dispStr);
            } else {
                selectedEndDate = dateStr;
                tvEndDate.setText(dispStr);
            }
        }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH));

        dialog.show();
    }

    private void scheduleVacation() {
        if (selectedStartDate == null || selectedEndDate == null) {
            Toast.makeText(this, "Please select start and end dates.", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            Date start = apiFormat.parse(selectedStartDate);
            Date end = apiFormat.parse(selectedEndDate);
            if (start != null && end != null && start.after(end)) {
                Toast.makeText(this, "End date cannot be earlier than start date.", Toast.LENGTH_SHORT).show();
                return;
            }
        } catch (Exception ignored) {}

        long messId = sessionManager.getMessId();
        long userId = sessionManager.getUserId();
        String reason = etVacationReason.getText().toString().trim();
        if (reason.isEmpty()) reason = "Vacation / Home Visit";

        MealVacation vacation = new MealVacation(
                UUID.randomUUID().toString(),
                messId,
                userId,
                selectedStartDate,
                selectedEndDate,
                reason
        );

        vacationDao.insertOrUpdate(vacation);

        // Auto-lock meals to 0.0 across all dates in range
        int lockedDays = vacationDao.autoLockMealsForVacation(messId, userId, selectedStartDate, selectedEndDate, mealDao);

        // Post notification into local In-App Notification Center
        String msg = sessionManager.getUserName() + " activated Vacation Mode from "
                + DateTimeUtils.formatDisplayDate(selectedStartDate) + " to "
                + DateTimeUtils.formatDisplayDate(selectedEndDate) + " (" + lockedDays + " meal days auto-locked to 0).";
        NotificationCenterHelper.postNotification(this, messId, "🏖️ Vacation Mode Scheduled", msg, NotificationCenterHelper.TYPE_VACATION);

        Toast.makeText(this, "Vacation mode activated! " + lockedDays + " days auto-locked to 0.0.", Toast.LENGTH_LONG).show();
        etVacationReason.setText("");
        loadVacations();
    }

    private void loadVacations() {
        long messId = sessionManager.getMessId();
        long userId = sessionManager.getUserId();
        List<MealVacation> vacations = vacationDao.getActiveVacationsForUser(messId, userId);

        if (vacations.isEmpty()) {
            tvEmptyVacations.setVisibility(View.VISIBLE);
            rvVacationsList.setVisibility(View.GONE);
        } else {
            tvEmptyVacations.setVisibility(View.GONE);
            rvVacationsList.setVisibility(View.VISIBLE);
            adapter.setList(vacations);
        }
    }

    private class VacationAdapter extends RecyclerView.Adapter<VacationAdapter.ViewHolder> {
        private final List<MealVacation> items = new ArrayList<>();

        public void setList(List<MealVacation> list) {
            items.clear();
            items.addAll(list);
            notifyDataSetChanged();
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_vacation_card, parent, false);
            return new ViewHolder(v);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            MealVacation v = items.get(position);
            holder.tvVacationDates.setText(DateTimeUtils.formatDisplayDate(v.getStartDate()) + " - " + DateTimeUtils.formatDisplayDate(v.getEndDate()));
            holder.tvVacationReason.setText(v.getReason());

            if (v.isActive()) {
                holder.tvVacationStatusBadge.setText("ACTIVE");
                holder.tvVacationStatusBadge.setTextColor(ContextCompat.getColor(holder.itemView.getContext(), R.color.primary));
                holder.btnCancelVacation.setVisibility(View.VISIBLE);
            } else {
                holder.tvVacationStatusBadge.setText("CANCELLED");
                holder.tvVacationStatusBadge.setTextColor(ContextCompat.getColor(holder.itemView.getContext(), R.color.text_muted));
                holder.btnCancelVacation.setVisibility(View.GONE);
            }

            holder.btnCancelVacation.setOnClickListener(view -> {
                new AlertDialog.Builder(ActivityMealVacation.this)
                        .setTitle("Cancel Vacation Mode?")
                        .setMessage("Do you want to cancel this vacation? You will be able to log meals normally again.")
                        .setPositiveButton("Yes, Cancel", (dialog, which) -> {
                            vacationDao.cancelVacation(v.getId());
                            long currentMessId = sessionManager.getMessId();
                            NotificationCenterHelper.postNotification(ActivityMealVacation.this, currentMessId, "Vacation Mode Cancelled",
                                    sessionManager.getUserName() + " cancelled vacation (" + DateTimeUtils.formatDisplayDate(v.getStartDate()) + " to " + DateTimeUtils.formatDisplayDate(v.getEndDate()) + ")",
                                    NotificationCenterHelper.TYPE_VACATION);
                            Toast.makeText(ActivityMealVacation.this, "Vacation schedule cancelled.", Toast.LENGTH_SHORT).show();
                            loadVacations();
                        })
                        .setNegativeButton("Keep Active", null)
                        .show();
            });
        }

        @Override
        public int getItemCount() {
            return items.size();
        }

        class ViewHolder extends RecyclerView.ViewHolder {
            TextView tvVacationDates;
            TextView tvVacationStatusBadge;
            TextView tvVacationReason;
            TextView tvVacationInfo;
            MaterialButton btnCancelVacation;

            ViewHolder(View itemView) {
                super(itemView);
                tvVacationDates = itemView.findViewById(R.id.tvVacationDates);
                tvVacationStatusBadge = itemView.findViewById(R.id.tvVacationStatusBadge);
                tvVacationReason = itemView.findViewById(R.id.tvVacationReason);
                tvVacationInfo = itemView.findViewById(R.id.tvVacationInfo);
                btnCancelVacation = itemView.findViewById(R.id.btnCancelVacation);
            }
        }
    }
}

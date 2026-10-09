package com.smartmess.android.ui.meals;

import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SwitchCompat;

import com.google.android.material.button.MaterialButton;
import com.smartmess.android.R;
import com.smartmess.android.data.local.DatabaseHelper;
import com.smartmess.android.data.local.dao.MealDao;
import com.smartmess.android.data.local.dao.MessDao;
import com.smartmess.android.model.Meal;
import com.smartmess.android.model.Mess;
import com.smartmess.android.utils.DateTimeUtils;
import com.smartmess.android.utils.SessionManager;

import java.util.UUID;

public class DailyMealToggleActivity extends AppCompatActivity {

    private TextView tvTargetDate;
    private TextView tvCutoffStatusText;
    private View layoutCutoffBanner;
    private SwitchCompat switchBreakfast;
    private SwitchCompat switchLunch;
    private SwitchCompat switchDinner;
    private MaterialButton btnSaveDailyToggle;

    private MealDao mealDao;
    private MessDao messDao;
    private SessionManager sessionManager;
    private String tomorrowDate;
    private boolean isLocked = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_daily_toggle);

        tvTargetDate = findViewById(R.id.tvTargetDate);
        tvCutoffStatusText = findViewById(R.id.tvCutoffStatusText);
        layoutCutoffBanner = findViewById(R.id.layoutCutoffBanner);
        switchBreakfast = findViewById(R.id.switchBreakfast);
        switchLunch = findViewById(R.id.switchLunch);
        switchDinner = findViewById(R.id.switchDinner);
        btnSaveDailyToggle = findViewById(R.id.btnSaveDailyToggle);

        DatabaseHelper helper = DatabaseHelper.getInstance(this);
        mealDao = new MealDao(helper);
        messDao = new MessDao(helper);
        sessionManager = new SessionManager(this);

        tomorrowDate = DateTimeUtils.tomorrowDate();
        tvTargetDate.setText("Target Date: Tomorrow (" + DateTimeUtils.formatDisplayDate(tomorrowDate) + ")");

        checkCutoffRule();
        loadExistingMealState();

        btnSaveDailyToggle.setOnClickListener(v -> saveMealToggle());
    }

    private void checkCutoffRule() {
        long messId = sessionManager.getMessId();
        Mess mess = messDao.getById(messId);
        String cutoffTime = mess != null ? mess.getMealCutoffTime() : "22:00:00";

        boolean pastCutoff = DateTimeUtils.isPastCutoffTime(cutoffTime);
        boolean isManager = sessionManager.isManager();

        if (pastCutoff && !isManager) {
            isLocked = true;
            layoutCutoffBanner.setBackgroundColor(getResources().getColor(R.color.due_red_bg));
            tvCutoffStatusText.setText("⛔ Cut-off Time Reached (" + cutoffTime + "). Tomorrow's meals are locked. Contact your manager for adjustments.");
            tvCutoffStatusText.setTextColor(getResources().getColor(R.color.due_red));

            switchBreakfast.setEnabled(false);
            switchLunch.setEnabled(false);
            switchDinner.setEnabled(false);
            btnSaveDailyToggle.setEnabled(false);
        } else {
            isLocked = false;
            tvCutoffStatusText.setText("Active cutoff: " + cutoffTime + ". You can freely toggle your meals before this time.");
        }
    }

    private void loadExistingMealState() {
        long messId = sessionManager.getMessId();
        long userId = sessionManager.getUserId();

        Meal existing = mealDao.getUserMealForDate(messId, userId, tomorrowDate);
        if (existing != null) {
            switchBreakfast.setChecked(existing.getBreakfastCount() > 0);
            switchLunch.setChecked(existing.getLunchCount() > 0);
            switchDinner.setChecked(existing.getDinnerCount() > 0);
        } else {
            // Default on
            switchBreakfast.setChecked(true);
            switchLunch.setChecked(true);
            switchDinner.setChecked(true);
        }
    }

    private void saveMealToggle() {
        if (isLocked) {
            Toast.makeText(this, "Cut-off time has passed. Modifications are locked.", Toast.LENGTH_LONG).show();
            return;
        }

        long messId = sessionManager.getMessId();
        long userId = sessionManager.getUserId();

        double breakfast = switchBreakfast.isChecked() ? 1.0 : 0.0;
        double lunch = switchLunch.isChecked() ? 1.0 : 0.0;
        double dinner = switchDinner.isChecked() ? 1.0 : 0.0;

        String now = DateTimeUtils.nowIso();
        Meal meal = new Meal(
                UUID.randomUUID().toString(),
                messId,
                userId,
                tomorrowDate,
                breakfast,
                lunch,
                dinner,
                0.0
        );
        meal.setCreatedAt(now);
        meal.setUpdatedAt(now);

        mealDao.insertOrUpdate(meal);
        Toast.makeText(this, "Tomorrow's meal preferences updated successfully!", Toast.LENGTH_SHORT).show();
        finish();
    }
}

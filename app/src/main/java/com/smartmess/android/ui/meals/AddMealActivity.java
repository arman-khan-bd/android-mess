package com.smartmess.android.ui.meals;

import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.smartmess.android.R;
import com.smartmess.android.data.local.DatabaseHelper;
import com.smartmess.android.data.local.dao.MealDao;
import com.smartmess.android.data.local.dao.UserDao;
import com.smartmess.android.model.Meal;
import com.smartmess.android.model.User;
import com.smartmess.android.utils.DateTimeUtils;
import com.smartmess.android.utils.SessionManager;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class AddMealActivity extends AppCompatActivity {

    private Spinner spMember;
    private EditText etMealDate;
    private EditText etBreakfast;
    private EditText etLunch;
    private EditText etDinner;
    private EditText etGuestMeal;
    private MaterialButton btnSaveMeal;

    private MealDao mealDao;
    private UserDao userDao;
    private SessionManager sessionManager;
    private List<User> activeMembers = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_meal);

        spMember = findViewById(R.id.spMember);
        etMealDate = findViewById(R.id.etMealDate);
        etBreakfast = findViewById(R.id.etBreakfast);
        etLunch = findViewById(R.id.etLunch);
        etDinner = findViewById(R.id.etDinner);
        etGuestMeal = findViewById(R.id.etGuestMeal);
        btnSaveMeal = findViewById(R.id.btnSaveMeal);

        DatabaseHelper helper = DatabaseHelper.getInstance(this);
        mealDao = new MealDao(helper);
        userDao = new UserDao(helper);
        sessionManager = new SessionManager(this);

        if (!sessionManager.isManager()) {
            Toast.makeText(this, "শুধুমাত্র মেস ম্যানেজার মিলের সংখ্যা যোগ বা পরিবর্তন করতে পারবেন।", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        etMealDate.setText(DateTimeUtils.currentDate());
        loadMembersSpinner();

        btnSaveMeal.setOnClickListener(v -> saveMeal());
    }

    private void loadMembersSpinner() {
        long messId = sessionManager.getMessId();
        activeMembers = userDao.getActiveMembersByMess(messId);
        List<String> names = new ArrayList<>();
        for (User u : activeMembers) {
            names.add(u.getName());
        }

        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, names);
        spMember.setAdapter(adapter);
    }

    private void saveMeal() {
        if (activeMembers.isEmpty()) {
            Toast.makeText(this, "কোনো সক্রিয় সদস্য পাওয়া যায়নি", Toast.LENGTH_SHORT).show();
            return;
        }

        int pos = spMember.getSelectedItemPosition();
        User selectedMember = activeMembers.get(pos);

        String date = etMealDate.getText().toString().trim();
        if (date.isEmpty()) date = DateTimeUtils.currentDate();

        double breakfast = parseDouble(etBreakfast.getText().toString().trim(), 0.0);
        double lunch = parseDouble(etLunch.getText().toString().trim(), 1.0);
        double dinner = parseDouble(etDinner.getText().toString().trim(), 1.0);
        double guest = parseDouble(etGuestMeal.getText().toString().trim(), 0.0);

        String now = DateTimeUtils.nowIso();
        Meal meal = new Meal(
                UUID.randomUUID().toString(),
                sessionManager.getMessId(),
                selectedMember.getId(),
                date,
                breakfast,
                lunch,
                dinner,
                guest
        );
        meal.setCreatedAt(now);
        meal.setUpdatedAt(now);

        mealDao.insertOrUpdate(meal);
        if (sessionManager.isPro()) {
            com.smartmess.android.data.sync.SyncManager.triggerSync(getApplicationContext());
        }
        Toast.makeText(this, "সদস্যের মিল সফলভাবে সংরক্ষিত হয়েছে: " + selectedMember.getName(), Toast.LENGTH_SHORT).show();
        finish();
    }

    private double parseDouble(String str, double def) {
        if (str == null || str.isEmpty()) return def;
        try {
            return Double.parseDouble(str);
        } catch (NumberFormatException e) {
            return def;
        }
    }
}

package com.smartmess.android.ui.meals;

import android.content.Intent;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.smartmess.android.R;
import com.smartmess.android.data.local.DatabaseHelper;
import com.smartmess.android.data.local.dao.MealDao;
import com.smartmess.android.model.Meal;
import com.smartmess.android.utils.SessionManager;

import java.util.List;

public class MealListActivity extends AppCompatActivity {

    private RecyclerView rvMeals;
    private MaterialButton btnAddMeal;
    private MealDao mealDao;
    private SessionManager sessionManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_meal_list);

        rvMeals = findViewById(R.id.rvMeals);
        btnAddMeal = findViewById(R.id.btnAddMeal);

        rvMeals.setLayoutManager(new LinearLayoutManager(this));
        DatabaseHelper helper = DatabaseHelper.getInstance(this);
        mealDao = new MealDao(helper);
        sessionManager = new SessionManager(this);

        btnAddMeal.setOnClickListener(v -> startActivity(new Intent(this, AddMealActivity.class)));
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadMeals();
    }

    private void loadMeals() {
        long messId = sessionManager.getMessId();
        List<Meal> meals = mealDao.getRecentMeals(messId, 50);
        MealAdapter adapter = new MealAdapter(meals);
        rvMeals.setAdapter(adapter);
    }
}

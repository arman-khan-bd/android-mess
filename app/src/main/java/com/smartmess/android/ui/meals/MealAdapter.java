package com.smartmess.android.ui.meals;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.smartmess.android.R;
import com.smartmess.android.model.Meal;

import java.util.List;

public class MealAdapter extends RecyclerView.Adapter<MealAdapter.MealViewHolder> {

    private final List<Meal> meals;

    public MealAdapter(List<Meal> meals) {
        this.meals = meals;
    }

    @NonNull
    @Override
    public MealViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_meal, parent, false);
        return new MealViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull MealViewHolder holder, int position) {
        Meal meal = meals.get(position);
        holder.tvMemberName.setText(meal.getUserName() != null ? meal.getUserName() : "Member #" + meal.getUserId());
        holder.tvMealDate.setText(meal.getMealDate());
        holder.tvBreakfast.setText("B: " + meal.getBreakfastCount());
        holder.tvLunch.setText("L: " + meal.getLunchCount());
        holder.tvDinner.setText("D: " + meal.getDinnerCount());
        holder.tvGuest.setText("G: " + meal.getGuestMealCount());
        holder.tvTotal.setText("Total: " + meal.getTotalMeals());
    }

    @Override
    public int getItemCount() {
        return meals.size();
    }

    static class MealViewHolder extends RecyclerView.ViewHolder {
        TextView tvMemberName, tvMealDate, tvBreakfast, tvLunch, tvDinner, tvGuest, tvTotal;

        public MealViewHolder(@NonNull View itemView) {
            super(itemView);
            tvMemberName = itemView.findViewById(R.id.tvMemberName);
            tvMealDate = itemView.findViewById(R.id.tvMealDate);
            tvBreakfast = itemView.findViewById(R.id.tvBreakfast);
            tvLunch = itemView.findViewById(R.id.tvLunch);
            tvDinner = itemView.findViewById(R.id.tvDinner);
            tvGuest = itemView.findViewById(R.id.tvGuest);
            tvTotal = itemView.findViewById(R.id.tvTotal);
        }
    }
}

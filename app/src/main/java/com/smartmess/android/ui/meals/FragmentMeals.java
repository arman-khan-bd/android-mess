package com.smartmess.android.ui.meals;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * FragmentMeals
 * Enlarged Meals Stepper Fragment with accessible touch targets (48x48dp),
 * 0.5 step Breakfast, 1.0 step Lunch & Dinner, quick preset buttons (0.5, Full, Clear),
 * automated daily cutoff locking, and Vacation Mode integration.
 */
public class FragmentMeals extends FragmentMealSheet {

    public static FragmentMeals newInstance() {
        return new FragmentMeals();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return super.onCreateView(inflater, container, savedInstanceState);
    }
}

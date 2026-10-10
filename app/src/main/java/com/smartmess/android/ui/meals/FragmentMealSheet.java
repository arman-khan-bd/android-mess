package com.smartmess.android.ui.meals;

import android.app.DatePickerDialog;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.smartmess.android.R;
import com.smartmess.android.data.local.DatabaseHelper;
import com.smartmess.android.data.local.dao.MealDao;
import com.smartmess.android.data.local.dao.UserDao;
import com.smartmess.android.data.sync.SyncManager;
import com.smartmess.android.model.Meal;
import com.smartmess.android.model.User;
import com.smartmess.android.utils.DateTimeUtils;
import com.smartmess.android.utils.SessionManager;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

public class FragmentMealSheet extends Fragment {

    private ImageButton btnPrevDay;
    private ImageButton btnNextDay;
    private View layoutDateSelect;
    private TextView tvSelectedDate;
    private TextView tvCutoffStatus;
    private TextView tvAggBreakfast;
    private TextView tvAggLunch;
    private TextView tvAggDinner;
    private TextView tvAggTotal;
    private RecyclerView rvMealSheet;

    private MealDao mealDao;
    private UserDao userDao;
    private com.smartmess.android.data.local.dao.MealVacationDao vacationDao;
    private SessionManager sessionManager;
    private MealGridAdapter adapter;

    private Calendar currentCalendar = Calendar.getInstance();
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
    private final SimpleDateFormat displayFormat = new SimpleDateFormat("EEE, dd MMM yyyy", Locale.US);

    // Debounce runner for background sync
    private final Handler syncDebounceHandler = new Handler(Looper.getMainLooper());
    private Runnable syncDebounceRunnable;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_meal_sheet, container, false);

        DatabaseHelper helper = DatabaseHelper.getInstance(requireContext());
        mealDao = new MealDao(helper);
        userDao = new UserDao(helper);
        vacationDao = new com.smartmess.android.data.local.dao.MealVacationDao(helper);
        sessionManager = new SessionManager(requireContext());

        initViews(view);
        setupDatePickers();
        loadMealsForCurrentDate();

        return view;
    }

    private void initViews(View view) {
        btnPrevDay = view.findViewById(R.id.btnPrevDay);
        btnNextDay = view.findViewById(R.id.btnNextDay);
        layoutDateSelect = view.findViewById(R.id.layoutDateSelect);
        tvSelectedDate = view.findViewById(R.id.tvSelectedDate);
        tvCutoffStatus = view.findViewById(R.id.tvCutoffStatus);
        tvAggBreakfast = view.findViewById(R.id.tvAggBreakfast);
        tvAggLunch = view.findViewById(R.id.tvAggLunch);
        tvAggDinner = view.findViewById(R.id.tvAggDinner);
        tvAggTotal = view.findViewById(R.id.tvAggTotal);
        rvMealSheet = view.findViewById(R.id.rvMealSheet);

        rvMealSheet.setLayoutManager(new LinearLayoutManager(requireContext()));
        adapter = new MealGridAdapter();
        rvMealSheet.setAdapter(adapter);

        btnPrevDay.setOnClickListener(v -> {
            currentCalendar.add(Calendar.DAY_OF_YEAR, -1);
            loadMealsForCurrentDate();
        });

        btnNextDay.setOnClickListener(v -> {
            currentCalendar.add(Calendar.DAY_OF_YEAR, 1);
            loadMealsForCurrentDate();
        });

        layoutDateSelect.setOnClickListener(v -> showDatePicker());

        View fabAddMeal = view.findViewById(R.id.fabAddMeal);
        if (fabAddMeal != null) {
            fabAddMeal.setOnClickListener(v -> {
                android.content.Intent intent = new android.content.Intent(requireContext(), AddMealActivity.class);
                startActivity(intent);
            });
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        loadMealsForCurrentDate();
    }

    private void setupDatePickers() {
        // Init default calendar to today
        currentCalendar = Calendar.getInstance();
    }

    private void showDatePicker() {
        DatePickerDialog dialog = new DatePickerDialog(
                requireContext(),
                (view, year, month, dayOfMonth) -> {
                    currentCalendar.set(Calendar.YEAR, year);
                    currentCalendar.set(Calendar.MONTH, month);
                    currentCalendar.set(Calendar.DAY_OF_MONTH, dayOfMonth);
                    loadMealsForCurrentDate();
                },
                currentCalendar.get(Calendar.YEAR),
                currentCalendar.get(Calendar.MONTH),
                currentCalendar.get(Calendar.DAY_OF_MONTH)
        );
        dialog.show();
    }

    private void loadMealsForCurrentDate() {
        String dateStr = dateFormat.format(currentCalendar.getTime());
        tvSelectedDate.setText(displayFormat.format(currentCalendar.getTime()));

        boolean isPastCutoff = isDateLocked(dateStr);
        if (isPastCutoff) {
            tvCutoffStatus.setText("আজকের মিল বন্ধ (রাত ১০:০০ টার সময়সীমা পার হয়েছে)");
            tvCutoffStatus.setTextColor(androidx.core.content.ContextCompat.getColor(requireContext(), R.color.due_red));
        } else {
            tvCutoffStatus.setText("মিল পরিবর্তনের সুযোগ রয়েছে (রাত ১০:০০ টায় বন্ধ হবে)");
            tvCutoffStatus.setTextColor(androidx.core.content.ContextCompat.getColor(requireContext(), R.color.primary));
        }

        long messId = sessionManager.getMessId();
        List<User> memberList = userDao.getAllMembers(messId);
        List<Meal> existingMeals = mealDao.getMealsForDate(messId, dateStr);

        Map<Long, Meal> mealMap = new HashMap<>();
        for (Meal m : existingMeals) {
            mealMap.put(m.getUserId(), m);
        }

        List<MealItemRow> rowList = new ArrayList<>();
        double totalBf = 0, totalLn = 0, totalDn = 0, totalGuest = 0;

        for (User u : memberList) {
            boolean onVacation = vacationDao != null && vacationDao.isUserOnVacation(messId, u.getId(), dateStr);
            Meal m = mealMap.get(u.getId());
            if (m == null) {
                m = new Meal();
                m.setUuid(UUID.randomUUID().toString());
                m.setMessId(messId);
                m.setUserId(u.getId());
                m.setMealDate(dateStr);
                m.setBreakfastCount(0.0);
                m.setLunchCount(onVacation ? 0.0 : 1.0); // 0.0 if on vacation
                m.setDinnerCount(onVacation ? 0.0 : 1.0);
                m.setGuestMealCount(0.0);
                m.setIsLocked((isPastCutoff || onVacation) ? 1 : 0);
                m.setSyncStatus(0);
                m.setCreatedAt(DateTimeUtils.getCurrentDateTime());
                m.setUpdatedAt(DateTimeUtils.getCurrentDateTime());
                mealDao.insertOrUpdate(m);
            }

            totalBf += m.getBreakfastCount();
            totalLn += m.getLunchCount();
            totalDn += m.getDinnerCount();
            totalGuest += m.getGuestMealCount();

            rowList.add(new MealItemRow(u, m, isPastCutoff || onVacation, onVacation));
        }

        // Update aggregate header text
        tvAggBreakfast.setText(String.format(Locale.US, "সকাল: %.1f", totalBf));
        tvAggLunch.setText(String.format(Locale.US, "দুপুর: %.1f", totalLn));
        tvAggDinner.setText(String.format(Locale.US, "রাত: %.1f", totalDn));
        tvAggTotal.setText(String.format(Locale.US, "মোট: %.1f", (totalBf + totalLn + totalDn + totalGuest)));

        adapter.setItems(rowList);
    }

    private boolean isDateLocked(String dateStr) {
        String today = dateFormat.format(new Date());
        if (dateStr.compareTo(today) < 0) {
            return true; // Past days are locked
        }
        if (dateStr.compareTo(today) > 0) {
            return false; // Future days open
        }
        // If today, check if past 22:00:00
        Calendar now = Calendar.getInstance();
        return now.get(Calendar.HOUR_OF_DAY) >= 22;
    }

    private void scheduleDebouncedSync() {
        if (syncDebounceRunnable != null) {
            syncDebounceHandler.removeCallbacks(syncDebounceRunnable);
        }
        syncDebounceRunnable = () -> {
            if (getContext() != null) {
                SyncManager.triggerSync(requireContext().getApplicationContext());
            }
        };
        syncDebounceHandler.postDelayed(syncDebounceRunnable, 3000); // 3 seconds debounce
    }

    // ==========================================
    // RECYCLERVIEW ADAPTER & VIEW HOLDER
    // ==========================================
    public static class MealItemRow {
        public final User user;
        public final Meal meal;
        public final boolean isLocked;
        public final boolean isOnVacation;

        public MealItemRow(User user, Meal meal, boolean isLocked, boolean isOnVacation) {
            this.user = user;
            this.meal = meal;
            this.isLocked = isLocked;
            this.isOnVacation = isOnVacation;
        }

        public MealItemRow(User user, Meal meal, boolean isLocked) {
            this(user, meal, isLocked, false);
        }
    }

    private class MealGridAdapter extends RecyclerView.Adapter<MealGridAdapter.ViewHolder> {
        private final List<MealItemRow> items = new ArrayList<>();

        public void setItems(List<MealItemRow> newItems) {
            items.clear();
            items.addAll(newItems);
            notifyDataSetChanged();
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_member_meal, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            MealItemRow item = items.get(position);
            holder.bind(item);
        }

        @Override
        public int getItemCount() {
            return items.size();
        }

        class ViewHolder extends RecyclerView.ViewHolder {
            TextView tvAvatar, tvMemberName, tvMemberSubtext, tvDailyTotal, tvLockBadge;
            TextView tvBreakfastCount, tvLunchCount, tvDinnerCount, tvGuestCount;
            ImageButton btnDecBreakfast, btnIncBreakfast;
            ImageButton btnDecLunch, btnIncLunch;
            ImageButton btnDecDinner, btnIncDinner;
            ImageButton btnDecGuest, btnIncGuest;
            MaterialButton btnPresetHalf, btnPresetFull, btnPresetClear;

            public ViewHolder(@NonNull View v) {
                super(v);
                tvAvatar = v.findViewById(R.id.tvAvatar);
                tvMemberName = v.findViewById(R.id.tvMemberName);
                tvMemberSubtext = v.findViewById(R.id.tvMemberSubtext);
                tvDailyTotal = v.findViewById(R.id.tvDailyTotal);
                tvLockBadge = v.findViewById(R.id.tvLockBadge);

                tvBreakfastCount = v.findViewById(R.id.tvBreakfastCount);
                tvLunchCount = v.findViewById(R.id.tvLunchCount);
                tvDinnerCount = v.findViewById(R.id.tvDinnerCount);
                tvGuestCount = v.findViewById(R.id.tvGuestCount);

                btnDecBreakfast = v.findViewById(R.id.btnDecBreakfast);
                btnIncBreakfast = v.findViewById(R.id.btnIncBreakfast);
                btnDecLunch = v.findViewById(R.id.btnDecLunch);
                btnIncLunch = v.findViewById(R.id.btnIncLunch);
                btnDecDinner = v.findViewById(R.id.btnDecDinner);
                btnIncDinner = v.findViewById(R.id.btnIncDinner);
                btnDecGuest = v.findViewById(R.id.btnDecGuest);
                btnIncGuest = v.findViewById(R.id.btnIncGuest);

                btnPresetHalf = v.findViewById(R.id.btnPresetHalf);
                btnPresetFull = v.findViewById(R.id.btnPresetFull);
                btnPresetClear = v.findViewById(R.id.btnPresetClear);
            }

            public void bind(MealItemRow row) {
                User user = row.user;
                Meal meal = row.meal;
                boolean isManager = sessionManager.isManager();
                // Managers can edit meals only if member is NOT on vacation
                boolean canEdit = isManager && !row.isOnVacation;

                tvMemberName.setText(user.getName());
                String initial = (user.getName() != null && !user.getName().isEmpty())
                        ? String.valueOf(user.getName().charAt(0)).toUpperCase(Locale.ROOT)
                        : "M";
                tvAvatar.setText(initial);
                if (row.isOnVacation) {
                    tvMemberSubtext.setText("🏖️ ছুটিতে আছেন (ভ্যাকেশন মোড) • মিল স্বয়ংক্রিয়ভাবে বন্ধ (০.০)");
                    tvLockBadge.setText("ছুটিতে (ভ্যাকেশন)");
                    tvLockBadge.setVisibility(View.VISIBLE);
                } else if (!isManager) {
                    tvMemberSubtext.setText("রুম: " + (user.getRole() != null ? user.getRole() : "Member") + " • " + user.getPhone());
                    tvLockBadge.setText("শুধুমাত্র দেখার অনুমতি");
                    tvLockBadge.setVisibility(View.VISIBLE);
                } else {
                    tvMemberSubtext.setText("রুম: " + (user.getRole() != null ? user.getRole() : "Member") + " • " + user.getPhone());
                    if (row.isLocked) {
                        tvLockBadge.setText("ম্যানেজার এডিট");
                        tvLockBadge.setVisibility(View.VISIBLE);
                    } else {
                        tvLockBadge.setVisibility(View.GONE);
                    }
                }

                updateCountsDisplay(meal);

                // Enable/disable stepper clicks based strictly on manager permission and vacation status
                btnDecBreakfast.setEnabled(canEdit);
                btnIncBreakfast.setEnabled(canEdit);
                btnDecLunch.setEnabled(canEdit);
                btnIncLunch.setEnabled(canEdit);
                btnDecDinner.setEnabled(canEdit);
                btnIncDinner.setEnabled(canEdit);
                btnDecGuest.setEnabled(canEdit);
                btnIncGuest.setEnabled(canEdit);
                if (btnPresetHalf != null) btnPresetHalf.setEnabled(canEdit);
                if (btnPresetFull != null) btnPresetFull.setEnabled(canEdit);
                if (btnPresetClear != null) btnPresetClear.setEnabled(canEdit);

                if (row.isOnVacation) {
                    View.OnClickListener vacationNotice = v -> Toast.makeText(itemView.getContext(),
                            "সদস্য ছুটিতে আছেন (ভ্যাকেশন মোড)। এই তারিখে মিল পরিবর্তন করা যাবে না।", Toast.LENGTH_SHORT).show();
                    itemView.setOnClickListener(vacationNotice);
                } else if (!isManager) {
                    View.OnClickListener memberNotice = v -> Toast.makeText(itemView.getContext(),
                            "শুধুমাত্র মেস ম্যানেজার মিলের সংখ্যা পরিবর্তন করতে পারবেন।", Toast.LENGTH_SHORT).show();
                    itemView.setOnClickListener(memberNotice);
                } else {
                    itemView.setOnClickListener(null);
                }

                // Quick Presets: 0.5 (নাস্তা), 1.0 (পূর্ণ মিল), and Clear (0.0)
                if (btnPresetHalf != null) {
                    btnPresetHalf.setOnClickListener(v -> {
                        if (!canEdit) {
                            showEditBlockedNotice(row);
                            return;
                        }
                        meal.setBreakfastCount(0.5);
                        meal.setLunchCount(0.0);
                        meal.setDinnerCount(0.0);
                        meal.setGuestMealCount(0.0);
                        saveMeal(meal, row);
                    });
                }

                if (btnPresetFull != null) {
                    btnPresetFull.setOnClickListener(v -> {
                        if (!canEdit) {
                            showEditBlockedNotice(row);
                            return;
                        }
                        if (meal.getLunchCount() == 1.0 && meal.getDinnerCount() == 0.0) {
                            meal.setDinnerCount(1.0);
                        } else {
                            meal.setBreakfastCount(0.0);
                            meal.setLunchCount(1.0);
                            meal.setDinnerCount(0.0);
                            meal.setGuestMealCount(0.0);
                        }
                        saveMeal(meal, row);
                    });
                }

                if (btnPresetClear != null) {
                    btnPresetClear.setOnClickListener(v -> {
                        if (!canEdit) {
                            showEditBlockedNotice(row);
                            return;
                        }
                        meal.setBreakfastCount(0.0);
                        meal.setLunchCount(0.0);
                        meal.setDinnerCount(0.0);
                        meal.setGuestMealCount(0.0);
                        saveMeal(meal, row);
                    });
                }

                // 1. Breakfast (0.5 step)
                btnDecBreakfast.setOnClickListener(v -> {
                    if (!canEdit) {
                        showEditBlockedNotice(row);
                        return;
                    }
                    if (meal.getBreakfastCount() >= 0.5) {
                        meal.setBreakfastCount(meal.getBreakfastCount() - 0.5);
                        saveMeal(meal, row);
                    }
                });
                btnIncBreakfast.setOnClickListener(v -> {
                    if (!canEdit) {
                        showEditBlockedNotice(row);
                        return;
                    }
                    meal.setBreakfastCount(meal.getBreakfastCount() + 0.5);
                    saveMeal(meal, row);
                });

                // 2. Lunch (1.0 step)
                btnDecLunch.setOnClickListener(v -> {
                    if (!canEdit) {
                        showEditBlockedNotice(row);
                        return;
                    }
                    if (meal.getLunchCount() >= 1.0) {
                        meal.setLunchCount(meal.getLunchCount() - 1.0);
                        saveMeal(meal, row);
                    }
                });
                btnIncLunch.setOnClickListener(v -> {
                    if (!canEdit) {
                        showEditBlockedNotice(row);
                        return;
                    }
                    meal.setLunchCount(meal.getLunchCount() + 1.0);
                    saveMeal(meal, row);
                });

                // 3. Dinner (1.0 step)
                btnDecDinner.setOnClickListener(v -> {
                    if (!canEdit) {
                        showEditBlockedNotice(row);
                        return;
                    }
                    if (meal.getDinnerCount() >= 1.0) {
                        meal.setDinnerCount(meal.getDinnerCount() - 1.0);
                        saveMeal(meal, row);
                    }
                });
                btnIncDinner.setOnClickListener(v -> {
                    if (!canEdit) {
                        showEditBlockedNotice(row);
                        return;
                    }
                    meal.setDinnerCount(meal.getDinnerCount() + 1.0);
                    saveMeal(meal, row);
                });

                // 4. Guest (1.0 step)
                btnDecGuest.setOnClickListener(v -> {
                    if (!canEdit) {
                        showEditBlockedNotice(row);
                        return;
                    }
                    if (meal.getGuestMealCount() >= 1.0) {
                        meal.setGuestMealCount(meal.getGuestMealCount() - 1.0);
                        saveMeal(meal, row);
                    }
                });
                btnIncGuest.setOnClickListener(v -> {
                    if (!canEdit) {
                        showEditBlockedNotice(row);
                        return;
                    }
                    meal.setGuestMealCount(meal.getGuestMealCount() + 1.0);
                    saveMeal(meal, row);
                });
            }

            private void showEditBlockedNotice(MealItemRow row) {
                if (row.isOnVacation) {
                    Toast.makeText(itemView.getContext(),
                            "সদস্য ছুটিতে আছেন (ভ্যাকেশন মোড)। এই তারিখে মিল পরিবর্তন করা যাবে না।", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(itemView.getContext(),
                            "শুধুমাত্র মেস ম্যানেজার মিলের সংখ্যা পরিবর্তন করতে পারবেন।", Toast.LENGTH_SHORT).show();
                }
            }

            private void saveMeal(Meal meal, MealItemRow row) {
                if (row.isOnVacation || (vacationDao != null && vacationDao.isUserOnVacation(sessionManager.getMessId(), meal.getUserId(), meal.getMealDate()))) {
                    Toast.makeText(itemView.getContext(),
                            "সদস্য ছুটিতে (ভ্যাকেশন মোড) আছেন। এই তারিখে মিল এডিট করা যাবে না।", Toast.LENGTH_SHORT).show();
                    return;
                }
                meal.setSyncStatus(0);
                meal.setUpdatedAt(DateTimeUtils.getCurrentDateTime());
                mealDao.insertOrUpdate(meal);
                updateCountsDisplay(meal);
                loadMealsForCurrentDate(); // recalculates aggregate totals

                // Auto store on database with cloud if Pro subscription
                if (sessionManager.isPro() && getContext() != null) {
                    SyncManager.triggerSync(requireContext().getApplicationContext());
                } else {
                    scheduleDebouncedSync();
                }
            }

            private void updateCountsDisplay(Meal meal) {
                tvBreakfastCount.setText(String.format(Locale.US, "%.1f", meal.getBreakfastCount()));
                tvLunchCount.setText(String.format(Locale.US, "%.1f", meal.getLunchCount()));
                tvDinnerCount.setText(String.format(Locale.US, "%.1f", meal.getDinnerCount()));
                tvGuestCount.setText(String.format(Locale.US, "%.1f", meal.getGuestMealCount()));

                double dailySum = meal.getBreakfastCount() + meal.getLunchCount() + meal.getDinnerCount() + meal.getGuestMealCount();
                tvDailyTotal.setText(String.format(Locale.US, "মোট: %.1f", dailySum));
            }
        }
    }
}

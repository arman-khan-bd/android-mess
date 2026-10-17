package com.smartmess.android.ui.meals;

import android.app.DatePickerDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import android.text.Editable;
import android.text.TextWatcher;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.checkbox.MaterialCheckBox;
import com.smartmess.android.R;
import com.smartmess.android.data.local.DatabaseHelper;
import com.smartmess.android.data.local.dao.AppNotificationDao;
import com.smartmess.android.data.local.dao.BazarSessionDao;
import com.smartmess.android.data.local.dao.ExpenseDao;
import com.smartmess.android.data.local.dao.ItemizedConsumptionDao;
import com.smartmess.android.data.local.dao.MealDao;
import com.smartmess.android.data.local.dao.MealRequestDao;
import com.smartmess.android.data.local.dao.MealVacationDao;
import com.smartmess.android.data.local.dao.UserDao;
import com.smartmess.android.data.sync.SyncManager;
import com.smartmess.android.model.AppNotification;
import com.smartmess.android.model.BazarSession;
import com.smartmess.android.model.Expense;
import com.smartmess.android.model.ItemizedConsumptionLog;
import com.smartmess.android.model.Meal;
import com.smartmess.android.model.MealRequest;
import com.smartmess.android.model.User;
import com.smartmess.android.utils.CurrencyUtils;
import com.smartmess.android.utils.DateTimeUtils;
import com.smartmess.android.utils.NotificationCenterHelper;
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

public class AddMealActivity extends AppCompatActivity {

    // Top Navigation & Tabs
    private ImageButton btnBack;
    private TextView tvScreenTitle;
    private TextView btnTabAddMeal;
    private TextView btnTabMealRequest;

    // Tab 1 Views
    private View layoutAddMealTab;
    private Spinner spMemberSelection;
    private View layoutDatePicker;
    private TextView tvSelectedMealDate;
    private ImageButton btnAutoResetMeals;
    private RecyclerView rvBulkMembers;
    private MaterialButton btnConfirmMeals;

    // Flexible Meal Views
    private MaterialCheckBox cbFlexibleMeal;
    private View layoutFlexibleOptions;
    private Spinner spLinkedBazarSession;
    private Spinner spFlexibleMealType;
    private Spinner spRawBazarItem;
    private EditText etCustomRawBazar;
    private EditText etRawMealAmount;
    private TextView tvSharedPricePreview;
    private ExpenseDao expenseDao;
    private BazarSessionDao bazarSessionDao;
    private ItemizedConsumptionDao itemizedConsumptionDao;

    private boolean isFlexibleMeal = false;
    private String selectedMealType = "lunch";
    private String selectedRawBazarName = "গরুর মাংস (Beef)";
    private double currentRawMealAmount = 0.0;
    private double calculatedPerMealRate = 0.0;
    private int activeDinersCount = 0;
    private String selectedBazarSessionUuid = null;
    private final List<BazarSession> linkedBazarSessionList = new ArrayList<>();
    private final List<String> rawBazarOptions = new ArrayList<>();

    // Tab 2 Views (Meal Request)
    private View layoutMealRequestTab;
    private View layoutReqDatePicker;
    private TextView tvReqSelectedDate;
    private ImageButton btnReqMinusBreakfast, btnReqPlusBreakfast;
    private TextView tvReqBreakfastCount;
    private ImageButton btnReqMinusLunch, btnReqPlusLunch;
    private TextView tvReqLunchCount;
    private ImageButton btnReqMinusDinner, btnReqPlusDinner;
    private TextView tvReqDinnerCount;
    private ImageButton btnReqMinusGuest, btnReqPlusGuest;
    private TextView tvReqGuestCount;
    private EditText etReqNote;
    private MaterialButton btnSubmitRequest;
    private TextView tvRequestsHeader;
    private TextView tvEmptyRequests;
    private RecyclerView rvMealRequests;

    // DAOs & Helpers
    private MealDao mealDao;
    private UserDao userDao;
    private MealVacationDao vacationDao;
    private MealRequestDao mealRequestDao;
    private AppNotificationDao notificationDao;
    private SessionManager sessionManager;

    // State Variables
    private long currentMessId;
    private long currentUserId;
    private boolean isManager;
    private Calendar selectedMealCalendar = Calendar.getInstance();
    private Calendar selectedReqCalendar = Calendar.getInstance();
    private final SimpleDateFormat isoFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
    private final SimpleDateFormat displayFormat = new SimpleDateFormat("dd MMM yyyy", Locale.US);

    private List<User> allActiveMembers = new ArrayList<>();
    private List<User> displayedMembers = new ArrayList<>();
    private final Map<Long, MemberMealState> memberMealMap = new HashMap<>();
    private BulkMealAdapter bulkMealAdapter;

    // Tab 2 Stepper State
    private double reqBreakfast = 0.0;
    private double reqLunch = 1.0;
    private double reqDinner = 1.0;
    private double reqGuest = 0.0;
    private final List<MealRequest> mealRequestsList = new ArrayList<>();
    private MealRequestAdapter mealRequestAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_meal);

        DatabaseHelper helper = DatabaseHelper.getInstance(this);
        mealDao = new MealDao(helper);
        userDao = new UserDao(helper);
        vacationDao = new MealVacationDao(helper);
        mealRequestDao = new MealRequestDao(helper);
        notificationDao = new AppNotificationDao(helper);
        expenseDao = new ExpenseDao(helper);
        bazarSessionDao = new BazarSessionDao(this);
        itemizedConsumptionDao = new ItemizedConsumptionDao(this);
        sessionManager = new SessionManager(this);

        currentMessId = sessionManager.getMessId();
        currentUserId = sessionManager.getUserId();
        isManager = sessionManager.isManager();

        initViews();
        setupTabs();
        setupDatePickers();
        setupBulkMealRecycler();
        setupFlexibleMealSection();
        setupRequestSteppers();
        setupRequestRecycler();

        loadMembersAndInitializeMeals();
        loadMealRequests();

        // Check if intent specifies to open the request tab directly
        if (getIntent().getBooleanExtra("open_request_tab", false)) {
            switchToMealRequestTab();
        }
    }

    private void initViews() {
        btnBack = findViewById(R.id.btnBack);
        tvScreenTitle = findViewById(R.id.tvScreenTitle);
        btnTabAddMeal = findViewById(R.id.btnTabAddMeal);
        btnTabMealRequest = findViewById(R.id.btnTabMealRequest);

        // Tab 1 Views
        layoutAddMealTab = findViewById(R.id.layoutAddMealTab);
        spMemberSelection = findViewById(R.id.spMemberSelection);
        layoutDatePicker = findViewById(R.id.layoutDatePicker);
        tvSelectedMealDate = findViewById(R.id.tvSelectedMealDate);
        btnAutoResetMeals = findViewById(R.id.btnAutoResetMeals);
        rvBulkMembers = findViewById(R.id.rvBulkMembers);
        btnConfirmMeals = findViewById(R.id.btnConfirmMeals);

        // Flexible Meal Views
        cbFlexibleMeal = findViewById(R.id.cbFlexibleMeal);
        layoutFlexibleOptions = findViewById(R.id.layoutFlexibleOptions);
        spLinkedBazarSession = findViewById(R.id.spLinkedBazarSession);
        spFlexibleMealType = findViewById(R.id.spFlexibleMealType);
        spRawBazarItem = findViewById(R.id.spRawBazarItem);
        etCustomRawBazar = findViewById(R.id.etCustomRawBazar);
        etRawMealAmount = findViewById(R.id.etRawMealAmount);
        tvSharedPricePreview = findViewById(R.id.tvSharedPricePreview);

        // Tab 2 Views
        layoutMealRequestTab = findViewById(R.id.layoutMealRequestTab);
        layoutReqDatePicker = findViewById(R.id.layoutReqDatePicker);
        tvReqSelectedDate = findViewById(R.id.tvReqSelectedDate);
        btnReqMinusBreakfast = findViewById(R.id.btnReqMinusBreakfast);
        btnReqPlusBreakfast = findViewById(R.id.btnReqPlusBreakfast);
        tvReqBreakfastCount = findViewById(R.id.tvReqBreakfastCount);
        btnReqMinusLunch = findViewById(R.id.btnReqMinusLunch);
        btnReqPlusLunch = findViewById(R.id.btnReqPlusLunch);
        tvReqLunchCount = findViewById(R.id.tvReqLunchCount);
        btnReqMinusDinner = findViewById(R.id.btnReqMinusDinner);
        btnReqPlusDinner = findViewById(R.id.btnReqPlusDinner);
        tvReqDinnerCount = findViewById(R.id.tvReqDinnerCount);
        btnReqMinusGuest = findViewById(R.id.btnReqMinusGuest);
        btnReqPlusGuest = findViewById(R.id.btnReqPlusGuest);
        tvReqGuestCount = findViewById(R.id.tvReqGuestCount);
        etReqNote = findViewById(R.id.etReqNote);
        btnSubmitRequest = findViewById(R.id.btnSubmitRequest);
        tvRequestsHeader = findViewById(R.id.tvRequestsHeader);
        tvEmptyRequests = findViewById(R.id.tvEmptyRequests);
        rvMealRequests = findViewById(R.id.rvMealRequests);

        btnBack.setOnClickListener(v -> finish());
    }

    private void setupTabs() {
        btnTabAddMeal.setOnClickListener(v -> switchToAddMealTab());
        btnTabMealRequest.setOnClickListener(v -> switchToMealRequestTab());
    }

    private void switchToAddMealTab() {
        btnTabAddMeal.setBackgroundResource(R.drawable.bg_pill_selected);
        btnTabAddMeal.setTextColor(ContextCompat.getColor(this, R.color.surface));

        btnTabMealRequest.setBackgroundResource(R.drawable.bg_pill_unselected);
        btnTabMealRequest.setTextColor(ContextCompat.getColor(this, R.color.text_secondary));

        layoutAddMealTab.setVisibility(View.VISIBLE);
        layoutMealRequestTab.setVisibility(View.GONE);
        tvScreenTitle.setText("মিল যুক্ত");
    }

    private void switchToMealRequestTab() {
        btnTabMealRequest.setBackgroundResource(R.drawable.bg_pill_selected);
        btnTabMealRequest.setTextColor(ContextCompat.getColor(this, R.color.surface));

        btnTabAddMeal.setBackgroundResource(R.drawable.bg_pill_unselected);
        btnTabAddMeal.setTextColor(ContextCompat.getColor(this, R.color.text_secondary));

        layoutAddMealTab.setVisibility(View.GONE);
        layoutMealRequestTab.setVisibility(View.VISIBLE);
        tvScreenTitle.setText("মিল রিকুয়েস্ট");

        loadMealRequests();
    }

    private void setupDatePickers() {
        // Tab 1 Date Picker
        tvSelectedMealDate.setText(displayFormat.format(selectedMealCalendar.getTime()));
        layoutDatePicker.setOnClickListener(v -> {
            int year = selectedMealCalendar.get(Calendar.YEAR);
            int month = selectedMealCalendar.get(Calendar.MONTH);
            int day = selectedMealCalendar.get(Calendar.DAY_OF_MONTH);

            DatePickerDialog dialog = new DatePickerDialog(this, (view, y, m, d) -> {
                selectedMealCalendar.set(y, m, d);
                tvSelectedMealDate.setText(displayFormat.format(selectedMealCalendar.getTime()));
                loadMealsForDate();
            }, year, month, day);
            dialog.show();
        });

        // Tab 2 Date Picker (defaults to tomorrow or today)
        tvReqSelectedDate.setText(displayFormat.format(selectedReqCalendar.getTime()));
        layoutReqDatePicker.setOnClickListener(v -> {
            int year = selectedReqCalendar.get(Calendar.YEAR);
            int month = selectedReqCalendar.get(Calendar.MONTH);
            int day = selectedReqCalendar.get(Calendar.DAY_OF_MONTH);

            DatePickerDialog dialog = new DatePickerDialog(this, (view, y, m, d) -> {
                selectedReqCalendar.set(y, m, d);
                tvReqSelectedDate.setText(displayFormat.format(selectedReqCalendar.getTime()));
            }, year, month, day);
            dialog.show();
        });

        // Quick auto-reset button (pencil icon)
        btnAutoResetMeals.setOnClickListener(v -> {
            autoSetDefaultMeals();
            if (bulkMealAdapter != null) bulkMealAdapter.notifyDataSetChanged();
            Toast.makeText(this, "সকল মেম্বারের জন্য ডিফল্ট মিল সেট করা হয়েছে।", Toast.LENGTH_SHORT).show();
        });
    }

    private void setupBulkMealRecycler() {
        rvBulkMembers.setLayoutManager(new LinearLayoutManager(this));
        bulkMealAdapter = new BulkMealAdapter();
        rvBulkMembers.setAdapter(bulkMealAdapter);

        btnConfirmMeals.setOnClickListener(v -> confirmAndSaveMeals());
    }

    private void setupFlexibleMealSection() {
        // 1. Setup Linked Bazar Session Dropdown
        loadLinkedBazarSessions();

        // 2. Setup Meal Type Spinner
        List<String> mealTypeLabels = new ArrayList<>();
        mealTypeLabels.add("দুপুরের মিল (Lunch)");
        mealTypeLabels.add("রাতের মিল (Dinner)");
        mealTypeLabels.add("সকালের মিল (Breakfast)");
        mealTypeLabels.add("বিশেষ / দাওয়াত মিল (Special)");

        ArrayAdapter<String> typeAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, mealTypeLabels);
        spFlexibleMealType.setAdapter(typeAdapter);

        spFlexibleMealType.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                switch (position) {
                    case 0: selectedMealType = "lunch"; break;
                    case 1: selectedMealType = "dinner"; break;
                    case 2: selectedMealType = "breakfast"; break;
                    case 3: selectedMealType = "special"; break;
                    default: selectedMealType = "lunch"; break;
                }
                updateFlexibleCalculations();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        // 3. Setup Raw Bazar Item Selector
        rawBazarOptions.clear();
        rawBazarOptions.add("গরুর মাংস (Beef)");
        rawBazarOptions.add("মুরগির মাংস (Chicken)");
        rawBazarOptions.add("রুই / কাতলা / ইলিশ মাছ (Fish)");
        rawBazarOptions.add("বেগুন ও ভাজি (Begun / Bhaji)");
        rawBazarOptions.add("সবজি ও ডাল (Sobji / Dal)");
        rawBazarOptions.add("ডিম ভুনা (Egg Curry)");
        rawBazarOptions.add("বিরিয়ানি / তেহারি (Biryani)");
        rawBazarOptions.add("খিচুড়ি ও ডিম (Khichuri)");

        // Include any recent raw meal titles from Bazar expenses
        try {
            List<Expense> recentBazar = expenseDao.getExpensesInDateRange(currentMessId,
                    DateTimeUtils.getCurrentMonthStart(), DateTimeUtils.getCurrentMonthEnd());
            if (recentBazar != null) {
                for (Expense exp : recentBazar) {
                    if (exp.isRawMeal() && exp.getTitle() != null && !exp.getTitle().trim().isEmpty()) {
                        String title = exp.getTitle().trim();
                        if (!rawBazarOptions.contains(title)) {
                            rawBazarOptions.add(title);
                        }
                    }
                }
            }
        } catch (Exception ignored) {}

        rawBazarOptions.add("অন্যান্য / কাস্টম আইটেম...");

        ArrayAdapter<String> rawAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, rawBazarOptions);
        spRawBazarItem.setAdapter(rawAdapter);

        spRawBazarItem.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String selected = rawBazarOptions.get(position);
                if (selected.contains("অন্যান্য / কাস্টম")) {
                    etCustomRawBazar.setVisibility(View.VISIBLE);
                } else {
                    etCustomRawBazar.setVisibility(View.GONE);
                }
                selectedRawBazarName = selected;
                updateFlexibleCalculations();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        // 4. Checkbox Listener
        cbFlexibleMeal.setOnCheckedChangeListener((buttonView, isChecked) -> {
            isFlexibleMeal = isChecked;
            layoutFlexibleOptions.setVisibility(isChecked ? View.VISIBLE : View.GONE);
            updateFlexibleCalculations();
        });

        // 5. Amount input watcher
        etRawMealAmount.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                updateFlexibleCalculations();
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        etCustomRawBazar.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                updateFlexibleCalculations();
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });
    }

    private void loadLinkedBazarSessions() {
        if (spLinkedBazarSession == null) return;
        linkedBazarSessionList.clear();
        List<String> labels = new ArrayList<>();
        labels.add("কোনো নির্দিষ্ট বাজার লিংক নেই (ম্যানুয়াল এন্ট্রি)");

        List<com.smartmess.android.model.BazarSession> sessions = bazarSessionDao.getAll(currentMessId);
        if (sessions != null && !sessions.isEmpty()) {
            for (com.smartmess.android.model.BazarSession bs : sessions) {
                linkedBazarSessionList.add(bs);
                String title = bs.buildSummaryTitle();
                labels.add("[" + bs.getDate() + "] " + title + " (৳" + String.format(Locale.US, "%.0f", bs.getTotalItemCost()) + ")");
            }
        }

        ArrayAdapter<String> sessionAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, labels);
        spLinkedBazarSession.setAdapter(sessionAdapter);

        spLinkedBazarSession.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (position == 0) {
                    selectedBazarSessionUuid = null;
                } else if (position - 1 < linkedBazarSessionList.size()) {
                    com.smartmess.android.model.BazarSession bs = linkedBazarSessionList.get(position - 1);
                    selectedBazarSessionUuid = bs.getUuid();
                    if (bs.getMealType() != null) {
                        setMealTypeSpinnerSelection(bs.getMealType());
                    }
                    double cost = bs.getCategoryTotal(com.smartmess.android.model.BazarItem.CAT_RAW_MEAL);
                    if (cost <= 0) cost = bs.getTotalItemCost();
                    etRawMealAmount.setText(String.format(Locale.US, "%.2f", cost));

                    if (bs.getItems() != null && !bs.getItems().isEmpty()) {
                        String firstItemName = bs.getItems().get(0).getName();
                        if (firstItemName != null && !firstItemName.trim().isEmpty()) {
                            setRawBazarSpinnerSelection(firstItemName.trim());
                        }
                    }
                    updateFlexibleCalculations();
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });
    }

    private String getEffectiveRawItemName() {
        if (spRawBazarItem.getSelectedItem() != null) {
            String selected = spRawBazarItem.getSelectedItem().toString();
            if (selected.contains("অন্যান্য / কাস্টম") && etCustomRawBazar.getText() != null) {
                String custom = etCustomRawBazar.getText().toString().trim();
                if (!custom.isEmpty()) return custom;
            }
            return selected;
        }
        return "কাঁচা বাজার আইটেম";
    }

    private void updateFlexibleCalculations() {
        if (!isFlexibleMeal) {
            calculatedPerMealRate = 0.0;
            activeDinersCount = 0;
            if (bulkMealAdapter != null) bulkMealAdapter.notifyDataSetChanged();
            return;
        }

        int activeDiners = 0;
        for (User u : displayedMembers) {
            MemberMealState s = memberMealMap.get(u.getId());
            if (s == null || s.isOnVacation) continue;

            double mealCount = 0.0;
            if ("breakfast".equals(selectedMealType)) {
                mealCount = s.breakfast;
            } else if ("lunch".equals(selectedMealType)) {
                mealCount = s.lunch + s.guest;
            } else if ("dinner".equals(selectedMealType)) {
                mealCount = s.dinner;
            } else {
                mealCount = s.getTotal();
            }

            if (mealCount > 0) {
                activeDiners++;
            }
        }
        activeDinersCount = activeDiners;

        String amtStr = etRawMealAmount.getText() != null ? etRawMealAmount.getText().toString().trim() : "";
        try {
            currentRawMealAmount = !amtStr.isEmpty() ? Double.parseDouble(amtStr) : 0.0;
        } catch (Exception e) {
            currentRawMealAmount = 0.0;
        }

        if (activeDiners > 0 && currentRawMealAmount > 0) {
            calculatedPerMealRate = currentRawMealAmount / activeDiners;
        } else {
            calculatedPerMealRate = 0.0;
        }

        tvSharedPricePreview.setText(String.format(Locale.US,
                "মোট বাজার: %s • সক্রিয় মিল গ্রহণকারী: %d জন • জনপ্রতি খরচ: %s (অনুপস্থিত মেম্বার: ৳০.০০)",
                CurrencyUtils.format(currentRawMealAmount),
                activeDiners,
                CurrencyUtils.format(calculatedPerMealRate)
        ));

        if (bulkMealAdapter != null) {
            bulkMealAdapter.notifyDataSetChanged();
        }
    }

    private void setMealTypeSpinnerSelection(String type) {
        if ("lunch".equalsIgnoreCase(type)) spFlexibleMealType.setSelection(0);
        else if ("dinner".equalsIgnoreCase(type)) spFlexibleMealType.setSelection(1);
        else if ("breakfast".equalsIgnoreCase(type)) spFlexibleMealType.setSelection(2);
        else if ("special".equalsIgnoreCase(type)) spFlexibleMealType.setSelection(3);
    }

    private void setRawBazarSpinnerSelection(String rawName) {
        if (rawName == null) return;
        for (int i = 0; i < rawBazarOptions.size(); i++) {
            if (rawBazarOptions.get(i).equalsIgnoreCase(rawName.trim())) {
                spRawBazarItem.setSelection(i);
                return;
            }
        }
        // If custom, set last and put text
        int customPos = rawBazarOptions.size() - 1;
        spRawBazarItem.setSelection(customPos);
        etCustomRawBazar.setVisibility(View.VISIBLE);
        etCustomRawBazar.setText(rawName);
    }

    private void loadMembersAndInitializeMeals() {
        allActiveMembers = userDao.getActiveMembersByMess(currentMessId);

        List<String> spinnerOptions = new ArrayList<>();
        spinnerOptions.add("সকল মেম্বারের মিল যুক্ত");
        for (User u : allActiveMembers) {
            spinnerOptions.add(u.getName());
        }

        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, spinnerOptions);
        spMemberSelection.setAdapter(adapter);

        spMemberSelection.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                displayedMembers.clear();
                if (position == 0) {
                    displayedMembers.addAll(allActiveMembers);
                } else if (position - 1 < allActiveMembers.size()) {
                    displayedMembers.add(allActiveMembers.get(position - 1));
                }
                if (bulkMealAdapter != null) {
                    bulkMealAdapter.notifyDataSetChanged();
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        loadMealsForDate();
    }

    /**
     * Loads existing meals for the selected date or applies auto-set meals.
     * "otherwise auto set meal": If no existing record or custom request exists,
     * auto-set default meals (0.0 breakfast, 1.0 lunch, 1.0 dinner, 0.0 guest, or 0 if on vacation).
     */
    private void loadMealsForDate() {
        String dateStr = isoFormat.format(selectedMealCalendar.getTime());
        memberMealMap.clear();

        for (User user : allActiveMembers) {
            boolean onVacation = vacationDao.isUserOnVacation(currentMessId, user.getId(), dateStr);
            Meal existing = mealDao.getUserMealForDate(currentMessId, user.getId(), dateStr);
            MealRequest approvedReq = mealRequestDao.getRequestForUserAndDate(currentMessId, user.getId(), dateStr);

            MemberMealState state = new MemberMealState(user);
            state.isOnVacation = onVacation;

            if (onVacation) {
                state.breakfast = 0.0;
                state.lunch = 0.0;
                state.dinner = 0.0;
                state.guest = 0.0;
            } else if (existing != null) {
                state.breakfast = existing.getBreakfastCount();
                state.lunch = existing.getLunchCount();
                state.dinner = existing.getDinnerCount();
                state.guest = existing.getGuestMealCount();
            } else if (approvedReq != null && "approved".equals(approvedReq.getStatus())) {
                state.breakfast = approvedReq.getBreakfastCount();
                state.lunch = approvedReq.getLunchCount();
                state.dinner = approvedReq.getDinnerCount();
                state.guest = approvedReq.getGuestCount();
            } else {
                // AUTO SET MEAL RULE
                state.breakfast = 0.0;
                state.lunch = 1.0;
                state.dinner = 1.0;
                state.guest = 0.0;
            }
            memberMealMap.put(user.getId(), state);
        }

        displayedMembers.clear();
        int selectedPos = spMemberSelection.getSelectedItemPosition();
        if (selectedPos <= 0) {
            displayedMembers.addAll(allActiveMembers);
        } else if (selectedPos - 1 < allActiveMembers.size()) {
            displayedMembers.add(allActiveMembers.get(selectedPos - 1));
        }

        // Restore flexible meal state if already saved for this date
        boolean foundFlexible = false;
        String existingFlexType = "lunch";
        String existingRawName = "";
        double existingRawAmt = 0.0;
        for (User user : allActiveMembers) {
            Meal existing = mealDao.getUserMealForDate(currentMessId, user.getId(), dateStr);
            if (existing != null && existing.getIsFlexible() == 1) {
                foundFlexible = true;
                if (existing.getMealType() != null) existingFlexType = existing.getMealType();
                if (existing.getRawBazarName() != null) existingRawName = existing.getRawBazarName();
                if (existing.getRawBazarAmount() > 0) existingRawAmt = existing.getRawBazarAmount();
                break;
            }
        }

        if (cbFlexibleMeal != null) {
            cbFlexibleMeal.setChecked(foundFlexible);
            if (foundFlexible) {
                if (existingRawAmt > 0) {
                    etRawMealAmount.setText(formatNumber(existingRawAmt));
                }
                setMealTypeSpinnerSelection(existingFlexType);
                if (!existingRawName.isEmpty()) {
                    setRawBazarSpinnerSelection(existingRawName);
                }
            } else {
                etRawMealAmount.setText("");
            }
        }

        updateFlexibleCalculations();

        if (bulkMealAdapter != null) {
            bulkMealAdapter.notifyDataSetChanged();
        }
    }

    private void autoSetDefaultMeals() {
        String dateStr = isoFormat.format(selectedMealCalendar.getTime());
        for (User user : allActiveMembers) {
            MemberMealState state = memberMealMap.get(user.getId());
            if (state == null) {
                state = new MemberMealState(user);
                memberMealMap.put(user.getId(), state);
            }
            boolean onVacation = vacationDao.isUserOnVacation(currentMessId, user.getId(), dateStr);
            state.isOnVacation = onVacation;
            if (onVacation) {
                state.breakfast = 0.0;
                state.lunch = 0.0;
                state.dinner = 0.0;
                state.guest = 0.0;
            } else {
                state.breakfast = 0.0;
                state.lunch = 1.0;
                state.dinner = 1.0;
                state.guest = 0.0;
            }
        }
    }

    private void confirmAndSaveMeals() {
        if (displayedMembers.isEmpty()) {
            Toast.makeText(this, "সংরক্ষণ করার মতো কোনো মেম্বার পাওয়া যায়নি।", Toast.LENGTH_SHORT).show();
            return;
        }

        String dateStr = isoFormat.format(selectedMealCalendar.getTime());
        String nowIso = DateTimeUtils.nowIso();
        String rawItemName = isFlexibleMeal ? getEffectiveRawItemName() : null;
        int savedCount = 0;
        boolean hasEditedExisting = false;
        boolean isBackdated = dateStr.compareTo(DateTimeUtils.currentDate()) < 0;

        List<ItemizedConsumptionLog> consumptionLogs = new ArrayList<>();

        for (User user : displayedMembers) {
            MemberMealState state = memberMealMap.get(user.getId());
            if (state == null) continue;

            double userTypeMeals = 0.0;
            if ("breakfast".equals(selectedMealType)) userTypeMeals = state.breakfast;
            else if ("lunch".equals(selectedMealType)) userTypeMeals = state.lunch + state.guest;
            else if ("dinner".equals(selectedMealType)) userTypeMeals = state.dinner;
            else userTypeMeals = state.getTotal();

            double userShare = 0.0;
            if (isFlexibleMeal && calculatedPerMealRate > 0 && !state.isOnVacation && userTypeMeals > 0) {
                userShare = calculatedPerMealRate; // Active diner cost splitting
            }

            Meal existing = mealDao.getUserMealForDate(currentMessId, user.getId(), dateStr);
            if (existing != null) {
                hasEditedExisting = true;
                existing.setBreakfastCount(state.breakfast);
                existing.setLunchCount(state.lunch);
                existing.setDinnerCount(state.dinner);
                existing.setGuestMealCount(state.guest);
                existing.setIsFlexible(isFlexibleMeal ? 1 : 0);
                existing.setMealType(isFlexibleMeal ? selectedMealType : "mixed");
                existing.setRawBazarName(isFlexibleMeal ? rawItemName : null);
                existing.setRawBazarAmount(isFlexibleMeal ? currentRawMealAmount : 0.0);
                existing.setSharedPrice(isFlexibleMeal ? userShare : 0.0);
                existing.setBazarSessionUuid(selectedBazarSessionUuid);
                existing.setUpdatedAt(nowIso);
                mealDao.insertOrUpdate(existing);
            } else {
                Meal newMeal = new Meal(
                        UUID.randomUUID().toString(),
                        currentMessId,
                        user.getId(),
                        dateStr,
                        state.breakfast,
                        state.lunch,
                        state.dinner,
                        state.guest
                );
                newMeal.setIsFlexible(isFlexibleMeal ? 1 : 0);
                newMeal.setMealType(isFlexibleMeal ? selectedMealType : "mixed");
                newMeal.setRawBazarName(isFlexibleMeal ? rawItemName : null);
                newMeal.setRawBazarAmount(isFlexibleMeal ? currentRawMealAmount : 0.0);
                newMeal.setSharedPrice(isFlexibleMeal ? userShare : 0.0);
                newMeal.setBazarSessionUuid(selectedBazarSessionUuid);
                newMeal.setCreatedAt(nowIso);
                newMeal.setUpdatedAt(nowIso);
                mealDao.insertOrUpdate(newMeal);
            }

            // If flexible meal and active diner ate, create itemized consumption log
            if (isFlexibleMeal && currentRawMealAmount > 0 && activeDinersCount > 0 && !state.isOnVacation && userTypeMeals > 0) {
                ItemizedConsumptionLog log = new ItemizedConsumptionLog();
                log.setUuid(UUID.randomUUID().toString());
                log.setMessId(currentMessId);
                log.setUserId(user.getId());
                log.setMealDate(dateStr);
                log.setMealType(selectedMealType);
                log.setBazarSessionUuid(selectedBazarSessionUuid);
                log.setItemName(rawItemName != null ? rawItemName : "কাঁচা বাজার আইটেম");
                log.setItemCategory("raw_meal");
                log.setTotalItemCost(currentRawMealAmount);
                log.setActiveDinersCount(activeDinersCount);
                log.setCostShare(calculatedPerMealRate);
                log.setMealCount(userTypeMeals);
                log.setCreatedAt(nowIso);
                log.setUpdatedAt(nowIso);
                consumptionLogs.add(log);
            }

            savedCount++;
        }

        // Persist itemized consumption logs
        if (itemizedConsumptionDao != null) {
            itemizedConsumptionDao.deleteByMealDetails(currentMessId, dateStr, isFlexibleMeal ? selectedMealType : "mixed");
            if (!consumptionLogs.isEmpty()) {
                itemizedConsumptionDao.insertOrUpdateBatch(consumptionLogs);
            }
        }

        // Audit notification broadcast if modified or backdated
        if (hasEditedExisting || isBackdated) {
            String actionType = isBackdated ? "পূর্বের তারিখের মিল যুক্ত" : "মিল এন্ট্রি পরিবর্তন";
            String modifierName = sessionManager.getUserName();
            if (modifierName == null || modifierName.trim().isEmpty()) modifierName = "ম্যানেজার";
            String notifTitle = "🔔 " + actionType + " করা হয়েছে";
            String notifMsg = String.format(Locale.US,
                    "%s তারিখের মিলের তথ্য %s কর্তৃক %s হালনাগাদ করা হয়েছে। মোট সক্রিয় ডিনার: %d জন।",
                    dateStr, modifierName, DateTimeUtils.getCurrentDateTime(), activeDinersCount);
            NotificationCenterHelper.postNotification(AddMealActivity.this, currentMessId, notifTitle, notifMsg, NotificationCenterHelper.TYPE_NOTICE);
        }

        SyncManager.triggerSync(getApplicationContext());

        Toast.makeText(this, savedCount + " জন মেম্বারের মিল সফলভাবে সংরক্ষিত হয়েছে।", Toast.LENGTH_SHORT).show();
        finish();
    }

    // ==========================================
    // TAB 2: MEAL REQUEST LOGIC & STEPPERS
    // ==========================================
    private void setupRequestSteppers() {
        updateRequestStepperViews();

        btnReqMinusBreakfast.setOnClickListener(v -> {
            if (reqBreakfast >= 0.5) {
                reqBreakfast -= 0.5;
                updateRequestStepperViews();
            }
        });
        btnReqPlusBreakfast.setOnClickListener(v -> {
            if (reqBreakfast < 10.0) {
                reqBreakfast += 0.5;
                updateRequestStepperViews();
            }
        });

        btnReqMinusLunch.setOnClickListener(v -> {
            if (reqLunch >= 0.5) {
                reqLunch -= 0.5;
                updateRequestStepperViews();
            }
        });
        btnReqPlusLunch.setOnClickListener(v -> {
            if (reqLunch < 10.0) {
                reqLunch += 0.5;
                updateRequestStepperViews();
            }
        });

        btnReqMinusDinner.setOnClickListener(v -> {
            if (reqDinner >= 0.5) {
                reqDinner -= 0.5;
                updateRequestStepperViews();
            }
        });
        btnReqPlusDinner.setOnClickListener(v -> {
            if (reqDinner < 10.0) {
                reqDinner += 0.5;
                updateRequestStepperViews();
            }
        });

        btnReqMinusGuest.setOnClickListener(v -> {
            if (reqGuest >= 1.0) {
                reqGuest -= 1.0;
                updateRequestStepperViews();
            }
        });
        btnReqPlusGuest.setOnClickListener(v -> {
            if (reqGuest < 20.0) {
                reqGuest += 1.0;
                updateRequestStepperViews();
            }
        });

        btnSubmitRequest.setOnClickListener(v -> submitMealRequest());
    }

    private void updateRequestStepperViews() {
        tvReqBreakfastCount.setText(formatNumber(reqBreakfast));
        tvReqLunchCount.setText(formatNumber(reqLunch));
        tvReqDinnerCount.setText(formatNumber(reqDinner));
        tvReqGuestCount.setText(formatNumber(reqGuest));
    }

    private void submitMealRequest() {
        String dateStr = isoFormat.format(selectedReqCalendar.getTime());
        String note = etReqNote.getText().toString().trim();

        MealRequest request = new MealRequest(
                UUID.randomUUID().toString(),
                currentMessId,
                currentUserId,
                dateStr,
                reqBreakfast,
                reqLunch,
                reqDinner,
                reqGuest,
                note
        );
        request.setCreatedAt(DateTimeUtils.nowIso());
        request.setUpdatedAt(DateTimeUtils.nowIso());

        long insertedId = mealRequestDao.insertOrUpdate(request);
        if (insertedId > 0) {
            // Send notification to manager
            String senderName = sessionManager.getUserName();
            double total = reqBreakfast + reqLunch + reqDinner + reqGuest;
            String guestPart = reqGuest > 0 ? (" এবং " + formatNumber(reqGuest) + " জন গেস্ট") : "";
            AppNotification notif = new AppNotification(
                    UUID.randomUUID().toString(),
                    currentMessId,
                    0, // 0 for broadcast / manager view
                    "নতুন মিল রিকুয়েস্ট: " + senderName,
                    senderName + " " + dateStr + " তারিখের জন্য " + formatNumber(total) + " টি মিল" + guestPart + " এর রিকুয়েস্ট করেছেন।",
                    "meal_request",
                    DateTimeUtils.nowIso()
            );
            notificationDao.insert(notif);

            if (sessionManager.isPro()) {
                SyncManager.triggerSync(getApplicationContext());
            }

            Toast.makeText(this, "আপনার মিল রিকুয়েস্ট সফলভাবে পাঠানো হয়েছে।", Toast.LENGTH_SHORT).show();
            etReqNote.setText("");
            loadMealRequests();
        } else {
            Toast.makeText(this, "রিকুয়েস্ট পাঠানো সম্ভব হয়নি। অনুগ্রহ করে পুনরায় চেষ্টা করুন।", Toast.LENGTH_SHORT).show();
        }
    }

    private void setupRequestRecycler() {
        rvMealRequests.setLayoutManager(new LinearLayoutManager(this));
        mealRequestAdapter = new MealRequestAdapter();
        rvMealRequests.setAdapter(mealRequestAdapter);
    }

    private void loadMealRequests() {
        mealRequestsList.clear();
        if (isManager) {
            tvRequestsHeader.setText("মেসের রিকুয়েস্ট সমূহ");
            mealRequestsList.addAll(mealRequestDao.getAllRequestsForMess(currentMessId));
        } else {
            tvRequestsHeader.setText("আমার রিকুয়েস্ট সমূহ");
            mealRequestsList.addAll(mealRequestDao.getRequestsForUser(currentMessId, currentUserId));
        }

        if (mealRequestsList.isEmpty()) {
            tvEmptyRequests.setVisibility(View.VISIBLE);
        } else {
            tvEmptyRequests.setVisibility(View.GONE);
        }

        if (mealRequestAdapter != null) {
            mealRequestAdapter.notifyDataSetChanged();
        }
    }

    private String formatNumber(double value) {
        if (value == (long) value) {
            return String.format(Locale.US, "%d", (long) value);
        } else {
            return String.format(Locale.US, "%.1f", value);
        }
    }

    // ==========================================
    // INNER ADAPTER: Bulk Member Meal Steppers
    // ==========================================
    private static class MemberMealState {
        final User user;
        double breakfast = 0.0;
        double lunch = 1.0;
        double dinner = 1.0;
        double guest = 0.0;
        boolean isOnVacation = false;

        MemberMealState(User user) {
            this.user = user;
        }

        double getTotal() {
            return breakfast + lunch + dinner + guest;
        }
    }

    private class BulkMealAdapter extends RecyclerView.Adapter<BulkMealAdapter.ViewHolder> {

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_bulk_meal_member, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            User user = displayedMembers.get(position);
            MemberMealState state = memberMealMap.get(user.getId());
            if (state == null) {
                state = new MemberMealState(user);
                memberMealMap.put(user.getId(), state);
            }

            holder.tvMemberName.setText(user.getName());
            if (state.isOnVacation) {
                holder.tvVacationBadge.setVisibility(View.VISIBLE);
            } else {
                holder.tvVacationBadge.setVisibility(View.GONE);
            }

            updateCardViews(holder, state);

            // Flexible meal share badge
            if (isFlexibleMeal && calculatedPerMealRate > 0) {
                if (state.isOnVacation) {
                    holder.tvFlexibleSharedInfo.setVisibility(View.VISIBLE);
                    holder.tvFlexibleSharedInfo.setText("🏖️ ছুটিতে আছেন (মিল বন্ধ • ৳০.০০)");
                    holder.tvFlexibleSharedInfo.setTextColor(ContextCompat.getColor(AddMealActivity.this, R.color.text_secondary));
                } else {
                    double userTypeMeals = 0.0;
                    if ("breakfast".equals(selectedMealType)) userTypeMeals = state.breakfast;
                    else if ("lunch".equals(selectedMealType)) userTypeMeals = state.lunch + state.guest;
                    else if ("dinner".equals(selectedMealType)) userTypeMeals = state.dinner;
                    else userTypeMeals = state.getTotal();

                    holder.tvFlexibleSharedInfo.setVisibility(View.VISIBLE);
                    if (userTypeMeals > 0) {
                        double userCost = calculatedPerMealRate;
                        String itemName = getEffectiveRawItemName();
                        holder.tvFlexibleSharedInfo.setText(String.format(Locale.US,
                                "খাবার শেয়ার: %s (%s মিল • %s)",
                                CurrencyUtils.format(userCost), formatNumber(userTypeMeals), itemName));
                        holder.tvFlexibleSharedInfo.setTextColor(ContextCompat.getColor(AddMealActivity.this, R.color.credit_green));
                    } else {
                        holder.tvFlexibleSharedInfo.setText("মিল নেননি: ৳০.০০ (খরচ প্রযোজ্য নয়)");
                        holder.tvFlexibleSharedInfo.setTextColor(ContextCompat.getColor(AddMealActivity.this, R.color.text_secondary));
                    }
                }
            } else {
                holder.tvFlexibleSharedInfo.setVisibility(View.GONE);
            }

            final MemberMealState finalState = state;

            // Breakfast Stepper
            holder.btnMinusBreakfast.setOnClickListener(v -> {
                if (finalState.breakfast >= 0.5) {
                    finalState.breakfast -= 0.5;
                    updateCardViews(holder, finalState);
                    updateFlexibleCalculations();
                }
            });
            holder.btnPlusBreakfast.setOnClickListener(v -> {
                if (finalState.breakfast < 10.0) {
                    finalState.breakfast += 0.5;
                    updateCardViews(holder, finalState);
                    updateFlexibleCalculations();
                }
            });

            // Lunch Stepper
            holder.btnMinusLunch.setOnClickListener(v -> {
                if (finalState.lunch >= 0.5) {
                    finalState.lunch -= 0.5;
                    updateCardViews(holder, finalState);
                    updateFlexibleCalculations();
                }
            });
            holder.btnPlusLunch.setOnClickListener(v -> {
                if (finalState.lunch < 10.0) {
                    finalState.lunch += 0.5;
                    updateCardViews(holder, finalState);
                    updateFlexibleCalculations();
                }
            });

            // Dinner Stepper
            holder.btnMinusDinner.setOnClickListener(v -> {
                if (finalState.dinner >= 0.5) {
                    finalState.dinner -= 0.5;
                    updateCardViews(holder, finalState);
                    updateFlexibleCalculations();
                }
            });
            holder.btnPlusDinner.setOnClickListener(v -> {
                if (finalState.dinner < 10.0) {
                    finalState.dinner += 0.5;
                    updateCardViews(holder, finalState);
                    updateFlexibleCalculations();
                }
            });

            // Guest Stepper
            holder.btnMinusGuest.setOnClickListener(v -> {
                if (finalState.guest >= 0.5) {
                    finalState.guest -= 0.5;
                    updateCardViews(holder, finalState);
                    updateFlexibleCalculations();
                }
            });
            holder.btnPlusGuest.setOnClickListener(v -> {
                if (finalState.guest < 20.0) {
                    finalState.guest += 0.5;
                    updateCardViews(holder, finalState);
                    updateFlexibleCalculations();
                }
            });
        }

        private void updateCardViews(ViewHolder holder, MemberMealState state) {
            holder.tvBreakfast.setText(formatNumber(state.breakfast));
            holder.tvLunch.setText(formatNumber(state.lunch));
            holder.tvDinner.setText(formatNumber(state.dinner));
            holder.tvGuest.setText(formatNumber(state.guest));
            holder.tvTotalCount.setText("মোট: " + formatNumber(state.getTotal()));
        }

        @Override
        public int getItemCount() {
            return displayedMembers.size();
        }

        class ViewHolder extends RecyclerView.ViewHolder {
            ImageView ivAvatar;
            TextView tvMemberName, tvVacationBadge, tvTotalCount;
            ImageButton btnMinusBreakfast, btnPlusBreakfast;
            TextView tvBreakfast;
            ImageButton btnMinusLunch, btnPlusLunch;
            TextView tvLunch;
            ImageButton btnMinusDinner, btnPlusDinner;
            TextView tvDinner;
            ImageButton btnMinusGuest, btnPlusGuest;
            TextView tvGuest;
            TextView tvFlexibleSharedInfo;

            ViewHolder(@NonNull View itemView) {
                super(itemView);
                ivAvatar = itemView.findViewById(R.id.ivAvatar);
                tvMemberName = itemView.findViewById(R.id.tvMemberName);
                tvVacationBadge = itemView.findViewById(R.id.tvVacationBadge);
                tvTotalCount = itemView.findViewById(R.id.tvTotalCount);

                btnMinusBreakfast = itemView.findViewById(R.id.btnMinusBreakfast);
                tvBreakfast = itemView.findViewById(R.id.tvBreakfast);
                btnPlusBreakfast = itemView.findViewById(R.id.btnPlusBreakfast);

                btnMinusLunch = itemView.findViewById(R.id.btnMinusLunch);
                tvLunch = itemView.findViewById(R.id.tvLunch);
                btnPlusLunch = itemView.findViewById(R.id.btnPlusLunch);

                btnMinusDinner = itemView.findViewById(R.id.btnMinusDinner);
                tvDinner = itemView.findViewById(R.id.tvDinner);
                btnPlusDinner = itemView.findViewById(R.id.btnPlusDinner);

                btnMinusGuest = itemView.findViewById(R.id.btnMinusGuest);
                tvGuest = itemView.findViewById(R.id.tvGuest);
                btnPlusGuest = itemView.findViewById(R.id.btnPlusGuest);
                tvFlexibleSharedInfo = itemView.findViewById(R.id.tvFlexibleSharedInfo);
            }
        }
    }

    // ==========================================
    // INNER ADAPTER: Meal Requests List
    // ==========================================
    private class MealRequestAdapter extends RecyclerView.Adapter<MealRequestAdapter.ViewHolder> {

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_meal_request_card, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            MealRequest req = mealRequestsList.get(position);

            holder.tvReqMemberName.setText(req.getUserName() != null ? req.getUserName() : "মেম্বার #" + req.getUserId());
            holder.tvReqDate.setText("তারিখ: " + req.getRequestDate());

            holder.tvReqBreakfast.setText("সকাল: " + formatNumber(req.getBreakfastCount()));
            holder.tvReqLunch.setText("দুপুর: " + formatNumber(req.getLunchCount()));
            holder.tvReqDinner.setText("রাত: " + formatNumber(req.getDinnerCount()));
            holder.tvReqGuest.setText("অতিথি: " + formatNumber(req.getGuestCount()));

            if (req.getNote() != null && !req.getNote().trim().isEmpty()) {
                holder.tvReqNote.setVisibility(View.VISIBLE);
                holder.tvReqNote.setText("নোট: " + req.getNote());
            } else {
                holder.tvReqNote.setVisibility(View.GONE);
            }

            // Status Badge
            String status = req.getStatus() != null ? req.getStatus() : "pending";
            if ("approved".equalsIgnoreCase(status)) {
                holder.tvReqStatus.setText("অনুমোদিত");
                holder.tvReqStatus.setTextColor(ContextCompat.getColor(AddMealActivity.this, R.color.credit_green));
                holder.tvReqStatus.setBackgroundTintList(ContextCompat.getColorStateList(AddMealActivity.this, R.color.credit_green_bg));
                holder.layoutManagerActions.setVisibility(View.GONE);
            } else if ("rejected".equalsIgnoreCase(status)) {
                holder.tvReqStatus.setText("বাতিলকৃত");
                holder.tvReqStatus.setTextColor(ContextCompat.getColor(AddMealActivity.this, R.color.error_red));
                holder.tvReqStatus.setBackgroundTintList(ContextCompat.getColorStateList(AddMealActivity.this, R.color.due_red_bg));
                holder.layoutManagerActions.setVisibility(View.GONE);
            } else {
                holder.tvReqStatus.setText("অপেক্ষমাণ");
                holder.tvReqStatus.setTextColor(ContextCompat.getColor(AddMealActivity.this, R.color.warning_amber));
                holder.tvReqStatus.setBackgroundTintList(ContextCompat.getColorStateList(AddMealActivity.this, R.color.warning_amber_bg));

                if (isManager) {
                    holder.layoutManagerActions.setVisibility(View.VISIBLE);
                } else {
                    holder.layoutManagerActions.setVisibility(View.GONE);
                }
            }

            // Manager Approval / Rejection Handlers
            holder.btnApproveReq.setOnClickListener(v -> {
                boolean success = mealRequestDao.approveAndApplyToMeal(req, mealDao);
                if (success) {
                    // Send notification to member
                    AppNotification notif = new AppNotification(
                            UUID.randomUUID().toString(),
                            currentMessId,
                            req.getUserId(),
                            "মিল রিকুয়েস্ট অনুমোদিত",
                            req.getRequestDate() + " তারিখের আপনার মিল রিকুয়েস্ট অনুমোদিত হয়েছে।",
                            "meal_approved",
                            DateTimeUtils.nowIso()
                    );
                    notificationDao.insert(notif);

                    if (sessionManager.isPro()) {
                        SyncManager.triggerSync(getApplicationContext());
                    }

                    Toast.makeText(AddMealActivity.this, "রিকুয়েস্ট অনুমোদন করা হয়েছে এবং মিল সেভ হয়েছে।", Toast.LENGTH_SHORT).show();
                    loadMealRequests();
                    loadMealsForDate();
                }
            });

            holder.btnRejectReq.setOnClickListener(v -> {
                mealRequestDao.updateStatus(req.getId(), "rejected");
                AppNotification notif = new AppNotification(
                        UUID.randomUUID().toString(),
                        currentMessId,
                        req.getUserId(),
                        "মিল রিকুয়েস্ট বাতিল",
                        req.getRequestDate() + " তারিখের আপনার মিল রিকুয়েস্ট বাতিল করা হয়েছে।",
                        "meal_rejected",
                        DateTimeUtils.nowIso()
                );
                notificationDao.insert(notif);

                if (sessionManager.isPro()) {
                    SyncManager.triggerSync(getApplicationContext());
                }

                Toast.makeText(AddMealActivity.this, "রিকুয়েস্ট বাতিল করা হয়েছে।", Toast.LENGTH_SHORT).show();
                loadMealRequests();
            });
        }

        @Override
        public int getItemCount() {
            return mealRequestsList.size();
        }

        class ViewHolder extends RecyclerView.ViewHolder {
            TextView tvReqMemberName, tvReqDate, tvReqStatus;
            TextView tvReqBreakfast, tvReqLunch, tvReqDinner, tvReqGuest;
            TextView tvReqNote;
            LinearLayout layoutManagerActions;
            MaterialButton btnRejectReq, btnApproveReq;

            ViewHolder(@NonNull View itemView) {
                super(itemView);
                tvReqMemberName = itemView.findViewById(R.id.tvReqMemberName);
                tvReqDate = itemView.findViewById(R.id.tvReqDate);
                tvReqStatus = itemView.findViewById(R.id.tvReqStatus);

                tvReqBreakfast = itemView.findViewById(R.id.tvReqBreakfast);
                tvReqLunch = itemView.findViewById(R.id.tvReqLunch);
                tvReqDinner = itemView.findViewById(R.id.tvReqDinner);
                tvReqGuest = itemView.findViewById(R.id.tvReqGuest);

                tvReqNote = itemView.findViewById(R.id.tvReqNote);
                layoutManagerActions = itemView.findViewById(R.id.layoutManagerActions);
                btnRejectReq = itemView.findViewById(R.id.btnRejectReq);
                btnApproveReq = itemView.findViewById(R.id.btnApproveReq);
            }
        }
    }
}

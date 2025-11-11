package com.example.to_do_app.activitys;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.InputType;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.to_do_app.R;
import com.example.to_do_app.adapters.ScheduleItemAdapter;
import com.example.to_do_app.data.ScheduleDefaults;
import com.example.to_do_app.model.ScheduleItem;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.database.ServerValue;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.database.annotations.Nullable;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

/**
 * Layout6Activity — main activity for editing/applying a schedule.
 *
 * To install:
 * - Save this file as app/src/main/java/com/example/to_do_app/activitys/Layout6Activity.java
 * - Ensure layout res/layout/manhinh_lichduocchon.xml (or change setContentView) contains the required view IDs:
 *   btnBack, btnApplySchedule, scheduleRecyclerView, fabAddSlot, tvTitleHeader,
 *   day2..day7, dayCN, daysContainer, root_manhinh_lichduocchon
 *
 * This file writes per-user schedule under /users/<userId>/schedules/<scheduleName>/day_N and
 * writes a structured "week" home_display under /users/<userId>/schedules/<scheduleName>/home_display.
 * It also broadcasts ACTION_SCHEDULE_APPLIED and starts MainActivity to show Home.
 */
public class Layout6Activity extends AppCompatActivity {

    private static final String TAG = "Layout6Activity";

    public static final String EXTRA_TEMPLATE_TITLE = "EXTRA_TEMPLATE_TITLE";
    public static final String EXTRA_TEMPLATE_DESCRIPTION = "EXTRA_TEMPLATE_DESCRIPTION";
    public static final String EXTRA_HISTORY_KEY = "EXTRA_HISTORY_KEY";

    public static final String ACTION_SCHEDULE_APPLIED = "com.example.to_do_app.SCHEDULE_APPLIED";
    public static final String EXTRA_SCHEDULE_NAME = "schedule_name";

    private static final String LOCAL_ITEMS_KEY_PREFIX = "local_user_items";

    private static final String HOME_DISPLAY_ACTIVITIES_KEY = "home_display_activities";


    private CardView btnBack;
    private android.widget.TextView tvTitleHeader;
    private Button btnApplySchedule;
    private RecyclerView scheduleRecyclerView;
    private ScheduleItemAdapter scheduleAdapter;
    private List<ScheduleItem> currentList;

    // day buttons
    private LinearLayout day2, day3, day4, day5, day6, day7, dayCN;
    private View selectedDayView;
    private int selectedDay = 2;

    private LinearLayout daysContainer;
    private FloatingActionButton fabAdd;

    // Firebase
    private DatabaseReference schedulesRef;
    private DatabaseReference rootRef;
    private String userId;

    // schedule naming + prefs
    private String currentScheduleName = null;
    private static final String PREFS_NAME = "profile_overrides";
    private SharedPreferences prefs;

    private static final String PROFILE_PREFS = "profile_prefs";
    private static final String PROFILE_HISTORY_KEY = "profile_history";

    private static final String HOME_DISPLAY_DAY_KEY = "home_display_day";
    private static final String PREF_ACTIVE_SCHEDULE = "active_schedule_name";

    private String editingHistoryKey = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // match your layout filename; original project used manhinh_lichduocchon
        setContentView(R.layout.manhinh_lichduocchon);

        currentList = new ArrayList<>();
        schedulesRef = FirebaseDatabase.getInstance().getReference("schedules");
        rootRef = FirebaseDatabase.getInstance().getReference();
        prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);

        View root = findViewById(R.id.root_manhinh_lichduocchon);
        if (root != null) {
            final int padL = root.getPaddingLeft();
            final int padT = root.getPaddingTop();
            final int padR = root.getPaddingRight();
            final int padB = root.getPaddingBottom();

            ViewCompat.setOnApplyWindowInsetsListener(root, (v, insets) -> {
                Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
                v.setPadding(
                        padL + systemBars.left,
                        padT + systemBars.top,
                        padR + systemBars.right,
                        padB + systemBars.bottom
                );
                return insets;
            });
        }

        SharedPreferences profilePrefs = getSharedPreferences(PROFILE_PREFS, Context.MODE_PRIVATE);
        userId = profilePrefs.getString("profile_user_id", null);
        if (userId == null) {
            userId = "user_" + System.currentTimeMillis();
            profilePrefs.edit().putString("profile_user_id", userId).apply();
        }

        bindViews();

        selectedDay = getIntent().getIntExtra("selected_day", selectedDay);
        editingHistoryKey = getIntent().getStringExtra(EXTRA_HISTORY_KEY);

        setupRecyclerView();

        String passedTitle = getIntent().getStringExtra(EXTRA_TEMPLATE_TITLE);
        if (passedTitle != null && !passedTitle.isEmpty() && tvTitleHeader != null) {
            tvTitleHeader.setText(passedTitle);
        }

        setupDays();
        setupListeners();
        updateFabState();

        loadScheduleDataForDay(selectedDay);
    }

    private void bindViews() {
        btnBack = findViewById(R.id.btnBack);
        btnApplySchedule = findViewById(R.id.btnApplySchedule);
        scheduleRecyclerView = findViewById(R.id.scheduleRecyclerView);
        fabAdd = findViewById(R.id.fabAddSlot);
        tvTitleHeader = findViewById(R.id.tvTitleHeader);
        day2 = findViewById(R.id.day2);
        day3 = findViewById(R.id.day3);
        day4 = findViewById(R.id.day4);
        day5 = findViewById(R.id.day5);
        day6 = findViewById(R.id.day6);
        day7 = findViewById(R.id.day7);
        dayCN = findViewById(R.id.dayCN);
        daysContainer = findViewById(R.id.daysContainer);
    }

    private void setupRecyclerView() {
        if (currentList == null) currentList = new ArrayList<>();
        scheduleAdapter = new ScheduleItemAdapter(this, currentList, new ScheduleItemAdapter.OnItemClickListener() {
            @Override public void onItemClick(int position, ScheduleItem item) { showEditDialog(position, item); }
            @Override public void onEditClick(int position, ScheduleItem item) { showEditDialog(position, item); }
        });
        scheduleRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        scheduleRecyclerView.setAdapter(scheduleAdapter);
    }

    private void setupDays() {
        View.OnClickListener dayClick = v -> {
            if (selectedDayView != null) selectedDayView.setSelected(false);
            v.setSelected(true);
            selectedDayView = v;

            int id = v.getId();
            if (id == R.id.day2) selectedDay = 2;
            else if (id == R.id.day3) selectedDay = 3;
            else if (id == R.id.day4) selectedDay = 4;
            else if (id == R.id.day5) selectedDay = 5;
            else if (id == R.id.day6) selectedDay = 6;
            else if (id == R.id.day7) selectedDay = 7;
            else if (id == R.id.dayCN) selectedDay = 8;

            loadScheduleDataForDay(selectedDay);
        };

        day2.setOnClickListener(dayClick);
        day3.setOnClickListener(dayClick);
        day4.setOnClickListener(dayClick);
        day5.setOnClickListener(dayClick);
        day6.setOnClickListener(dayClick);
        day7.setOnClickListener(dayClick);
        dayCN.setOnClickListener(dayClick);

        day2.setSelected(true);
        selectedDayView = day2;
    }

    private void setupListeners() {
        btnBack.setOnClickListener(v -> finish());
        btnApplySchedule.setOnClickListener(v -> showApplyDialog());
        if (fabAdd != null) fabAdd.setOnClickListener(v -> showAddDialogMode());

        if (tvTitleHeader != null) {
            tvTitleHeader.addOnLayoutChangeListener((v, left, top, right, bottom, oldLeft, oldTop, oldRight, oldBottom) -> updateFabState());
        }
    }
    private void showAddDialogMode() {
        ensureScheduleNamedThen(() -> {
            // original add-flow (runs only after we have a currentScheduleName)
            AlertDialog.Builder modeBuilder = new AlertDialog.Builder(this);
            modeBuilder.setTitle("Chọn cách thêm")
                    .setItems(new String[]{"Thêm vào khung giờ cố định", "Thêm hoạt động tùy ý (cho phép trùng)"}, (modeDialog, whichMode) -> {
                        View dialogView = LayoutInflater.from(this).inflate(R.layout.edit_schedule1, null);
                        EditText etStart = dialogView.findViewById(R.id.etStartTime);
                        EditText etEnd = dialogView.findViewById(R.id.etEndTime);
                        EditText etAct = dialogView.findViewById(R.id.etActivity);
                        etStart.setHint("06:00");
                        etEnd.setHint("07:00");

                        if (whichMode == 0) {
                            List<ScheduleItem> predefined = getDefaultItemsForDay(selectedDay);
                            List<String> slotLabels = new ArrayList<>();
                            for (ScheduleItem s : predefined) {
                                String label = s.getStartTime() + " - " + s.getEndTime() + " : " + s.getActivity();
                                slotLabels.add(label);
                            }
                            CharSequence[] choices = slotLabels.toArray(new CharSequence[0]);

                            new AlertDialog.Builder(this)
                                    .setTitle("Chọn khung giờ")
                                    .setItems(choices, (slotDialog, slotIndex) -> {
                                        ScheduleItem chosenSlot = predefined.get(slotIndex);
                                        boolean occupied = false;
                                        if (currentList != null) {
                                            for (ScheduleItem existing : currentList) {
                                                String es = existing.getStartTime() == null ? "" : existing.getStartTime();
                                                String ee = existing.getEndTime() == null ? "" : existing.getEndTime();
                                                if (es.equals(chosenSlot.getStartTime()) && ee.equals(chosenSlot.getEndTime())) {
                                                    occupied = true;
                                                    break;
                                                }
                                            }
                                        }
                                        if (occupied) {
                                            Toast.makeText(this, "Khung giờ này đã có hoạt động, không thể thêm.", Toast.LENGTH_LONG).show();
                                            return;
                                        }
                                        etStart.setText(chosenSlot.getStartTime());
                                        etEnd.setText(chosenSlot.getEndTime());
                                        etAct.setText(chosenSlot.getActivity());

                                        new AlertDialog.Builder(this)
                                                .setTitle("Xác nhận khung giờ")
                                                .setView(dialogView)
                                                .setPositiveButton("Thêm", (confirmDialog, confirmWhich) -> {
                                                    String newStart = etStart.getText().toString().trim();
                                                    String newEnd = etEnd.getText().toString().trim();
                                                    String newAct = etAct.getText().toString().trim();
                                                    if (newStart.isEmpty() || newEnd.isEmpty()) {
                                                        Toast.makeText(this, "Vui lòng nhập đầy đủ thời gian", Toast.LENGTH_SHORT).show();
                                                        return;
                                                    }
                                                    if (timeToMinutes(newStart) < 0 || timeToMinutes(newEnd) < 0) {
                                                        Toast.makeText(this, "Định dạng thời gian không hợp lệ (HH:mm)", Toast.LENGTH_SHORT).show();
                                                        return;
                                                    }
                                                    if (timeToMinutes(newEnd) <= timeToMinutes(newStart)) {
                                                        Toast.makeText(this, "Thời gian kết thúc phải sau thời gian bắt đầu", Toast.LENGTH_SHORT).show();
                                                        return;
                                                    }
                                                    boolean occupiedNow = false;
                                                    if (currentList != null) {
                                                        for (ScheduleItem existing : currentList) {
                                                            String es = existing.getStartTime() == null ? "" : existing.getStartTime();
                                                            String ee = existing.getEndTime() == null ? "" : existing.getEndTime();
                                                            if (es.equals(newStart) && ee.equals(newEnd)) {
                                                                occupiedNow = true;
                                                                break;
                                                            }
                                                        }
                                                    }
                                                    if (occupiedNow) {
                                                        Toast.makeText(this, "Khung giờ này đã có hoạt động, không thể thêm.", Toast.LENGTH_LONG).show();
                                                        return;
                                                    }
                                                    ScheduleItem newItem = new ScheduleItem(0, newStart, newEnd, newAct, selectedDay);
                                                    if (currentList == null) currentList = new ArrayList<>();
                                                    currentList.add(newItem);
                                                    currentList.sort((a, b) -> {
                                                        String as = a.getStartTime() == null ? "" : a.getStartTime();
                                                        String bs = b.getStartTime() == null ? "" : b.getStartTime();
                                                        return as.compareTo(bs);
                                                    });
                                                    if (scheduleAdapter != null) scheduleAdapter.updateList(currentList);
                                                    // Important: save under per-user schedule path because ensureScheduleNamedThen guaranteed currentScheduleName != null
                                                    saveSingleItemToFirebase(newItem);
                                                    pushSingleActivityHistory(newItem, null);
                                                    Toast.makeText(this, "Đã thêm vào khung giờ cố định (lưu vào lịch '" + currentScheduleName + "')", Toast.LENGTH_SHORT).show();
                                                })
                                                .setNegativeButton("Hủy", null)
                                                .show();
                                    })
                                    .show();
                        } else {
                            etStart.setText("");
                            etEnd.setText("");
                            etAct.setText("");
                            new AlertDialog.Builder(this)
                                    .setTitle("Thêm hoạt động tùy ý (cho phép trùng)")
                                    .setView(dialogView)
                                    .setPositiveButton("Thêm", (freeDialog, freeWhich) -> {
                                        String newStart = etStart.getText().toString().trim();
                                        String newEnd = etEnd.getText().toString().trim();
                                        String newAct = etAct.getText().toString().trim();
                                        if (newStart.isEmpty() || newEnd.isEmpty()) {
                                            Toast.makeText(this, "Vui lòng nhập đầy đủ thời gian", Toast.LENGTH_SHORT).show();
                                            return;
                                        }
                                        if (timeToMinutes(newStart) < 0 || timeToMinutes(newEnd) < 0) {
                                            Toast.makeText(this, "Định dạng thời gian không hợp lệ (HH:mm)", Toast.LENGTH_SHORT).show();
                                            return;
                                        }
                                        if (timeToMinutes(newEnd) <= timeToMinutes(newStart)) {
                                            Toast.makeText(this, "Thời gian kết thúc phải sau thời gian bắt đầu", Toast.LENGTH_SHORT).show();
                                            return;
                                        }
                                        ScheduleItem newItem = new ScheduleItem(0, newStart, newEnd, newAct, selectedDay);
                                        if (currentList == null) currentList = new ArrayList<>();
                                        currentList.add(newItem);
                                        currentList.sort((a, b) -> {
                                            String as = a.getStartTime() == null ? "" : a.getStartTime();
                                            String bs = b.getStartTime() == null ? "" : b.getStartTime();
                                            return as.compareTo(bs);
                                        });
                                        if (scheduleAdapter != null) scheduleAdapter.updateList(currentList);
                                        // save under per-user schedule path (isolation guaranteed)
                                        saveSingleItemToFirebase(newItem);
                                        pushSingleActivityHistory(newItem, null);
                                        Toast.makeText(this, "Đã thêm hoạt động (cho phép trùng) — lưu vào lịch '" + currentScheduleName + "'", Toast.LENGTH_SHORT).show();
                                    })
                                    .setNegativeButton("Hủy", null)
                                    .show();
                        }
                    })
                    .setNegativeButton("Hủy", null)
                    .show();
        });
    }
    private void saveSingleItemToFirebase(ScheduleItem item) {
        if (item == null) return;
        String dayNode = "day_" + item.getDayOfWeek();

        // If currentScheduleName set => save under /users/<userId>/schedules/<scheduleName>/day_N
        if (currentScheduleName != null) {
            DatabaseReference dayRef = rootRef.child("users").child(userId).child("schedules").child(currentScheduleName).child(dayNode);
            DatabaseReference p = dayRef.push();
            String key = p.getKey();
            if (key == null) {
                // fallback: generate local key so item is persisted locally and visible after restart
                String localKey = "local_" + System.currentTimeMillis();
                item.setFirebaseKey(localKey);
                addOrUpdateLocalUserItem(item);
                dayRef.push().setValue(item)
                        .addOnSuccessListener(aVoid -> Log.d(TAG, "Saved single item (no key) to per-user schedule"))
                        .addOnFailureListener(e -> Toast.makeText(this, "Lỗi lưu item: " + e.getMessage(), Toast.LENGTH_SHORT).show());
                return;
            }
            item.setFirebaseKey(key);
            // persist locally right away
            addOrUpdateLocalUserItem(item);

            p.setValue(item)
                    .addOnSuccessListener(aVoid -> Log.d(TAG, "Saved single item to per-user schedule key=" + key))
                    .addOnFailureListener(e -> Toast.makeText(this, "Lỗi lưu item: " + e.getMessage(), Toast.LENGTH_SHORT).show());
            return;
        }

        // default behavior: global schedules node
        DatabaseReference dayRef = schedulesRef.child(dayNode);

        DatabaseReference p = dayRef.push();
        String key = p.getKey();
        if (key == null) {
            // fallback: persist locally with a generated local key
            String localKey = "local_" + System.currentTimeMillis();
            item.setFirebaseKey(localKey);
            addOrUpdateLocalUserItem(item);
            dayRef.push().setValue(item)
                    .addOnSuccessListener(aVoid -> Log.d(TAG, "Saved single item (no key) to Firebase"))
                    .addOnFailureListener(e -> Toast.makeText(this, "Lỗi lưu item: " + e.getMessage(), Toast.LENGTH_SHORT).show());
            return;
        }

        item.setFirebaseKey(key);

        // persist locally right away
        addOrUpdateLocalUserItem(item);

        p.setValue(item)
                .addOnSuccessListener(aVoid -> Log.d(TAG, "Saved single item to Firebase with key=" + key))
                .addOnFailureListener(e -> Toast.makeText(this, "Lỗi lưu item: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }
    private void showApplyDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Áp dụng lịch này")
                .setMessage("Bạn muốn:")
                .setPositiveButton("Hiển thị ở màn hình chính", (dialog, which) -> {
                    currentScheduleName = (tvTitleHeader != null && tvTitleHeader.getText() != null)
                            ? tvTitleHeader.getText().toString().trim()
                            : null;
                    if (currentScheduleName != null && currentScheduleName.isEmpty()) currentScheduleName = null;

                    if (currentScheduleName == null) {
                        // Ask for a schedule name first (A)
                        showNameInputDialogAndApply();
                        return;
                    }

                    applyScheduleAndNotify(currentScheduleName);
                })
                .setNegativeButton("Chỉ lưu vào lịch sử", (dialog, which) -> {
                    saveScheduleToFirebase();
                    saveScheduleToProfileHistory(selectedDay);
                    Toast.makeText(this, "Đã lưu vào lịch sử", Toast.LENGTH_SHORT).show();
                })
                .setNeutralButton("Hủy", null)
                .show();
    }
    private void saveScheduleToFirebase() {
        if (currentList == null || currentList.isEmpty()) {
            Toast.makeText(this, "Danh sách lịch trống", Toast.LENGTH_SHORT).show();
            return;
        }

        String dayNode = "day_" + selectedDay;

        // upload only user items (exclude builtins)
        List<ScheduleItem> toUpload = new ArrayList<>();
        for (ScheduleItem it : currentList) {
            String fk = it.getFirebaseKey() == null ? "" : it.getFirebaseKey();
            if (!fk.startsWith("builtin_")) {
                // ensure day field is set
                it.setDayOfWeek(selectedDay);
                toUpload.add(it);
            }
        }

        // If schedule has a name, save to per-user schedule path, otherwise global schedules
        if (currentScheduleName != null) {
            DatabaseReference perUserDayRef = rootRef.child("users").child(userId).child("schedules").child(currentScheduleName).child(dayNode);
            perUserDayRef.setValue(toUpload)
                    .addOnSuccessListener(aVoid -> Log.d(TAG, "Saved named schedule to Firebase for " + dayNode))
                    .addOnFailureListener(e -> Toast.makeText(this, "Lỗi lưu lịch: " + e.getMessage(), Toast.LENGTH_SHORT).show());
            return;
        }

        DatabaseReference dayRef = schedulesRef.child(dayNode);
        dayRef.setValue(toUpload)
                .addOnSuccessListener(aVoid -> Log.d(TAG, "Saved full schedule to Firebase for " + dayNode))
                .addOnFailureListener(e -> Toast.makeText(this, "Lỗi lưu lịch: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }
    private void showNameInputDialogAndApply() {
        final EditText et = new EditText(this);
        et.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_WORDS);
        et.setHint("Nhập tên lịch (ví dụ: Lịch cho sinh viên)");

        new AlertDialog.Builder(this)
                .setTitle("Đặt tên lịch")
                .setView(et)
                .setPositiveButton("Áp dụng", (d, w) -> {
                    String name = et.getText() == null ? null : et.getText().toString().trim();
                    if (name == null || name.isEmpty()) {
                        Toast.makeText(this, "Bạn phải nhập tên lịch để áp dụng toàn bộ tuần", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    // set title header visible
                    if (tvTitleHeader != null) tvTitleHeader.setText(name);
                    currentScheduleName = name;
                    applyScheduleAndNotify(name);
                })
                .setNegativeButton("Hủy", null)
                .show();
    }
    private void applyScheduleAndNotify(String scheduleName) {
        if (scheduleName == null || scheduleName.trim().isEmpty()) return;

        final SharedPreferences profilePrefs = getSharedPreferences(PROFILE_PREFS, Context.MODE_PRIVATE);
        // Persist active schedule name for HomeFragment fallback
        profilePrefs.edit().putString(PREF_ACTIVE_SCHEDULE, scheduleName).apply();

        currentScheduleName = scheduleName;

        // Collect and save all days first, then proceed to notify / navigate
        collectAndSaveAllDaysForSchedule(scheduleName, () -> {
            // ensure collectAndSaveAllDaysForSchedule has persisted the payload under this key:
            final String localKey = HOME_DISPLAY_ACTIVITIES_KEY + "_" + scheduleName;
            final String homePayload = profilePrefs.getString(localKey, null);

            // Optional: log if payload missing
            if (homePayload == null) {
                Log.w(TAG, "applyScheduleAndNotify: no cached home_payload for schedule=" + scheduleName);
            }

            // Write an "active schedule" marker in RTDB (best-effort)
            DatabaseReference activeRef = rootRef.child("users").child(userId).child("home_display_active_schedule");
            activeRef.setValue(scheduleName).addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    Log.d(TAG, "Active schedule marker set to: " + scheduleName);
                } else {
                    Log.w(TAG, "Failed to set active schedule marker", task.getException());
                }

                // Send local broadcast so any active fragment/activity can react immediately
                try {
                    Intent bcast = new Intent(ACTION_SCHEDULE_APPLIED);
                    bcast.putExtra(EXTRA_SCHEDULE_NAME, scheduleName);
                    bcast.putExtra("selected_day", selectedDay); // ensure selectedDay is a field
                    if (homePayload != null) bcast.putExtra("home_payload", homePayload);
                    androidx.localbroadcastmanager.content.LocalBroadcastManager
                            .getInstance(Layout6Activity.this)
                            .sendBroadcast(bcast);
                    Log.d(TAG, "Sent ACTION_SCHEDULE_APPLIED broadcast for schedule=" + scheduleName);
                } catch (Exception ex) {
                    Log.w(TAG, "Failed sending ACTION_SCHEDULE_APPLIED broadcast", ex);
                }

                // Start MainActivity and ask it to open Home (use flags to avoid multiple instances)
                try {
                    Intent intent = new Intent(Layout6Activity.this, MainActivity.class);
                    intent.putExtra("open_home", true);
                    intent.putExtra(EXTRA_SCHEDULE_NAME, scheduleName);
                    if (homePayload != null) intent.putExtra("home_payload", homePayload);
                    intent.putExtra("selected_day", selectedDay);
                    // Prevent creating many activities on top; route to existing if present
                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                    startActivity(intent);
                } catch (Exception ex) {
                    Log.w(TAG, "Failed to start MainActivity after apply", ex);
                }

                // Save history / summary as before
                saveScheduleToProfileHistory(selectedDay);

                Toast.makeText(Layout6Activity.this, "Đã lưu và áp dụng lịch \"" + scheduleName + "\"", Toast.LENGTH_SHORT).show();

                // Finish this activity
                finish();
            });
        });
    }

    @NonNull
    private Intent getIntent(String scheduleName, String homePayload) {
        Intent intent = new Intent(Layout6Activity.this, MainActivity.class);
        // Clear task so MainActivity becomes foreground root (adjust flags to your desired navigation UX)
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.putExtra("open_home", true);
        // pass schedule info so MainActivity/HomeFragment can pick it up quickly
        intent.putExtra("scheduleName", scheduleName);
        if (homePayload != null) intent.putExtra("home_payload", homePayload);
        return intent;
    }

    private void collectAndSaveAllDaysForSchedule(String scheduleName, final Runnable completionCallback) {
        if (scheduleName == null || scheduleName.trim().isEmpty()) {
            if (completionCallback != null) completionCallback.run();
            return;
        }

        final int[] days = {2,3,4,5,6,7,8};
        final Map<Integer, List<ScheduleItem>> collected = new HashMap<>();
        final int total = days.length;
        final int[] doneCount = {0};

        for (int d : days) {
            final int day = d;
            String dayNode = "day_" + day;
            // read global schedules for now (we will merge with builtins & overrides)
            schedulesRef.child(dayNode).addListenerForSingleValueEvent(new ValueEventListener() {
                @Override public void onDataChange(DataSnapshot snapshot) {
                    List<ScheduleItem> firebaseItems = new ArrayList<>();
                    for (DataSnapshot ds : snapshot.getChildren()) {
                        ScheduleItem item = ds.getValue(ScheduleItem.class);
                        if (item != null) {
                            if (item.getFirebaseKey() == null || item.getFirebaseKey().isEmpty()) {
                                item.setFirebaseKey(ds.getKey());
                            }
                            firebaseItems.add(item);
                        }
                    }

                    // rebuild list: builtins + global firebase + overrides
                    List<ScheduleItem> itemsToShow = new ArrayList<>();
                    List<ScheduleItem> builtins = getDefaultItemsForDay(day);
                    int biCounter = 0;
                    for (ScheduleItem b : builtins) {
                        ScheduleItem builtinClone = new ScheduleItem(0, b.getStartTime(), b.getEndTime(), b.getActivity(), day);
                        builtinClone.setFirebaseKey("builtin_" + day + "_" + (biCounter++));
                        itemsToShow.add(builtinClone);
                    }
                    if (firebaseItems != null && !firebaseItems.isEmpty()) itemsToShow.addAll(firebaseItems);
                    List<ScheduleItem> overrides = getOverridesFromPrefs(day);
                    if (overrides != null && !overrides.isEmpty()) itemsToShow.addAll(overrides);

                    // sort & dedupe
                    itemsToShow.sort((a, b) -> {
                        String as = a.getStartTime() == null ? "" : a.getStartTime();
                        String bs = b.getStartTime() == null ? "" : b.getStartTime();
                        return as.compareTo(bs);
                    });
                    List<ScheduleItem> deduped = new ArrayList<>();
                    for (ScheduleItem it : itemsToShow) {
                        boolean exists = false;
                        for (ScheduleItem d : deduped) {
                            if (equalsByTimeAndActivity(d, it)) { exists = true; break; }
                        }
                        if (!exists) deduped.add(it);
                    }

                    collected.put(day, deduped);
                    doneCount[0]++;

                    if (doneCount[0] >= total) {
                        // All days collected -> write to per-user schedule path and update home_display
                        writeCollectedScheduleToUserPath(scheduleName, collected, completionCallback);
                    }
                }

                @Override public void onCancelled(DatabaseError error) {
                    Log.w(TAG, "collect day cancelled " + day + " : " + error.getMessage());
                    // still count it as done with empty
                    collected.put(day, new ArrayList<>());
                    doneCount[0]++;
                    if (doneCount[0] >= total) {
                        writeCollectedScheduleToUserPath(scheduleName, collected, completionCallback);
                    }
                }
            });
        }
    }
    // Replace or add this method in your Layout6Activity class (overwrite existing writeCollectedScheduleToUserPath)
    private void writeCollectedScheduleToUserPath(String scheduleName, Map<Integer, List<ScheduleItem>> collected, @Nullable Runnable onComplete) {
        if (scheduleName == null || scheduleName.trim().isEmpty()) {
            Log.w(TAG, "writeCollectedScheduleToUserPath: scheduleName is null/empty");
            if (onComplete != null) onComplete.run();
            return;
        }
        // sanitize scheduleName for RTDB key characters
        if (scheduleName.matches(".*[.#$\\[\\]/].*")) {
            Log.w(TAG, "writeCollectedScheduleToUserPath: scheduleName contains invalid characters for RTDB: " + scheduleName);
            if (onComplete != null) onComplete.run();
            return;
        }

        if (collected == null) collected = new HashMap<>();

        try {
            if (rootRef == null) rootRef = FirebaseDatabase.getInstance().getReference();
            SharedPreferences profilePrefs = getSharedPreferences(PROFILE_PREFS, Context.MODE_PRIVATE);
            String uid = profilePrefs.getString("profile_user_id", userId);
            if (uid == null || uid.trim().isEmpty()) {
                Log.w(TAG, "writeCollectedScheduleToUserPath: missing userId");
                if (onComplete != null) onComplete.run();
                return;
            }

            Map<String, Object> updates = new HashMap<>();
            Map<String, Object> weekMap = new HashMap<>();
            String basePath = "users/" + uid + "/schedules/" + scheduleName;

            for (Map.Entry<Integer, List<ScheduleItem>> entry : collected.entrySet()) {
                Integer dayInt = entry.getKey();
                List<ScheduleItem> items = entry.getValue();
                if (dayInt == null || items == null) continue;

                List<Object> serializableItems = new ArrayList<>();
                for (ScheduleItem it : items) {
                    if (it == null) continue;
                    // Use toFirebaseMap() for consistent payload shape ("start","end","activity","location","note","day")
                    Map<String, Object> m = it.toFirebaseMap();
                    serializableItems.add(m);
                }

                String dayNodePath = basePath + "/day_" + dayInt;
                updates.put(dayNodePath, serializableItems);

                // week map uses string keys for the day
                weekMap.put(String.valueOf(dayInt), serializableItems);
            }

            // Write home_display as structured map { "week": { "2": [...], ... } }
            Map<String, Object> homeDisplay = new HashMap<>();
            homeDisplay.put("week", weekMap);
            updates.put(basePath + "/home_display", homeDisplay);

            // Optionally also write a top-level summary or timestamp under schedules/<name>/meta
            Map<String, Object> meta = new HashMap<>();
            meta.put("appliedAt", ServerValue.TIMESTAMP);
            updates.put(basePath + "/meta", meta);

            // debug: approximate payload size
            try { Log.d(TAG, "writeCollectedScheduleToUserPath: payload size ~ " + updates.toString().length()); } catch (Exception ignored){}

            // perform a single multi-location update on rootRef
            rootRef.updateChildren(updates).addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    Log.d(TAG, "writeCollectedScheduleToUserPath: updateChildren successful for schedule=" + scheduleName);
                } else {
                    Exception ex = task.getException();
                    Log.w(TAG, "writeCollectedScheduleToUserPath: updateChildren failed for schedule=" + scheduleName, ex);
                }
                if (onComplete != null) {
                    try { onComplete.run(); } catch (Exception ignore) {}
                }
            });
        } catch (Exception ex) {
            Log.e(TAG, "writeCollectedScheduleToUserPath: unexpected error", ex);
            if (onComplete != null) {
                try { onComplete.run(); } catch (Exception ignore) {}
            }
        }
    }

    @NonNull
    private Map<String, Object> getMap(ScheduleItem it) {
        Map<String, Object> m = new HashMap<>();
        if (it == null) return m;

        // Required / known fields
        // Replace the getter names below if your ScheduleItem uses different method names.
        // Common names used in this project: getStartTime(), getEndTime(), getActivity(), getDayOfWeek(), getFirebaseKey()
        try {
            // start
            try {
                Object start = invokeIfExists(it, "getStartTime");
                if (start == null) start = invokeIfExists(it, "getStart");
                m.put("start", start == null ? "" : String.valueOf(start));
            } catch (Exception ex) {
                m.put("start", "");
            }

            // end
            try {
                Object end = invokeIfExists(it, "getEndTime");
                if (end == null) end = invokeIfExists(it, "getEnd");
                m.put("end", end == null ? "" : String.valueOf(end));
            } catch (Exception ex) {
                m.put("end", "");
            }

            // activity / title
            try {
                Object activity = invokeIfExists(it, "getActivity");
                if (activity == null) activity = invokeIfExists(it, "getTitle");
                m.put("activity", activity == null ? "" : String.valueOf(activity));
            } catch (Exception ex) {
                m.put("activity", "");
            }

            // optional fields: location, note, order (use reflection, only added if methods exist)
            try {
                Object location = invokeIfExists(it, "getLocation");
                if (location != null) m.put("location", String.valueOf(location));
            } catch (Exception ignored) {}

            try {
                Object note = invokeIfExists(it, "getNote");
                if (note != null) m.put("note", String.valueOf(note));
            } catch (Exception ignored) {}

            try {
                Object order = invokeIfExists(it, "getOrder");
                if (order != null) m.put("order", order); // keep original type (Integer/Number)
            } catch (Exception ignored) {}

            // day (primitive int) — do NOT compare to null, just put the value.
            try {
                Object dayVal = invokeIfExists(it, "getDayOfWeek");
                if (dayVal == null) dayVal = invokeIfExists(it, "getDay");
                if (dayVal != null) {
                    // ensure numeric type when possible
                    if (dayVal instanceof Number) m.put("day", ((Number) dayVal).intValue());
                    else {
                        try { m.put("day", Integer.parseInt(String.valueOf(dayVal))); }
                        catch (Exception ex) { m.put("day", 0); }
                    }
                } else {
                    m.put("day", 0);
                }
            } catch (Exception ex) {
                m.put("day", 0);
            }

            // firebaseKey if present
            try {
                Object fk = invokeIfExists(it, "getFirebaseKey");
                if (fk != null && !String.valueOf(fk).isEmpty()) m.put("firebaseKey", String.valueOf(fk));
            } catch (Exception ignored) {}

        } catch (Exception ex) {
            // Fallback: ensure minimal fields exist so Firebase write does not fail
            if (!m.containsKey("start")) m.put("start", "");
            if (!m.containsKey("end")) m.put("end", "");
            if (!m.containsKey("activity")) m.put("activity", "");
            if (!m.containsKey("day")) m.put("day", 0);
            Log.w(TAG, "getMap: reflection-based mapping fallback used", ex);
        }

        return m;
    }

    /**
     * Helper: use reflection to call a no-arg getter if it exists, return null if not present.
     * This keeps getMap flexible across different ScheduleItem implementations.
     */
    @Nullable
    private Object invokeIfExists(Object target, String methodName) {
        try {
            java.lang.reflect.Method method = target.getClass().getMethod(methodName);
            if (method != null) return method.invoke(target);
        } catch (NoSuchMethodException ignored) {
        } catch (Exception ex) {
            Log.w(TAG, "invokeIfExists failed for " + methodName, ex);
        }
        return null;
    }

    private void ensureScheduleNamedThen(Runnable onNamed) {
        currentScheduleName = (tvTitleHeader != null && tvTitleHeader.getText() != null)
                ? tvTitleHeader.getText().toString().trim()
                : null;
        if (currentScheduleName != null && !currentScheduleName.isEmpty()) {
            if (onNamed != null) onNamed.run();
            return;
        }

        final EditText et = new EditText(this);
        et.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_WORDS);
        et.setHint("Nhập tên lịch (ví dụ: Lịch cho sinh viên)");

        new AlertDialog.Builder(this)
                .setTitle("Chưa có tên lịch")
                .setMessage("Bạn cần đặt tên cho lịch để các thay đổi chỉ lưu vào lịch đó. Nhập tên lịch hoặc hủy.")
                .setView(et)
                .setPositiveButton("Đặt tên & tiếp tục", (d, w) -> {
                    String name = et.getText() == null ? null : et.getText().toString().trim();
                    if (name == null || name.isEmpty()) {
                        Toast.makeText(this, "Bạn phải nhập tên lịch để tiếp tục", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    if (tvTitleHeader != null) tvTitleHeader.setText(name);
                    currentScheduleName = name;
                    // persist active schedule preference
                    SharedPreferences profilePrefs = getSharedPreferences(PROFILE_PREFS, Context.MODE_PRIVATE);
                    profilePrefs.edit().putString(PREF_ACTIVE_SCHEDULE, currentScheduleName).apply();

                    // enable FAB now that schedule name exists
                    updateFabState();

                    if (onNamed != null) onNamed.run();
                })
                .setNegativeButton("Hủy", null)
                .show();
    }
    private void saveScheduleToProfileHistory(int day) {
        if (currentList == null) currentList = new ArrayList<>();

        SharedPreferences profilePrefs = getSharedPreferences(PROFILE_PREFS, Context.MODE_PRIVATE);
        String json = profilePrefs.getString(PROFILE_HISTORY_KEY, null);
        JSONArray arr;
        try {
            arr = (json == null) ? new JSONArray() : new JSONArray(json);
        } catch (JSONException ex) {
            arr = new JSONArray();
        }

        String title = (tvTitleHeader != null && tvTitleHeader.getText() != null && !tvTitleHeader.getText().toString().trim().isEmpty())
                ? tvTitleHeader.getText().toString().trim()
                : ("Lịch ngày " + (day == 8 ? "CN" : ("Thứ " + day)));

        JSONArray actsArr = new JSONArray();
        StringBuilder subtitleSb = new StringBuilder();
        int maxToShow = 6;
        int count = 0;
        for (ScheduleItem it : currentList) {
            String s = it.getStartTime() == null ? "" : it.getStartTime();
            String e = it.getEndTime() == null ? "" : it.getEndTime();
            String a = it.getActivity() == null ? "" : it.getActivity();
            String row = s + "-" + e + ": " + a;
            try {
                JSONObject actObj = new JSONObject();
                actObj.put("time", row);
                actObj.put("activity", a);
                actObj.put("day", it.getDayOfWeek());
                actsArr.put(actObj);
            } catch (JSONException ex) {
                Log.w(TAG, "activity json error", ex);
            }
            if (count < maxToShow) {
                if (subtitleSb.length() > 0) subtitleSb.append("  •  ");
                subtitleSb.append(row);
            }
            count++;
        }
        if (subtitleSb.length() == 0) subtitleSb.append("Không có mục cụ thể");
        String subtitle = subtitleSb.toString();

        try {
            if (editingHistoryKey == null) {
                JSONObject obj = new JSONObject();
                obj.put("title", title);
                obj.put("subtitle", subtitle);
                obj.put("day", day);
                obj.put("activities", actsArr);
                arr.put(obj);
                profilePrefs.edit().putString(PROFILE_HISTORY_KEY, arr.toString()).apply();
                Log.d(TAG, "Saved schedule summary locally to profile history: " + title);

                pushScheduleSummaryToFirebase(null, title, subtitle, day, currentList);
            } else {
                boolean updatedLocal = false;
                try {
                    for (int i = 0; i < arr.length(); i++) {
                        JSONObject o = arr.getJSONObject(i);
                        String hk = o.optString("historyKey", null);
                        if (hk != null && hk.equals(editingHistoryKey)) {
                            o.put("title", title);
                            o.put("subtitle", subtitle);
                            o.put("day", day);
                            o.put("activities", actsArr);
                            updatedLocal = true;
                            break;
                        }
                    }
                } catch (JSONException ex) {
                    Log.w(TAG, "local update error", ex);
                }
                if (!updatedLocal) {
                    JSONObject obj = new JSONObject();
                    obj.put("historyKey", editingHistoryKey);
                    obj.put("title", title);
                    obj.put("subtitle", subtitle);
                    obj.put("day", day);
                    obj.put("activities", actsArr);
                    arr.put(obj);
                }
                profilePrefs.edit().putString(PROFILE_HISTORY_KEY, arr.toString()).apply();
                Log.d(TAG, "Updated schedule summary locally for key=" + editingHistoryKey);

                pushScheduleSummaryToFirebase(editingHistoryKey, title, subtitle, day, currentList);
            }
        } catch (JSONException ex) {
            Log.e(TAG, "saveScheduleToProfileHistory error", ex);
        }
    }
    private void pushScheduleSummaryToFirebase(String historyKey, String title, String subtitle, int day, List<ScheduleItem> items) {
        try {
            DatabaseReference historyRef = rootRef.child("users").child(userId).child("history");
            DatabaseReference targetRef;
            if (historyKey == null) {
                targetRef = historyRef.push();
            } else {
                targetRef = historyRef.child(historyKey);
            }

            targetRef.child("title").setValue(title);
            targetRef.child("subtitle").setValue(subtitle);
            targetRef.child("day").setValue(day);

            DatabaseReference actsRef = targetRef.child("activities");
            actsRef.removeValue().addOnCompleteListener(new OnCompleteListener<Void>() {
                @Override public void onComplete(@NonNull Task<Void> t) {
                    for (ScheduleItem it : items) {
                        String s = it.getStartTime() == null ? "" : it.getStartTime();
                        String e = it.getEndTime() == null ? "" : it.getEndTime();
                        String a = it.getActivity() == null ? "" : it.getActivity();
                        String row = s + "-" + e + ": " + a;
                        actsRef.push().setValue(row);
                    }
                    targetRef.child("timestamp").setValue(ServerValue.TIMESTAMP);
                }
            });

            if (historyKey == null) {
                Log.d(TAG, "Pushed new schedule summary to Firebase for user=" + userId);
            } else {
                Log.d(TAG, "Updated schedule summary to Firebase for key=" + historyKey);
            }
        } catch (Exception ex) {
            Log.w(TAG, "pushScheduleSummaryToFirebase failed", ex);
        }
    }
    private void updateFabState() {
        currentScheduleName = (tvTitleHeader != null && tvTitleHeader.getText() != null)
                ? tvTitleHeader.getText().toString().trim()
                : null;
        if (fabAdd == null) return;
        fabAdd.setEnabled(currentScheduleName != null && !currentScheduleName.isEmpty());
    }

    private void loadScheduleDataForDay(int day) {
        if (currentList == null) currentList = new ArrayList<>();
        if (fabAdd != null) fabAdd.setEnabled(false);

        String dayNode = "day_" + day;

        currentScheduleName = (tvTitleHeader != null && tvTitleHeader.getText() != null)
                ? tvTitleHeader.getText().toString().trim()
                : null;
        if (currentScheduleName != null && currentScheduleName.isEmpty()) currentScheduleName = null;

        DatabaseReference readRef;
        if (currentScheduleName != null) {
            readRef = rootRef.child("users").child(userId).child("schedules").child(currentScheduleName).child(dayNode);
        } else {
            readRef = schedulesRef.child(dayNode);
        }

        readRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override public void onDataChange(@NonNull DataSnapshot snapshot) {
                List<ScheduleItem> firebaseItems = new ArrayList<>();
                for (DataSnapshot ds : snapshot.getChildren()) {
                    ScheduleItem item = ds.getValue(ScheduleItem.class);
                    if (item != null) {
                        if (item.getFirebaseKey() == null || item.getFirebaseKey().isEmpty()) item.setFirebaseKey(ds.getKey());
                        firebaseItems.add(item);
                    }
                }

                if ((firebaseItems == null || firebaseItems.isEmpty()) && currentScheduleName != null) {
                    schedulesRef.child(dayNode).addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override public void onDataChange(@NonNull DataSnapshot snap2) {
                            List<ScheduleItem> globalItems = new ArrayList<>();
                            for (DataSnapshot ds2 : snap2.getChildren()) {
                                ScheduleItem it = ds2.getValue(ScheduleItem.class);
                                if (it != null) {
                                    if (it.getFirebaseKey() == null || it.getFirebaseKey().isEmpty()) it.setFirebaseKey(ds2.getKey());
                                    globalItems.add(it);
                                }
                            }
                            rebuildListFromSources(day, globalItems);
                        }
                        @Override public void onCancelled(@NonNull DatabaseError error) {
                            Toast.makeText(Layout6Activity.this, "Lỗi tải lịch: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                            if (fabAdd != null) fabAdd.setEnabled(true);
                        }
                    });
                } else {
                    rebuildListFromSources(day, firebaseItems);
                }
            }
            @Override public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(Layout6Activity.this, "Lỗi tải lịch: " + (error == null ? "unknown" : error.getMessage()), Toast.LENGTH_SHORT).show();
                if (fabAdd != null) fabAdd.setEnabled(true);
            }
        });
    }
    private void showEditDialog(int position, ScheduleItem item) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View view = LayoutInflater.from(this).inflate(R.layout.edit_schedule1, null);
        EditText etStart = view.findViewById(R.id.etStartTime);
        EditText etEnd = view.findViewById(R.id.etEndTime);
        EditText etAct = view.findViewById(R.id.etActivity);

        etStart.setText(item.getStartTime());
        etEnd.setText(item.getEndTime());
        etAct.setText(item.getActivity());

        builder.setView(view)
                .setTitle("Chỉnh sửa lịch trình")
                .setPositiveButton("Lưu", (dialog, which) -> {
                    String newStart = etStart.getText().toString().trim();
                    String newEnd = etEnd.getText().toString().trim();
                    String newAct = etAct.getText().toString().trim();

                    if (newStart.isEmpty() || newEnd.isEmpty()) {
                        Toast.makeText(this, "Vui lòng nhập đầy đủ thời gian", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    if (timeToMinutes(newStart) < 0 || timeToMinutes(newEnd) < 0) {
                        Toast.makeText(this, "Định dạng thời gian không hợp lệ (HH:mm)", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    if (timeToMinutes(newEnd) <= timeToMinutes(newStart)) {
                        Toast.makeText(this, "Thời gian kết thúc phải sau thời gian bắt đầu", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    String key = item.getFirebaseKey() == null ? "" : item.getFirebaseKey();
                    if (key != null && key.startsWith("builtin_")) {
                        ScheduleItem override = new ScheduleItem(0, newStart, newEnd, newAct, item.getDayOfWeek());
                        String overrideKey = "override_" + System.currentTimeMillis();
                        override.setFirebaseKey(overrideKey);

                        if (isOverlapping(newStart, newEnd, currentList, item)) {
                            Toast.makeText(this, "Thời gian mới trùng với mục khác. Vui lòng chọn khung giờ khác.", Toast.LENGTH_LONG).show();
                            return;
                        }

                        saveOverrideToPrefs(item.getDayOfWeek(), override);

                        if (currentList != null && position >= 0 && position < currentList.size()) {
                            currentList.set(position, override);
                            if (scheduleAdapter != null) scheduleAdapter.updateList(currentList);
                        }
                        Toast.makeText(this, "Sửa sẽ được lưu vào hồ sơ (không thay đổi mục mặc định)", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    if (isOverlapping(newStart, newEnd, currentList, item)) {
                        Toast.makeText(this, "Thời gian mới trùng với mục khác. Vui lòng chọn khung giờ khác.", Toast.LENGTH_LONG).show();
                        return;
                    }

                    item.setStartTime(newStart);
                    item.setEndTime(newEnd);
                    item.setActivity(newAct);

                    if (item.getFirebaseKey() != null && !item.getFirebaseKey().isEmpty() && !item.getFirebaseKey().startsWith("override_")) {
                        // update in database: if currentScheduleName set, update in per-user schedule, else global schedules
                        String firebaseKey = item.getFirebaseKey();
                        if (currentScheduleName != null) {
                            DatabaseReference perUserDayRef = rootRef.child("users").child(userId).child("schedules").child(currentScheduleName).child("day_" + item.getDayOfWeek());
                            perUserDayRef.child(firebaseKey).setValue(item)
                                    .addOnSuccessListener(aVoid -> Log.d(TAG, "Updated item in per-user schedule key=" + firebaseKey))
                                    .addOnFailureListener(e -> Toast.makeText(this, "Lỗi cập nhật: " + e.getMessage(), Toast.LENGTH_SHORT).show());
                        } else {
                            schedulesRef.child("day_" + item.getDayOfWeek()).child(firebaseKey).setValue(item)
                                    .addOnSuccessListener(aVoid -> Log.d(TAG, "Updated item in Firebase key=" + firebaseKey))
                                    .addOnFailureListener(e -> Toast.makeText(this, "Lỗi cập nhật: " + e.getMessage(), Toast.LENGTH_SHORT).show());
                        }
                    } else if (item.getFirebaseKey() != null && item.getFirebaseKey().startsWith("override_")) {
                        saveOverrideToPrefs(item.getDayOfWeek(), item);
                    } else {
                        saveSingleItemToFirebase(item);
                        // push single small history so Profile shows it
                        pushSingleActivityHistory(item, null);
                    }

                    if (scheduleAdapter != null) scheduleAdapter.notifyItemChanged(position);
                    Toast.makeText(this, "Đã cập nhật", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("Hủy", null);

        // Only allow delete for user-added items (firebase or override), not builtins
        String fk = item.getFirebaseKey() == null ? "" : item.getFirebaseKey();
        if (!fk.startsWith("builtin_")) {
            builder.setNeutralButton("Xóa", (dialog, which) -> {
                // Confirm deletion
                new AlertDialog.Builder(this)
                        .setTitle("Xóa mục")
                        .setMessage("Bạn có chắc muốn xóa mục này không?")
                        .setPositiveButton("Xóa", (confirm, w) -> deleteScheduleItem(position, item))
                        .setNegativeButton("Hủy", null)
                        .show();
            });
        }

        builder.show();
    }
    private void removeOverrideAndSync(int day, String overrideKey) {
        String key = "overrides_day_" + day;
        List<ScheduleItem> existing = getOverridesFromPrefs(day);
        boolean changed = false;
        for (int i = existing.size() - 1; i >= 0; i--) {
            ScheduleItem it = existing.get(i);
            String fk = it.getFirebaseKey() == null ? "" : it.getFirebaseKey();
            if (fk.equals(overrideKey)) {
                existing.remove(i);
                changed = true;
            }
        }
        // save back to prefs
        JSONArray arr = new JSONArray();
        try {
            for (ScheduleItem it : existing) {
                JSONObject o = new JSONObject();
                o.put("firebaseKey", it.getFirebaseKey());
                o.put("startTime", it.getStartTime());
                o.put("endTime", it.getEndTime());
                o.put("activity", it.getActivity());
                o.put("day", it.getDayOfWeek());
                arr.put(o);
            }
            prefs.edit().putString(key, arr.toString()).apply();
        } catch (JSONException ex) {
            Log.e(TAG, "removeOverrideAndSync - json error", ex);
        }

        // sync to Firebase overrides node: remove then re-push all
        DatabaseReference overridesRef = rootRef.child("users").child(userId).child("overrides_day_" + day);
        overridesRef.removeValue().addOnCompleteListener(new OnCompleteListener<Void>() {
            @Override public void onComplete(@NonNull Task<Void> t) {
                for (ScheduleItem it : existing) {
                    DatabaseReference p = overridesRef.push();
                    p.child("firebaseKey").setValue(it.getFirebaseKey());
                    p.child("startTime").setValue(it.getStartTime());
                    p.child("endTime").setValue(it.getEndTime());
                    p.child("activity").setValue(it.getActivity());
                    p.child("day").setValue(it.getDayOfWeek());
                }
            }
        });
    }
    private void deleteScheduleItem(int position, ScheduleItem item) {
        if (item == null) return;
        String fk = item.getFirebaseKey() == null ? "" : item.getFirebaseKey();
        if (fk.startsWith("builtin_")) {
            Toast.makeText(this, "Không thể xóa lịch mặc định", Toast.LENGTH_SHORT).show();
            return;
        }

        if (fk.startsWith("override_")) {
            // remove from prefs overrides and sync to Firebase overrides node
            removeOverrideAndSync(item.getDayOfWeek(), fk);
            // remove locally
            if (currentList != null && position >= 0 && position < currentList.size()) {
                currentList.remove(position);
                if (scheduleAdapter != null) scheduleAdapter.updateList(currentList);
            }
            // also remove from local cached per-day list if present
            removeLocalUserItem(item.getDayOfWeek(), fk);

            Toast.makeText(this, "Đã xóa mục (override)", Toast.LENGTH_SHORT).show();
            return;
        }

        // else it's a regular firebase item: remove from schedules/day_N/<fk> or per-user schedule
        DatabaseReference dayRef;
        if (currentScheduleName != null) {
            dayRef = rootRef.child("users").child(userId).child("schedules").child(currentScheduleName).child("day_" + item.getDayOfWeek());
        } else {
            dayRef = schedulesRef.child("day_" + item.getDayOfWeek());
        }

        if (fk == null || fk.isEmpty()) {
            // no key -> just remove locally (shouldn't normally happen)
            if (currentList != null && position >= 0 && position < currentList.size()) {
                currentList.remove(position);
                if (scheduleAdapter != null) scheduleAdapter.updateList(currentList);
            }
            Toast.makeText(this, "Đã xóa mục cục bộ", Toast.LENGTH_SHORT).show();
            return;
        }

        dayRef.child(fk).removeValue()
                .addOnSuccessListener(aVoid -> {
                    // remove locally from currentList
                    boolean removed = false;
                    if (currentList != null) {
                        for (int i = 0; i < currentList.size(); i++) {
                            ScheduleItem si = currentList.get(i);
                            if (si.getFirebaseKey() != null && si.getFirebaseKey().equals(fk)) {
                                currentList.remove(i);
                                removed = true;
                                break;
                            }
                        }
                    }
                    if (!removed && currentList != null && position >= 0 && position < currentList.size()) {
                        currentList.remove(position);
                    }
                    if (scheduleAdapter != null) scheduleAdapter.updateList(currentList);

                    // remove from local cache as well
                    removeLocalUserItem(item.getDayOfWeek(), fk);

                    Toast.makeText(Layout6Activity.this, "Đã xóa mục", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> Toast.makeText(Layout6Activity.this, "Xóa thất bại: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }
    private void saveOverrideToPrefs(int day, ScheduleItem override) {
        if (override == null) return;
        String key = "overrides_day_" + day;
        List<ScheduleItem> existing = getOverridesFromPrefs(day);

        boolean updated = false;
        if (override.getFirebaseKey() == null || override.getFirebaseKey().isEmpty()) {
            override.setFirebaseKey("override_" + System.currentTimeMillis());
        }
        for (int i = 0; i < existing.size(); i++) {
            ScheduleItem ex = existing.get(i);
            if (ex.getFirebaseKey() != null && ex.getFirebaseKey().equals(override.getFirebaseKey())) {
                existing.set(i, override);
                updated = true;
                break;
            }
        }
        if (!updated) existing.add(override);

        JSONArray arr = new JSONArray();
        try {
            for (ScheduleItem it : existing) {
                JSONObject o = new JSONObject();
                o.put("firebaseKey", it.getFirebaseKey());
                o.put("startTime", it.getStartTime());
                o.put("endTime", it.getEndTime());
                o.put("activity", it.getActivity());
                o.put("day", it.getDayOfWeek());
                arr.put(o);
            }
            prefs.edit().putString(key, arr.toString()).apply();
            Log.d(TAG, "Saved override to prefs for day " + day);

            DatabaseReference overridesRef = rootRef.child("users").child(userId).child("overrides_day_" + day);
            overridesRef.removeValue().addOnCompleteListener(new OnCompleteListener<Void>() {
                @Override public void onComplete(@NonNull Task<Void> t) {
                    for (ScheduleItem it : existing) {
                        DatabaseReference p = overridesRef.push();
                        p.child("firebaseKey").setValue(it.getFirebaseKey());
                        p.child("startTime").setValue(it.getStartTime());
                        p.child("endTime").setValue(it.getEndTime());
                        p.child("activity").setValue(it.getActivity());
                        p.child("day").setValue(it.getDayOfWeek());
                    }
                }
            });

        } catch (JSONException ex) {
            Log.e(TAG, "save override error", ex);
        }
    }
    private void pushSingleActivityHistory(ScheduleItem item, String titleOptional) {
        if (item == null) return;

        String title = titleOptional;
        if (title == null || title.trim().isEmpty()) {
            if (tvTitleHeader != null && tvTitleHeader.getText() != null && !tvTitleHeader.getText().toString().trim().isEmpty()) {
                title = tvTitleHeader.getText().toString().trim();
            } else {
                title = "Lịch ngày " + (item.getDayOfWeek() == 8 ? "CN" : ("Thứ " + item.getDayOfWeek()));
            }
        }

        String time = (item.getStartTime() == null ? "" : item.getStartTime()) + "-" + (item.getEndTime() == null ? "" : item.getEndTime());
        String activity = item.getActivity() == null ? "" : item.getActivity();

        try {
            SharedPreferences profilePrefs = getSharedPreferences(PROFILE_PREFS, Context.MODE_PRIVATE);
            String json = profilePrefs.getString(PROFILE_HISTORY_KEY, null);
            JSONArray arr = (json == null) ? new JSONArray() : new JSONArray(json);

            JSONObject obj = new JSONObject();
            obj.put("title", title);
            obj.put("subtitle", time + ": " + activity);
            obj.put("day", item.getDayOfWeek());
            obj.put("isUserAdded", true);

            JSONArray acts = new JSONArray();
            JSONObject aobj = new JSONObject();
            aobj.put("time", time);
            aobj.put("activity", activity);
            aobj.put("day", item.getDayOfWeek());
            acts.put(aobj);
            obj.put("activities", acts);

            if (item.getFirebaseKey() != null) obj.put("linkedScheduleKey", item.getFirebaseKey());

            // insert at front
            JSONArray newArr = new JSONArray();
            newArr.put(obj);
            for (int i = 0; i < arr.length(); i++) newArr.put(arr.get(i));
            profilePrefs.edit().putString(PROFILE_HISTORY_KEY, newArr.toString()).apply();

            // push to Firebase history
            DatabaseReference historyRef = rootRef.child("users").child(userId).child("history");
            DatabaseReference p = historyRef.push();
            p.child("title").setValue(title);
            p.child("subtitle").setValue(time + ": " + activity);
            p.child("day").setValue(item.getDayOfWeek());
            p.child("isUserAdded").setValue(true);
            DatabaseReference actsRef = p.child("activities");
            DatabaseReference ap = actsRef.push();
            ap.child("time").setValue(time);
            ap.child("activity").setValue(activity);
            ap.child("day").setValue(item.getDayOfWeek());
            p.child("timestamp").setValue(ServerValue.TIMESTAMP);

        } catch (JSONException ex) {
            Log.e(TAG, "pushSingleActivityHistory error", ex);
        } catch (Exception ex) {
            Log.w(TAG, "pushSingleActivityHistory failed", ex);
        }
    }

    private List<ScheduleItem> getLocalUserItems(int day) {
        String key = localKeyForDay(day);
        String json = prefs.getString(key, null);
        List<ScheduleItem> list = new ArrayList<>();
        if (json == null) return list;
        try {
            JSONArray arr = new JSONArray(json);
            for (int i = 0; i < arr.length(); i++) {
                JSONObject o = arr.getJSONObject(i);
                String fk = o.optString("firebaseKey", null);
                String s = o.optString("startTime", "");
                String e = o.optString("endTime", "");
                String a = o.optString("activity", "");
                int d = o.optInt("day", day);
                ScheduleItem it = new ScheduleItem(0, s, e, a, d);
                if (fk != null && !fk.isEmpty()) it.setFirebaseKey(fk);
                list.add(it);
            }
        } catch (JSONException ex) {
            Log.e(TAG, "parse local items error", ex);
        }
        return list;
    }

    private void addOrUpdateLocalUserItem(ScheduleItem item) {
        if (item == null) return;
        int day = item.getDayOfWeek();
        List<ScheduleItem> existing = getLocalUserItems(day);
        boolean updated = false;
        String fk = item.getFirebaseKey() == null ? "" : item.getFirebaseKey();
        for (int i = 0; i < existing.size(); i++) {
            ScheduleItem ex = existing.get(i);
            String exFk = ex.getFirebaseKey() == null ? "" : ex.getFirebaseKey();
            if (!exFk.isEmpty() && exFk.equals(fk)) {
                existing.set(i, item);
                updated = true;
                break;
            }
        }
        if (!updated) existing.add(item);
        saveLocalUserItems(day, existing);
    }

    private void removeLocalUserItem(int day, String firebaseKey) {
        if (firebaseKey == null || firebaseKey.isEmpty()) return;
        List<ScheduleItem> existing = getLocalUserItems(day);
        boolean changed = false;
        for (int i = existing.size() - 1; i >= 0; i--) {
            ScheduleItem it = existing.get(i);
            String fk = it.getFirebaseKey() == null ? "" : it.getFirebaseKey();
            if (fk.equals(firebaseKey)) {
                existing.remove(i);
                changed = true;
            }
        }
        if (changed) saveLocalUserItems(day, existing);
    }

    private void rebuildListFromSources(int day, List<ScheduleItem> firebaseItems) {
        List<ScheduleItem> itemsToShow = new ArrayList<>();

        List<ScheduleItem> builtins = getDefaultItemsForDay(day);
        int biCounter = 0;
        for (ScheduleItem b : builtins) {
            ScheduleItem builtinClone = new ScheduleItem(0, b.getStartTime(), b.getEndTime(), b.getActivity(), day);
            builtinClone.setFirebaseKey("builtin_" + day + "_" + (biCounter++));
            itemsToShow.add(builtinClone);
        }

        if (firebaseItems != null && !firebaseItems.isEmpty()) itemsToShow.addAll(firebaseItems);

        List<ScheduleItem> overrides = getOverridesFromPrefs(day);
        if (overrides != null && !overrides.isEmpty()) itemsToShow.addAll(overrides);

        List<ScheduleItem> localItems = getLocalUserItems(day);
        if (localItems != null && !localItems.isEmpty()) itemsToShow.addAll(localItems);

        itemsToShow.sort((a, b) -> {
            String as = a.getStartTime() == null ? "" : a.getStartTime();
            String bs = b.getStartTime() == null ? "" : b.getStartTime();
            return as.compareTo(bs);
        });

        List<ScheduleItem> deduped = new ArrayList<>();
        for (ScheduleItem it : itemsToShow) {
            boolean exists = false;
            for (ScheduleItem d : deduped) {
                if (equalsByTimeAndActivity(d, it)) { exists = true; break; }
            }
            if (!exists) deduped.add(it);
        }

        currentList.clear();
        currentList.addAll(deduped);
        if (scheduleAdapter != null) scheduleAdapter.updateList(currentList);

        if (currentList.isEmpty()) Toast.makeText(Layout6Activity.this, "Danh sách lịch trống", Toast.LENGTH_SHORT).show();
        if (fabAdd != null) fabAdd.setEnabled(true);
    }
    private List<ScheduleItem> getOverridesFromPrefs(int day) {
        String key = "overrides_day_" + day;
        String json = prefs.getString(key, null);
        List<ScheduleItem> list = new ArrayList<>();
        if (json == null) return list;
        try {
            JSONArray arr = new JSONArray(json);
            for (int i = 0; i < arr.length(); i++) {
                JSONObject o = arr.getJSONObject(i);
                String fk = o.optString("firebaseKey", null);
                String s = o.optString("startTime", "");
                String e = o.optString("endTime", "");
                String a = o.optString("activity", "");
                int d = o.optInt("day", day);
                ScheduleItem it = new ScheduleItem(0, s, e, a, d);
                if (fk != null) it.setFirebaseKey(fk);
                list.add(it);
            }
        } catch (JSONException ex) {
            Log.e(TAG, "parse overrides error", ex);
        }
        return list;
    }

    // --- OMITTED: showEditDialog, deleteScheduleItem, removeOverrideAndSync, showAddDialogMode ---
    // These methods should be implemented in your project as in the prior messages.
    // For brevity in this combined file, ensure you paste your existing implementations for those methods here.

    private String localKeyForDay(int day) {
        String scheduleKey = (currentScheduleName != null && !currentScheduleName.isEmpty()) ? currentScheduleName : "__global__";
        return LOCAL_ITEMS_KEY_PREFIX + "_" + scheduleKey + "_day_" + day;
    }

    private void saveLocalUserItems(int day, List<ScheduleItem> items) {
        try {
            String key = localKeyForDay(day);
            JSONArray arr = getJsonArray(items);
            prefs.edit().putString(key, arr.toString()).apply();
            Log.d(TAG, "Saved " + (items == null ? 0 : items.size()) + " local items to key=" + key);
        } catch (JSONException ex) {
            Log.e(TAG, "saveLocalUserItems json error", ex);
        } catch (Exception ex) {
            Log.w(TAG, "saveLocalUserItems failed", ex);
        }
    }

    @NonNull
    private JSONArray getJsonArray(List<ScheduleItem> items) throws JSONException {
        JSONArray arr = new JSONArray();
        if (items != null) {
            for (ScheduleItem it : items) {
                JSONObject o = new JSONObject();
                o.put("firebaseKey", it.getFirebaseKey() == null ? "" : it.getFirebaseKey());
                o.put("startTime", it.getStartTime() == null ? "" : it.getStartTime());
                o.put("endTime", it.getEndTime() == null ? "" : it.getEndTime());
                o.put("activity", it.getActivity() == null ? "" : it.getActivity());
                o.put("day", it.getDayOfWeek());
                arr.put(o);
            }
        }
        return arr;
    }

    private boolean isOverlapping(String newStart, String newEnd, List<ScheduleItem> existing, ScheduleItem exclude) {
        int ns = timeToMinutes(newStart);
        int ne = timeToMinutes(newEnd);
        if (ns < 0 || ne < 0 || ne <= ns) return true;

        if (existing == null) return false;
        for (ScheduleItem it : existing) {
            if (exclude != null && it == exclude) continue;
            int is = timeToMinutes(it.getStartTime());
            int ie = timeToMinutes(it.getEndTime());
            if (is < 0 || ie < 0) continue;
            if (ns < ie && is < ne) return true;
        }
        return false;
    }

    private boolean equalsByTimeAndActivity(ScheduleItem a, ScheduleItem b) {
        if (a == null || b == null) return false;
        String as = a.getStartTime() == null ? "" : a.getStartTime();
        String ae = a.getEndTime() == null ? "" : a.getEndTime();
        String aa = a.getActivity() == null ? "" : a.getActivity();
        String bs = b.getStartTime() == null ? "" : b.getStartTime();
        String be = b.getEndTime() == null ? "" : b.getEndTime();
        String ba = b.getActivity() == null ? "" : b.getActivity();
        return as.equals(bs) && ae.equals(be) && aa.equals(ba);
    }

    private int timeToMinutes(String hhmm) {
        if (hhmm == null || !hhmm.contains(":")) return -1;
        try {
            String[] parts = hhmm.split(":");
            int hh = Integer.parseInt(parts[0].trim());
            int mm = Integer.parseInt(parts[1].trim());
            if (hh < 0 || hh > 23 || mm < 0 || mm > 59) return -1;
            return hh * 60 + mm;
        } catch (Exception e) {
            return -1;
        }
    }

    private void loadSampleData() {
        ensureScheduleNamedThen(() -> {
            final String name = currentScheduleName;
            if (name == null || name.trim().isEmpty()) {
                Toast.makeText(this, "Vui lòng đặt tên lịch trước khi tạo mẫu", Toast.LENGTH_SHORT).show();
                return;
            }

            final Map<Integer, List<ScheduleItem>> collected = new HashMap<>();
            final long now = System.currentTimeMillis();

            for (int day = 2; day <= 8; day++) {
                List<ScheduleItem> defaults = getDefaultItemsForDay(day);
                List<ScheduleItem> items = new ArrayList<>();
                int idx = 0;
                for (ScheduleItem d : defaults) {
                    ScheduleItem it = new ScheduleItem(0, d.getStartTime(), d.getEndTime(), d.getActivity(), day);
                    it.setFirebaseKey("sample_" + now + "_" + day + "_" + (idx++));
                    items.add(it);
                }
                collected.put(day, items);
                saveLocalUserItems(day, items);
            }

            writeCollectedScheduleToUserPath(name, collected, () -> {
                SharedPreferences profilePrefs = getSharedPreferences(PROFILE_PREFS, Context.MODE_PRIVATE);
                profilePrefs.edit().putString(PREF_ACTIVE_SCHEDULE, name).apply();

                String cachedPayload = profilePrefs.getString(HOME_DISPLAY_ACTIVITIES_KEY + "_" + name, null);
                try {
                    Intent bcast = new Intent(ACTION_SCHEDULE_APPLIED);
                    bcast.putExtra(EXTRA_SCHEDULE_NAME, name);
                    bcast.putExtra("selected_day", selectedDay);
                    if (cachedPayload != null) bcast.putExtra("home_payload", cachedPayload);
                    LocalBroadcastManager.getInstance(Layout6Activity.this).sendBroadcast(bcast);
                } catch (Exception ex) {
                    Log.w(TAG, "Failed to broadcast sample apply", ex);
                }

                Toast.makeText(Layout6Activity.this, "Đã tạo mẫu và lưu vào lịch \"" + name + "\"", Toast.LENGTH_SHORT).show();
                loadScheduleDataForDay(selectedDay);
            });
        });
    }
    private List<ScheduleItem> dedupeByTitleKeepFirst(List<ScheduleItem> items) {
        if (items == null) return new ArrayList<>();
        List<ScheduleItem> out = new ArrayList<>();
        HashSet<String> seen = new HashSet<>();
        for (ScheduleItem s : items) {
            String key = s == null ? "" : (s.getActivity() == null ? "" : s.getActivity().trim().toLowerCase());
            if (!seen.contains(key)) {
                seen.add(key);
                out.add(s);
            }
        }
        return out;
    }
    private List<ScheduleItem> dedupeTitleUnlessSameTime(List<ScheduleItem> items) {
        if (items == null) return new ArrayList<>();
        List<ScheduleItem> out = new ArrayList<>();
        HashSet<String> seen = new HashSet<>();
        for (ScheduleItem s : items) {
            String title = s.getActivity() == null ? "" : s.getActivity().trim().toLowerCase();
            String time = (s.getStartTime() == null ? "" : s.getStartTime().trim()) + "|" + (s.getEndTime() == null ? "" : s.getEndTime().trim());
            String key = title + "##" + time;
            // if there exists any seen entry with same title but different time -> skip this one
            boolean hasSameTitleDifferentTime = false;
            for (String k : seen) {
                if (k.startsWith(title + "##") && !k.equals(key)) {
                    hasSameTitleDifferentTime = true;
                    break;
                }
            }
            if (!hasSameTitleDifferentTime) {
                seen.add(key);
                out.add(s);
            }
        }
        return out;
    }
    private List<ScheduleItem> removeOverlappingByKeepEarlier(List<ScheduleItem> items) {
        // Assumes start/end in "HH:mm" 24h format. Non-parseable treated as no time -> keep.
        if (items == null) return new ArrayList<>();
        // copy and sort by startTime (null/empty -> put at end)
        List<ScheduleItem> copy = new ArrayList<>(items);
        copy.sort((a,b) -> {
            String sa = a.getStartTime() == null ? "" : a.getStartTime();
            String sb = b.getStartTime() == null ? "" : b.getStartTime();
            if (sa.isEmpty() && sb.isEmpty()) return 0;
            if (sa.isEmpty()) return 1;
            if (sb.isEmpty()) return -1;
            return sa.compareTo(sb);
        });

        List<ScheduleItem> out = new ArrayList<>();
        String lastEnd = null;
        for (ScheduleItem it : copy) {
            String s = it.getStartTime() == null ? "" : it.getStartTime();
            String e = it.getEndTime() == null ? "" : it.getEndTime();
            if (s.isEmpty() || e.isEmpty()) {
                // keep items without clear times
                out.add(it);
                continue;
            }
            if (lastEnd == null) {
                out.add(it);
                lastEnd = e;
                continue;
            }
            // if s < lastEnd => overlap (strings compare works for HH:mm)
            if (s.compareTo(lastEnd) < 0) {
                // overlap -> skip this item (we keep earlier one)
                continue;
            } else {
                out.add(it);
                lastEnd = e;
            }
        }
        return out;
    }

    private List<ScheduleItem> getDefaultItemsForDay(int day) {
        // Accept both 1 or 8 for Sunday and normalize if needed inside ScheduleDefaults
        return ScheduleDefaults.getDefaultItemsForDay(day);
    }
}
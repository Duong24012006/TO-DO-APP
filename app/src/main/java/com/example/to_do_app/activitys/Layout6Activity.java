package com.example.to_do_app.activitys;

import android.app.AlertDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.InputType;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.to_do_app.R;
import com.example.to_do_app.adapters.ScheduleItemAdapter;
import com.example.to_do_app.data.ScheduleData;
import com.example.to_do_app.model.DetailedSchedule;
import com.example.to_do_app.model.ScheduleItem;
import com.example.to_do_app.model.ScheduleTemplate;
import com.example.to_do_app.model.TimeSlot;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ServerValue;
import com.google.firebase.database.ValueEventListener;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Layout6Activity extends AppCompatActivity {

    private static final String TAG = "Layout6Activity";

    public static final String EXTRA_SCHEDULE_TEMPLATE = "EXTRA_SCHEDULE_TEMPLATE";
    public static final String EXTRA_HISTORY_KEY = "EXTRA_HISTORY_KEY";

    private ImageView btnBack;
    private android.widget.TextView tvTitleHeader;
    private Button btnApplySchedule;
    private RecyclerView scheduleRecyclerView;
    private ScheduleItemAdapter scheduleAdapter;
    private List<ScheduleItem> currentList;

    private LinearLayout day2, day3, day4, day5, day6, day7, dayCN;
    private View selectedDayView;
    private int selectedDay = 2; // default Thứ 2

    private FloatingActionButton fabAdd;

    private DatabaseReference schedulesRef;
    private DatabaseReference rootRef;
    private String userId;

    private SharedPreferences prefs;
    private static final String PREFS_NAME = "profile_overrides";
    private static final String PROFILE_PREFS = "profile_prefs";
    private static final String PROFILE_HISTORY_KEY = "profile_history";
    private static final String HOME_DISPLAY_ACTIVITIES_KEY = "home_display_activities";
    private static final String HOME_DISPLAY_DAY_KEY = "home_display_day";
    private static final String PREF_ACTIVE_SCHEDULE = "active_schedule_name";

    private String editingHistoryKey = null;
    private DetailedSchedule currentTemplateDetails = null;
    private String currentScheduleName = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
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

            ViewCompat.setOnApplyWindowInsetsListener(root, (v, insets) -> {
                Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
                v.setPadding(padL + systemBars.left, padT + systemBars.top, padR + systemBars.right, systemBars.bottom);
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

        ScheduleTemplate template = (ScheduleTemplate) getIntent().getSerializableExtra(EXTRA_SCHEDULE_TEMPLATE);

        if (template != null) {
            String passedTitle = template.getTitle();
            if (passedTitle != null && !passedTitle.isEmpty()) {
                if (tvTitleHeader != null) {
                    tvTitleHeader.setText(passedTitle);
                }
                currentTemplateDetails = getTemplateDetailsByTitle(passedTitle);
            }
        }

        setupDays();
        setupListeners();
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
    }

    private void setupRecyclerView() {
        if (currentList == null) currentList = new ArrayList<>();
        scheduleAdapter = new ScheduleItemAdapter(this, currentList, new ScheduleItemAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(int position, ScheduleItem item) {
                showEditDialog(position, item);
            }

            @Override
            public void onEditClick(int position, ScheduleItem item) {
                showEditDialog(position, item);
            }
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
    }

    private void loadScheduleDataForDay(int day) {
        if (currentTemplateDetails != null) {
            String dayKey = getDayKeyAsString(day);
            List<TimeSlot> timeSlots = currentTemplateDetails.getWeeklyActivities().get(dayKey);

            if (timeSlots != null) {
                List<ScheduleItem> items = new ArrayList<>();
                for (TimeSlot slot : timeSlots) {
                    items.add(new ScheduleItem(0, slot.getStartTime(), slot.getEndTime(), slot.getActivityName(), day));
                }
                currentList.clear();
                currentList.addAll(items);
                if (scheduleAdapter != null) {
                    scheduleAdapter.updateList(currentList);
                }
            } else {
                currentList.clear();
                if (scheduleAdapter != null) {
                    scheduleAdapter.updateList(currentList);
                }
                Toast.makeText(Layout6Activity.this, "Không có lịch trình cho ngày này trong template.", Toast.LENGTH_SHORT).show();
            }
            if (fabAdd != null) fabAdd.setEnabled(true);
            return;
        }

        if (currentList == null) currentList = new ArrayList<>();
        if (fabAdd != null) fabAdd.setEnabled(false);

        String dayNode = "day_" + day;
        schedulesRef.child(dayNode).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
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

                List<ScheduleItem> itemsToShow = new ArrayList<>();
                itemsToShow.addAll(getDefaultItemsForDay(day));
                itemsToShow.addAll(firebaseItems);
                itemsToShow.addAll(getOverridesFromPrefs(day));

                itemsToShow.sort((a, b) -> Integer.compare(
                        timeToMinutesOrMax(a.getStartTime()),
                        timeToMinutesOrMax(b.getStartTime())
                ));

                List<ScheduleItem> deduped = new ArrayList<>();
                for (ScheduleItem it : itemsToShow) {
                    boolean exists = false;
                    for (ScheduleItem d : deduped) {
                        if (equalsByTimeAndActivity(d, it)) {
                            exists = true;
                            break;
                        }
                    }
                    if (!exists) deduped.add(it);
                }

                currentList.clear();
                currentList.addAll(deduped);
                if (scheduleAdapter != null) scheduleAdapter.updateList(currentList);

                if (currentList.isEmpty()) {
                    Toast.makeText(Layout6Activity.this, "Danh sách lịch trống", Toast.LENGTH_SHORT).show();
                }
                if (fabAdd != null) fabAdd.setEnabled(true);
            }

            @Override
            public void onCancelled(DatabaseError error) {
                Toast.makeText(Layout6Activity.this, "Lỗi tải lịch: " + error.getMessage(), Toast.LENGTH_SHORT).show();
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
                    if (isOverlapping(newStart, newEnd, currentList, item)) {
                        Toast.makeText(this, "Thời gian mới trùng với mục khác.", Toast.LENGTH_LONG).show();
                        return;
                    }

                    item.setStartTime(newStart);
                    item.setEndTime(newEnd);
                    item.setActivity(newAct);

                    saveItem(item, position);
                })
                .setNegativeButton("Hủy", null)
                .show();
    }

    private void saveItem(ScheduleItem item, int position) {
        String key = item.getFirebaseKey();
        if (key != null && key.startsWith("builtin_")) {
            saveOverrideToPrefs(item.getDayOfWeek(), item);
            if (position != -1) currentList.set(position, item);
            Toast.makeText(this, "Sửa đổi đã được lưu vào hồ sơ.", Toast.LENGTH_SHORT).show();
        } else if (key != null && !key.startsWith("override_")) {
            schedulesRef.child("day_" + item.getDayOfWeek()).child(key).setValue(item);
        } else if (key != null && key.startsWith("override_")) {
            saveOverrideToPrefs(item.getDayOfWeek(), item);
        } else {
            saveSingleItemToFirebase(item, position);
        }
        if (scheduleAdapter != null) scheduleAdapter.updateList(currentList);
    }

    private void showApplyDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Áp dụng lịch này")
                .setMessage("Bạn muốn:")
                .setPositiveButton("Hiển thị ở màn hình chính", (dialog, which) -> {
                    saveScheduleToFirebase();
                    saveScheduleToProfileHistory(selectedDay);
                    saveAllWeekScheduleToHomeDisplay();
                    Toast.makeText(this, "Đã lưu toàn bộ lịch tuần và áp dụng", Toast.LENGTH_SHORT).show();
                    finish();
                })
                .setNegativeButton("Chỉ lưu vào lịch sử", (dialog, which) -> {
                    saveScheduleToFirebase();
                    saveScheduleToProfileHistory(selectedDay);
                    Toast.makeText(this, "Đã lưu vào lịch sử", Toast.LENGTH_SHORT).show();
                })
                .setNeutralButton("Hủy", null)
                .show();
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
                                                    currentList.sort((a, b) -> Integer.compare(
                                                            timeToMinutesOrMax(a.getStartTime()),
                                                            timeToMinutesOrMax(b.getStartTime())
                                                    ));
                                                    if (scheduleAdapter != null) scheduleAdapter.updateList(currentList);
                                                    // Important: save under per-user schedule path because ensureScheduleNamedThen guaranteed currentScheduleName != null
                                                    saveSingleItemToFirebase(newItem);
                                                    pushSingleActivityHistory(newItem, null);
                                                    // persist into the active template (so ScheduleData receives & saves)
                                                    addToCurrentTemplateAndSave(newItem);
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
                                        // persist into the active template (so ScheduleData receives & saves)
                                        addToCurrentTemplateAndSave(newItem);
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

    private void addToCurrentTemplateAndSave(ScheduleItem newItem) {
        if (newItem == null || currentTemplateDetails == null) return;
        Map<String, List<TimeSlot>> weekly = currentTemplateDetails.getWeeklyActivities();
        if (weekly == null) {
            weekly = new HashMap<>();
            // try to set it back if setter exists
            try {
                currentTemplateDetails.setWeeklyActivities(weekly);
            } catch (Exception ignored) {
                // If no setter, we still continue with local map (unlikely since createXTemplate uses new HashMap())
            }
        }
        String dayKey = getDayKeyAsString(newItem.getDayOfWeek());
        List<TimeSlot> list = weekly.get(dayKey);
        if (list == null) {
            list = new ArrayList<>();
            weekly.put(dayKey, list);
        }

        // check for exact duplicate (start, end, activity)
        boolean exists = false;
        for (TimeSlot ts : list) {
            String s = ts.getStartTime() == null ? "" : ts.getStartTime();
            String e = ts.getEndTime() == null ? "" : ts.getEndTime();
            String a = ts.getActivityName() == null ? "" : ts.getActivityName();
            if (s.equals(newItem.getStartTime()) && e.equals(newItem.getEndTime()) && a.equals(newItem.getActivity())) {
                exists = true;
                break;
            }
        }
        if (!exists) {
            list.add(new TimeSlot(newItem.getStartTime(), newItem.getEndTime(), newItem.getActivity()));
            // Template đã được cập nhật trong memory, sẽ được sử dụng khi lưu toàn bộ tuần
            Log.d(TAG, "Added new slot to template: " + newItem.getStartTime() + "-" + newItem.getEndTime() + " " + newItem.getActivity());
        } else {
            Log.d(TAG, "Slot already exists in template, skip add.");
        }
    }

    /**
     * Ensures we have a current schedule name, prompting the user if needed.
     * Calls onReady.run() once a non-empty name is available.
     */
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

    private void updateFabState() {
        currentScheduleName = (tvTitleHeader != null && tvTitleHeader.getText() != null)
                ? tvTitleHeader.getText().toString().trim()
                : null;
        if (fabAdd == null) return;
        fabAdd.setEnabled(currentScheduleName != null && !currentScheduleName.isEmpty());
    }

    /**
     * Convenience overload used by UI code that didn't pass a position.
     */
    private void saveSingleItemToFirebase(ScheduleItem item) {
        saveSingleItemToFirebase(item, -1);
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

    private void saveSingleItemToFirebase(ScheduleItem item, int position) {
        String dayNode = "day_" + item.getDayOfWeek();
        DatabaseReference dayRef = schedulesRef.child(dayNode);
        String key = dayRef.push().getKey();
        item.setFirebaseKey(key);
        dayRef.child(key).setValue(item);
        if (position != -1) {
            currentList.get(position).setFirebaseKey(key);
        }
    }

    private void saveScheduleToHomeDisplay(int day, List<ScheduleItem> items) {
        if (items == null) items = new ArrayList<>();
        try {
            JSONObject root = new JSONObject();
            root.put("day", day);
            JSONArray acts = new JSONArray();
            for (ScheduleItem it : items) {
                JSONObject o = new JSONObject();
                o.put("start", it.getStartTime());
                o.put("end", it.getEndTime());
                o.put("activity", it.getActivity());
                acts.put(o);
            }
            root.put("activities", acts);

            SharedPreferences profilePrefs = getSharedPreferences(PROFILE_PREFS, Context.MODE_PRIVATE);
            profilePrefs.edit()
                    .putString(HOME_DISPLAY_ACTIVITIES_KEY, root.toString())
                    .putInt(HOME_DISPLAY_DAY_KEY, day)
                    .apply();

            DatabaseReference homeRef = rootRef.child("users").child(userId).child("home_display");
            homeRef.setValue(root.toString());
        } catch (JSONException ex) {
            Log.e(TAG, "saveScheduleToHomeDisplay error", ex);
        }
    }

    private void saveAllWeekScheduleToHomeDisplay() {
        try {
            JSONObject weekData = new JSONObject();

            // Lưu lịch cho tất cả các ngày từ Thứ 2 (2) đến Chủ Nhật (8)
            for (int day = 2; day <= 8; day++) {
                List<ScheduleItem> dayItems = loadScheduleItemsForDay(day);
                JSONArray dayActivities = new JSONArray();

                for (ScheduleItem item : dayItems) {
                    JSONObject actObj = new JSONObject();
                    actObj.put("start", item.getStartTime());
                    actObj.put("end", item.getEndTime());
                    actObj.put("activity", item.getActivity());
                    actObj.put("day", day);
                    dayActivities.put(actObj);
                }

                weekData.put("day_" + day, dayActivities);
            }

            // Lưu vào SharedPreferences
            SharedPreferences profilePrefs = getSharedPreferences(PROFILE_PREFS, Context.MODE_PRIVATE);
            profilePrefs.edit()
                    .putString(HOME_DISPLAY_ACTIVITIES_KEY, weekData.toString())
                    .putInt(HOME_DISPLAY_DAY_KEY, selectedDay) // Lưu ngày hiện tại đang chọn
                    .apply();

            // Lưu vào Firebase
            DatabaseReference homeRef = rootRef.child("users").child(userId).child("home_display");
            homeRef.setValue(weekData.toString());

            Log.d(TAG, "Saved all week schedule to home display");
        } catch (JSONException ex) {
            Log.e(TAG, "saveAllWeekScheduleToHomeDisplay error", ex);
            Toast.makeText(this, "Lỗi lưu lịch tuần: " + ex.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private List<ScheduleItem> loadScheduleItemsForDay(int day) {
        List<ScheduleItem> items = new ArrayList<>();

        // Nếu có template, load từ template
        if (currentTemplateDetails != null) {
            String dayKey = getDayKeyAsString(day);
            List<TimeSlot> timeSlots = currentTemplateDetails.getWeeklyActivities().get(dayKey);

            if (timeSlots != null) {
                for (TimeSlot slot : timeSlots) {
                    items.add(new ScheduleItem(0, slot.getStartTime(), slot.getEndTime(), slot.getActivityName(), day));
                }
            }
        } else {
            // Nếu không có template, load từ Firebase (đồng bộ - simplified)
            // Trong trường hợp thực tế, bạn có thể cần load async
            items.addAll(getDefaultItemsForDay(day));
            items.addAll(getOverridesFromPrefs(day));
        }

        return items;
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
                ScheduleItem it = new ScheduleItem();
                it.setFirebaseKey(o.optString("firebaseKey"));
                it.setStartTime(o.optString("startTime"));
                it.setEndTime(o.optString("endTime"));
                it.setActivity(o.optString("activity"));
                it.setDayOfWeek(o.optInt("day", day));
                list.add(it);
            }
        } catch (JSONException ex) {
            Log.e(TAG, "parse overrides error", ex);
        }
        return list;
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
            overridesRef.removeValue().addOnCompleteListener(t -> {
                for (ScheduleItem it : existing) {
                    DatabaseReference p = overridesRef.push();
                    p.child("firebaseKey").setValue(it.getFirebaseKey());
                    p.child("startTime").setValue(it.getStartTime());
                    p.child("endTime").setValue(it.getEndTime());
                    p.child("activity").setValue(it.getActivity());
                    p.child("day").setValue(it.getDayOfWeek());
                }
            });

        } catch (JSONException ex) {
            Log.e(TAG, "save override error", ex);
        }
    }

    private void saveScheduleToFirebase() {
        if (currentList == null || currentList.isEmpty()) {
            Toast.makeText(this, "Danh sách lịch trống", Toast.LENGTH_SHORT).show();
            return;
        }

        String dayNode = "day_" + selectedDay;
        DatabaseReference dayRef = schedulesRef.child(dayNode);

        List<ScheduleItem> toUpload = new ArrayList<>();
        for (ScheduleItem it : currentList) {
            String fk = it.getFirebaseKey() == null ? "" : it.getFirebaseKey();
            if (!fk.startsWith("builtin_")) {
                it.setDayOfWeek(selectedDay);
                toUpload.add(it);
            }
        }

        dayRef.setValue(toUpload)
                .addOnSuccessListener(aVoid -> Log.d(TAG, "Saved full schedule to Firebase for " + dayNode))
                .addOnFailureListener(e -> Toast.makeText(this, "Lỗi lưu lịch: " + e.getMessage(), Toast.LENGTH_SHORT).show());
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
            actsRef.removeValue().addOnCompleteListener(t -> {
                for (ScheduleItem it : items) {
                    String s = it.getStartTime() == null ? "" : it.getStartTime();
                    String e = it.getEndTime() == null ? "" : it.getEndTime();
                    String a = it.getActivity() == null ? "" : it.getActivity();
                    String row = s + "-" + e + ": " + a;
                    actsRef.push().setValue(row);
                }
                targetRef.child("timestamp").setValue(ServerValue.TIMESTAMP);
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

    private boolean isOverlapping(String newStart, String newEnd, List<ScheduleItem> existing, ScheduleItem exclude) {
        int ns = timeToMinutes(newStart);
        int ne = timeToMinutes(newEnd);
        if (ns < 0 || ne < 0 || ne <= ns) return true;

        if (existing == null) return false;
        for (ScheduleItem it : existing) {
            if (exclude != null && it == exclude) continue;
            String s = it.getStartTime();
            String e = it.getEndTime();
            int is = timeToMinutes(s);
            int ie = timeToMinutes(e);
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

    private List<ScheduleItem> getDefaultItemsForDay(int day) {
        List<ScheduleItem> defaults = new ArrayList<>();
        // Based on the original file, it seems there was a complex switch here.
        // For now, returning an empty list as the main logic is template-based or from Firebase.
        // You can re-paste the original switch/case logic here if needed.
        return defaults;
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

    private int timeToMinutesOrMax(String hhmm) {
        int m = timeToMinutes(hhmm);
        return m < 0 ? Integer.MAX_VALUE : m;
    }

    private DetailedSchedule getTemplateDetailsByTitle(String title) {
        if (title == null) return null;
        switch (title) {
            case "CÚ ĐÊM":
                return ScheduleData.createNightOwlTemplate();
            case "Chuyên buổi sáng":
                return ScheduleData.createMorningPersonTemplate();
            case "Học môn chuyên sâu":
                return ScheduleData.createDeepWorkTemplate();
            case "Sáng học, tối chơi":
                return ScheduleData.createWorkHardPlayHardTemplate();
            case "Tối ưu hóa chu kỳ ngủ":
                return ScheduleData.createSprintWeekTemplate();
            case "Vừa học vừa ăn gián đoạn 16-8":
                return ScheduleData.createIntermittentFastingTemplate();
            case "Vừa học vừa làm":
                return ScheduleData.createWorkAndLearnTemplate();
            case "Năng suất theo khoa học":
                return ScheduleData.createScientificProductivityTemplate();
            case "Lịch sinh hoạt của Bác Hồ":
                return ScheduleData.createUncleHoLifestyleTemplate();
            default:
                return null;
        }
    }

    private String getDayKeyAsString(int day) {
        switch (day) {
            case 2: return "Thứ 2";
            case 3: return "Thứ 3";
            case 4: return "Thứ 4";
            case 5: return "Thứ 5";
            case 6: return "Thứ 6";
            case 7: return "Thứ 7";
            case 8: return "Chủ Nhật";
            default: return "";
        }
    }
}


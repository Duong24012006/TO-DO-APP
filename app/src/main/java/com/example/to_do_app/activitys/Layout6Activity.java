package com.example.to_do_app.activitys;

import android.app.AlertDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
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
import java.util.List;

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

    private String editingHistoryKey = null;
    private DetailedSchedule currentTemplateDetails = null;

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

                itemsToShow.sort((a, b) -> a.getStartTime().compareTo(b.getStartTime()));

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
                    saveScheduleToHomeDisplay(selectedDay, currentList);
                    Toast.makeText(this, "Đã lưu và áp dụng", Toast.LENGTH_SHORT).show();
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
        new AlertDialog.Builder(this)
                .setTitle("Chọn cách thêm")
                .setItems(new String[]{"Thêm vào khung giờ cố định", "Thêm hoạt động tùy ý"}, (modeDialog, whichMode) -> {
                    if (whichMode == 0) {
                        //showAddPredefinedSlotDialog();
                    } else {
                        //showAddFreeActivityDialog();
                    }
                })
                .show();
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
        // ... (implementation remains the same)
    }

    private void saveScheduleToFirebase() {
        // ... (implementation remains the same)
    }

    private void saveScheduleToProfileHistory(int day) {
        // ... (implementation remains the same)
    }

    private boolean isOverlapping(String newStart, String newEnd, List<ScheduleItem> existing, ScheduleItem exclude) {
        // ... (implementation remains the same)
        return false;
    }

    private boolean equalsByTimeAndActivity(ScheduleItem a, ScheduleItem b) {
        // ... (implementation remains the same)
        return false;
    }

    private List<ScheduleItem> getDefaultItemsForDay(int day) {
        // This method may not be needed if templates are used, but kept for non-template mode
        return new ArrayList<>();
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

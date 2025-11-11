package com.example.to_do_app.activitys;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
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
import androidx.cardview.widget.CardView;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.to_do_app.R;
import com.example.to_do_app.adapters.ScheduleItemAdapter;
import com.example.to_do_app.model.ScheduleItem;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.database.ServerValue;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 * Layout6Activity — updated with:
 * - Add modes (predefined slot / free activity)
 * - Save new items into schedules/day_N nodes
 * - Push single activity history entries (isUserAdded=true)
 * - Save "home display" both to SharedPreferences and to Firebase under /users/<userId>/home_display
 *
 * Notes:
 * - ScheduleItem must include: startTime, endTime, activity, dayOfWeek, firebaseKey (getters/setters).
 * - HomeFragment listens to /users/<userId>/home_display in realtime (see HomeFragment).
 */
public class Layout6Activity extends AppCompatActivity {

    private static final String TAG = "Layout6Activity";

    public static final String EXTRA_TEMPLATE_TITLE = "EXTRA_TEMPLATE_TITLE";
    public static final String EXTRA_TEMPLATE_DESCRIPTION = "EXTRA_TEMPLATE_DESCRIPTION";
    public static final String EXTRA_HISTORY_KEY = "EXTRA_HISTORY_KEY";

    private ImageView btnBack;
    private android.widget.TextView tvTitleHeader;
    private Button btnApplySchedule;
    private RecyclerView scheduleRecyclerView;
    private ScheduleItemAdapter scheduleAdapter;
    private List<ScheduleItem> currentList;

    // Day cards
    private LinearLayout day2, day3, day4, day5, day6, day7, dayCN;
    private View selectedDayView;
    private int selectedDay = 2; // default Thứ 2

    private LinearLayout daysContainer;
    private FloatingActionButton fabAdd;

    // Firebase
    private DatabaseReference schedulesRef; // schedules/day_N
    private DatabaseReference rootRef; // root
    private String userId;

    // Profile (overrides) storage
    private static final String PREFS_NAME = "profile_overrides";
    private SharedPreferences prefs;

    // Profile history prefs (local copy)
    private static final String PROFILE_PREFS = "profile_prefs";
    private static final String PROFILE_HISTORY_KEY = "profile_history";

    // Home display keys (local)
    private static final String HOME_DISPLAY_ACTIVITIES_KEY = "home_display_activities"; // JSON
    private static final String HOME_DISPLAY_DAY_KEY = "home_display_day";

    // If editing an existing applied history entry, this holds its Firebase history key
    private String editingHistoryKey = null;

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
                v.setPadding(
                        padL + systemBars.left,
                        padT + systemBars.top,
                        padR + systemBars.right,
                        systemBars.bottom
                );
                return insets;
            });
        }


        // userId stored in profile prefs (shared with ProfileFragment)
        SharedPreferences profilePrefs = getSharedPreferences(PROFILE_PREFS, Context.MODE_PRIVATE);
        userId = profilePrefs.getString("profile_user_id", null);
        if (userId == null) {
            userId = "user_" + System.currentTimeMillis();
            profilePrefs.edit().putString("profile_user_id", userId).apply();
        }

        bindViews();

        // selected day and optional history key (editing)
        selectedDay = getIntent().getIntExtra("selected_day", selectedDay);
        editingHistoryKey = getIntent().getStringExtra(EXTRA_HISTORY_KEY);

        setupRecyclerView();

        String passedTitle = getIntent().getStringExtra(EXTRA_TEMPLATE_TITLE);
        String passedDescription = getIntent().getStringExtra(EXTRA_TEMPLATE_DESCRIPTION);
        ArrayList<String> passedTags = getIntent().getStringArrayListExtra("EXTRA_TEMPLATE_TAGS");

        if (passedTitle != null && !passedTitle.isEmpty() && tvTitleHeader != null) {
            tvTitleHeader.setText(passedTitle);
        }

        boolean hasTemplate = (passedTitle != null && !passedTitle.isEmpty())
                || (passedDescription != null && !passedDescription.isEmpty())
                || (passedTags != null && !passedTags.isEmpty());

        if (hasTemplate) {
            // show preview but DO NOT add to currentList (prevents duplicate/triple)
            // If you need a preview in list, handle separately in UI (not in currentList)
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
        daysContainer = findViewById(R.id.daysContainer);
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

    /**
     * loadScheduleDataForDay
     * - rebuilds currentList from builtins + firebase user items + overrides
     * - dedupes by start+end+activity
     */
    private void loadScheduleDataForDay(int day) {
        if (currentList == null) currentList = new ArrayList<>();

        // disable FAB until load done to avoid add before data is present
        if (fabAdd != null) fabAdd.setEnabled(false);

        String dayNode = "day_" + day;
        schedulesRef.child(dayNode).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                // collect firebase items
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

                // rebuild list from builtins + firebase + overrides
                List<ScheduleItem> itemsToShow = new ArrayList<>();

                List<ScheduleItem> builtins = getDefaultItemsForDay(day);
                int biCounter = 0;
                for (ScheduleItem b : builtins) {
                    // clone builtin so we don't modify original list's fields unexpectedly
                    ScheduleItem builtinClone = new ScheduleItem(0, b.getStartTime(), b.getEndTime(), b.getActivity(), day);
                    builtinClone.setFirebaseKey("builtin_" + day + "_" + (biCounter++));
                    itemsToShow.add(builtinClone);
                }

                if (firebaseItems != null && !firebaseItems.isEmpty()) itemsToShow.addAll(firebaseItems);

                List<ScheduleItem> overrides = getOverridesFromPrefs(day);
                if (overrides != null && !overrides.isEmpty()) itemsToShow.addAll(overrides);

                // sort by start time
                itemsToShow.sort((a, b) -> {
                    String as = a.getStartTime() == null ? "" : a.getStartTime();
                    String bs = b.getStartTime() == null ? "" : b.getStartTime();
                    return as.compareTo(bs);
                });

                // dedupe by start+end+activity
                List<ScheduleItem> deduped = new ArrayList<>();
                for (ScheduleItem it : itemsToShow) {
                    boolean exists = false;
                    for (ScheduleItem d : deduped) {
                        if (equalsByTimeAndActivity(d, it)) { exists = true; break; }
                    }
                    if (!exists) deduped.add(it);
                }

                // replace currentList completely
                currentList.clear();
                currentList.addAll(deduped);
                if (scheduleAdapter != null) scheduleAdapter.updateList(currentList);

                if (currentList.isEmpty()) {
                    Toast.makeText(Layout6Activity.this, "Danh sách lịch trống", Toast.LENGTH_SHORT).show();
                }

                // re-enable FAB
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

                    if (timeToMinutes(newStart) < 0 || timeToMinutes(newEnd) < 0) {
                        Toast.makeText(this, "Định dạng thời gian không hợp lệ (HH:mm)", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    if (timeToMinutes(newEnd) <= timeToMinutes(newStart)) {
                        Toast.makeText(this, "Thời gian kết thúc phải sau thời gian bắt đầu", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    String key = item.getFirebaseKey() == null ? "" : item.getFirebaseKey();
                    if (key.startsWith("builtin_")) {
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
                        String firebaseKey = item.getFirebaseKey();
                        schedulesRef.child("day_" + item.getDayOfWeek()).child(firebaseKey).setValue(item)
                                .addOnSuccessListener(aVoid -> Log.d(TAG, "Updated item in Firebase key=" + firebaseKey))
                                .addOnFailureListener(e -> Toast.makeText(this, "Lỗi cập nhật: " + e.getMessage(), Toast.LENGTH_SHORT).show());
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
                .setNegativeButton("Hủy", null)
                .show();
    }

    private void showApplyDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Áp dụng lịch này")
                .setMessage("Bạn muốn:")
                .setPositiveButton("Hiển thị ở màn hình chính", (dialog, which) -> {
                    saveScheduleToFirebase();
                    saveScheduleToProfileHistory(selectedDay);
                    // save to both prefs and Firebase home_display
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

    /**
     * showAddDialogMode - two modes: predefined slot (no dup) and free activity (allow overlap)
     */
    private void showAddDialogMode() {
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
                                                saveSingleItemToFirebase(newItem);
                                                // push single small history so Profile shows it
                                                pushSingleActivityHistory(newItem, null);
                                                Toast.makeText(this, "Đã thêm vào khung giờ cố định", Toast.LENGTH_SHORT).show();
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
                                    saveSingleItemToFirebase(newItem);
                                    pushSingleActivityHistory(newItem, null);
                                    Toast.makeText(this, "Đã thêm hoạt động (cho phép trùng)", Toast.LENGTH_SHORT).show();
                                })
                                .setNegativeButton("Hủy", null)
                                .show();
                    }
                })
                .setNegativeButton("Hủy", null)
                .show();
    }

    private void saveSingleItemToFirebase(ScheduleItem item) {
        if (item == null) return;
        String dayNode = "day_" + item.getDayOfWeek();
        DatabaseReference dayRef = schedulesRef.child(dayNode);

        String key = dayRef.push().getKey();
        if (key == null) {
            dayRef.push().setValue(item)
                    .addOnSuccessListener(aVoid -> Log.d(TAG, "Saved single item (no key) to Firebase"))
                    .addOnFailureListener(e -> Toast.makeText(this, "Lỗi lưu item: " + e.getMessage(), Toast.LENGTH_SHORT).show());
            return;
        }

        item.setFirebaseKey(key);

        dayRef.child(key).setValue(item)
                .addOnSuccessListener(aVoid -> Log.d(TAG, "Saved single item to Firebase with key=" + key))
                .addOnFailureListener(e -> Toast.makeText(this, "Lỗi lưu item: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    /**
     * Push a small history entry for a single activity (user-added).
     * Renamed method to avoid duplicate-signature issues.
     */
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

    /**
     * Save schedule to be displayed on Home.
     * Saves to SharedPreferences (for immediate use) AND to Firebase under /users/<userId>/home_display for realtime sync.
     */
    private void saveScheduleToHomeDisplay(int day, List<ScheduleItem> items) {
        if (items == null) items = new ArrayList<>();
        try {
            // Build JSON payload
            JSONObject root = new JSONObject();
            root.put("day", day);
            JSONArray acts = new JSONArray();
            for (ScheduleItem it : items) {
                JSONObject o = new JSONObject();
                o.put("start", it.getStartTime() == null ? "" : it.getStartTime());
                o.put("end", it.getEndTime() == null ? "" : it.getEndTime());
                o.put("activity", it.getActivity() == null ? "" : it.getActivity());
                o.put("day", it.getDayOfWeek());
                acts.put(o);
            }
            root.put("activities", acts);

            // Save local prefs
            SharedPreferences profilePrefs = getSharedPreferences(PROFILE_PREFS, Context.MODE_PRIVATE);
            profilePrefs.edit()
                    .putString(HOME_DISPLAY_ACTIVITIES_KEY, root.toString())
                    .putInt(HOME_DISPLAY_DAY_KEY, day)
                    .apply();

            // Also push to Firebase for realtime HomeFragment consumption
            DatabaseReference homeRef = rootRef.child("users").child(userId).child("home_display");
            homeRef.setValue(root.toString())
                    .addOnSuccessListener(aVoid -> Log.d(TAG, "Saved home display to Firebase"))
                    .addOnFailureListener(e -> Log.w(TAG, "Failed saving home display to Firebase", e));
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

    private void mergeFirebaseItems(List<ScheduleItem> firebaseItems) {
        if (currentList == null) currentList = new ArrayList<>();
        if (firebaseItems == null || firebaseItems.isEmpty()) {
            if (scheduleAdapter != null) scheduleAdapter.updateList(currentList);
            return;
        }

        for (ScheduleItem item : firebaseItems) {
            boolean exists = false;
            for (ScheduleItem c : currentList) {
                String cs = c.getStartTime() == null ? "" : c.getStartTime();
                String ce = c.getEndTime() == null ? "" : c.getEndTime();
                String as = item.getStartTime() == null ? "" : item.getStartTime();
                String ae = item.getEndTime() == null ? "" : item.getEndTime();
                String ca = c.getActivity() == null ? "" : c.getActivity();
                String ia = item.getActivity() == null ? "" : item.getActivity();

                if (cs.equals(as) && ce.equals(ae) && ca.equals(ia)) {
                    exists = true;
                    break;
                }
            }
            if (!exists) currentList.add(item);
        }

        currentList.sort((a, b) -> {
            String as = a.getStartTime() == null ? "" : a.getStartTime();
            String bs = b.getStartTime() == null ? "" : b.getStartTime();
            return as.compareTo(bs);
        });

        if (scheduleAdapter != null) scheduleAdapter.updateList(currentList);
    }

    private void saveScheduleToFirebase() {
        if (currentList == null || currentList.isEmpty()) {
            Toast.makeText(this, "Danh sách lịch trống", Toast.LENGTH_SHORT).show();
            return;
        }

        String dayNode = "day_" + selectedDay;
        DatabaseReference dayRef = schedulesRef.child(dayNode);

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

        dayRef.setValue(toUpload)
                .addOnSuccessListener(aVoid -> Log.d(TAG, "Saved full schedule to Firebase for " + dayNode))
                .addOnFailureListener(e -> Toast.makeText(this, "Lỗi lưu lịch: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    // saveScheduleToProfileHistory left unchanged (saves whole schedule summary when user clicks Apply)
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

    // Utility - convert "HH:mm" to minutes since midnight
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

    // Check overlap: return true if new interval [s1,e1) overlaps any existing item in list
    private boolean isOverlapping(String newStart, String newEnd, List<ScheduleItem> existing, ScheduleItem exclude) {
        int ns = timeToMinutes(newStart);
        int ne = timeToMinutes(newEnd);
        if (ns < 0 || ne < 0 || ne <= ns) return true; // invalid => treat as overlapping/invalid

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
        switch (day) {
            case 2:
                defaults.add(new ScheduleItem(0, "06:00", "08:00", "Thức dậy & vệ sinh", day));
                defaults.add(new ScheduleItem(0, "08:00", "10:00", "Ăn sáng & Chuẩn bị", day));
                defaults.add(new ScheduleItem(0, "10:00", "12:00", "Làm việc / Học buổi sáng", day));
                defaults.add(new ScheduleItem(0, "12:00", "14:00", "Ăn trưa & nghỉ", day));
                defaults.add(new ScheduleItem(0, "14:00", "16:00", "Tiếp tục công việc", day));
                defaults.add(new ScheduleItem(0, "16:00", "18:00", "Ôn tập / Học thêm", day));
                defaults.add(new ScheduleItem(0, "19:00", "21:00", "Thư giãn / Gia đình", day));
                break;
            case 3:
                defaults.add(new ScheduleItem(0, "06:00", "08:00", "Yoga & Sáng", day));
                defaults.add(new ScheduleItem(0, "08:00", "10:00", "Ăn sáng & chuẩn bị", day));
                defaults.add(new ScheduleItem(0, "10:00", "12:00", "Họp nhóm / Công việc", day));
                defaults.add(new ScheduleItem(0, "12:00", "14:00", "Ăn trưa & nghỉ", day));
                defaults.add(new ScheduleItem(0, "14:00", "16:00", "Công việc chuyên môn", day));
                defaults.add(new ScheduleItem(0, "16:00", "18:00", "Task cá nhân", day));
                defaults.add(new ScheduleItem(0, "19:00", "21:00", "Thư giãn", day));
                break;
            case 4:
                defaults.add(new ScheduleItem(0, "06:00", "08:00", "Chạy bộ & vệ sinh", day));
                defaults.add(new ScheduleItem(0, "08:00", "10:00", "Ăn sáng & chuẩn bị", day));
                defaults.add(new ScheduleItem(0, "10:00", "12:00", "Học / Khóa học", day));
                defaults.add(new ScheduleItem(0, "12:00", "14:00", "Ăn trưa & nghỉ", day));
                defaults.add(new ScheduleItem(0, "14:00", "16:00", "Side project", day));
                defaults.add(new ScheduleItem(0, "16:00", "18:00", "Gym / Thể dục", day));
                defaults.add(new ScheduleItem(0, "19:00", "21:00", "Lập kế hoạch tuần", day));
                break;
            case 5:
                defaults.add(new ScheduleItem(0, "06:00", "08:00", "Thiền & Chuẩn bị", day));
                defaults.add(new ScheduleItem(0, "08:00", "10:00", "Ăn sáng & công việc nhẹ", day));
                defaults.add(new ScheduleItem(0, "10:00", "12:00", "Deep work", day));
                defaults.add(new ScheduleItem(0, "12:00", "14:00", "Ăn trưa & nghỉ", day));
                defaults.add(new ScheduleItem(0, "14:00", "16:00", "Chạy việc / Mua sắm", day));
                defaults.add(new ScheduleItem(0, "16:00", "18:00", "Học thêm", day));
                defaults.add(new ScheduleItem(0, "19:00", "21:00", "Giải trí", day));
                break;
            case 6:
                defaults.add(new ScheduleItem(0, "06:00", "08:00", "Đi bộ & Sáng", day));
                defaults.add(new ScheduleItem(0, "08:00", "10:00", "Ăn sáng & đọc tin", day));
                defaults.add(new ScheduleItem(0, "10:00", "12:00", "Hoàn thiện công việc", day));
                defaults.add(new ScheduleItem(0, "12:00", "14:00", "Ăn trưa", day));
                defaults.add(new ScheduleItem(0, "14:00", "16:00", "Kiểm tra email", day));
                defaults.add(new ScheduleItem(0, "16:00", "18:00", "Gặp gỡ bạn bè", day));
                defaults.add(new ScheduleItem(0, "19:00", "21:00", "Thư giãn", day));
                break;
            case 7:
                defaults.add(new ScheduleItem(0, "08:00", "10:00", "Dọn dẹp & Chuẩn bị", day));
                defaults.add(new ScheduleItem(0, "10:00", "12:00", "Mua sắm / Công việc gia đình", day));
                defaults.add(new ScheduleItem(0, "12:00", "14:00", "Nấu ăn & ăn trưa", day));
                defaults.add(new ScheduleItem(0, "14:00", "16:00", "Hobby / Sở thích", day));
                defaults.add(new ScheduleItem(0, "16:00", "18:00", "Thể dục", day));
                defaults.add(new ScheduleItem(0, "18:00", "20:00", "Gặp gỡ", day));
                defaults.add(new ScheduleItem(0, "20:00", "22:00", "Chuẩn bị cho CN", day));
                break;
            case 8:
                defaults.add(new ScheduleItem(0, "08:00", "10:00", "Ngủ nướng & ăn sáng", day));
                defaults.add(new ScheduleItem(0, "10:00", "12:00", "Thư giãn (sách, cafe)", day));
                defaults.add(new ScheduleItem(0, "12:00", "14:00", "Ăn trưa gia đình", day));
                defaults.add(new ScheduleItem(0, "14:00", "16:00", "Lập kế hoạch tuần", day));
                defaults.add(new ScheduleItem(0, "16:00", "18:00", "Đi dạo", day));
                defaults.add(new ScheduleItem(0, "18:00", "20:00", "Chuẩn bị thức ăn", day));
                defaults.add(new ScheduleItem(0, "20:00", "22:00", "Xem phim / Thư giãn", day));
                break;
            default:
                defaults.add(new ScheduleItem(0, "06:00", "08:00", "Thức dậy & vệ sinh", day));
                defaults.add(new ScheduleItem(0, "08:00", "10:00", "Ăn sáng", day));
                defaults.add(new ScheduleItem(0, "10:00", "12:00", "Làm việc", day));
                defaults.add(new ScheduleItem(0, "12:00", "14:00", "Ăn trưa", day));
                defaults.add(new ScheduleItem(0, "14:00", "16:00", "Tiếp tục", day));
                defaults.add(new ScheduleItem(0, "16:00", "18:00", "Thư giãn", day));
                defaults.add(new ScheduleItem(0, "19:00", "21:00", "Gia đình", day));
                break;
        }
        return defaults;
    }
}

package com.example.to_do_app.fragment;

import android.app.AlertDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.to_do_app.R;
import com.example.to_do_app.adapters.ScheduleItemAdapter;
import com.example.to_do_app.model.ScheduleItem;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class HomeFragment extends Fragment {

    private static final String TAG = "HomeFragment";

    // Constants
    private static final String PROFILE_PREFS = "profile_prefs";
    private static final String HOME_DISPLAY_ACTIVITIES_KEY = "home_display_activities";
    private static final String HOME_DISPLAY_DAY_KEY = "home_display_day";
    private static final String KEY_DISPLAY_NAME = "display_name";
    private static final String PREFS = "todo_prefs";
    private static final String PREF_ACTIVE_SCHEDULE_NAME = "active_schedule_name";

    // Firebase
    private FirebaseAuth mAuth;
    private DatabaseReference homeDisplayRef;
    private ValueEventListener homeDisplayListener;
    private String userId;

    // UI Components
    private LinearLayout llMon, llTue, llWed, llThu, llFri, llSat, llSun;
    private View selectedDayView = null;
    private RecyclerView recyclerView;
    private ScheduleItemAdapter scheduleAdapter;
    private TextView tvGreeting, tvScheduleName;


    // State
    private Map<Integer, List<ScheduleItem>> taskMap = new HashMap<>();
    private int selectedDay = 2;
    private List<ScheduleItem> currentList = new ArrayList<>();
    private SharedPreferences profilePrefs;
    private SharedPreferences prefs;

    public HomeFragment() { }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.home_fragment, container, false);

        // Initialization
        initializeFirebase();
        initializePrefs();
        initializeViews(view);
        setupListeners();

        // Load initial data
        selectDay(llMon, 2); // Default to Monday
        attachHomeDisplayListener();

        return view;
    }

    private void initializeFirebase() {
        mAuth = FirebaseAuth.getInstance();
        FirebaseDatabase database = FirebaseDatabase.getInstance();
        FirebaseUser currentUser = mAuth.getCurrentUser();

        if (currentUser != null) {
            userId = currentUser.getUid();
        } else {
            // Fallback for guest users
            userId = requireContext().getSharedPreferences(PROFILE_PREFS, Context.MODE_PRIVATE).getString("profile_user_id", null);
            if (userId == null) {
                userId = "guest_" + System.currentTimeMillis();
                requireContext().getSharedPreferences(PROFILE_PREFS, Context.MODE_PRIVATE).edit().putString("profile_user_id", userId).apply();
            }
        }
        homeDisplayRef = database.getReference("users").child(userId).child("home_display");
    }

    private void initializePrefs() {
        profilePrefs = requireContext().getSharedPreferences(PROFILE_PREFS, Context.MODE_PRIVATE);
        prefs = requireContext().getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    private void initializeViews(View view) {
        tvGreeting = view.findViewById(R.id.tvGreeting);
        llMon = view.findViewById(R.id.llMon);
        llTue = view.findViewById(R.id.llTue);
        llWed = view.findViewById(R.id.llWed);
        llThu = view.findViewById(R.id.llThu);
        llFri = view.findViewById(R.id.llFri);
        llSat = view.findViewById(R.id.llSat);
        llSun = view.findViewById(R.id.llSun);
        recyclerView = view.findViewById(R.id.homerecyclerview);
        tvScheduleName = view.findViewById(R.id.tenLich);

        // Setup RecyclerView
        scheduleAdapter = new ScheduleItemAdapter(getContext(), currentList, new ScheduleItemAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(int position, ScheduleItem item) {
                // Simple click shows a toast
                Toast.makeText(getContext(), "Công việc: " + item.getActivity(), Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onEditClick(int position, ScheduleItem item) {
                // Edit button click opens the dialog
                showEditDialog(position, item);
            }
        }, true); // Pass true to show the edit button

        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerView.setAdapter(scheduleAdapter);
    }

    private void setupListeners() {
        llMon.setOnClickListener(v -> selectDay(llMon, 2));
        llTue.setOnClickListener(v -> selectDay(llTue, 3));
        llWed.setOnClickListener(v -> selectDay(llWed, 4));
        llThu.setOnClickListener(v -> selectDay(llThu, 5));
        llFri.setOnClickListener(v -> selectDay(llFri, 6));
        llSat.setOnClickListener(v -> selectDay(llSat, 7));
        llSun.setOnClickListener(v -> selectDay(llSun, 8)); // Sunday = 8
    }

    @Override
    public void onResume() {
        super.onResume();
        updateGreeting();
        updateScheduleName();
        loadHomeDisplayFromPrefsIfAny(); // Reload data on resume
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        // Clean up the listener to avoid memory leaks
        if (homeDisplayRef != null && homeDisplayListener != null) {
            homeDisplayRef.removeEventListener(homeDisplayListener);
        }
    }

    private void attachHomeDisplayListener() {
        if (homeDisplayRef == null) return;

        homeDisplayListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!snapshot.exists()) {
                    loadHomeDisplayFromPrefsIfAny(); // Try loading from local backup
                    return;
                }
                String jsonString = String.valueOf(snapshot.getValue());
                applyHomeDisplayJson(jsonString);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.w(TAG, "Firebase onCancelled", error.toException());
                loadHomeDisplayFromPrefsIfAny(); // On error, try loading from local backup
            }
        };
        homeDisplayRef.addValueEventListener(homeDisplayListener);
    }

    private void loadHomeDisplayFromPrefsIfAny() {
        String json = profilePrefs.getString(HOME_DISPLAY_ACTIVITIES_KEY, null);
        if (json != null) {
            applyHomeDisplayJson(json);
        }
    }

    private void applyHomeDisplayJson(String json) {
        if (json == null || json.isEmpty()) return;
        try {
            JSONObject root = new JSONObject(json);
            taskMap.clear();

            for (int day = 2; day <= 8; day++) {
                String dayKey = "day_" + day;
                JSONArray acts = root.optJSONArray(dayKey);

                if (acts != null) {
                    List<ScheduleItem> list = new ArrayList<>();
                    for (int i = 0; i < acts.length(); i++) {
                        JSONObject o = acts.optJSONObject(i);
                        if (o == null) continue;
                        ScheduleItem item = new ScheduleItem();
                        item.setStartTime(o.optString("start", ""));
                        item.setEndTime(o.optString("end", ""));
                        item.setActivity(o.optString("activity", ""));
                        item.setDayOfWeek(o.optInt("day", day));
                        list.add(item);
                    }
                    sortListByStartTime(list);
                    taskMap.put(day, list);
                }
            }

            int savedDay = profilePrefs.getInt(HOME_DISPLAY_DAY_KEY, 2);
            selectDay(getLayoutForDay(savedDay), savedDay);

            // Keep prefs in sync with the loaded data
            profilePrefs.edit()
                    .putString(HOME_DISPLAY_ACTIVITIES_KEY, json)
                    .apply();

        } catch (JSONException ex) {
            Log.e(TAG, "applyHomeDisplayJson error", ex);
        }
    }

    private void selectDay(LinearLayout dayLayout, int dayKey) {
        if (selectedDayView != null) {
            selectedDayView.setSelected(false);
        }
        dayLayout.setSelected(true);
        selectedDayView = dayLayout;
        selectedDay = dayKey;

        currentList = taskMap.getOrDefault(dayKey, new ArrayList<>());
        sortListByStartTime(currentList);
        scheduleAdapter.updateList(currentList);

        // Save the currently selected day to restore view later
        profilePrefs.edit().putInt(HOME_DISPLAY_DAY_KEY, dayKey).apply();
    }

    private LinearLayout getLayoutForDay(int day) {
        switch (day) {
            case 2: return llMon;
            case 3: return llTue;
            case 4: return llWed;
            case 5: return llThu;
            case 6: return llFri;
            case 7: return llSat;
            case 8: return llSun;
            default: return llMon;
        }
    }

    private void updateScheduleName() {
        if (tvScheduleName == null) return;
        String scheduleName = profilePrefs.getString(PREF_ACTIVE_SCHEDULE_NAME, "Lịch trình");
        tvScheduleName.setText(scheduleName);
    }

    private void updateGreeting() {
        if (tvGreeting == null) return;
        FirebaseUser user = mAuth.getCurrentUser();
        String nameToShow = "User";

        if (user != null) {
            if (user.getDisplayName() != null && !user.getDisplayName().trim().isEmpty()) {
                nameToShow = user.getDisplayName().trim();
            } else {
                String savedName = prefs.getString(KEY_DISPLAY_NAME, null);
                if (savedName != null && !savedName.trim().isEmpty()) {
                    nameToShow = savedName;
                } else if (user.getEmail() != null && user.getEmail().contains("@")) {
                    nameToShow = user.getEmail().substring(0, user.getEmail().indexOf("@"));
                }
            }
        }
        tvGreeting.setText("Xin chào, " + nameToShow);
        if (nameToShow != null) {
            prefs.edit().putString(KEY_DISPLAY_NAME, nameToShow).apply();
        }
    }

    private void showEditDialog(int position, ScheduleItem item) {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        View view = LayoutInflater.from(getContext()).inflate(R.layout.edit_schedule1, null);
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
                        Toast.makeText(getContext(), "Vui lòng nhập đầy đủ thời gian", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    if (isOverlapping(newStart, newEnd, currentList, item)) {
                        Toast.makeText(getContext(), "Thời gian mới trùng với mục khác.", Toast.LENGTH_LONG).show();
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
        if (position != -1 && position < currentList.size()) {
            // Update the item in the local list
            currentList.set(position, item);

            // Re-sort the list for the current day
            sortListByStartTime(currentList);

            // Put the updated list back into the main map
            taskMap.put(selectedDay, currentList);

            // Notify the adapter of the change
            scheduleAdapter.updateList(currentList);

            // Persist the entire week's schedule to Firebase and SharedPreferences
            saveCurrentWeekToHomeDisplay();

            Toast.makeText(getContext(), "Lịch trình đã được cập nhật.", Toast.LENGTH_SHORT).show();
        }
    }

    private void saveCurrentWeekToHomeDisplay() {
        if (homeDisplayRef == null) {
            Log.e(TAG, "homeDisplayRef is null, cannot save schedule.");
            return;
        }
        try {
            JSONObject weekData = new JSONObject();

            // Serialize the in-memory taskMap to JSON
            for (Map.Entry<Integer, List<ScheduleItem>> entry : taskMap.entrySet()) {
                JSONArray dayActivities = new JSONArray();
                for (ScheduleItem item : entry.getValue()) {
                    JSONObject actObj = new JSONObject();
                    actObj.put("start", item.getStartTime());
                    actObj.put("end", item.getEndTime());
                    actObj.put("activity", item.getActivity());
                    actObj.put("day", item.getDayOfWeek());
                    dayActivities.put(actObj);
                }
                weekData.put("day_" + entry.getKey(), dayActivities);
            }

            String jsonToSave = weekData.toString();

            // Save to Firebase
            homeDisplayRef.setValue(jsonToSave);

            // Also save to SharedPreferences as a local backup
            profilePrefs.edit()
                    .putString(HOME_DISPLAY_ACTIVITIES_KEY, jsonToSave)
                    .apply();

            Log.d(TAG, "Saved current week schedule to home display");
        } catch (JSONException ex) {
            Log.e(TAG, "saveCurrentWeekToHomeDisplay JSONException", ex);
        }
    }

    private void sortListByStartTime(List<ScheduleItem> list) {
        if (list == null) return;
        Collections.sort(list, new Comparator<ScheduleItem>() {
            @Override
            public int compare(ScheduleItem o1, ScheduleItem o2) {
                int t1 = parseTimeToMinutes(o1.getStartTime());
                int t2 = parseTimeToMinutes(o2.getStartTime());
                return Integer.compare(t1, t2);
            }
        });
    }

    private int parseTimeToMinutes(String time) {
        if (time == null || !time.contains(":")) return Integer.MAX_VALUE;
        try {
            String[] parts = time.split(":");
            int hh = Integer.parseInt(parts[0].trim());
            int mm = Integer.parseInt(parts[1].trim());
            return hh * 60 + mm;
        } catch (NumberFormatException e) {
            return Integer.MAX_VALUE;
        }
    }

    private boolean isOverlapping(String newStart, String newEnd, List<ScheduleItem> existing, ScheduleItem exclude) {
        int ns = parseTimeToMinutes(newStart);
        int ne = parseTimeToMinutes(newEnd);
        if (ns < 0 || ne < 0 || ne <= ns) return true; // Invalid time

        if (existing == null) return false;
        for (ScheduleItem it : existing) {
            if (exclude != null && it.equals(exclude)) continue;
            int is = parseTimeToMinutes(it.getStartTime());
            int ie = parseTimeToMinutes(it.getEndTime());
            if (is < 0 || ie < 0) continue;
            // Check for overlap: (StartA < EndB) and (StartB < EndA)
            if (ns < ie && is < ne) return true;
        }
        return false;
    }
}

package com.example.to_do_app.fragment;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ScrollView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.to_do_app.R;
import com.example.to_do_app.adapters.HomeActivitiesAdapter;
import com.example.to_do_app.util.FileStore;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

/**
 * HomeFragment — show all activities applied from Layout6Activity.
 * - Listens to /users/<userId>/home_display in realtime.
 * - When activities exist for a day, shows rvHomeActivities with all items (no fixed 7 slots).
 * - Falls back to legacy slots if no activities.
 */
public class HomeFragment extends Fragment {

    private static final String TAG = "HomeFragment";

    private LinearLayout llMon, llTue, llWed, llThu, llFri, llSat, llSun;
    private List<LinearLayout> dayLayouts;
    private View selectedDay = null;

    private RecyclerView rvHomeActivities;
    private ScrollView scrollLegacySlots;
    private HomeActivitiesAdapter adapter;

    // internal model
    private static class Task {
        String start, end, title, location, note;
        Task(String start, String end, String title, String location, String note) {
            this.start = start; this.end = end; this.title = title; this.location = location; this.note = note;
        }
        String getTime() {
            if ((start == null || start.isEmpty()) && (end == null || end.isEmpty())) return "";
            if (end == null || end.isEmpty()) return start == null ? "" : start;
            if (start == null || start.isEmpty()) return end;
            return start + " - " + end;
        }
    }

    private Map<Integer, List<Task>> taskMap = new HashMap<>();

    // prefs / firebase keys
    private static final String PROFILE_PREFS = "profile_prefs";
    private static final String PROFILE_USER_ID_KEY = "profile_user_id";
    private static final String HOME_DISPLAY_ACTIVITIES_KEY = "home_display_activities";
    private static final String HOME_DISPLAY_DAY_KEY = "home_display_day";

    private SharedPreferences profilePrefs;
    private DatabaseReference rootRef;
    private DatabaseReference homeDisplayRef;
    private String userId;

    public HomeFragment() { }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.home_fragment, container, false);

        profilePrefs = requireContext().getSharedPreferences(PROFILE_PREFS, Context.MODE_PRIVATE);
        rootRef = FirebaseDatabase.getInstance().getReference();

        userId = profilePrefs.getString(PROFILE_USER_ID_KEY, null);
        if (userId == null) {
            userId = "user_" + System.currentTimeMillis();
            profilePrefs.edit().putString(PROFILE_USER_ID_KEY, userId).apply();
        }

        homeDisplayRef = rootRef.child("users").child(userId).child("home_display");

        // day selectors
        llMon = view.findViewById(R.id.llMon);
        llTue = view.findViewById(R.id.llTue);
        llWed = view.findViewById(R.id.llWed);
        llThu = view.findViewById(R.id.llThu);
        llFri = view.findViewById(R.id.llFri);
        llSat = view.findViewById(R.id.llSat);
        llSun = view.findViewById(R.id.llSun);

        dayLayouts = new ArrayList<>();
        dayLayouts.add(llMon); // 2
        dayLayouts.add(llTue); // 3
        dayLayouts.add(llWed); // 4
        dayLayouts.add(llThu); // 5
        dayLayouts.add(llFri); // 6
        dayLayouts.add(llSat); // 7
        dayLayouts.add(llSun); // 1

        // recycler & legacy container
        rvHomeActivities = view.findViewById(R.id.rvHomeActivities);
        scrollLegacySlots = view.findViewById(R.id.scroll_legacy_slots);

        adapter = new HomeActivitiesAdapter(new ArrayList<>());
        if (rvHomeActivities != null) {
            rvHomeActivities.setLayoutManager(new LinearLayoutManager(requireContext()));
            rvHomeActivities.setAdapter(adapter);
            rvHomeActivities.setVisibility(View.GONE);
        }

        // initialize empty taskMap keys (1..7)
        for (int d = 1; d <= 7; d++) taskMap.put(d, new ArrayList<>());

        // day click listeners: show tasks for that day
        for (int i = 0; i < dayLayouts.size(); i++) {
            final int dayKey = (i + 2) <= 7 ? (i + 2) : 1;
            final LinearLayout ll = dayLayouts.get(i);
            ll.setOnClickListener(v -> {
                if (selectedDay != null) selectedDay.setSelected(false);
                ll.setSelected(true);
                selectedDay = ll;
                List<Task> tasks = taskMap.get(dayKey);
                showTasks(tasks);
            });
        }

        attachHomeDisplayListener();

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        // fallback: load stored JSON if any
        String json = profilePrefs.getString(HOME_DISPLAY_ACTIVITIES_KEY, null);
        if (json != null && !json.trim().isEmpty()) parseJsonAndApply(json);
    }

    private void attachHomeDisplayListener() {
        if (homeDisplayRef == null) return;
        homeDisplayRef.addValueEventListener(new com.google.firebase.database.ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!isAdded()) return;
                Object val = snapshot.getValue();
                if (val == null) return;
                try {
                    if (val instanceof Map) {
                        @SuppressWarnings("unchecked")
                        Map<String, Object> map = (Map<String, Object>) val;
                        parseMapAndApply(map);
                    } else {
                        String json = String.valueOf(val);
                        parseJsonAndApply(json);
                    }
                } catch (Exception ex) {
                    Log.w(TAG, "Error parsing home_display payload", ex);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.w(TAG, "home_display listener cancelled: " + (error == null ? "unknown" : error.getMessage()));
            }
        });
    }

    private void parseMapAndApply(Map<String, Object> map) {
        if (map == null) return;
        Object dayObj = map.get("day");
        int day = toInt(dayObj, profilePrefs.getInt(HOME_DISPLAY_DAY_KEY, 2));

        List<Task> parsed = new ArrayList<>();
        Object actsObj = map.get("activities");

        if (actsObj instanceof List) {
            @SuppressWarnings("unchecked")
            List<Object> acts = (List<Object>) actsObj;
            for (Object o : acts) {
                if (!(o instanceof Map)) continue;
                @SuppressWarnings("unchecked")
                Map<String, Object> m = (Map<String, Object>) o;
                String start = safeToString(m.get("start"));
                String end = safeToString(m.get("end"));
                String title = safeToString(m.get("activity"));
                String location = safeToString(m.get("location"));
                String note = safeToString(m.get("note"));
                parsed.add(new Task(start, end, title, location, note));
            }
        } else if (actsObj instanceof Map) {
            @SuppressWarnings("unchecked")
            Map<String, Object> actsMap = (Map<String, Object>) actsObj;
            for (Object v : actsMap.values()) {
                if (!(v instanceof Map)) continue;
                @SuppressWarnings("unchecked")
                Map<String, Object> m = (Map<String, Object>) v;
                String start = safeToString(m.get("start"));
                String end = safeToString(m.get("end"));
                String title = safeToString(m.get("activity"));
                String location = safeToString(m.get("location"));
                String note = safeToString(m.get("note"));
                parsed.add(new Task(start, end, title, location, note));
            }
        }

        taskMap.put(day, parsed);

        // persist as JSON fallback
        try {
            JSONObject root = new JSONObject();
            root.put("day", day);
            JSONArray arr = new JSONArray();
            for (Task t : parsed) {
                JSONObject o = new JSONObject();
                o.put("start", t.start == null ? "" : t.start);
                o.put("end", t.end == null ? "" : t.end);
                o.put("activity", t.title == null ? "" : t.title);
                o.put("location", t.location == null ? "" : t.location);
                o.put("note", t.note == null ? "" : t.note);
                arr.put(o);
            }
            root.put("activities", arr);
            String jsonString = root.toString();
            profilePrefs.edit()
                    .putString(HOME_DISPLAY_ACTIVITIES_KEY, jsonString)
                    .putInt(HOME_DISPLAY_DAY_KEY, day)
                    .apply();
            String filename = buildHomeDisplayFilename(userId, day);
            FileStore.saveJson(requireContext(), filename, jsonString);
        } catch (JSONException ignored) {}

        // show dynamic list
        showTasksForDay(day);
    }

    private void parseJsonAndApply(String json) {
        if (json == null || json.trim().isEmpty()) return;
        try {
            JSONObject root = new JSONObject(json);
            int day = root.optInt("day", profilePrefs.getInt(HOME_DISPLAY_DAY_KEY, 2));
            JSONArray arr = root.optJSONArray("activities");
            List<Task> parsed = new ArrayList<>();
            if (arr != null) {
                for (int i = 0; i < arr.length(); i++) {
                    JSONObject o = arr.optJSONObject(i);
                    if (o == null) continue;
                    String start = o.optString("start", "");
                    String end = o.optString("end", "");
                    String title = o.optString("activity", "");
                    String location = o.optString("location", "");
                    String note = o.optString("note", "");
                    parsed.add(new Task(start, end, title, location, note));
                }
            }

            taskMap.put(day, parsed);

            profilePrefs.edit()
                    .putString(HOME_DISPLAY_ACTIVITIES_KEY, json)
                    .putInt(HOME_DISPLAY_DAY_KEY, day)
                    .apply();
            String filename = buildHomeDisplayFilename(userId, day);
            FileStore.saveJson(requireContext(), filename, json);

            showTasksForDay(day);
        } catch (JSONException ex) {
            Log.w(TAG, "parseJsonAndApply failed", ex);
        }
    }

    private void showTasksForDay(int day) {
        List<Task> tasks = taskMap.get(day);
        showTasks(tasks);
        // update selected day UI
        LinearLayout tl = mapDayToLayout(day);
        if (tl != null) {
            if (selectedDay != null) selectedDay.setSelected(false);
            tl.setSelected(true);
            selectedDay = tl;
        }
    }

    private void showTasks(List<Task> tasks) {
        if (tasks != null && !tasks.isEmpty()) {
            List<HomeActivitiesAdapter.Item> items = new ArrayList<>();
            for (Task t : tasks) {
                items.add(new HomeActivitiesAdapter.Item(t.getTime(), t.title, t.location, t.note));
            }
            if (rvHomeActivities != null) {
                adapter.setItems(items);
                rvHomeActivities.setVisibility(View.VISIBLE);
            }
            if (scrollLegacySlots != null) scrollLegacySlots.setVisibility(View.GONE);
        } else {
            if (rvHomeActivities != null) rvHomeActivities.setVisibility(View.GONE);
            if (scrollLegacySlots != null) scrollLegacySlots.setVisibility(View.VISIBLE);
        }
    }

    private LinearLayout mapDayToLayout(int day) {
        switch (day) {
            case 2: return llMon;
            case 3: return llTue;
            case 4: return llWed;
            case 5: return llThu;
            case 6: return llFri;
            case 7: return llSat;
            case 1: return llSun;
            default: return llMon;
        }
    }

    // helpers
    private static String safeToString(Object o) {
        if (o == null) return "";
        return String.valueOf(o);
    }

    private static int toInt(Object o, int fallback) {
        if (o == null) return fallback;
        try {
            if (o instanceof Number) return ((Number) o).intValue();
            return Integer.parseInt(String.valueOf(o));
        } catch (Exception ex) {
            return fallback;
        }
    }

    private static String buildHomeDisplayFilename(String userId, int day) {
        if (userId == null || userId.isEmpty()) userId = "user_unknown";
        return "home_display_" + userId + "_day_" + day + ".json";
    }
}
package com.example.to_do_app.fragment;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.to_do_app.R;
import com.example.to_do_app.activitys.Layout6Activity;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * HomeFragment — listens for payloads from Layout6Activity (via local broadcast and RTDB)
 * and displays them in a RecyclerView. Accepts both new ("start","end","activity","location","note","day")
 * and legacy keys ("startTime","endTime","title"/"activity","dayOfWeek").
 *
 * Key fixes made:
 * - Ensures BroadcastReceiver is created & registered reliably in onStart and unregistered in onStop.
 * - Ensures UI updates (adapter changes / view selection) always run on UI thread.
 * - Provides small helpers (taskToJson / buildDayJson) and normalizes Sunday mapping (1 or 8).
 * - Adds defensive null-safety and logs.
 */
public class HomeFragment extends Fragment {

    private static final String TAG = "HomeFragment";

    private LinearLayout llMon, llTue, llWed, llThu, llFri, llSat, llSun;
    private List<LinearLayout> dayLayouts;
    private View selectedDayLayout = null;

    // RecyclerView for displaying activities (no time column)
    private RecyclerView rvHomeActivities;
    private ScheduleSimpleAdapter scheduleAdapter;

    // Map day -> list of Task
    private final Map<Integer, List<Task>> taskMap = new HashMap<>();

    private int selectedDayKey = 2; // currently selected day (2..7, or 1/8 for Sunday)

    private static class Task {
        String time, title, location, note;
        Task(String time, String title, String location, String note) {
            this.time = time; this.title = title; this.location = location; this.note = note;
        }
    }

    // Shared prefs / firebase keys
    private static final String PROFILE_PREFS = "profile_prefs";
    private static final String HOME_DISPLAY_ACTIVITIES_KEY = "home_display_activities";
    private static final String HOME_DISPLAY_DAY_KEY = "home_display_day";
    private static final String PREF_ACTIVE_SCHEDULE = "active_schedule_name";

    private SharedPreferences profilePrefs;
    private DatabaseReference rootRef;
    private DatabaseReference homeDisplayRef; // currently attached DB ref
    private com.google.firebase.database.ValueEventListener homeValueListener;

    private String userId;
    private String activeScheduleName; // current active schedule name (may be null = legacy global)

    // Local broadcast receiver to handle immediate "apply" events from Layout6Activity
    private BroadcastReceiver scheduleAppliedReceiver;

    public HomeFragment() { /* required empty constructor */ }

    @Override
    public void onStart() {
        super.onStart();
        registerScheduleAppliedReceiver();
        // load cached payload in prefs if exists (this will update UI)
        loadHomeDisplayFromPrefsIfAny();
    }

    @Override
    public void onStop() {
        super.onStop();
        unregisterScheduleAppliedReceiver();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.home_fragment, container, false);

        profilePrefs = requireContext().getSharedPreferences(PROFILE_PREFS, Context.MODE_PRIVATE);
        rootRef = FirebaseDatabase.getInstance().getReference();

        userId = profilePrefs.getString("profile_user_id", null);
        if (userId == null) {
            userId = "user_" + System.currentTimeMillis();
            profilePrefs.edit().putString("profile_user_id", userId).apply();
        }

        activeScheduleName = profilePrefs.getString(PREF_ACTIVE_SCHEDULE, null);

        // UI bindings (ids must exist in fragment_home.xml)
        llMon = view.findViewById(R.id.llMon);
        llTue = view.findViewById(R.id.llTue);
        llWed = view.findViewById(R.id.llWed);
        llThu = view.findViewById(R.id.llThu);
        llFri = view.findViewById(R.id.llFri);
        llSat = view.findViewById(R.id.llSat);
        llSun = view.findViewById(R.id.llSun);

        dayLayouts = new ArrayList<>();
        dayLayouts.add(llMon); // index 0 -> Thứ 2 (key 2)
        dayLayouts.add(llTue); // key 3
        dayLayouts.add(llWed); // key 4
        dayLayouts.add(llThu); // key 5
        dayLayouts.add(llFri); // key 6
        dayLayouts.add(llSat); // key 7
        dayLayouts.add(llSun); // key 1 or 8 for CN

        // Setup RecyclerView
        rvHomeActivities = view.findViewById(R.id.rvHomeActivities);
        scheduleAdapter = new ScheduleSimpleAdapter(new ArrayList<>());
        rvHomeActivities.setLayoutManager(new LinearLayoutManager(requireContext()));
        rvHomeActivities.setAdapter(scheduleAdapter);

        // day click listeners -> update recycler view for selected day
        for (int i = 0; i < dayLayouts.size(); i++) {
            final int dayKey = (i + 2) <= 7 ? (i + 2) : 1; // 2..7, 1 == Sunday
            final LinearLayout dayLayout = dayLayouts.get(i);
            dayLayout.setOnClickListener(v -> {
                selectedDayKey = dayKey;
                profilePrefs.edit().putInt(HOME_DISPLAY_DAY_KEY, selectedDayKey).apply();
                selectDay(dayLayout, dayKey);
            });
        }

        // restore previously selected day if any
        selectedDayKey = profilePrefs.getInt(HOME_DISPLAY_DAY_KEY, 2);
        LinearLayout startLayout = mapDayToLayout(selectedDayKey);
        if (startLayout == null) startLayout = llMon;
        selectDay(startLayout, selectedDayKey);

        // Attach Firebase listener for the currently active schedule
        attachHomeDisplayListener();

        return view;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        detachHomeDisplayListener();
    }

    // --- Broadcast registration ---
    private void registerScheduleAppliedReceiver() {
        try {
            if (scheduleAppliedReceiver == null) {
                scheduleAppliedReceiver = new BroadcastReceiver() {
                    @Override
                    public void onReceive(Context context, Intent intent) {
                        if (intent == null) return;
                        Log.d(TAG, "Received ACTION_SCHEDULE_APPLIED broadcast: " + intent);
                        String scheduleName = intent.getStringExtra(Layout6Activity.EXTRA_SCHEDULE_NAME);
                        String payload = intent.getStringExtra("home_payload");
                        int selDay = intent.getIntExtra("selected_day", -1);

                        if (scheduleName != null && !scheduleName.trim().isEmpty()) {
                            profilePrefs.edit().putString(PREF_ACTIVE_SCHEDULE, scheduleName).apply();
                            activeScheduleName = scheduleName;
                        }

                        if (selDay >= 1) {
                            selectedDayKey = selDay;
                            profilePrefs.edit().putInt(HOME_DISPLAY_DAY_KEY, selectedDayKey).apply();
                        }

                        if (payload != null && !payload.trim().isEmpty()) {
                            // Ensure parsing and UI update run on main thread
                            try {
                                requireActivity().runOnUiThread(() -> parseHomePayload(payload));
                            } catch (IllegalStateException e) {
                                // fallback if fragment not attached
                                parseHomePayload(payload);
                            }
                        } else {
                            // No payload in broadcast: re-attach DB listener to pick up remote data
                            detachHomeDisplayListener();
                            attachHomeDisplayListener();
                        }
                    }
                };
            }

            IntentFilter filter = new IntentFilter(Layout6Activity.ACTION_SCHEDULE_APPLIED);
            LocalBroadcastManager.getInstance(requireContext()).registerReceiver(scheduleAppliedReceiver, filter);
            Log.d(TAG, "Registered scheduleAppliedReceiver");
        } catch (Exception ex) {
            Log.w(TAG, "registerScheduleAppliedReceiver failed", ex);
        }
    }

    private void unregisterScheduleAppliedReceiver() {
        try {
            if (scheduleAppliedReceiver != null) {
                LocalBroadcastManager.getInstance(requireContext()).unregisterReceiver(scheduleAppliedReceiver);
                Log.d(TAG, "Unregistered scheduleAppliedReceiver");
            }
        } catch (Exception ex) {
            Log.w(TAG, "unregisterScheduleAppliedReceiver failed", ex);
        }
    }

    // --- Firebase listener attach/detach ---
    private void attachHomeDisplayListener() {
        detachHomeDisplayListener();

        activeScheduleName = profilePrefs.getString(PREF_ACTIVE_SCHEDULE, activeScheduleName);
        DatabaseReference ref;
        if (activeScheduleName != null && !activeScheduleName.trim().isEmpty()) {
            ref = rootRef.child("users").child(userId).child("schedules").child(activeScheduleName).child("home_display");
            Log.d(TAG, "Attaching to per-schedule home_display: " + activeScheduleName);
        } else {
            ref = rootRef.child("users").child(userId).child("home_display");
            Log.d(TAG, "Attaching to legacy home_display");
        }

        homeDisplayRef = ref;

        homeValueListener = new com.google.firebase.database.ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                Object val = snapshot.getValue();
                Log.d(TAG, "home_display onDataChange value=" + String.valueOf(val));
                if (val == null) {
                    loadHomeDisplayFromPrefsIfAny();
                    return;
                }
                // Snapshot callbacks are on main thread, but ensure UI-safety
                try {
                    requireActivity().runOnUiThread(() -> parseHomePayload(val));
                } catch (IllegalStateException e) {
                    parseHomePayload(val);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.w(TAG, "home_display listener cancelled: " + (error == null ? "unknown" : error.getMessage()));
                loadHomeDisplayFromPrefsIfAny();
            }
        };

        try {
            homeDisplayRef.addValueEventListener(homeValueListener);
        } catch (Exception ex) {
            Log.w(TAG, "attachHomeDisplayListener addValueEventListener failed", ex);
        }
    }

    private void detachHomeDisplayListener() {
        try {
            if (homeDisplayRef != null && homeValueListener != null) {
                homeDisplayRef.removeEventListener(homeValueListener);
            }
        } catch (Exception ignored) {}
        homeValueListener = null;
        homeDisplayRef = null;
    }

    private void loadHomeDisplayFromPrefsIfAny() {
        String key = HOME_DISPLAY_ACTIVITIES_KEY;
        activeScheduleName = profilePrefs.getString(PREF_ACTIVE_SCHEDULE, activeScheduleName);
        if (activeScheduleName != null && !activeScheduleName.trim().isEmpty()) {
            key = HOME_DISPLAY_ACTIVITIES_KEY + "_" + activeScheduleName;
        }
        String json = profilePrefs.getString(key, null);
        if (json != null && !json.trim().isEmpty()) {
            // parse on UI thread
            try {
                requireActivity().runOnUiThread(() -> parseHomePayload(json));
            } catch (IllegalStateException e) {
                parseHomePayload(json);
            }
        }
    }

    /**
     * Parse a payload which may be:
     * - a Map (from Firebase structured Map), or
     * - a JSON string (single-day or "week" object)
     */
    @SuppressWarnings("unchecked")
    private void parseHomePayload(Object payload) {
        if (payload == null) return;
        try {
            if (payload instanceof Map) {
                handleMapPayload((Map<String, Object>) payload);
                return;
            }

            String s = String.valueOf(payload);
            if (s.trim().isEmpty()) return;

            try {
                JSONObject root = new JSONObject(s);

                if (root.has("week")) {
                    JSONObject weekObj = root.optJSONObject("week");
                    if (weekObj != null) {
                        Map<String, Object> weekMap = new HashMap<>();
                        Iterator<String> keys = weekObj.keys();
                        while (keys.hasNext()) {
                            String key = keys.next();
                            weekMap.put(key, weekObj.opt(key));
                        }
                        Map<String, Object> wrapper = new HashMap<>();
                        wrapper.put("week", weekMap);
                        handleMapPayload(wrapper);
                        return;
                    }
                }

                if (root.has("days")) {
                    JSONArray daysArr = root.optJSONArray("days");
                    if (daysArr != null) {
                        Map<String, Object> map = new HashMap<>();
                        for (int i = 0; i < daysArr.length(); i++) {
                            JSONObject dayObj = daysArr.optJSONObject(i);
                            if (dayObj == null) continue;
                            String key = String.valueOf(dayObj.optInt("day", 0));
                            map.put(key, dayObj.opt("activities"));
                        }
                        Map<String, Object> wrapper = new HashMap<>();
                        wrapper.put("week", map);
                        handleMapPayload(wrapper);
                        return;
                    }
                }

                // fallback to single-day JSON shape handled below
                applyHomeDisplayJson(s);
            } catch (JSONException ex) {
                Log.w(TAG, "parseHomePayload: value not JSON, ignoring", ex);
            }
        } catch (Exception ex) {
            Log.w(TAG, "parseHomePayload failed", ex);
        }
    }

    @SuppressWarnings("unchecked")
    private void handleMapPayload(Map<String, Object> map) {
        if (map == null || map.isEmpty()) return;

        Map<String, Object> daysMap = null;
        if (map.containsKey("week") && map.get("week") instanceof Map) {
            daysMap = (Map<String, Object>) map.get("week");
        } else {
            boolean found = true;
            for (String k : map.keySet()) {
                if (!isDayKey(k)) { found = false; break; }
            }
            if (found) daysMap = (Map<String, Object>) map;
        }

        if (daysMap == null) {
            try {
                JSONObject obj = new JSONObject(mapToJsonString(map));
                applyHomeDisplayJson(obj.toString());
            } catch (Exception ex) {
                Log.w(TAG, "handleMapPayload fallback failed", ex);
            }
            return;
        }

        for (Map.Entry<String, Object> e : daysMap.entrySet()) {
            String dayKey = e.getKey();
            int day = parseDayKey(dayKey);
            Object actsObj = e.getValue();
            List<Task> parsed = parseActivitiesToList(actsObj);
            taskMap.put(day, parsed);

            // persist a single-day snapshot to prefs for quick fallback
            try {
                JSONObject dayJson = buildDayJson(day, parsed);
                String key = HOME_DISPLAY_ACTIVITIES_KEY;
                String schedule = profilePrefs.getString(PREF_ACTIVE_SCHEDULE, null);
                if (schedule != null && !schedule.trim().isEmpty()) key = HOME_DISPLAY_ACTIVITIES_KEY + "_" + schedule;
                profilePrefs.edit().putString(key, dayJson.toString()).apply();
            } catch (JSONException ignored) {}
        }

        // show selected day content (force UI thread update)
        int currentSelectedDay = profilePrefs.getInt(HOME_DISPLAY_DAY_KEY, selectedDayKey);
        selectedDayKey = currentSelectedDay;
        LinearLayout target = mapDayToLayout(currentSelectedDay);
        if (target != null) {
            try {
                requireActivity().runOnUiThread(() -> selectDay(target, currentSelectedDay));
            } catch (IllegalStateException e) {
                selectDay(target, currentSelectedDay);
            }
        } else {
            try {
                requireActivity().runOnUiThread(() -> selectDay(llMon, 2));
            } catch (IllegalStateException e) {
                selectDay(llMon, 2);
            }
        }
    }

    /**
     * Build a JSONObject representing a day snapshot:
     * { "day": N, "activities": [ {start, end, activity, location, note}, ... ] }
     */
    @NonNull
    private JSONObject buildDayJson(int day, List<Task> parsedActivities) throws JSONException {
        JSONObject dayJson = new JSONObject();
        dayJson.put("day", day);
        JSONArray arr = new JSONArray();
        if (parsedActivities != null) {
            for (Task t : parsedActivities) {
                JSONObject o = taskToJson(t);
                arr.put(o);
            }
        }
        dayJson.put("activities", arr);
        return dayJson;
    }

    /**
     * Convert a Task -> JSONObject using canonical keys used by HomeFragment and Layout6Activity.
     * Keys: start, end, activity, location, note
     */
    @NonNull
    private JSONObject taskToJson(@NonNull Task t) throws JSONException {
        JSONObject o = new JSONObject();
        String start = "";
        String end = "";
        if (t.time != null && t.time.contains("-")) {
            String[] p = t.time.split("-");
            start = p[0].trim();
            end = p.length > 1 ? p[1].trim() : "";
        }
        o.put("start", start);
        o.put("end", end);
        o.put("activity", t.title == null ? "" : t.title);
        o.put("location", t.location == null ? "" : t.location);
        o.put("note", t.note == null ? "" : t.note);
        return o;
    }

    @SuppressWarnings("unchecked")
    private List<Task> parseActivitiesToList(Object actsObj) {
        List<Task> parsed = new ArrayList<>();
        if (actsObj == null) return parsed;
        try {
            if (actsObj instanceof List) {
                List<Object> list = (List<Object>) actsObj;
                for (Object o : list) {
                    if (o instanceof Map) {
                        Map<String, Object> m = (Map<String, Object>) o;
                        // Prefer new keys "start"/"end" then fallback to legacy "startTime"/"endTime"
                        String start = safeToString(m.getOrDefault("start", m.get("startTime")));
                        String end = safeToString(m.getOrDefault("end", m.get("endTime")));
                        // activity can be stored under "activity", "title", or legacy "name"
                        String title = safeToString(m.getOrDefault("activity", m.get("title")));
                        if (title.isEmpty()) title = safeToString(m.getOrDefault("name", m.get("activity")));
                        String loc = safeToString(m.getOrDefault("location", m.get("place")));
                        String note = safeToString(m.getOrDefault("note", m.get("description")));
                        String time = (start.isEmpty() && end.isEmpty()) ? "" : (start + (end.isEmpty() ? "" : " - " + end));
                        parsed.add(new Task(time, title, loc, note));
                    } else if (o instanceof String) {
                        parsed.add(parseRowStringToTask((String) o));
                    }
                }
            } else if (actsObj instanceof Map) {
                Map<String, Object> actsMap = (Map<String, Object>) actsObj;
                for (Object v : actsMap.values()) {
                    if (!(v instanceof Map)) continue;
                    Map<String, Object> m = (Map<String, Object>) v;
                    String start = safeToString(m.getOrDefault("start", m.get("startTime")));
                    String end = safeToString(m.getOrDefault("end", m.get("endTime")));
                    String title = safeToString(m.getOrDefault("activity", m.get("title")));
                    if (title.isEmpty()) title = safeToString(m.getOrDefault("name", m.get("activity")));
                    String loc = safeToString(m.getOrDefault("location", m.get("place")));
                    String note = safeToString(m.getOrDefault("note", m.get("description")));
                    String time = (start.isEmpty() && end.isEmpty()) ? "" : (start + (end.isEmpty() ? "" : " - " + end));
                    parsed.add(new Task(time, title, loc, note));
                }
            } else if (actsObj instanceof String) {
                String s = (String) actsObj;
                try {
                    JSONArray arr = new JSONArray(s);
                    for (int i = 0; i < arr.length(); i++) {
                        JSONObject o = arr.optJSONObject(i);
                        if (o == null) continue;
                        // check both "start" and "startTime"
                        String start = o.optString("start", o.optString("startTime", ""));
                        String end = o.optString("end", o.optString("endTime", ""));
                        String title = o.optString("activity", o.optString("title", ""));
                        if (title.isEmpty()) title = o.optString("name", "");
                        String loc = o.optString("location", o.optString("place", ""));
                        String note = o.optString("note", o.optString("description", ""));
                        String time = (start.isEmpty() && end.isEmpty()) ? "" : (start + (end.isEmpty() ? "" : " - " + end));
                        parsed.add(new Task(time, title, loc, note));
                    }
                } catch (JSONException ex) {
                    parsed.add(parseRowStringToTask(s));
                }
            }
        } catch (Exception ex) {
            Log.w(TAG, "parseActivitiesToList failed", ex);
        }
        return parsed;
    }

    private Task parseRowStringToTask(String row) {
        if (row == null) return new Task("", "", "", "");
        String start = "", end = "";
        String title = row;
        try {
            if (row.contains("-")) {
                String[] parts = row.split("-");
                if (parts.length >= 2) {
                    start = parts[0].trim();
                    String right = parts[1].trim();
                    if (right.length() >= 5) end = right.substring(0, Math.min(5, right.length())).trim();
                }
            }
        } catch (Exception ignored) {}
        return new Task((start.isEmpty() && end.isEmpty()) ? "" : (start + " - " + end), title, "", "");
    }

    private void applyHomeDisplayJson(String json) {
        if (json == null || json.trim().isEmpty()) return;
        try {
            JSONObject root = new JSONObject(json);
            if (root.has("week")) {
                JSONObject weekObj = root.optJSONObject("week");
                if (weekObj != null) {
                    Map<String, Object> weekMap = new HashMap<>();
                    Iterator<String> keys = weekObj.keys();
                    while (keys.hasNext()) {
                        String key = keys.next();
                        weekMap.put(key, weekObj.opt(key));
                    }
                    Map<String, Object> wrapper = new HashMap<>();
                    wrapper.put("week", weekMap);
                    handleMapPayload(wrapper);
                    return;
                }
            }

            int day = root.optInt("day", 2);
            JSONArray acts = root.optJSONArray("activities");
            if (acts == null) return;

            List<Task> list = new ArrayList<>();
            for (int i = 0; i < acts.length(); i++) {
                JSONObject o = acts.optJSONObject(i);
                if (o == null) continue;
                String start = o.optString("start", "");
                String end = o.optString("end", "");
                String activity = o.optString("activity", "");
                String location = o.optString("location", "");
                String note = o.optString("note", "");
                String time = (start.isEmpty() && end.isEmpty()) ? "" : (start + " - " + end);
                Task t = new Task(time, activity, location, note);
                list.add(t);
            }

            taskMap.put(day, list);
            profilePrefs.edit().putString(HOME_DISPLAY_ACTIVITIES_KEY, json).putInt(HOME_DISPLAY_DAY_KEY, day).apply();

            LinearLayout targetLayout = mapDayToLayout(day);
            if (targetLayout != null) {
                try {
                    requireActivity().runOnUiThread(() -> selectDay(targetLayout, day));
                } catch (IllegalStateException e) {
                    selectDay(targetLayout, day);
                }
            }
        } catch (JSONException ex) {
            Log.w(TAG, "applyHomeDisplayJson parse failed", ex);
        }
    }

    private LinearLayout mapDayToLayout(int day) {
        switch (day) {
            case 3: return llTue;
            case 4: return llWed;
            case 5: return llThu;
            case 6: return llFri;
            case 7: return llSat;
            // Normalize Sunday: accept 1 or 8 externally, but internally map to same view
            case 1:
            case 8:
                return llSun;
            default: return llMon;
        }
    }

    private void selectDay(LinearLayout dayLayout, int dayKey) {
        if (selectedDayLayout != null) selectedDayLayout.setSelected(false);
        dayLayout.setSelected(true);
        selectedDayLayout = dayLayout;
        selectedDayKey = dayKey;
        // update recycler view with tasks for that day on UI thread
        List<Task> tasks = taskMap.get(dayKey);
        if (tasks == null) tasks = new ArrayList<>();
        try {
            List<Task> finalTasks = tasks;
            requireActivity().runOnUiThread(() -> scheduleAdapter.updateData(finalTasks));
        } catch (IllegalStateException e) {
            scheduleAdapter.updateData(tasks);
        }
    }

    // Helpers
    private static String safeToString(Object o) {
        if (o == null) return "";
        return String.valueOf(o);
    }

    private static boolean isDayKey(String k) {
        if (k == null) return false;
        try {
            int val = Integer.parseInt(k);
            return (val >= 1 && val <= 8);
        } catch (Exception ex) {
            return false;
        }
    }

    private static int parseDayKey(String k) {
        try {
            return Integer.parseInt(k);
        } catch (Exception ex) {
            return 2;
        }
    }

    private static String mapToJsonString(Map<String, Object> map) {
        try {
            JSONObject o = new JSONObject(map);
            return o.toString();
        } catch (Exception ex) {
            return "{}";
        }
    }

    /* -----------------------------
       Simple RecyclerView Adapter
       Expect item layout res/layout/item_schedule_simple.xml with:
          TextView @id/tv_item_title
          TextView @id/tv_item_location
          TextView @id/tv_item_note
       ----------------------------- */
    private static class ScheduleSimpleAdapter extends RecyclerView.Adapter<ScheduleSimpleAdapter.VH> {

        private List<Task> items;

        ScheduleSimpleAdapter(List<Task> items) {
            this.items = items == null ? new ArrayList<>() : items;
        }

        void updateData(List<Task> newItems) {
            this.items = newItems == null ? new ArrayList<>() : newItems;
            notifyDataSetChanged();
        }

        @NonNull
        @Override
        public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_schedule_simple, parent, false);
            return new VH(v);
        }

        @Override
        public void onBindViewHolder(@NonNull VH holder, int position) {
            Task t = items.get(position);
            holder.title.setText(t.title == null ? "" : t.title);
            holder.location.setText((t.location == null || t.location.isEmpty()) ? "" : "Địa điểm: " + t.location);
            holder.note.setText((t.note == null) ? "" : t.note);
            // Optional: click to open Layout6Activity to edit this activity (left as UI decision)
        }

        @Override
        public int getItemCount() { return items == null ? 0 : items.size(); }

        static class VH extends RecyclerView.ViewHolder {
            TextView title, location, note;
            VH(@NonNull View itemView) {
                super(itemView);
                title = itemView.findViewById(R.id.tv_item_title);
                location = itemView.findViewById(R.id.tv_item_location);
                note = itemView.findViewById(R.id.tv_item_note);
            }
        }
    }
}
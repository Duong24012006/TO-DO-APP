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
import android.widget.ScrollView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.to_do_app.R;
import com.example.to_do_app.adapters.HomeActivitiesAdapter;
import com.example.to_do_app.util.FileStore;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase; // realtime
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore; // firestore
import com.google.firebase.firestore.FirebaseFirestoreException;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.Iterator;

/**
 * HomeFragment — listens for per-schedule home_display data and active-schedule broadcasts.
 *
 * Behavior:
 * - Listens to RTDB /users/<userId>/home_display/<scheduleName>, /users/<userId>/schedules/<scheduleName>/home_display,
 *   and legacy /users/<userId>/home_display.
 * - Listens to Firestore /users/<userId>/home_display/<scheduleName> (optional).
 * - Listens to active schedule marker /users/<userId>/home_display_active_schedule.
 * - Receives LocalBroadcast ACTION_SCHEDULE_APPLIED from Layout6Activity with:
 *     EXTRA_SCHEDULE_NAME (String), "selected_day" (int 2..8) and optional "home_payload" (String JSON)
 *   On receiving broadcast it:
 *     - switches active schedule,
 *     - if "home_payload" present parse & apply it immediately,
 *     - shows a toast with schedule name,
 *     - attempts to show the specified day immediately.
 *
 * - Displays full-week or single-day activities in RecyclerView (rvHomeActivities).
 */
public class HomeFragment extends Fragment {

    private static final String TAG = "HomeFragment";

    // Broadcast/action constants (must match Layout6Activity)
    public static final String ACTION_SCHEDULE_APPLIED = "com.example.to_do_app.SCHEDULE_APPLIED";
    public static final String EXTRA_SCHEDULE_NAME = "schedule_name";

    private static final String PREF_ACTIVE_SCHEDULE = "active_schedule_name";

    private LinearLayout llMon, llTue, llWed, llThu, llFri, llSat, llSun;
    private List<LinearLayout> dayLayouts;
    private View selectedDay = null;

    private RecyclerView rvHomeActivities;
    private ScrollView scrollLegacySlots;
    private HomeActivitiesAdapter activitiesAdapter;

    // internal Task model
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

    // day -> list of tasks (1..8, 8 = CN)
    private final Map<Integer, List<Task>> taskMap = new HashMap<>();

    // prefs / firebase keys (base names)
    private static final String PROFILE_PREFS = "profile_prefs";
    private static final String PROFILE_USER_ID_KEY = "profile_user_id";
    private static final String HOME_DISPLAY_ACTIVITIES_KEY_BASE = "home_display_activities";
    private static final String HOME_DISPLAY_DAY_KEY_BASE = "home_display_day";

    private SharedPreferences profilePrefs;
    private DatabaseReference rootRef;
    private final List<DatabaseReference> homeRefs = new ArrayList<>();
    private final List<com.google.firebase.database.ValueEventListener> dbListeners = new ArrayList<>();
    private FirebaseFirestore firestore;
    private String userId;

    private String scheduleName = "default_schedule";
    private com.google.firebase.firestore.ListenerRegistration fsListener = null;

    private DatabaseReference activeScheduleRef = null;
    private com.google.firebase.database.ValueEventListener activeScheduleListener = null;

    private BroadcastReceiver appliedReceiver = null;

    public HomeFragment() { }

    @Override
    public void onStart() {
        super.onStart();
        // Register broadcast receiver here so fragment receives broadcasts while visible
        registerAppliedReceiver();
    }

    public static HomeFragment newInstance(String scheduleName) {
        HomeFragment f = new HomeFragment();
        Bundle b = new Bundle();
        b.putString("ARG_SCHEDULE_NAME", scheduleName);
        f.setArguments(b);
        return f;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.home_fragment, container, false);

        profilePrefs = requireContext().getSharedPreferences(PROFILE_PREFS, Context.MODE_PRIVATE);
        rootRef = FirebaseDatabase.getInstance().getReference();
        firestore = FirebaseFirestore.getInstance();

        FirebaseUser fu = FirebaseAuth.getInstance().getCurrentUser();
        userId = (fu != null) ? fu.getUid() : profilePrefs.getString(PROFILE_USER_ID_KEY, null);
        if (userId == null) {
            userId = "user_" + System.currentTimeMillis();
            profilePrefs.edit().putString(PROFILE_USER_ID_KEY, userId).apply();
        }

        Bundle args = getArguments();
        if (args != null && args.getString("ARG_SCHEDULE_NAME") != null) {
            scheduleName = args.getString("ARG_SCHEDULE_NAME");
            profilePrefs.edit().putString(PREF_ACTIVE_SCHEDULE, scheduleName).apply();
        } else {
            String pref = profilePrefs.getString(PREF_ACTIVE_SCHEDULE, null);
            if (pref != null && !pref.trim().isEmpty()) scheduleName = pref;
        }

        llMon = view.findViewById(R.id.llMon);
        llTue = view.findViewById(R.id.llTue);
        llWed = view.findViewById(R.id.llWed);
        llThu = view.findViewById(R.id.llThu);
        llFri = view.findViewById(R.id.llFri);
        llSat = view.findViewById(R.id.llSat);
        llSun = view.findViewById(R.id.llSun);

        dayLayouts = new ArrayList<>();
        if (llMon != null) dayLayouts.add(llMon);
        if (llTue != null) dayLayouts.add(llTue);
        if (llWed != null) dayLayouts.add(llWed);
        if (llThu != null) dayLayouts.add(llThu);
        if (llFri != null) dayLayouts.add(llFri);
        if (llSat != null) dayLayouts.add(llSat);
        if (llSun != null) dayLayouts.add(llSun);

        for (int d = 1; d <= 8; d++) taskMap.put(d, new ArrayList<>());

        rvHomeActivities = view.findViewById(R.id.rvHomeActivities);
        scrollLegacySlots = view.findViewById(R.id.scroll_legacy_slots);
        if (rvHomeActivities == null) {
            int altRvId = requireContext().getResources().getIdentifier("scheduleNamesRecycler", "id", requireContext().getPackageName());
            if (altRvId != 0) rvHomeActivities = view.findViewById(altRvId);
        }
        if (rvHomeActivities != null) {
            activitiesAdapter = new HomeActivitiesAdapter(new ArrayList<>());
            rvHomeActivities.setLayoutManager(new LinearLayoutManager(requireContext()));
            rvHomeActivities.setAdapter(activitiesAdapter);
            rvHomeActivities.setVisibility(View.GONE);
        }

        for (int i = 0; i < dayLayouts.size(); i++) {
            final int dayKey = (i + 2) <= 7 ? (i + 2) : 1;
            final LinearLayout ll = dayLayouts.get(i);
            if (ll == null) continue;
            ll.setOnClickListener(v -> {
                if (selectedDay != null) selectedDay.setSelected(false);
                ll.setSelected(true);
                selectedDay = ll;
                showTasksForDay(dayKey);
            });
        }

        // Attach DB listeners immediately so data will come in when fragment created.
        attachHomeDisplayListeners();

        // apply cached local JSON if available
        String localKey = HOME_DISPLAY_ACTIVITIES_KEY_BASE + "_" + scheduleName;
        String json = profilePrefs.getString(localKey, null);
        if (json != null && !json.trim().isEmpty()) parseJsonAndApply(json);

        return view;
    }


    @Override
    public void onStop() {
        super.onStop();
        unregisterAppliedReceiver();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        detachHomeDisplayListeners();
    }

    /**
     * Attach listeners to various possible home_display locations and active marker.
     */
    private void attachHomeDisplayListeners() {
        detachHomeDisplayListeners();

        // primary path: /users/<userId>/home_display/<scheduleName>
        DatabaseReference perUserHomeRef = rootRef.child("users").child(userId).child("home_display").child(scheduleName);
        addDbListener(perUserHomeRef);

        // alternate: /users/<userId>/schedules/<scheduleName>/home_display
        DatabaseReference altHomeRef = rootRef.child("users").child(userId).child("schedules").child(scheduleName).child("home_display");
        addDbListener(altHomeRef);

        // legacy parent: /users/<userId>/home_display
        DatabaseReference legacyRef = rootRef.child("users").child(userId).child("home_display");
        com.google.firebase.database.ValueEventListener legacyListener = new com.google.firebase.database.ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!isAdded()) return;
                if (snapshot == null) return;
                // If there's a child with our scheduleName prefer it
                if (snapshot.hasChild(scheduleName)) {
                    Object val = snapshot.child(scheduleName).getValue();
                    if (val != null) {
                        try {
                            parseHomePayload(val);
                            return;
                        } catch (Exception ex) {
                            Log.w(TAG, "legacy scheduleName child parse error", ex);
                        }
                    }
                }
                // else try parse whole snapshot
                Object val = snapshot.getValue();
                if (val == null) return;
                try {
                    parseHomePayload(val);
                } catch (Exception ex) {
                    Log.w(TAG, "legacy fallback parse error", ex);
                }
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.w(TAG, "legacy home_display RTDB listener cancelled: " + (error == null ? "unknown" : error.getMessage()));
            }
        };
        legacyRef.addValueEventListener(legacyListener);
        homeRefs.add(legacyRef);
        dbListeners.add(legacyListener);

        // firestore listener (optional)
        try {
            DocumentReference fsDoc = firestore.collection("users").document(userId)
                    .collection("home_display").document(scheduleName);
            fsListener = fsDoc.addSnapshotListener(new EventListener<DocumentSnapshot>() {
                @Override
                public void onEvent(@Nullable DocumentSnapshot snapshot, @Nullable FirebaseFirestoreException error) {
                    if (!isAdded()) return;
                    if (error != null) {
                        Log.w(TAG, "firestore home_display listener error", error);
                        return;
                    }
                    if (snapshot != null && snapshot.exists()) {
                        Object payload = snapshot.get("payload");
                        if (payload != null) {
                            parseHomePayload(payload);
                        } else {
                            Map<String, Object> map = snapshot.getData();
                            if (map != null) parseHomePayload(map);
                        }
                    }
                }
            });
        } catch (Exception ex) {
            Log.w(TAG, "attach firestore listener failed", ex);
        }

        // active schedule marker listener
        addActiveScheduleListener();
    }

    private void addActiveScheduleListener() {
        try {
            activeScheduleRef = rootRef.child("users").child(userId).child("home_display_active_schedule");
            activeScheduleListener = new com.google.firebase.database.ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    if (!isAdded()) return;
                    Object val = snapshot.getValue();
                    if (val == null) return;
                    String newSchedule = String.valueOf(val).trim();
                    if (newSchedule.isEmpty()) return;
                    if (!newSchedule.equals(scheduleName)) {
                        setActiveScheduleName(newSchedule);
                    } else {
                        // same schedule: reapply local cache if present
                        String localKey = HOME_DISPLAY_ACTIVITIES_KEY_BASE + "_" + scheduleName;
                        String json = profilePrefs.getString(localKey, null);
                        if (json != null && !json.trim().isEmpty()) parseJsonAndApply(json);
                    }
                }
                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                    Log.w(TAG, "active schedule listener cancelled: " + (error == null ? "unknown" : error.getMessage()));
                }
            };
            activeScheduleRef.addValueEventListener(activeScheduleListener);
            homeRefs.add(activeScheduleRef);
            dbListeners.add(activeScheduleListener);
        } catch (Exception ex) {
            Log.w(TAG, "addActiveScheduleListener failed", ex);
        }
    }

    private void addDbListener(DatabaseReference ref) {
        if (ref == null) return;
        com.google.firebase.database.ValueEventListener listener = new com.google.firebase.database.ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!isAdded()) return;
                Object val = snapshot.getValue();
                if (val == null) return;
                try {
                    parseHomePayload(val);
                } catch (Exception ex) {
                    Log.w(TAG, "Error parsing home_display payload from ref " + ref.toString(), ex);
                }
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.w(TAG, "home_display RTDB listener cancelled: " + (error == null ? "unknown" : error.getMessage()));
            }
        };
        ref.addValueEventListener(listener);
        homeRefs.add(ref);
        dbListeners.add(listener);
    }

    private void detachHomeDisplayListeners() {
        try {
            for (int i = 0; i < homeRefs.size(); i++) {
                try {
                    DatabaseReference r = homeRefs.get(i);
                    com.google.firebase.database.ValueEventListener l = dbListeners.get(i);
                    if (r != null && l != null) r.removeEventListener(l);
                } catch (Exception ignored) {}
            }
        } catch (Exception ignored) {}
        homeRefs.clear();
        dbListeners.clear();

        try {
            if (fsListener != null) fsListener.remove();
        } catch (Exception ignored) {}
        fsListener = null;

        activeScheduleRef = null;
        activeScheduleListener = null;
    }

    @SuppressWarnings("unchecked")
    private void parseHomePayload(Object payload) {
        if (payload == null) return;
        if (payload instanceof Map) handleMapPayload((Map<String, Object>) payload);
        else parseJsonAndApply(String.valueOf(payload));
    }

    @SuppressWarnings("unchecked")
    private void handleMapPayload(Map<String, Object> map) {
        if (map == null) return;

        Object weekObj = map.get("week");
        if (weekObj == null) weekObj = map.get("days");
        if (weekObj instanceof Map) {
            parseDaysMap((Map<String,Object>)weekObj);
            showWholeWeek();
            return;
        }

        boolean foundDayKeys = false;
        Map<String, Object> possibleDays = new HashMap<>();
        for (String key : map.keySet()) {
            if (isDayKey(key)) {
                foundDayKeys = true;
                possibleDays.put(key, map.get(key));
            }
        }
        if (foundDayKeys) {
            parseDaysMap(possibleDays);
            showWholeWeek();
            return;
        }

        Object actsObj = map.get("activities");
        Object dayObj = map.get("day");
        if (actsObj != null && dayObj != null) {
            int day = toInt(dayObj, profilePrefs.getInt(HOME_DISPLAY_DAY_KEY_BASE + "_" + scheduleName, 2));
            List<Task> parsed = parseActivitiesToList(actsObj);
            taskMap.put(day, parsed);
            persistLocalHomeDisplaySingle(day, parsed);
            showTasksForDay(day);
            return;
        } else if (actsObj != null) {
            List<Object> actsList = null;
            if (actsObj instanceof List) actsList = (List<Object>) actsObj;
            else if (actsObj instanceof Map) actsList = new ArrayList<>(((Map) actsObj).values());
            if (actsList != null) {
                Map<Integer, List<Task>> grouped = new HashMap<>();
                for (Object o : actsList) {
                    if (!(o instanceof Map)) continue;
                    Map<String, Object> m = (Map<String, Object>) o;
                    int day = toInt(m.get("day"), profilePrefs.getInt(HOME_DISPLAY_DAY_KEY_BASE + "_" + scheduleName, 2));
                    String start = safeToString(m.get("start"));
                    String end = safeToString(m.get("end"));
                    String title = safeToString(m.get("activity"));
                    String location = safeToString(m.get("location"));
                    String note = safeToString(m.get("note"));
                    Task t = new Task(start, end, title, location, note);
                    List<Task> list = grouped.computeIfAbsent(day, k -> new ArrayList<>());
                    list.add(t);
                }
                for (Map.Entry<Integer, List<Task>> e : grouped.entrySet()) {
                    taskMap.put(e.getKey(), e.getValue());
                    persistLocalHomeDisplaySingle(e.getKey(), e.getValue());
                }
                showWholeWeek();
                return;
            }
        }

        Object day = map.get("day");
        if (day != null) {
            int d = toInt(day, profilePrefs.getInt(HOME_DISPLAY_DAY_KEY_BASE + "_" + scheduleName, 2));
            List<Task> parsed = new ArrayList<>();
            Object acts = map.get("activities");
            if (acts != null) parsed = parseActivitiesToList(acts);
            taskMap.put(d, parsed);
            persistLocalHomeDisplaySingle(d, parsed);
            showTasksForDay(d);
            return;
        }

        try {
            JSONObject fallback = new JSONObject(mapToJsonString(map));
            parseJsonAndApply(fallback.toString());
        } catch (Exception ex) {
            Log.w(TAG, "Unhandled home_display map payload", ex);
        }
    }

    @SuppressWarnings("unchecked")
    private List<Task> parseActivitiesToList(Object actsObj) {
        List<Task> parsed = new ArrayList<>();
        if (actsObj == null) return parsed;
        if (actsObj instanceof List) {
            List<Object> acts = (List<Object>) actsObj;
            for (Object o : acts) {
                if (o instanceof Map) {
                    Map<String, Object> m = (Map<String, Object>) o;
                    String start = safeToString(m.get("start"));
                    String end = safeToString(m.get("end"));
                    String title = safeToString(m.get("activity"));
                    String location = safeToString(m.get("location"));
                    String note = safeToString(m.get("note"));
                    parsed.add(new Task(start, end, title, location, note));
                } else if (o instanceof String) {
                    parsed.add(parseRowStringToTask((String)o));
                }
            }
        } else if (actsObj instanceof Map) {
            Map<String, Object> actsMap = (Map<String, Object>) actsObj;
            for (Object v : actsMap.values()) {
                if (!(v instanceof Map)) continue;
                Map<String, Object> m = (Map<String, Object>) v;
                String start = safeToString(m.get("start"));
                String end = safeToString(m.get("end"));
                String title = safeToString(m.get("activity"));
                String location = safeToString(m.get("location"));
                String note = safeToString(m.get("note"));
                parsed.add(new Task(start, end, title, location, note));
            }
        } else if (actsObj instanceof String) {
            String json = (String) actsObj;
            try {
                JSONArray arr = new JSONArray(json);
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
            } catch (JSONException ex) {
                parsed.add(parseRowStringToTask(json));
            }
        }
        return parsed;
    }

    private Task parseRowStringToTask(String row) {
        if (row == null) return new Task("", "", row == null ? "" : row, "", "");
        String s = row;
        String start = "";
        String end = "";
        String title = s;
        try {
            if (s.contains("-")) {
                String[] parts = s.split("-");
                if (parts.length >= 2) {
                    start = parts[0].trim();
                    String right = parts[1].trim();
                    if (right.length() >= 5) end = right.substring(0, Math.min(5, right.length())).trim();
                }
            }
        } catch (Exception ignored) {}
        return new Task(start, end, title, "", "");
    }

    private void persistLocalHomeDisplaySingle(int day, List<Task> parsed) {
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

            String localActivitiesKey = HOME_DISPLAY_ACTIVITIES_KEY_BASE + "_" + scheduleName;
            String localDayKey = HOME_DISPLAY_DAY_KEY_BASE + "_" + scheduleName;

            profilePrefs.edit()
                    .putString(localActivitiesKey, jsonString)
                    .putInt(localDayKey, day)
                    .apply();

            String filename = buildHomeDisplayFilename(userId, scheduleName, day);
            FileStore.saveJson(requireContext(), filename, jsonString);
        } catch (JSONException ignored) {}
    }

    private void parseJsonAndApply(String json) {
        if (json == null || json.trim().isEmpty()) return;
        try {
            JSONObject root = new JSONObject(json);

            if (root.has("week") || root.has("days")) {
                JSONObject daysObj = root.has("week") ? root.optJSONObject("week") : root.optJSONObject("days");
                if (daysObj != null) {
                    Map<String, Object> daysMap = jsonObjectToMap(daysObj);
                    parseDaysMap(daysMap);
                    showWholeWeek();
                    return;
                }
            }

            boolean hasDayKeys = false;
            JSONObject dayKeysObj = new JSONObject();
            Iterator<String> rootKeys = root.keys();
            while (rootKeys.hasNext()) {
                String k = rootKeys.next();
                if (isDayKey(k)) {
                    hasDayKeys = true;
                    dayKeysObj.put(k, root.opt(k));
                }
            }
            if (hasDayKeys) {
                parseDaysMap(jsonObjectToMap(dayKeysObj));
                showWholeWeek();
                return;
            }

            if (root.has("activities")) {
                JSONArray arr = root.optJSONArray("activities");
                if (arr != null) {
                    boolean itemsHaveDay = false;
                    for (int i = 0; i < arr.length(); i++) {
                        JSONObject o = arr.optJSONObject(i);
                        if (o != null && o.has("day")) { itemsHaveDay = true; break; }
                    }
                    if (itemsHaveDay) {
                        Map<Integer, List<Task>> grouped = new HashMap<>();
                        for (int i = 0; i < arr.length(); i++) {
                            JSONObject o = arr.optJSONObject(i);
                            if (o == null) continue;
                            int day = o.optInt("day", profilePrefs.getInt(HOME_DISPLAY_DAY_KEY_BASE + "_" + scheduleName, 2));
                            String start = o.optString("start", "");
                            String end = o.optString("end", "");
                            String title = o.optString("activity", "");
                            String location = o.optString("location", "");
                            String note = o.optString("note", "");
                            Task t = new Task(start, end, title, location, note);
                            List<Task> list = grouped.computeIfAbsent(day, k -> new ArrayList<>());
                            list.add(t);
                        }
                        for (Map.Entry<Integer, List<Task>> e : grouped.entrySet()) {
                            taskMap.put(e.getKey(), e.getValue());
                            persistLocalHomeDisplaySingle(e.getKey(), e.getValue());
                        }
                        showWholeWeek();
                        return;
                    }
                }
            }

            int day = root.optInt("day", profilePrefs.getInt(HOME_DISPLAY_DAY_KEY_BASE + "_" + scheduleName, 2));
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

            String localActivitiesKey = HOME_DISPLAY_ACTIVITIES_KEY_BASE + "_" + scheduleName;
            String localDayKey = HOME_DISPLAY_DAY_KEY_BASE + "_" + scheduleName;
            profilePrefs.edit()
                    .putString(localActivitiesKey, json)
                    .putInt(localDayKey, day)
                    .apply();

            String filename = buildHomeDisplayFilename(userId, scheduleName, day);
            FileStore.saveJson(requireContext(), filename, json);

            showTasksForDay(day);
        } catch (JSONException ex) {
            Log.w(TAG, "parseJsonAndApply failed", ex);
        }
    }

    private void parseDaysMap(Map<String, Object> daysMap) {
        if (daysMap == null) return;
        for (Map.Entry<String, Object> entry : daysMap.entrySet()) {
            String key = entry.getKey();
            if (!isDayKey(key)) continue;
            int day = parseDayKey(key);
            Object actsObj = entry.getValue();
            List<Task> parsed = parseActivitiesToList(actsObj);
            taskMap.put(day, parsed);
            persistLocalHomeDisplaySingle(day, parsed);
        }
    }

    private void showWholeWeek() {
        if (activitiesAdapter == null || rvHomeActivities == null) return;
        List<HomeActivitiesAdapter.Item> items = new ArrayList<>();
        for (int day = 2; day <= 8; day++) {
            String headerTitle = (day == 8) ? "Chủ Nhật" : ("Thứ " + day);
            items.add(new HomeActivitiesAdapter.Item("", headerTitle, "", "")); // header
            List<Task> tasks = taskMap.get(day);
            if (tasks == null || tasks.isEmpty()) {
                items.add(new HomeActivitiesAdapter.Item("", "Không có mục", "", ""));
            } else {
                for (Task t : tasks) items.add(new HomeActivitiesAdapter.Item(t.getTime(), t.title, t.location, t.note));
            }
        }
        activitiesAdapter.setItems(items);
        rvHomeActivities.setVisibility(View.VISIBLE);
        if (scrollLegacySlots != null) scrollLegacySlots.setVisibility(View.GONE);
    }

    private void showTasksForDay(int day) {
        List<Task> tasks = taskMap.get(day);
        showTasks(tasks);
        LinearLayout tl = mapDayToLayout(day);
        if (tl != null) {
            if (selectedDay != null) selectedDay.setSelected(false);
            tl.setSelected(true);
            selectedDay = tl;
        }
    }

    private void showTasks(List<Task> tasks) {
        if (activitiesAdapter == null || rvHomeActivities == null) return;
        if (tasks != null && !tasks.isEmpty()) {
            List<HomeActivitiesAdapter.Item> items = new ArrayList<>();
            for (Task t : tasks) items.add(new HomeActivitiesAdapter.Item(t.getTime(), t.title, t.location, t.note));
            activitiesAdapter.setItems(items);
            rvHomeActivities.setVisibility(View.VISIBLE);
            if (scrollLegacySlots != null) scrollLegacySlots.setVisibility(View.GONE);
        } else {
            rvHomeActivities.setVisibility(View.GONE);
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

    // LocalBroadcast register/unregister
    private void registerAppliedReceiver() {
        try {
            if (appliedReceiver != null) return; // already registered
            appliedReceiver = new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {
                    if (intent == null) return;
                    String action = intent.getAction();
                    if (action == null) return;
                    if (ACTION_SCHEDULE_APPLIED.equals(action)) {
                        String newSchedule = intent.getStringExtra(EXTRA_SCHEDULE_NAME);
                        int selDay = intent.getIntExtra("selected_day", -1);
                        String payload = intent.getStringExtra("home_payload"); // optional precomputed JSON

                        Log.d(TAG, "Received ACTION_SCHEDULE_APPLIED broadcast, schedule=" + newSchedule + ", day=" + selDay + ", hasPayload=" + (payload != null));

                        if (getActivity() == null) return;
                        getActivity().runOnUiThread(() -> {
                            if (newSchedule != null && !newSchedule.trim().isEmpty()) {
                                // Switch active schedule first so parseJsonAndApply uses correct scheduleName and storage keys
                                setActiveScheduleName(newSchedule);
                                Toast.makeText(requireContext(), "Đã áp dụng lịch: " + newSchedule, Toast.LENGTH_SHORT).show();
                            }

                            // If payload provided by Layout6Activity, parse & apply immediately (fast UI update)
                            if (payload != null && !payload.trim().isEmpty()) {
                                try {
                                    parseJsonAndApply(payload);
                                } catch (Exception ex) {
                                    Log.w(TAG, "Failed to parse home_payload from broadcast", ex);
                                }
                            } else {
                                // No payload: rely on DB listeners already attached to read the updated data,
                                // but attempt to reapply cached local JSON if available for quicker UI update.
                                String localKey = HOME_DISPLAY_ACTIVITIES_KEY_BASE + "_" + scheduleName;
                                String json = profilePrefs.getString(localKey, null);
                                if (json != null && !json.trim().isEmpty()) {
                                    try { parseJsonAndApply(json); } catch (Exception ex) { Log.w(TAG, "failed apply cached json", ex); }
                                }
                            }

                            // Finally, show requested day if provided
                            if (selDay >= 1 && selDay <= 8) {
                                try {
                                    showTasksForDay(selDay);
                                } catch (Exception ex) {
                                    Log.w(TAG, "showTasksForDay failed after applied broadcast", ex);
                                }
                            }
                        });
                    }
                }
            };
            LocalBroadcastManager.getInstance(requireContext())
                    .registerReceiver(appliedReceiver, new IntentFilter(ACTION_SCHEDULE_APPLIED));
            Log.d(TAG, "AppliedReceiver registered");
        } catch (Exception ex) {
            Log.w(TAG, "registerAppliedReceiver failed", ex);
        }
    }


    private void unregisterAppliedReceiver() {
        try {
            if (appliedReceiver != null) {
                LocalBroadcastManager.getInstance(requireContext()).unregisterReceiver(appliedReceiver);
                appliedReceiver = null;
                Log.d(TAG, "AppliedReceiver unregistered");
            }
        } catch (Exception ex) {
            Log.w(TAG, "unregisterAppliedReceiver failed", ex);
        }
    }


    /**
     * Switch active schedule programmatically (save pref, reattach listeners and apply cached JSON).
     */
    public void setActiveScheduleName(@NonNull String newScheduleName) {
        if (newScheduleName == null || newScheduleName.trim().isEmpty()) return;
        if (newScheduleName.equals(this.scheduleName)) {
            String localKey = HOME_DISPLAY_ACTIVITIES_KEY_BASE + "_" + scheduleName;
            String json = profilePrefs.getString(localKey, null);
            if (json != null && !json.trim().isEmpty()) parseJsonAndApply(json);
            return;
        }
        this.scheduleName = newScheduleName;
        profilePrefs.edit().putString(PREF_ACTIVE_SCHEDULE, newScheduleName).apply();
        detachHomeDisplayListeners();
        attachHomeDisplayListeners();
        String localKey = HOME_DISPLAY_ACTIVITIES_KEY_BASE + "_" + scheduleName;
        String json = profilePrefs.getString(localKey, null);
        if (json != null && !json.trim().isEmpty()) parseJsonAndApply(json);
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

    private static Map<String, Object> jsonObjectToMap(JSONObject o) {
        Map<String, Object> m = new HashMap<>();
        if (o == null) return m;
        try {
            Iterator<String> it = o.keys();
            while (it.hasNext()) {
                String k = it.next();
                Object v = o.opt(k);
                m.put(k, v);
            }
        } catch (Exception ignored) {}
        return m;
    }

    private static String buildHomeDisplayFilename(String userId, String scheduleName, int day) {
        if (userId == null || userId.isEmpty()) userId = "user_unknown";
        if (scheduleName == null || scheduleName.isEmpty()) scheduleName = "default_schedule";
        return "home_display_" + userId + "_" + scheduleName + "_day_" + day + ".json";
    }
}
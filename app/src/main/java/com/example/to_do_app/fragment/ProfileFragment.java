package com.example.to_do_app.fragment;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.to_do_app.R;
import com.example.to_do_app.activitys.Layout6Activity;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 * ProfileFragment — shows only user-added history entries (isUserAdded == true).
 *
 * Behavior:
 * - Attaches a ChildEventListener to /users/<userId>/history so newly pushed history items
 *   created by Layout6Activity appear immediately (child_added).
 * - loadHistory() reads from Firebase once as a fallback, but the listener keeps the UI realtime.
 * - onStart() also triggers reload (safe fallback).
 * - Local prefs are kept in sync (saveHistoryListToLocalWithKeys).
 *
 * Also listens for Layout6Activity.ACTION_SCHEDULE_APPLIED broadcasts so profile UI refreshes
 * when a schedule is applied (e.g. update active schedule name in prefs and reload history).
 *
 * NOTE: This version ensures the displayed user name comes from the most authoritative
 * available source in this order:
 *  1) todo_prefs (KEY_DISPLAY_NAME) — where MainActivity writes displayName extras
 *  2) profile_prefs (KEY_NAME) — local profile name managed by this fragment
 *  3) remote Firebase /users/<userId>/profile/name
 */
public class ProfileFragment extends Fragment {

    private static final String PREFS_NAME = "profile_prefs";
    private static final String KEY_NAME = "profile_name";
    private static final String KEY_HISTORY = "profile_history";
    private static final String KEY_USER_ID = "profile_user_id";
    private static final String PREF_ACTIVE_SCHEDULE = "active_schedule_name";

    // todo_prefs used by MainActivity for displayName
    private static final String TODO_PREFS = "todo_prefs";
    private static final String KEY_DISPLAY_NAME = "display_name";

    private TextView tvProfileTitle;
    private ImageView ivAvatar;
    private TextView tvUserName;
    private RecyclerView rvHistory;

    private SharedPreferences prefs;
    private List<HistoryItem> historyList;
    private HistoryListAdapter adapter;

    // Firebase
    private DatabaseReference rootRef;
    private DatabaseReference historyRef;
    private ChildEventListener historyListener;
    private String userId;

    // Broadcast receiver to react when Layout6Activity applies a schedule
    private android.content.BroadcastReceiver scheduleAppliedReceiver;

    public ProfileFragment() { }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.profile_fragment, container, false);

        prefs = requireContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);

        // Firebase reference
        rootRef = FirebaseDatabase.getInstance().getReference();

        // Ensure userId exists (for simple per-device identity). Replace with FirebaseAuth uid if available.
        userId = prefs.getString(KEY_USER_ID, null);
        if (userId == null) {
            userId = "user_" + System.currentTimeMillis();
            prefs.edit().putString(KEY_USER_ID, userId).apply();
        }

        // historyRef points to user's history node
        historyRef = rootRef.child("users").child(userId).child("history");

        tvProfileTitle = root.findViewById(R.id.tv_profile_title);
        ivAvatar = root.findViewById(R.id.iv_avatar);
        tvUserName = root.findViewById(R.id.tv_user_name);

        // Prepare RecyclerView area: replace static sample with RecyclerView if needed
        View sampleItem = root.findViewById(R.id.ll_history_item);
        ViewGroup cardInner = null;
        if (sampleItem != null && sampleItem.getParent() instanceof ViewGroup) {
            cardInner = (ViewGroup) sampleItem.getParent();
        } else {
            View card = root.findViewById(R.id.card_history);
            if (card instanceof ViewGroup && ((ViewGroup) card).getChildCount() > 0) {
                View child = ((ViewGroup) card).getChildAt(0);
                if (child instanceof ViewGroup) cardInner = (ViewGroup) child;
            }
        }

        if (cardInner != null) {
            int titleIndex = -1;
            for (int i = 0; i < cardInner.getChildCount(); i++) {
                if (cardInner.getChildAt(i).getId() == R.id.tv_section_history) {
                    titleIndex = i;
                    break;
                }
            }
            if (titleIndex != -1) {
                int keepUntil = Math.min(titleIndex + 1, cardInner.getChildCount() - 1);
                for (int i = cardInner.getChildCount() - 1; i > keepUntil; i--) {
                    cardInner.removeViewAt(i);
                }
            } else {
                if (sampleItem != null) cardInner.removeView(sampleItem);
            }

            rvHistory = new RecyclerView(requireContext());
            rvHistory.setLayoutParams(new ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
            ));
            rvHistory.setId(View.generateViewId());
            cardInner.addView(rvHistory);
        } else {
            rvHistory = new RecyclerView(requireContext());
            ViewGroup rootGroup = (ViewGroup) root;
            rvHistory.setLayoutParams(new ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
            ));
            rootGroup.addView(rvHistory);
        }

        // load profile name and history
        loadProfileName();
        loadHistory(); // initial load

        historyList = historyList == null ? new ArrayList<>() : historyList;
        adapter = new HistoryListAdapter(historyList);
        rvHistory.setLayoutManager(new LinearLayoutManager(requireContext()));
        rvHistory.setAdapter(adapter);

        // edit name on tap
        tvUserName.setOnClickListener(v -> showEditNameDialog());

        return root;
    }

    @Override
    public void onStart() {
        super.onStart();
        attachHistoryListener();
        registerScheduleAppliedReceiver();
    }

    @Override
    public void onStop() {
        super.onStop();
        detachHistoryListener();
        unregisterScheduleAppliedReceiver();
    }

    @Override
    public void onResume() {
        super.onResume();
        // ensure we have a fresh baseline (listener handles realtime updates)
        loadHistory();
        // Also refresh name in case MainActivity or another flow updated todo_prefs
        loadProfileName();
    }

    private void registerScheduleAppliedReceiver() {
        try {
            if (scheduleAppliedReceiver != null) return;
            scheduleAppliedReceiver = new android.content.BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {
                    if (intent == null) return;
                    String scheduleName = intent.getStringExtra(Layout6Activity.EXTRA_SCHEDULE_NAME);
                    String payload = intent.getStringExtra("home_payload");
                    int selDay = intent.getIntExtra("selected_day", -1);

                    // If there is an active schedule name, persist it so other fragments (Home) use it
                    if (scheduleName != null && !scheduleName.trim().isEmpty()) {
                        prefs.edit().putString(PREF_ACTIVE_SCHEDULE, scheduleName).apply();
                    }

                    // Reload history to reflect any new entries Layout6Activity pushed
                    loadHistory();

                    // Refresh displayed user name (in case Layout6Activity or MainActivity passed a displayName to todo_prefs)
                    loadProfileName();

                    // Optionally notify user
                    if (scheduleName != null && !scheduleName.isEmpty()) {
                        Toast.makeText(requireContext(), "Đã áp dụng lịch: " + scheduleName, Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(requireContext(), "Đã áp dụng lịch", Toast.LENGTH_SHORT).show();
                    }
                }
            };
            IntentFilter f = new IntentFilter(Layout6Activity.ACTION_SCHEDULE_APPLIED);
            LocalBroadcastManager.getInstance(requireContext()).registerReceiver(scheduleAppliedReceiver, f);
        } catch (Exception ex) {
            android.util.Log.w("ProfileFragment", "registerScheduleAppliedReceiver failed", ex);
        }
    }

    private void unregisterScheduleAppliedReceiver() {
        try {
            if (scheduleAppliedReceiver != null) {
                LocalBroadcastManager.getInstance(requireContext()).unregisterReceiver(scheduleAppliedReceiver);
                scheduleAppliedReceiver = null;
            }
        } catch (Exception ex) {
            android.util.Log.w("ProfileFragment", "unregisterScheduleAppliedReceiver failed", ex);
        }
    }

    private void attachHistoryListener() {
        if (historyRef == null) return;
        if (historyListener != null) return; // already attached

        historyListener = new ChildEventListener() {
            @Override
            public void onChildAdded(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {
                // Only respond to user-added entries (isUserAdded == true)
                Boolean isUserAdded = false;
                if (snapshot.child("isUserAdded").exists()) {
                    isUserAdded = snapshot.child("isUserAdded").getValue(Boolean.class);
                }
                if (isUserAdded == null || !isUserAdded) return;

                String historyKey = snapshot.getKey();
                String title = snapshot.child("title").getValue(String.class);
                String subtitle = snapshot.child("subtitle").getValue(String.class);

                Integer day = null;
                if (snapshot.child("day").exists()) {
                    Long d = snapshot.child("day").getValue(Long.class);
                    if (d != null) day = d.intValue();
                }

                List<String> activities = new ArrayList<>();
                if (snapshot.child("activities").exists()) {
                    for (DataSnapshot act : snapshot.child("activities").getChildren()) {
                        if (act.child("time").exists() && act.child("activity").exists()) {
                            String t = act.child("time").getValue(String.class);
                            String a = act.child("activity").getValue(String.class);
                            if (t != null && a != null) activities.add(t + ": " + a);
                        } else {
                            String s = act.getValue(String.class);
                            if (s != null) activities.add(s);
                        }
                    }
                }

                // Avoid duplicates
                for (HistoryItem h : historyList) {
                    if (h.historyKey != null && h.historyKey.equals(historyKey)) return;
                }

                HistoryItem newItem = new HistoryItem(historyKey,
                        title != null ? title : "",
                        subtitle != null ? subtitle : "",
                        day,
                        activities);

                historyList.add(0, newItem);
                saveHistoryListToLocalWithKeys();
                if (adapter != null) adapter.updateData(historyList);
            }

            @Override
            public void onChildChanged(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {
                String historyKey = snapshot.getKey();
                for (int i = 0; i < historyList.size(); i++) {
                    HistoryItem it = historyList.get(i);
                    if (it.historyKey != null && it.historyKey.equals(historyKey)) {
                        String title = snapshot.child("title").getValue(String.class);
                        String subtitle = snapshot.child("subtitle").getValue(String.class);
                        Integer day = null;
                        if (snapshot.child("day").exists()) {
                            Long d = snapshot.child("day").getValue(Long.class);
                            if (d != null) day = d.intValue();
                        }
                        List<String> activities = new ArrayList<>();
                        if (snapshot.child("activities").exists()) {
                            for (DataSnapshot act : snapshot.child("activities").getChildren()) {
                                if (act.child("time").exists() && act.child("activity").exists()) {
                                    String t = act.child("time").getValue(String.class);
                                    String a = act.child("activity").getValue(String.class);
                                    if (t != null && a != null) activities.add(t + ": " + a);
                                } else {
                                    String s = act.getValue(String.class);
                                    if (s != null) activities.add(s);
                                }
                            }
                        }
                        it.title = title != null ? title : it.title;
                        it.subtitle = subtitle != null ? subtitle : it.subtitle;
                        it.day = day != null ? day : it.day;
                        it.activities = activities;
                        saveHistoryListToLocalWithKeys();
                        if (adapter != null) adapter.updateData(historyList);
                        break;
                    }
                }
            }

            @Override
            public void onChildRemoved(@NonNull DataSnapshot snapshot) {
                String historyKey = snapshot.getKey();
                for (int i = 0; i < historyList.size(); i++) {
                    HistoryItem it = historyList.get(i);
                    if (it.historyKey != null && it.historyKey.equals(historyKey)) {
                        historyList.remove(i);
                        saveHistoryListToLocalWithKeys();
                        if (adapter != null) adapter.updateData(historyList);
                        break;
                    }
                }
            }

            @Override
            public void onChildMoved(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {
                // not used
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                // optional: log or show toast
            }
        };

        historyRef.addChildEventListener(historyListener);
    }

    private void detachHistoryListener() {
        if (historyRef != null && historyListener != null) {
            historyRef.removeEventListener(historyListener);
            historyListener = null;
        }
    }

    /**
     * Load profile name with preference order:
     * 1) todo_prefs.KEY_DISPLAY_NAME (written by MainActivity when it receives displayName)
     * 2) profile_prefs.KEY_NAME (local profile edited by user)
     * 3) remote Firebase /users/<userId>/profile/name
     *
     * After determining name, update tvUserName and persist into profile_prefs for future.
     */
    private void loadProfileName() {
        // 1) check todo_prefs (set by MainActivity)
        SharedPreferences todoPrefs = requireContext().getSharedPreferences(TODO_PREFS, Context.MODE_PRIVATE);
        String displayName = todoPrefs.getString(KEY_DISPLAY_NAME, null);
        if (!TextUtils.isEmpty(displayName)) {
            tvUserName.setText(displayName);
            // also persist into profile_prefs KEY_NAME for consistency
            prefs.edit().putString(KEY_NAME, displayName).apply();
            return;
        }

        // 2) check profile_prefs
        String name = prefs.getString(KEY_NAME, null);
        if (!TextUtils.isEmpty(name)) {
            tvUserName.setText(name);
            return;
        }

        // 3) fallback: try remote once and update both tv and prefs when present
        tvUserName.setText("User"); // temporary placeholder while waiting for remote
        rootRef.child("users").child(userId).child("profile").get().addOnCompleteListener(task -> {
            if (!task.isSuccessful()) return;
            DataSnapshot snap = task.getResult();
            if (snap.exists()) {
                String remoteName = snap.child("name").getValue(String.class);
                if (!TextUtils.isEmpty(remoteName)) {
                    tvUserName.setText(remoteName);
                    prefs.edit().putString(KEY_NAME, remoteName).apply();
                }
            }
        });
    }

    private void loadHistory() {
        historyList = new ArrayList<>();

        // Read from Firebase /users/<userId>/history once (initial load). The ChildEventListener will handle realtime additions.
        historyRef.get().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                DataSnapshot snap = task.getResult();
                if (snap.exists()) {
                    // load items with their firebase keys but only those marked isUserAdded == true
                    for (DataSnapshot child : snap.getChildren()) {
                        Boolean isUserAdded = false;
                        if (child.child("isUserAdded").exists()) {
                            isUserAdded = child.child("isUserAdded").getValue(Boolean.class);
                        }
                        if (isUserAdded == null || !isUserAdded) continue;

                        String historyKey = child.getKey();
                        String title = child.child("title").getValue(String.class);
                        String subtitle = child.child("subtitle").getValue(String.class);

                        Integer day = null;
                        if (child.child("day").exists()) {
                            Long d = child.child("day").getValue(Long.class);
                            if (d != null) day = d.intValue();
                        }

                        List<String> activities = new ArrayList<>();
                        if (child.child("activities").exists()) {
                            for (DataSnapshot act : child.child("activities").getChildren()) {
                                if (act.child("time").exists() && act.child("activity").exists()) {
                                    String t = act.child("time").getValue(String.class);
                                    String a = act.child("activity").getValue(String.class);
                                    if (t != null && a != null) activities.add(t + ": " + a);
                                } else {
                                    String s = act.getValue(String.class);
                                    if (s != null) activities.add(s);
                                }
                            }
                        }

                        historyList.add(new HistoryItem(historyKey,
                                title != null ? title : "",
                                subtitle != null ? subtitle : "",
                                day,
                                activities));
                    }
                    saveHistoryListToLocalWithKeys();
                    if (adapter != null) adapter.updateData(historyList);
                    return;
                }
            }
            // fallback to local
            loadHistoryFromLocalWithKeys();
            if (adapter != null) adapter.updateData(historyList);
        });
    }

    private void loadHistoryFromLocalWithKeys() {
        historyList = new ArrayList<>();
        String json = prefs.getString(KEY_HISTORY, null);
        if (json == null) {
            // no local history
            return;
        }
        try {
            JSONArray arr = new JSONArray(json);
            for (int i = 0; i < arr.length(); i++) {
                JSONObject o = arr.getJSONObject(i);
                String key = o.optString("historyKey", null);
                String title = o.optString("title", "");
                String subtitle = o.optString("subtitle", "");
                Integer day = o.has("day") ? o.optInt("day") : null;
                List<String> activities = new ArrayList<>();
                if (o.has("activities")) {
                    // activities may be array of strings or array of objects
                    JSONArray a = o.optJSONArray("activities");
                    if (a != null) {
                        for (int j = 0; j < a.length(); j++) {
                            Object el = a.opt(j);
                            if (el instanceof String) activities.add((String) el);
                            else if (el instanceof JSONObject) {
                                JSONObject jo = (JSONObject) el;
                                String time = jo.optString("time", "");
                                String act = jo.optString("activity", "");
                                if (!TextUtils.isEmpty(time) && !TextUtils.isEmpty(act)) activities.add(time + ": " + act);
                            } else {
                                activities.add(String.valueOf(el));
                            }
                        }
                    }
                } else if (o.has("activities_text")) {
                    String at = o.optString("activities_text", "");
                    if (!TextUtils.isEmpty(at)) {
                        String[] parts = at.split("  •  ");
                        for (String p : parts) if (!p.isEmpty()) activities.add(p);
                    }
                }
                historyList.add(new HistoryItem(key, title, subtitle, day, activities));
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    private void saveHistoryListToLocalWithKeys() {
        JSONArray arr = new JSONArray();
        try {
            for (HistoryItem it : historyList) {
                JSONObject o = new JSONObject();
                if (it.historyKey != null) o.put("historyKey", it.historyKey);
                o.put("title", it.title);
                o.put("subtitle", it.subtitle);
                if (it.day != null) o.put("day", it.day);
                if (it.activities != null && !it.activities.isEmpty()) {
                    JSONArray a = new JSONArray();
                    for (String s : it.activities) a.put(s);
                    o.put("activities", a);
                }
                arr.put(o);
            }
            prefs.edit().putString(KEY_HISTORY, arr.toString()).apply();
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    private void pushLocalHistoryToFirebase() {
        // keep this for synchronization if needed; but primary source is Firebase
        DatabaseReference historyRefLocal = rootRef.child("users").child(userId).child("history");
        historyRefLocal.removeValue().addOnCompleteListener(t -> {
            for (HistoryItem it : historyList) {
                DatabaseReference p = historyRefLocal.push();
                p.child("title").setValue(it.title);
                p.child("subtitle").setValue(it.subtitle);
                if (it.day != null) p.child("day").setValue(it.day);
                if (it.activities != null && !it.activities.isEmpty()) {
                    DatabaseReference actsRef = p.child("activities");
                    for (String a : it.activities) actsRef.push().setValue(a);
                }
            }
        });
    }

    private void showEditNameDialog() {
        AlertDialog.Builder b = new AlertDialog.Builder(requireContext());
        b.setTitle("Chỉnh sửa tên");

        final EditText et = new EditText(requireContext());
        et.setText(tvUserName.getText());
        et.setSelectAllOnFocus(true);
        b.setView(et);

        b.setPositiveButton("Lưu", (dialog, which) -> {
            String newName = et.getText().toString().trim();
            if (TextUtils.isEmpty(newName)) {
                Toast.makeText(requireContext(), "Tên không được để trống", Toast.LENGTH_SHORT).show();
                return;
            }
            // persist in both prefs stores for consistency across app
            prefs.edit().putString(KEY_NAME, newName).apply();
            requireContext().getSharedPreferences(TODO_PREFS, Context.MODE_PRIVATE)
                    .edit().putString(KEY_DISPLAY_NAME, newName).apply();

            tvUserName.setText(newName);
            rootRef.child("users").child(userId).child("profile").child("name").setValue(newName);
            Toast.makeText(requireContext(), "Đã lưu tên", Toast.LENGTH_SHORT).show();
        });
        b.setNegativeButton("Hủy", null);
        b.show();
    }

    // Adapter with actions (view, open to edit in Layout6Activity, delete)
    private class HistoryListAdapter extends RecyclerView.Adapter<HistoryListAdapter.VH> {
        private List<HistoryItem> items;

        HistoryListAdapter(List<HistoryItem> items) {
            this.items = items;
        }

        void updateData(List<HistoryItem> newItems) {
            this.items = newItems;
            notifyDataSetChanged();
        }

        @NonNull
        @Override
        public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.profile_history_item, parent, false);
            return new VH(v);
        }

        @Override
        public void onBindViewHolder(@NonNull VH holder, int position) {
            HistoryItem it = items.get(position);
            holder.tvTitle.setText(it.title);

            StringBuilder sb = new StringBuilder();
            if (it.day != null) {
                sb.append(dayLabel(it.day)).append("\n");
            }
            if (it.activities != null && !it.activities.isEmpty()) {
                int show = Math.min(3, it.activities.size());
                for (int i = 0; i < show; i++) {
                    sb.append(it.activities.get(i));
                    if (i < show - 1) sb.append("\n");
                }
                if (it.activities.size() > show) sb.append("\n...");
            } else if (!TextUtils.isEmpty(it.subtitle)) {
                sb.append(it.subtitle);
            } else {
                sb.append("Không có chi tiết");
            }
            holder.tvSubtitle.setText(sb.toString());

            holder.itemView.setOnClickListener(v -> {
                AlertDialog.Builder b = new AlertDialog.Builder(requireContext());
                b.setTitle(it.title);
                String[] opts = new String[]{"Xem chi tiết", "Mở để chỉnh sửa", "Xóa"};
                b.setItems(opts, (dialog, which) -> {
                    switch (which) {
                        case 0:
                            showDetailDialog(it);
                            break;
                        case 1:
                            // Open Layout6Activity for editing this applied schedule.
                            openLayoutForEditing(it);
                            break;
                        case 2:
                            confirmDelete(position);
                            break;
                    }
                });
                b.show();
            });
        }

        @Override
        public int getItemCount() {
            return items == null ? 0 : items.size();
        }

        class VH extends RecyclerView.ViewHolder {
            TextView tvTitle;
            TextView tvSubtitle;

            VH(@NonNull View itemView) {
                super(itemView);
                tvTitle = itemView.findViewById(R.id.tv_item_title);
                tvSubtitle = itemView.findViewById(R.id.tv_item_subtitle);
            }
        }
    }

    private void showDetailDialog(HistoryItem it) {
        AlertDialog.Builder b = new AlertDialog.Builder(requireContext());
        b.setTitle(it.title);
        StringBuilder sb = new StringBuilder();
        if (it.day != null) sb.append("Ngày: ").append(dayLabel(it.day)).append("\n\n");
        if (it.activities != null && !it.activities.isEmpty()) {
            for (String a : it.activities) sb.append("- ").append(a).append("\n");
        } else if (!TextUtils.isEmpty(it.subtitle)) {
            sb.append(it.subtitle);
        } else {
            sb.append("Không có chi tiết");
        }
        b.setMessage(sb.toString());
        b.setPositiveButton("Đóng", null);
        b.show();
    }

    private void confirmDelete(int position) {
        new AlertDialog.Builder(requireContext())
                .setTitle("Xác nhận")
                .setMessage("Bạn có muốn xóa mục này không?")
                .setPositiveButton("Xóa", (dialog, which) -> {
                    HistoryItem toRemove = historyList.remove(position);
                    // remove from Firebase if key exists
                    if (toRemove.historyKey != null) {
                        rootRef.child("users").child(userId).child("history").child(toRemove.historyKey).removeValue();
                    }
                    // update local copy
                    saveHistoryListToLocalWithKeys();
                    adapter.updateData(historyList);
                    Toast.makeText(requireContext(), "Đã xóa", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("Hủy", null)
                .show();
    }

    private void openLayoutForEditing(HistoryItem it) {
        // Build description: if activities exist, join them; otherwise use subtitle
        String desc;
        if (it.activities != null && !it.activities.isEmpty()) {
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < it.activities.size(); i++) {
                if (i > 0) sb.append("\n");
                sb.append(it.activities.get(i));
            }
            desc = sb.toString();
        } else {
            desc = it.subtitle != null ? it.subtitle : "";
        }

        Intent intent = new Intent(requireContext(), Layout6Activity.class);
        intent.putExtra("EXTRA_TEMPLATE_TITLE", it.title != null ? it.title : "");
        intent.putExtra("EXTRA_TEMPLATE_DESCRIPTION", desc);
        intent.putExtra("selected_day", it.day != null ? it.day : 2);
        // pass historyKey so Layout6Activity can know this was an applied item and handle saving/updating it
        if (it.historyKey != null) intent.putExtra("EXTRA_HISTORY_KEY", it.historyKey);
        startActivity(intent);
    }

    private String dayLabel(Integer day) {
        if (day == null) return "";
        switch (day) {
            case 2: return "Thứ 2";
            case 3: return "Thứ 3";
            case 4: return "Thứ 4";
            case 5: return "Thứ 5";
            case 6: return "Thứ 6";
            case 7: return "Thứ 7";
            case 8: return "Chủ nhật";
            default: return "Ngày " + day;
        }
    }

    // History item model (includes firebase key)
    public static class HistoryItem {
        public String historyKey; // firebase key for /users/<userId>/history/<historyKey>
        public String title;
        public String subtitle;
        public Integer day; // 2..8 where 8 = CN
        public List<String> activities;

        public HistoryItem(String historyKey, String title, String subtitle, Integer day, List<String> activities) {
            this.historyKey = historyKey;
            this.title = title;
            this.subtitle = subtitle;
            this.day = day;
            this.activities = activities == null ? new ArrayList<>() : activities;
        }
    }
}
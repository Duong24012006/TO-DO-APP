package com.example.to_do_app.model;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

/**
 * Model for a schedule item, now includes firebaseKey so each item can be edited/deleted individually.
 *
 * Mở rộng:
 * - thêm flag builtin để phân biệt item "áp cứng" (không xóa server) và item do user thêm
 * - include builtin trong toMap() để có thể đồng bộ nếu cần
 */
public class ScheduleItem implements Serializable {
    private String firebaseKey; // Firebase key for this item (null if not persisted yet)
    private int id;
    private String title;       // alias for activity/name
    private String name;
    private String time;
    private String startTime;
    private String endTime;
    private String activity;
    private int dayOfWeek;

    // Metadata
    private boolean builtin = false; // nếu true => item là builtin (không xóa server trực tiếp)

    // Default constructor required by Firebase
    public ScheduleItem() { }

    // Full constructor
    public ScheduleItem(int id, String startTime, String endTime, String activity, int dayOfWeek) {
        this(id, null, startTime, endTime, activity, dayOfWeek, false);
    }

    // Constructor with firebaseKey
    public ScheduleItem(String firebaseKey, int id, String startTime, String endTime, String activity, int dayOfWeek) {
        this(id, firebaseKey, startTime, endTime, activity, dayOfWeek, false);
    }

    /**
     * Internal full constructor supporting builtin flag
     */
    public ScheduleItem(int id, String firebaseKey, String startTime, String endTime, String activity, int dayOfWeek, boolean builtin) {
        this.firebaseKey = firebaseKey;
        this.id = id;
        this.startTime = startTime;
        this.endTime = endTime;
        this.activity = activity;
        this.title = activity;
        this.dayOfWeek = dayOfWeek;
        this.builtin = builtin;
    }

    // Minimal constructors
    public ScheduleItem(String name) {
        this.name = name;
        this.title = name;
        this.activity = name;
    }

    public ScheduleItem(String startTime, String endTime, String activity) {
        this.startTime = startTime;
        this.endTime = endTime;
        this.activity = activity;
        this.title = activity;
    }

    // firebaseKey
    public String getFirebaseKey() { return firebaseKey; }
    public void setFirebaseKey(String firebaseKey) { this.firebaseKey = firebaseKey; }

    // builtin flag
    public boolean isBuiltin() { return builtin; }
    public void setBuiltin(boolean builtin) { this.builtin = builtin; }

    // ID
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    // Title aliases (some UI/code expect getTitle())
    public String getTitle() {
        if (title != null) return title;
        if (activity != null) return activity;
        return name;
    }
    public void setTitle(String title) {
        this.title = title;
        this.activity = title;
        if (this.name == null) this.name = title;
    }

    // Name (alternate)
    public String getName() { return name; }
    public void setName(String name) {
        this.name = name;
        if (this.title == null) this.title = name;
        if (this.activity == null) this.activity = name;
    }

    // Start Time
    public String getStartTime() { return startTime; }
    public void setStartTime(String startTime) { this.startTime = startTime; }

    // End Time
    public String getEndTime() { return endTime; }
    public void setEndTime(String endTime) { this.endTime = endTime; }

    // Activity description
    public String getActivity() {
        if (activity != null) return activity;
        if (title != null) return title;
        return name;
    }
    public void setActivity(String activity) {
        this.activity = activity;
        this.title = activity;
    }

    // Time (optional)
    public String getTime() { return time; }
    public void setTime(String time) { this.time = time; }

    // Day of week
    public int getDayOfWeek() { return dayOfWeek; }
    public void setDayOfWeek(int dayOfWeek) { this.dayOfWeek = dayOfWeek; }

    /**
     * Helper to convert to a Map for Firebase updates (if needed).
     */
    public Map<String, Object> toMap() {
        Map<String, Object> m = new HashMap<>();
        m.put("firebaseKey", firebaseKey);
        m.put("id", id);
        m.put("title", title);
        m.put("name", name);
        m.put("time", time);
        m.put("startTime", startTime);
        m.put("endTime", endTime);
        m.put("activity", activity);
        m.put("dayOfWeek", dayOfWeek);
        m.put("builtin", builtin);
        return m;
    }

    @Override
    public String toString() {
        return "ScheduleItem{" +
                "firebaseKey='" + firebaseKey + '\'' +
                ", id=" + id +
                ", title='" + getTitle() + '\'' +
                ", startTime='" + startTime + '\'' +
                ", endTime='" + endTime + '\'' +
                ", activity='" + getActivity() + '\'' +
                ", dayOfWeek=" + dayOfWeek +
                ", builtin=" + builtin +
                '}';
    }
}
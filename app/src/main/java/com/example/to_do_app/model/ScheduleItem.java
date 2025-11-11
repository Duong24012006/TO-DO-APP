package com.example.to_do_app.model;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Model for a schedule item.
 *
 * Improvements:
 * - Added optional fields location, note, order used by UI and persisted payloads.
 * - Provides two map helpers:
 *    - toMap(): generic map (legacy keys)
 *    - toFirebaseMap(): payload shaped for HomeFragment/RTDB expects ("start","end","activity","location","note","day")
 * - Implements Serializable with serialVersionUID (keep using Parcelable if you need Intent performance).
 */
public class ScheduleItem implements Serializable {
    private static final long serialVersionUID = 1L;

    private String firebaseKey; // Firebase key for this item (null if not persisted yet)
    private int id;
    private String title;       // alias for activity/name
    private String name;
    private String time;
    private String startTime;
    private String endTime;
    private String activity;
    private int dayOfWeek;
    private boolean locked = false; // áp cứng (true = không được sửa/xóa)

    // optional fields used by UI / payloads
    private String location;
    private String note;
    private Integer order; // nullable order/index

    // Default constructor required by Firebase
    public ScheduleItem() { }

    // Full constructor
    public ScheduleItem(int id, String startTime, String endTime, String activity, int dayOfWeek) {
        this.id = id;
        this.startTime = startTime;
        this.endTime = endTime;
        this.activity = activity;
        this.title = activity;
        this.dayOfWeek = dayOfWeek;
    }

    // Constructor with firebaseKey
    public ScheduleItem(String firebaseKey, int id, String startTime, String endTime, String activity, int dayOfWeek) {
        this.firebaseKey = firebaseKey;
        this.id = id;
        this.startTime = startTime;
        this.endTime = endTime;
        this.activity = activity;
        this.title = activity;
        this.dayOfWeek = dayOfWeek;
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

    // Locked flag (áp cứng)
    public boolean isLocked() { return locked; }
    public void setLocked(boolean locked) { this.locked = locked; }

    // Optional: location / note / order
    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }

    public String getNote() { return note; }
    public void setNote(String note) { this.note = note; }

    public Integer getOrder() { return order; }
    public void setOrder(Integer order) { this.order = order; }

    /**
     * Generic helper to convert to a Map (legacy keys). Keeps all fields, may include nulls.
     */
    public Map<String, Object> toMap() {
        Map<String, Object> m = new HashMap<>();
        if (firebaseKey != null) m.put("firebaseKey", firebaseKey);
        m.put("id", id);
        if (title != null) m.put("title", title);
        if (name != null) m.put("name", name);
        if (time != null) m.put("time", time);
        if (startTime != null) m.put("startTime", startTime);
        if (endTime != null) m.put("endTime", endTime);
        if (activity != null) m.put("activity", activity);
        m.put("dayOfWeek", dayOfWeek);
        m.put("locked", locked);
        if (location != null) m.put("location", location);
        if (note != null) m.put("note", note);
        if (order != null) m.put("order", order);
        return m;
    }

    /**
     * Helper that returns a Map shaped for the HomeFragment / Layout6Activity parsing code:
     * keys: "start", "end", "activity", "location", "note", "day"
     * This is the recommended payload when writing to /home_display or sending in broadcasts.
     */
    public Map<String, Object> toFirebaseMap() {
        Map<String, Object> m = new HashMap<>();
        m.put("start", startTime == null ? "" : startTime);
        m.put("end", endTime == null ? "" : endTime);
        m.put("activity", getActivity() == null ? "" : getActivity());
        if (location != null) m.put("location", location);
        if (note != null) m.put("note", note);
        // use "day" key to match HomeFragment parsing; the value is numeric int
        m.put("day", dayOfWeek);
        if (firebaseKey != null && !firebaseKey.isEmpty()) m.put("firebaseKey", firebaseKey);
        if (order != null) m.put("order", order);
        // not including title/name/time/timeStart/.. duplicate fields to keep payload compact
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
                ", location='" + location + '\'' +
                ", note='" + note + '\'' +
                ", dayOfWeek=" + dayOfWeek +
                ", locked=" + locked +
                ", order=" + order +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ScheduleItem)) return false;
        ScheduleItem that = (ScheduleItem) o;
        // If firebaseKey available, prefer it for equality; otherwise compare fields
        if (firebaseKey != null && that.firebaseKey != null) {
            return Objects.equals(firebaseKey, that.firebaseKey);
        }
        return id == that.id &&
                dayOfWeek == that.dayOfWeek &&
                Objects.equals(getActivity(), that.getActivity()) &&
                Objects.equals(startTime, that.startTime) &&
                Objects.equals(endTime, that.endTime);
    }

    @Override
    public int hashCode() {
        if (firebaseKey != null) return firebaseKey.hashCode();
        return Objects.hash(id, getActivity(), startTime, endTime, dayOfWeek);
    }
}
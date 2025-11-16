package com.example.to_do_app.model;

import java.io.Serializable;
import java.util.Objects;

/**
 * Model cho một khung giờ hoạt động cụ thể trong ngày (thay thế cho ScheduleActivity).
 * Ví dụ: "6:00" - "7:00" - "tập thể dục buổi sáng"
 *
 * Added convenience alias methods:
 * - getActivity() / setActivity() as short aliases for getActivityName()/setActivityName()
 * - getStart()/getEnd() as aliases for start/end getters
 * Also added toString(), equals(), hashCode() to make comparisons and logs easier.
 */
public class TimeSlot implements Serializable {
    private String startTime;    // Ví dụ: "6:00"
    private String endTime;      // Ví dụ: "7:00"
    private String activityName; // Ví dụ: "tập thể dục buổi sáng"

    // Constructor rỗng bắt buộc cho Firebase
    public TimeSlot() {}

    public TimeSlot(String startTime, String endTime, String activityName) {
        this.startTime = startTime;
        this.endTime = endTime;
        this.activityName = activityName;
    }

    // --- Getters and Setters ---
    public String getStartTime() { return startTime; }
    public void setStartTime(String startTime) { this.startTime = startTime; }

    public String getEndTime() { return endTime; }
    public void setEndTime(String endTime) { this.endTime = endTime; }

    public String getActivityName() { return activityName; }
    public void setActivityName(String activityName) { this.activityName = activityName; }

    // --- Convenience alias methods for compatibility ---
    public String getActivity() { return getActivityName(); }
    public void setActivity(String activity) { setActivityName(activity); }

    public String getStart() { return getStartTime(); }
    public void setStart(String start) { setStartTime(start); }

    public String getEnd() { return getEndTime(); }
    public void setEnd(String end) { setEndTime(end); }

    // --- Utility overrides ---
    @Override
    public String toString() {
        return "TimeSlot{" +
                "startTime='" + startTime + '\'' +
                ", endTime='" + endTime + '\'' +
                ", activityName='" + activityName + '\'' +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        TimeSlot timeSlot = (TimeSlot) o;
        return Objects.equals(normalize(startTime), normalize(timeSlot.startTime)) &&
                Objects.equals(normalize(endTime), normalize(timeSlot.endTime)) &&
                Objects.equals(normalize(activityName), normalize(timeSlot.activityName));
    }

    @Override
    public int hashCode() {
        return Objects.hash(normalize(startTime), normalize(endTime), normalize(activityName));
    }

    private String normalize(String s) {
        return s == null ? "" : s.trim();
    }
}
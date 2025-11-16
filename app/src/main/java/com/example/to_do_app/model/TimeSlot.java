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
 *
 * Mở rộng:
 * - support firebaseKey để có thể map tới backend nếu cần
 * - đánh dấu builtin (áp cứng) để kiểm soát hành vi xóa/ghi đè
 */
public class TimeSlot implements Serializable {
    private String startTime;    // Ví dụ: "6:00"
    private String endTime;      // Ví dụ: "7:00"
    private String activityName; // Ví dụ: "tập thể dục buổi sáng"

    // Optional metadata
    private String firebaseKey;  // key nếu lưu trên Firebase (có thể null)
    private boolean builtin = false; // nếu true => slot là builtin (không cho xóa trực tiếp trên backend)

    // Constructor rỗng bắt buộc cho Firebase
    public TimeSlot() {}

    public TimeSlot(String startTime, String endTime, String activityName) {
        this(startTime, endTime, activityName, null, false);
    }

    /**
     * Full constructor, cho phép gán firebaseKey và builtin flag
     */
    public TimeSlot(String startTime, String endTime, String activityName, String firebaseKey, boolean builtin) {
        this.startTime = startTime;
        this.endTime = endTime;
        this.activityName = activityName;
        this.firebaseKey = firebaseKey;
        this.builtin = builtin;
    }

    // --- Getters and Setters ---
    public String getStartTime() { return startTime; }
    public void setStartTime(String startTime) { this.startTime = startTime; }

    public String getEndTime() { return endTime; }
    public void setEndTime(String endTime) { this.endTime = endTime; }

    public String getActivityName() { return activityName; }
    public void setActivityName(String activityName) { this.activityName = activityName; }

    // firebaseKey
    public String getFirebaseKey() { return firebaseKey; }
    public void setFirebaseKey(String firebaseKey) { this.firebaseKey = firebaseKey; }

    // builtin flag
    public boolean isBuiltin() { return builtin; }
    public void setBuiltin(boolean builtin) { this.builtin = builtin; }

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
                ", firebaseKey='" + firebaseKey + '\'' +
                ", builtin=" + builtin +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        TimeSlot other = (TimeSlot) o;

        // Nếu cả hai có firebaseKey rõ ràng, so sánh theo key (ưu tiên)
        String k1 = normalize(firebaseKey);
        String k2 = normalize(other.firebaseKey);
        if (!k1.isEmpty() || !k2.isEmpty()) {
            return k1.equals(k2);
        }

        // Ngược lại so sánh theo nội dung start/end/activity
        return Objects.equals(normalize(startTime), normalize(other.startTime)) &&
                Objects.equals(normalize(endTime), normalize(other.endTime)) &&
                Objects.equals(normalize(activityName), normalize(other.activityName));
    }

    @Override
    public int hashCode() {
        String k = normalize(firebaseKey);
        if (!k.isEmpty()) {
            return Objects.hash(k);
        }
        return Objects.hash(normalize(startTime), normalize(endTime), normalize(activityName));
    }

    private String normalize(String s) {
        return s == null ? "" : s.trim();
    }
}
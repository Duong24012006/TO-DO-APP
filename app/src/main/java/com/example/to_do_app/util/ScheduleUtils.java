package com.example.to_do_app.util;

import com.example.to_do_app.model.DetailedSchedule;
import com.example.to_do_app.model.TimeSlot;

import java.util.List;
import java.util.Map;

/**
 * Utils để gán flag builtin và firebaseKey cho TimeSlot trong template.
 */
public final class ScheduleUtils {
    private ScheduleUtils() {}

    /**
     * Đánh dấu toàn bộ TimeSlot trong schedule là builtin.
     * Nếu timeSlot chưa có firebaseKey, sẽ gán key dạng:
     *   builtin_<templateId>_<dayNormalized>_<startNormalized>
     *
     * templateId: short id cho template (vd "nightowl", "morning")
     */
    public static void markScheduleAsBuiltin(DetailedSchedule schedule, String templateId) {
        if (schedule == null || templateId == null) return;
        Map<String, List<TimeSlot>> weekly = schedule.getWeeklyActivities();
        if (weekly == null) return;

        for (Map.Entry<String, List<TimeSlot>> entry : weekly.entrySet()) {
            String day = entry.getKey() == null ? "day" : entry.getKey();
            String dayNormalized = day.replaceAll("\\s+", "_").toLowerCase();
            List<TimeSlot> slots = entry.getValue();
            if (slots == null) continue;
            for (TimeSlot ts : slots) {
                if (ts == null) continue;
                ts.setBuiltin(true);
                // gán firebaseKey nếu chưa có (giúp quản lý override đa thiết bị)
                if (ts.getFirebaseKey() == null || ts.getFirebaseKey().trim().isEmpty()) {
                    String start = ts.getStartTime() == null ? "nos" : ts.getStartTime().replace(":", "").replaceAll("\\s+", "");
                    String key = "builtin_" + templateId + "_" + dayNormalized + "_" + start;
                    ts.setFirebaseKey(key);
                }
            }
        }
    }

}
package com.example.to_do_app.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Simple model for schedule templates.
 * - Implements Serializable so it can be passed in Intents.
 * - Provides title, description and tags used by adapters and activities.
 *
 * Added weeklyActivities field with defensive getter/setter so code that calls
 * getWeeklyActivities() will work even if using ScheduleTemplate (not DetailedSchedule).
 */
public class ScheduleTemplate implements Serializable {
    private static final long serialVersionUID = 1L;

    private String title;
    private String description;
    private List<String> tags;

    // weeklyActivities stored as LinkedHashMap to preserve insertion/order of days
    private Map<String, List<TimeSlot>> weeklyActivities;

    // Default constructor required for Firebase / deserialization
    public ScheduleTemplate() {
        this.tags = new ArrayList<>();
        this.weeklyActivities = new LinkedHashMap<>();
    }

    public ScheduleTemplate(String title, String description, List<String> tags) {
        this.title = title;
        this.description = description;
        this.tags = (tags == null) ? new ArrayList<>() : new ArrayList<>(tags);
        this.weeklyActivities = new LinkedHashMap<>();
    }

    // Convenience constructor including weekly activities
    public ScheduleTemplate(String title, String description, List<String> tags, Map<String, List<TimeSlot>> weeklyActivities) {
        this.title = title;
        this.description = description;
        this.tags = (tags == null) ? new ArrayList<>() : new ArrayList<>(tags);
        setWeeklyActivities(weeklyActivities);
    }

    // Getters / setters
    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public List<String> getTags() {
        if (tags == null) tags = new ArrayList<>();
        return tags;
    }

    public void setTags(List<String> tags) {
        this.tags = (tags == null) ? new ArrayList<>() : new ArrayList<>(tags);
    }

    @Override
    public String toString() {
        return "ScheduleTemplate{" +
                "title='" + title + '\'' +
                ", description='" + description + '\'' +
                ", tags=" + tags +
                ", weeklyActivitiesKeys=" + (weeklyActivities == null ? "null" : weeklyActivities.keySet()) +
                '}';
    }

    /**
     * Return a defensive, unmodifiable copy of weeklyActivities.
     * - Preserves insertion order (LinkedHashMap) if present.
     * - Each day's list is copied to avoid external mutation.
     * - If internal map is null or empty, returns Collections.emptyMap().
     */
    public Map<String, List<TimeSlot>> getWeeklyActivities() {
        if (this.weeklyActivities == null || this.weeklyActivities.isEmpty()) {
            return Collections.emptyMap();
        }
        LinkedHashMap<String, List<TimeSlot>> copy = new LinkedHashMap<>(this.weeklyActivities.size());
        for (Map.Entry<String, List<TimeSlot>> e : this.weeklyActivities.entrySet()) {
            List<TimeSlot> list = e.getValue();
            copy.put(e.getKey(), list == null ? Collections.emptyList() : new ArrayList<>(list));
        }
        return Collections.unmodifiableMap(copy);
    }

    /**
     * Set weekly activities with defensive copies.
     * Accepts null (clears internal map).
     */
    public void setWeeklyActivities(Map<String, List<TimeSlot>> weekly) {
        if (weekly == null) {
            this.weeklyActivities = new LinkedHashMap<>();
            return;
        }
        LinkedHashMap<String, List<TimeSlot>> copy = new LinkedHashMap<>(weekly.size());
        for (Map.Entry<String, List<TimeSlot>> e : weekly.entrySet()) {
            List<TimeSlot> list = e.getValue();
            copy.put(e.getKey(), list == null ? new ArrayList<>() : new ArrayList<>(list));
        }
        this.weeklyActivities = copy;
    }
}
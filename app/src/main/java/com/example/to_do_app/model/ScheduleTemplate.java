package com.example.to_do_app.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Simple model for schedule templates.
 * - Implements Serializable so it can be passed in Intents.
 * - Provides title, description and tags used by adapters and activities.
 */
public class ScheduleTemplate implements Serializable {
    private String title;
    private String description;
    private List<String> tags;

    // Default constructor required for Firebase / deserialization
    public ScheduleTemplate() {
        this.tags = new ArrayList<>();
    }

    public ScheduleTemplate(String title, String description, List<String> tags) {
        this.title = title;
        this.description = description;
        this.tags = (tags == null) ? new ArrayList<>() : tags;
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
        this.tags = (tags == null) ? new ArrayList<>() : tags;
    }

    @Override
    public String toString() {
        return "ScheduleTemplate{" +
                "title='" + title + '\'' +
                ", description='" + description + '\'' +
                ", tags=" + tags +
                '}';
    }
}
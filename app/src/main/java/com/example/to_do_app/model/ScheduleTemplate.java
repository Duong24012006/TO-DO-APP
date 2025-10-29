package com.example.to_do_app.model;

import java.util.List;

public class ScheduleTemplate {
    private final String title;
    private final String description;
    private final List<String> tags;

    public ScheduleTemplate(String title, String description, List<String> tags) {
        this.title = title;
        this.description = description;
        this.tags = tags;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public List<String> getTags() {
        return tags;
    }
}

package de.ailimits.app;

import java.util.ArrayList;
import java.util.List;

public class QuotaGroup {
    public String displayName;
    public String description;
    public List<QuotaBucket> buckets = new ArrayList<>();
    public List<String> models = new ArrayList<>();

    public QuotaGroup(String displayName, String description) {
        this.displayName = displayName;
        this.description = description;
    }

    public List<String> extractModelNames() {
        if (!models.isEmpty()) {
            return models;
        }
        List<String> list = new ArrayList<>();
        if (description != null && description.contains(":")) {
            String sub = description.substring(description.indexOf(":") + 1).trim();
            String[] parts = sub.split(",");
            for (String p : parts) {
                String trimmed = p.trim();
                if (!trimmed.isEmpty()) {
                    list.add(trimmed);
                }
            }
        }
        if (list.isEmpty() && displayName != null) {
            list.add(displayName);
        }
        return list;
    }
}

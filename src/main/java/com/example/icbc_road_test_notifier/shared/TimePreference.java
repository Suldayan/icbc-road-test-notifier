package com.example.icbc_road_test_notifier.shared;

import lombok.Getter;

@Getter
public enum TimePreference {
    MORNING("Morning", 6, 12),
    AFTERNOON("Afternoon", 12, 17),
    EVENING("Evening", 17, 21),
    ANY("Any", 0, 24);

    private final String displayName;
    private final int startHour;
    private final int endHour;

    TimePreference(String displayName, int startHour, int endHour) {
        this.displayName = displayName;
        this.startHour = startHour;
        this.endHour = endHour;
    }
}
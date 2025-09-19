package com.example.icbc_road_test_notifier.appointment;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Getter
public enum TimePreference {
    MORNING("Morning", 6, 12),
    AFTERNOON("Afternoon", 12, 17),
    EVENING("Evening", 17, 21),
    ANY("Any", 0, 24);

    private final String displayName;
    private final int startHour;
    private final int endHour;
}
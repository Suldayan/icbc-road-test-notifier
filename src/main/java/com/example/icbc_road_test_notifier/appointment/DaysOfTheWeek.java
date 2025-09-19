package com.example.icbc_road_test_notifier.appointment;

import lombok.Getter;

@Getter
public enum DaysOfTheWeek {
    MONDAY("monday", "Monday"),
    TUESDAY("tuesday", "Tuesday"),
    WEDNESDAY("wednesday", "Wednesday"),
    THURSDAY("thursday", "Thursday"),
    FRIDAY("friday", "Friday"),
    SATURDAY("saturday", "Saturday"),
    SUNDAY("sunday", "Sunday");

    private final String name;
    private final String displayName;

    DaysOfTheWeek(String name, String displayName) {
        this.name = name;
        this.displayName = displayName;
    }
}
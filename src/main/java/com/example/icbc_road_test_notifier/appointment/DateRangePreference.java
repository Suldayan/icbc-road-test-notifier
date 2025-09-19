package com.example.icbc_road_test_notifier.appointment;

import lombok.RequiredArgsConstructor;

import java.time.LocalDate;

@RequiredArgsConstructor
public class DateRangePreference {
    private final LocalDate startDate;
    private final LocalDate endDate;

    public boolean isWithinRange(LocalDate date) {
        return (date.isEqual(startDate) || date.isAfter(startDate)) &&
                (date.isEqual(endDate) || date.isBefore(endDate));
    }
}

package com.example.icbc_road_test_notifier.shared;

import jakarta.persistence.Embeddable;
import lombok.RequiredArgsConstructor;

import java.time.LocalDate;

@Embeddable
@RequiredArgsConstructor
public class DateRangePreference {
    private final LocalDate startDate;
    private final LocalDate endDate;

    public boolean isWithinRange(LocalDate date) {
        return (date.isEqual(startDate) || date.isAfter(startDate)) &&
                (date.isEqual(endDate) || date.isBefore(endDate));
    }
}

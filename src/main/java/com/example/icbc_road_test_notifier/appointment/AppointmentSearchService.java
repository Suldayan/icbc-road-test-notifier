package com.example.icbc_road_test_notifier.appointment;

import com.microsoft.playwright.Page;

import java.util.Set;

public interface AppointmentSearchService {
    void configureAndSearch(Page page, String preferredLocation,
                            Set<DaysOfTheWeek> preferredDays,
                            TimePreference timePreference,
                            DateRangePreference dateRangePreference);
    AppointmentResults getLastResults();
}
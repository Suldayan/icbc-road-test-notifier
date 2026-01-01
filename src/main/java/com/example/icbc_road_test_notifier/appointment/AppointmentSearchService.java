package com.example.icbc_road_test_notifier.appointment;

import com.example.icbc_road_test_notifier.shared.DateRangePreference;
import com.example.icbc_road_test_notifier.shared.DaysOfTheWeek;
import com.example.icbc_road_test_notifier.shared.TimePreference;
import com.microsoft.playwright.Page;

import java.util.Set;

public interface AppointmentSearchService {
    void configureAndSearch(Page page, String preferredLocation,
                            Set<DaysOfTheWeek> preferredDays,
                            TimePreference timePreference,
                            DateRangePreference dateRangePreference);
    AppointmentResults getLastResults();
}
package com.example.icbc_road_test_notifier.appointment;

public interface AppointmentFilterService {
    AppointmentResults filterByPreferences(
            AppointmentResults rawResults,
            TimePreference timePreference,
            DateRangePreference dateRangePreference);
}
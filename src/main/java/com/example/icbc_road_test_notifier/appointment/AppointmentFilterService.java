package com.example.icbc_road_test_notifier.appointment;

import com.example.icbc_road_test_notifier.shared.DateRangePreference;
import com.example.icbc_road_test_notifier.shared.TimePreference;

public interface AppointmentFilterService {
    AppointmentResults filterByPreferences(
            AppointmentResults rawResults,
            TimePreference timePreference,
            DateRangePreference dateRangePreference);
}
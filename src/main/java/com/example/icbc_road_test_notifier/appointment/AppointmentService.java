package com.example.icbc_road_test_notifier.appointment;

import com.example.icbc_road_test_notifier.shared.DaysOfTheWeek;

import java.util.Set;

public interface AppointmentService {
    void authenticateAndSearchAppointments(
            String lastName,
            String licenseNumber,
            String keyword,
            String preferredLocation,
            Set<DaysOfTheWeek> preferredDays,
            TimePreference timePreference,
            DateRangePreference dateRangePreference);
}
package com.example.icbc_road_test_notifier.appointment;

import com.example.icbc_road_test_notifier.shared.DaysOfTheWeek;
import com.example.icbc_road_test_notifier.shared.IcbcConfig;

import java.util.Set;

public interface AppointmentService {
    void authenticateAndSearchAppointments(IcbcConfig icbcConfig);
}
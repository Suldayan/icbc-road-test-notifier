package com.example.icbc_road_test_notifier.appointment;

import com.microsoft.playwright.Page;

public interface AppointmentParsingService {
    AppointmentResults parseResults(Page page);
}

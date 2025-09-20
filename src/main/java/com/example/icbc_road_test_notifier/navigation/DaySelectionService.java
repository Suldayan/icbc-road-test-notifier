package com.example.icbc_road_test_notifier.navigation;

import com.example.icbc_road_test_notifier.appointment.DaysOfTheWeek;
import com.microsoft.playwright.Page;

import java.util.Set;

public interface DaySelectionService {
    void selectDays(Page page, Set<DaysOfTheWeek> preferredDays);
}

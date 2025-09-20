package com.example.icbc_road_test_notifier.navigation;

import com.microsoft.playwright.Page;

public interface NavigationService {
    void authenticate(Page page, String lastName, String licenseNumber, String keyword);
    void navigateToAppointmentSection(Page page);
}

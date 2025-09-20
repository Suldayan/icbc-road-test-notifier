package com.example.icbc_road_test_notifier.navigation;

import com.microsoft.playwright.Page;

public interface LocationSelectionService {
    void selectLocation(Page page, String locationQuery);
    void selectSpecificLocation(Page page, String preferredLocationName);
}

package com.example.icbc_road_test_notifier.appointment.internal;

import com.example.icbc_road_test_notifier.appointment.*;
import com.example.icbc_road_test_notifier.navigation.internal.DaySelectionServiceImpl;
import com.example.icbc_road_test_notifier.navigation.internal.LocationSelectionServiceImpl;
import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.PlaywrightException;
import com.microsoft.playwright.options.AriaRole;
import com.microsoft.playwright.options.LoadState;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.nio.file.Paths;
import java.util.Set;

@Service
@Slf4j
@RequiredArgsConstructor
public class AppointmentSearchServiceImpl implements AppointmentSearchService {
    private final LocationSelectionServiceImpl locationService;
    private final DaySelectionServiceImpl daySelectionService;
    private final AppointmentParsingService parsingService;
    private final AppointmentFilterService filterService;
    @Getter private AppointmentResults lastResults = AppointmentResults.empty();

    @Override
    public void configureAndSearch(Page page, String preferredLocation, Set<DaysOfTheWeek> preferredDays,
                                   TimePreference timePreference, DateRangePreference dateRangePreference) {
        log.debug("Configuring search preferences");

        page.waitForLoadState(LoadState.NETWORKIDLE);

        if (preferredLocation != null && !preferredLocation.trim().isEmpty()) {
            locationService.selectLocation(page, preferredLocation);
        }

        page.evaluate("window.scrollTo(0, document.body.scrollHeight / 2)");
        page.waitForTimeout(1000);

        if (preferredDays != null && !preferredDays.isEmpty()) {
            daySelectionService.selectDays(page, preferredDays);
        }

        page.evaluate("window.scrollTo(0, document.body.scrollHeight)");
        page.waitForTimeout(500);

        executeSearch(page, preferredLocation, timePreference, dateRangePreference);
    }

    private void executeSearch(Page page, String preferredLocationName,
                               TimePreference timePreference, DateRangePreference dateRangePreference) {
        log.debug("Executing appointment search");

        try {
            Locator searchButton = findSearchButton(page);

            if (searchButton.first().isDisabled()) {
                log.warn("Search button is disabled - required fields may not be filled");
                takeDebugScreenshot(page, "debug-disabled-search-button-" + System.currentTimeMillis() + ".png");
                lastResults = AppointmentResults.empty();
                return;
            }

            searchButton.first().scrollIntoViewIfNeeded();
            searchButton.first().click();
            log.debug("Clicked search button");

            page.waitForLoadState(LoadState.NETWORKIDLE);
            page.waitForTimeout(3000);

            handleLocationSelection(page, preferredLocationName);

            AppointmentResults rawResults = parsingService.parseResults(page);

            // Apply filtering only if preferences are provided
            if (timePreference != null || dateRangePreference != null) {
                lastResults = filterService.filterByPreferences(rawResults, timePreference, dateRangePreference);
                log.info("Filtered {} to {} appointments based on preferences",
                        rawResults.getSummary(), lastResults.getSummary());
            } else {
                lastResults = rawResults;
                log.info("No filtering applied: {}", lastResults.getSummary());
            }

        } catch (PlaywrightException e) {
            log.error("Failed to execute search: {}", e.getMessage());
            takeDebugScreenshot(page, "debug-search-error-" + System.currentTimeMillis() + ".png");
            lastResults = AppointmentResults.empty();
        }
    }

    private Locator findSearchButton(Page page) {
        return page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Search"))
                .or(page.locator("button:has-text('Search')"))
                .or(page.locator("button[type='submit']:has-text('Search')"));
    }

    private void handleLocationSelection(Page page, String preferredLocationName) {
        Locator locationResults = page.locator(".department-container")
                .or(page.locator(".first-office-container"))
                .or(page.locator(".other-locations-container"));

        if (locationResults.count() > 0) {
            log.debug("Location selection results appeared");
            locationService.selectSpecificLocation(page, preferredLocationName);
            page.waitForLoadState(LoadState.NETWORKIDLE);
            page.waitForTimeout(3000);
        }
    }

    private void takeDebugScreenshot(Page page, String filename) {
        try {
            page.screenshot(new Page.ScreenshotOptions().setPath(Paths.get(filename)).setFullPage(true));
        } catch (Exception e) {
            log.warn("Could not take debug screenshot: {}", e.getMessage());
        }
    }
}
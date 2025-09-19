package com.example.icbc_road_test_notifier.appointment.internal;

import com.example.icbc_road_test_notifier.appointment.AppointmentParsingService;
import com.example.icbc_road_test_notifier.appointment.AppointmentResults;
import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.PlaywrightException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

@Service
@Slf4j
public class AppointmentParsingServiceImpl implements AppointmentParsingService {

    @Override
    public AppointmentResults parseResults(Page page) {
        log.debug("Parsing appointment results");

        List<String> dates = new ArrayList<>();
        List<String> timeSlots = new ArrayList<>();

        try {
            expandAllSections(page);
            parseDates(page, dates);
            parseTimeSlots(page, timeSlots);

            log.info("Parsed {} dates and {} time slots", dates.size(), timeSlots.size());

            if (dates.isEmpty() && timeSlots.isEmpty()) {
                log.debug("No appointments found, checking for 'no results' messages");
                checkForNoResultsMessages(page);
            }

        } catch (Exception e) {
            log.error("Error parsing appointment results: {}", e.getMessage());
            takeDebugScreenshot(page, "debug-parsing-error-" + System.currentTimeMillis() + ".png");
        }

        return new AppointmentResults(dates, timeSlots);
    }

    private void expandAllSections(Page page) {
        log.debug("Expanding all collapsed sections");

        try {
            Locator viewMoreButton = page.locator(".view-more-btn");
            int expansions = 0;
            int maxExpansions = 10; // Prevent infinite loops

            while (viewMoreButton.count() > 0 && viewMoreButton.isVisible() && expansions < maxExpansions) {
                log.debug("Found 'view more' button, expanding section {}", expansions + 1);

                try {
                    viewMoreButton.first().scrollIntoViewIfNeeded();
                    page.waitForTimeout(200);
                    viewMoreButton.first().click();
                    page.waitForTimeout(1000);
                    expansions++;

                    // Re-query after DOM changes
                    viewMoreButton = page.locator(".view-more-btn");

                } catch (PlaywrightException e) {
                    log.warn("Failed to click 'view more' button: {}", e.getMessage());
                    break;
                }
            }

            if (expansions > 0) {
                log.debug("Expanded {} sections", expansions);
                // Give time for all content to load after expansions
                page.waitForTimeout(2000);
            } else {
                log.debug("No expandable sections found or all sections already expanded");
            }

        } catch (Exception e) {
            log.warn("Error expanding sections: {}", e.getMessage());
        }
    }

    private void parseDates(Page page, List<String> dates) {
        log.debug("Parsing available dates");

        try {
            // Multiple selectors to handle different page layouts and element structures
            Locator dateTitles = page.locator(".date-title")
                    .or(page.locator(".appointment-date"))
                    .or(page.locator("[class*='date']"))
                    .or(page.locator("h3, h4, h5").filter(new Locator.FilterOptions()
                            .setHasText(java.util.regex.Pattern.compile("\\d{1,2}/\\d{1,2}|\\w+ \\d{1,2}"))));

            int dateCount = dateTitles.count();
            log.debug("Found {} date elements", dateCount);

            for (int i = 0; i < dateCount; i++) {
                try {
                    String dateText = dateTitles.nth(i).textContent().trim();
                    if (!dateText.isEmpty() && isValidDateText(dateText)) {
                        dates.add(dateText);
                        log.debug("Added date: '{}'", dateText);
                    }
                } catch (PlaywrightException e) {
                    log.debug("Could not read date element {}: {}", i, e.getMessage());
                }
            }

            log.debug("Successfully parsed {} dates", dates.size());

        } catch (Exception e) {
            log.warn("Error parsing dates: {}", e.getMessage());
        }
    }

    private void parseTimeSlots(Page page, List<String> timeSlots) {
        log.debug("Parsing available time slots");

        try {
            // Multiple selectors for different time slot UI patterns
            Locator timeSlotButtons = page.locator(".mat-button-toggle-button .mat-button-toggle-label-content")
                    .or(page.locator(".time-slot"))
                    .or(page.locator(".appointment-time"))
                    .or(page.locator("button").filter(new Locator.FilterOptions()
                            .setHasText(java.util.regex.Pattern.compile("\\d{1,2}:\\d{2}|\\d{1,2} ?[ap]m", java.util.regex.Pattern.CASE_INSENSITIVE))))
                    .or(page.locator("[class*='time']").filter(new Locator.FilterOptions()
                            .setHasText(java.util.regex.Pattern.compile("\\d{1,2}:\\d{2}|\\d{1,2} ?[ap]m", java.util.regex.Pattern.CASE_INSENSITIVE))));

            int timeSlotCount = timeSlotButtons.count();
            log.debug("Found {} time slot elements", timeSlotCount);

            for (int i = 0; i < timeSlotCount; i++) {
                try {
                    String timeText = timeSlotButtons.nth(i).textContent().trim();
                    if (!timeText.isEmpty() && isValidTimeText(timeText)) {
                        timeSlots.add(timeText);
                        log.debug("Added time slot: '{}'", timeText);
                    }
                } catch (PlaywrightException e) {
                    log.debug("Could not read time slot element {}: {}", i, e.getMessage());
                }
            }

            log.debug("Successfully parsed {} time slots", timeSlots.size());

        } catch (Exception e) {
            log.warn("Error parsing time slots: {}", e.getMessage());
        }
    }

    private boolean isValidDateText(String dateText) {
        if (dateText == null || dateText.trim().isEmpty()) {
            return false;
        }

        String normalized = dateText.toLowerCase().trim();

        // Filter out common non-date UI text
        if (normalized.contains("no appointment") ||
                normalized.contains("not available") ||
                normalized.contains("select") ||
                normalized.length() < 3) {
            return false;
        }

        // Match common date patterns: mm/dd, mm-dd, Month dd, dd Month
        return dateText.matches(".*\\d{1,2}[/\\-]\\d{1,2}.*") ||
                dateText.matches(".*\\w+ \\d{1,2}.*") ||
                dateText.matches(".*\\d{1,2} \\w+.*");
    }

    private boolean isValidTimeText(String timeText) {
        if (timeText == null || timeText.trim().isEmpty()) {
            return false;
        }

        String normalized = timeText.toLowerCase().trim();

        // Filter out common non-time UI text
        if (normalized.contains("no appointment") ||
                normalized.contains("not available") ||
                normalized.contains("select") ||
                normalized.length() < 3) {
            return false;
        }

        // Match common time patterns: HH:MM, H AM/PM, HH:MM AM/PM
        return timeText.matches(".*\\d{1,2}:\\d{2}.*") ||
                timeText.matches(".*\\d{1,2} ?[ap]m.*") ||
                timeText.matches(".*\\d{1,2}:\\d{2} ?[ap]m.*");
    }

    private void checkForNoResultsMessages(Page page) {
        try {
            Locator noResultsMessages = page.locator(":has-text('No appointments')")
                    .or(page.locator(":has-text('not available')"))
                    .or(page.locator(":has-text('No results')"))
                    .or(page.locator(".no-results"))
                    .or(page.locator(".empty-results"))
                    .or(page.locator("[class*='no-appointment']"));

            if (noResultsMessages.count() > 0) {
                for (int i = 0; i < noResultsMessages.count(); i++) {
                    try {
                        String messageText = noResultsMessages.nth(i).textContent().trim();
                        if (!messageText.isEmpty()) {
                            log.info("Found 'no results' message: '{}'", messageText);
                        }
                    } catch (Exception e) {
                        log.debug("Could not read no-results message {}: {}", i, e.getMessage());
                    }
                }
            } else {
                log.debug("No explicit 'no results' messages found");
            }

        } catch (Exception e) {
            log.warn("Error checking for no-results messages: {}", e.getMessage());
        }
    }

    private void takeDebugScreenshot(Page page, String filename) {
        try {
            page.screenshot(new Page.ScreenshotOptions()
                    .setPath(Paths.get(filename))
                    .setFullPage(true));
            log.debug("Saved debug screenshot: {}", filename);
        } catch (Exception e) {
            log.warn("Could not take debug screenshot: {}", e.getMessage());
        }
    }
}
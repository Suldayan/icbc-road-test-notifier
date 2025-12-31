package com.example.icbc_road_test_notifier.navigation.internal;

import com.example.icbc_road_test_notifier.shared.DaysOfTheWeek;
import com.example.icbc_road_test_notifier.navigation.DaySelectionService;
import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.PlaywrightException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.nio.file.Paths;
import java.util.Set;

@Service
@Slf4j
public class DaySelectionServiceImpl implements DaySelectionService {

    @Override
    public void selectDays(Page page, Set<DaysOfTheWeek> preferredDays) {
        if (preferredDays == null || preferredDays.isEmpty()) {
            log.debug("No preferred days specified, skipping day selection");
            return;
        }

        log.debug("Selecting {} preferred days", preferredDays.size());

        for (DaysOfTheWeek day : preferredDays) {
            selectDay(page, day);
            page.waitForTimeout(200);
        }

        log.debug("Completed day selection for {} days", preferredDays.size());
    }

    private void selectDay(Page page, DaysOfTheWeek day) {
        log.debug("Attempting to select day: {}", day.getDisplayName());

        try {
            String dayName = day.getDisplayName().toLowerCase();

            Locator dayCheckbox = page.locator(String.format("mat-checkbox[name='%s']", dayName));

            // Fallback selectors in case ICBC changes their structure
            if (dayCheckbox.count() == 0) {
                dayCheckbox = page.locator("mat-checkbox").filter(new Locator.FilterOptions()
                        .setHasText(day.getDisplayName()));
            }

            if (dayCheckbox.count() == 0) {
                dayCheckbox = page.locator("mat-checkbox").filter(new Locator.FilterOptions()
                        .setHasText(dayName));
            }

            if (dayCheckbox.count() > 0) {
                processCheckboxSelection(page, dayCheckbox, day);
            } else {
                handleCheckboxNotFound(page, day);
            }

        } catch (PlaywrightException e) {
            log.warn("Failed to select day {}: {}", day.getDisplayName(), e.getMessage());
            takeDebugScreenshot(page, "debug-day-selection-" + day.getDisplayName().toLowerCase() + "-" + System.currentTimeMillis() + ".png");
        }
    }

    private void processCheckboxSelection(Page page, Locator dayCheckbox, DaysOfTheWeek day) {
        dayCheckbox.first().scrollIntoViewIfNeeded();
        page.waitForTimeout(200);

        boolean isAlreadyChecked = isCheckboxSelected(dayCheckbox);

        if (!isAlreadyChecked) {
            dayCheckbox.first().click();
            log.debug("Clicked checkbox for {}", day.getDisplayName());

            page.waitForTimeout(300);
            verifyCheckboxSelection(dayCheckbox, day);
        } else {
            log.debug("{} was already selected", day.getDisplayName());
        }
    }

    private boolean isCheckboxSelected(Locator dayCheckbox) {
        // Angular Material checkboxes use mat-checkbox-checked class when selected
        String checkboxClasses = dayCheckbox.first().getAttribute("class");
        boolean isAlreadyChecked = checkboxClasses != null && checkboxClasses.contains("mat-checkbox-checked");

        // Fallback: check aria-checked on the actual input element
        if (!isAlreadyChecked) {
            Locator actualInput = dayCheckbox.first().locator("input.mat-checkbox-input");
            if (actualInput.count() > 0) {
                String ariaChecked = actualInput.getAttribute("aria-checked");
                isAlreadyChecked = "true".equals(ariaChecked);
            }
        }

        return isAlreadyChecked;
    }

    private void verifyCheckboxSelection(Locator dayCheckbox, DaysOfTheWeek day) {
        String updatedClasses = dayCheckbox.first().getAttribute("class");
        if (updatedClasses != null && updatedClasses.contains("mat-checkbox-checked")) {
            log.debug("Confirmed {} is now selected", day.getDisplayName());
        } else {
            log.warn("Failed to confirm {} selection - classes: {}", day.getDisplayName(), updatedClasses);

            // Secondary verification method
            Locator actualInput = dayCheckbox.first().locator("input.mat-checkbox-input");
            if (actualInput.count() > 0) {
                String ariaChecked = actualInput.getAttribute("aria-checked");
                if ("true".equals(ariaChecked)) {
                    log.debug("Alternative verification: {} is selected (aria-checked=true)", day.getDisplayName());
                } else {
                    log.warn("Alternative verification also failed for {}", day.getDisplayName());
                }
            }
        }
    }

    private void handleCheckboxNotFound(Page page, DaysOfTheWeek day) {
        log.warn("Could not find checkbox for day: {}", day.getDisplayName());

        debugLogAllCheckboxes(page);
        takeDebugScreenshot(page, "debug-missing-checkbox-" + day.getDisplayName().toLowerCase() + "-" + System.currentTimeMillis() + ".png");
    }

    private void debugLogAllCheckboxes(Page page) {
        try {
            Locator allCheckboxes = page.locator("mat-checkbox");
            log.debug("Found {} total checkboxes", allCheckboxes.count());

            for (int i = 0; i < allCheckboxes.count(); i++) {
                try {
                    String name = allCheckboxes.nth(i).getAttribute("name");
                    String text = allCheckboxes.nth(i).textContent();
                    String classes = allCheckboxes.nth(i).getAttribute("class");
                    log.debug("Checkbox {}: name='{}', text='{}', classes='{}'",
                            i, name, text != null ? text.trim() : "null", classes);
                } catch (Exception e) {
                    log.debug("Could not read checkbox {}: {}", i, e.getMessage());
                }
            }
        } catch (Exception e) {
            log.warn("Failed to debug log checkboxes: {}", e.getMessage());
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
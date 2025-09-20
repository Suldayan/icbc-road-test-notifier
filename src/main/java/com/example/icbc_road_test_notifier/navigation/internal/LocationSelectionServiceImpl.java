package com.example.icbc_road_test_notifier.navigation.internal;

import com.example.icbc_road_test_notifier.navigation.LocationSelectionService;
import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.PlaywrightException;
import com.microsoft.playwright.options.LoadState;
import com.microsoft.playwright.options.WaitForSelectorState;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

@Service
@Slf4j
public class LocationSelectionServiceImpl implements LocationSelectionService {

    @Override
    public void selectLocation(Page page, String locationQuery) {
        log.debug("Selecting location: {}", locationQuery);

        if (locationQuery == null || locationQuery.trim().isEmpty()) {
            log.debug("No location query provided, skipping location selection");
            return;
        }

        try {
            Locator locationInput = page.locator("input[formcontrolname='finishedAutocomplete']")
                    .or(page.locator("input[placeholder='Start typing...']"))
                    .or(page.locator("input.mat-autocomplete-trigger"));

            locationInput.first().waitFor(new Locator.WaitForOptions()
                    .setState(WaitForSelectorState.VISIBLE)
                    .setTimeout(10000));

            locationInput.first().clear();
            page.waitForTimeout(500);

            // Method 1: Character-by-character typing to trigger Angular autocomplete
            log.debug("Typing '{}' character by character", locationQuery);
            locationInput.first().click();

            for (char c : locationQuery.toCharArray()) {
                locationInput.first().pressSequentially(String.valueOf(c));
                page.waitForTimeout(150);
            }

            page.waitForTimeout(3000);

            Locator autocompletePanel = page.locator(".mat-autocomplete-panel")
                    .or(page.locator("div[role='listbox']"));

            boolean dropdownAppeared = false;
            try {
                autocompletePanel.first().waitFor(new Locator.WaitForOptions()
                        .setState(WaitForSelectorState.VISIBLE)
                        .setTimeout(2000));
                dropdownAppeared = true;
                log.debug("Autocomplete dropdown appeared after character-by-character typing");
            } catch (PlaywrightException e) {
                log.warn("Dropdown still not visible after character typing, trying alternative methods");
            }

            // Method 2: Manual event triggers for stubborn autocompletes
            if (!dropdownAppeared) {
                log.debug("Attempting to trigger input events manually");

                locationInput.first().clear();
                page.waitForTimeout(300);

                locationInput.first().click();
                locationInput.first().fill(locationQuery);

                locationInput.first().dispatchEvent("input");
                locationInput.first().dispatchEvent("keyup");
                locationInput.first().dispatchEvent("focus");

                page.waitForTimeout(2000);

                try {
                    autocompletePanel.first().waitFor(new Locator.WaitForOptions()
                            .setState(WaitForSelectorState.VISIBLE)
                            .setTimeout(2000));
                    dropdownAppeared = true;
                    log.debug("Autocomplete dropdown appeared after manual event triggers");
                } catch (PlaywrightException e) {
                    log.warn("Dropdown still not visible after manual events");
                }
            }

            // Method 3: Keyboard events to wake up dormant autocomplete
            if (!dropdownAppeared) {
                log.debug("Attempting to trigger dropdown with keyboard events");

                locationInput.first().focus();
                page.waitForTimeout(300);

                locationInput.first().press("ArrowDown");
                page.waitForTimeout(1000);

                locationInput.first().press("Space");
                page.waitForTimeout(200);
                locationInput.first().press("Backspace");
                page.waitForTimeout(1000);

                try {
                    autocompletePanel.first().waitFor(new Locator.WaitForOptions()
                            .setState(WaitForSelectorState.VISIBLE)
                            .setTimeout(2000));
                    dropdownAppeared = true;
                    log.debug("Autocomplete dropdown appeared after keyboard triggers");
                } catch (PlaywrightException e) {
                    log.warn("Dropdown still not visible after keyboard events");
                }
            }

            // Method 4: Force-show hidden panels using JavaScript
            if (!dropdownAppeared) {
                log.debug("Checking for autocomplete options even if panel appears hidden");

                Locator hiddenOptions = page.locator("mat-option")
                        .or(page.locator(".mat-option"))
                        .or(page.locator("[role='option']"));

                if (hiddenOptions.count() > 0) {
                    log.debug("Found {} options in potentially hidden panel", hiddenOptions.count());

                    page.evaluate("document.querySelectorAll('.mat-autocomplete-panel').forEach(el => {" +
                            "el.style.display = 'block';" +
                            "el.style.visibility = 'visible';" +
                            "el.style.opacity = '1';" +
                            "el.classList.remove('mat-autocomplete-hidden');" +
                            "})");

                    page.waitForTimeout(1000);
                    dropdownAppeared = hiddenOptions.count() > 0;
                }
            }

            if (dropdownAppeared) {
                selectFromDropdownOptions(page, locationQuery);
            } else {
                handleNoDropdownFallback(page, locationQuery);
            }

        } catch (PlaywrightException e) {
            log.error("Failed to select location '{}': {}", locationQuery, e.getMessage());
            takeDebugScreenshot(page, "debug-location-error-" + System.currentTimeMillis() + ".png");
        }
    }

    @Override
    public void selectSpecificLocation(Page page, String preferredLocationName) {
        log.debug("Selecting specific location: {}", preferredLocationName);

        if (preferredLocationName == null || preferredLocationName.trim().isEmpty()) {
            log.debug("No specific location preference, using default selection");
            return;
        }

        try {
            page.waitForLoadState(LoadState.NETWORKIDLE);
            page.waitForTimeout(2000);

            List<Locator> allLocationContainers = new ArrayList<>();

            Locator nearestLocation = page.locator(".first-office-container .background-highlight");
            if (nearestLocation.count() > 0) {
                allLocationContainers.add(nearestLocation.first());
            }

            Locator otherLocations = page.locator(".other-locations-container .background-highlight.other-locations");
            for (int i = 0; i < otherLocations.count(); i++) {
                allLocationContainers.add(otherLocations.nth(i));
            }

            log.debug("Found {} total location options", allLocationContainers.size());

            String normalizedPreferred = preferredLocationName.toLowerCase().trim();
            Locator bestMatch = null;
            String bestMatchName = "";

            // Smart matching: exact > contains > city/area match
            for (int i = 0; i < allLocationContainers.size(); i++) {
                Locator locationContainer = allLocationContainers.get(i);

                try {
                    Locator titleElement = locationContainer.locator(".department-title");
                    if (titleElement.count() == 0) continue;

                    String locationTitle = titleElement.textContent().trim();
                    String normalizedTitle = locationTitle.toLowerCase();

                    log.debug("Checking location {}: '{}'", i, locationTitle);

                    boolean isExactMatch = normalizedTitle.equals(normalizedPreferred);
                    boolean containsPreferred = normalizedTitle.contains(normalizedPreferred);
                    boolean preferredContainsTitle = normalizedPreferred.contains(normalizedTitle);

                    // City/area matching (e.g., "vancouver" matches "Vancouver Driver Licensing")
                    boolean cityMatch = false;
                    String[] titleWords = normalizedTitle.split("\\s+");
                    String[] preferredWords = normalizedPreferred.split("\\s+");

                    for (String titleWord : titleWords) {
                        for (String preferredWord : preferredWords) {
                            if (titleWord.equals(preferredWord) && titleWord.length() > 3) {
                                cityMatch = true;
                                break;
                            }
                        }
                        if (cityMatch) break;
                    }

                    if (isExactMatch) {
                        bestMatch = locationContainer;
                        bestMatchName = locationTitle;
                        log.debug("Found exact match: '{}'", locationTitle);
                        break;
                    } else if (containsPreferred || preferredContainsTitle) {
                        if (bestMatch == null) {
                            bestMatch = locationContainer;
                            bestMatchName = locationTitle;
                            log.debug("Found contains match: '{}'", locationTitle);
                        }
                    } else if (cityMatch && bestMatch == null) {
                        bestMatch = locationContainer;
                        bestMatchName = locationTitle;
                        log.debug("Found city match: '{}'", locationTitle);
                    }

                } catch (PlaywrightException e) {
                    log.warn("Could not process location container {}: {}", i, e.getMessage());
                }
            }

            if (bestMatch != null) {
                clickLocationIfNotSelected(page, bestMatch, bestMatchName);
            } else {
                handleNoLocationMatch(page, preferredLocationName, allLocationContainers);
            }

        } catch (PlaywrightException e) {
            log.error("Failed to select location '{}': {}", preferredLocationName, e.getMessage());
            takeDebugScreenshot(page, "debug-location-selection-" + System.currentTimeMillis() + ".png");
        }
    }

    private void selectFromDropdownOptions(Page page, String locationQuery) {
        Locator autocompleteOptions = page.locator("mat-option.mat-option")
                .or(page.locator(".mat-autocomplete-panel mat-option"))
                .or(page.locator("[role='option']"))
                .or(page.locator(".cdk-overlay-pane mat-option"));

        if (autocompleteOptions.count() > 0) {
            log.debug("Found {} autocomplete options", autocompleteOptions.count());

            String normalizedQuery = locationQuery.toLowerCase().trim();

            for (int i = 0; i < autocompleteOptions.count(); i++) {
                Locator option = autocompleteOptions.nth(i);

                try {
                    String optionText = option.locator(".mat-option-text").count() > 0 ?
                            option.locator(".mat-option-text").textContent().trim() :
                            option.textContent().trim();

                    String normalizedOptionText = optionText.toLowerCase();
                    log.debug("Comparing '{}' with option: '{}'", normalizedQuery, optionText);

                    // Flexible matching for various location name formats
                    if (normalizedOptionText.equals(normalizedQuery) ||
                            normalizedOptionText.contains(normalizedQuery) ||
                            normalizedOptionText.startsWith(normalizedQuery) ||
                            normalizedQuery.contains(normalizedOptionText.split(",")[0].trim().toLowerCase())) {

                        option.scrollIntoViewIfNeeded();
                        page.waitForTimeout(200);
                        option.click();
                        log.info("Selected matching location: '{}'", optionText);
                        page.waitForTimeout(500);
                        return;
                    }
                } catch (PlaywrightException e) {
                    log.debug("Could not process option {}: {}", i, e.getMessage());
                }
            }

            selectFirstOption(page, autocompleteOptions);
        }
    }

    private void selectFirstOption(Page page, Locator autocompleteOptions) {
        try {
            String firstOptionText = autocompleteOptions.first().locator(".mat-option-text").count() > 0 ?
                    autocompleteOptions.first().locator(".mat-option-text").textContent().trim() :
                    autocompleteOptions.first().textContent().trim();

            autocompleteOptions.first().scrollIntoViewIfNeeded();
            page.waitForTimeout(200);
            autocompleteOptions.first().click();
            log.info("Selected first available option: '{}'", firstOptionText);
            page.waitForTimeout(500);
        } catch (PlaywrightException e) {
            log.error("Failed to select first option: {}", e.getMessage());
        }
    }

    private void handleNoDropdownFallback(Page page, String locationQuery) {
        log.warn("Autocomplete dropdown never appeared for query '{}'. This might indicate:", locationQuery);
        log.warn("1. The input field requires a minimum number of characters");
        log.warn("2. There's a network request delay loading the options");
        log.warn("3. The autocomplete is disabled or has JavaScript errors");
        log.warn("4. The page hasn't fully loaded yet");

        takeDebugScreenshot(page, "debug-location-no-dropdown-" + System.currentTimeMillis() + ".png");

        // Last resort: try Enter key to accept typed value
        try {
            Locator locationInput = page.locator("input[formcontrolname='finishedAutocomplete']");
            locationInput.first().press("Enter");
            log.debug("Pressed Enter as fallback to confirm typed location");
            page.waitForTimeout(1000);
        } catch (PlaywrightException e) {
            log.warn("Failed to press Enter as fallback: {}", e.getMessage());
        }
    }

    private void clickLocationIfNotSelected(Page page, Locator bestMatch, String bestMatchName) {
        // ICBC uses 'clicked' class to indicate selected location
        String containerClasses = bestMatch.getAttribute("class");
        boolean alreadySelected = containerClasses != null && containerClasses.contains("clicked");

        if (alreadySelected) {
            log.info("Location '{}' is already selected", bestMatchName);
        } else {
            bestMatch.scrollIntoViewIfNeeded();
            page.waitForTimeout(300);
            bestMatch.click();

            log.info("Selected location: '{}'", bestMatchName);

            page.waitForTimeout(1000);

            String updatedClasses = bestMatch.getAttribute("class");
            if (updatedClasses != null && updatedClasses.contains("clicked")) {
                log.debug("Confirmed location selection: '{}'", bestMatchName);
            } else {
                log.warn("Location selection may not have worked for: '{}'", bestMatchName);
            }
        }
    }

    private void handleNoLocationMatch(Page page, String preferredLocationName, List<Locator> allLocationContainers) {
        log.warn("Could not find a suitable match for location: '{}'", preferredLocationName);

        log.warn("Available locations:");
        for (int i = 0; i < allLocationContainers.size(); i++) {
            try {
                Locator titleElement = allLocationContainers.get(i).locator(".department-title");
                if (titleElement.count() > 0) {
                    String title = titleElement.textContent().trim();
                    log.warn("  {}: '{}'", i, title);
                }
            } catch (Exception e) {
                log.warn("  {}: Could not read title", i);
            }
        }

        if (!allLocationContainers.isEmpty()) {
            selectFallbackLocation(page, allLocationContainers.getFirst());
        }
    }

    private void selectFallbackLocation(Page page, Locator fallback) {
        try {
            String fallbackClasses = fallback.getAttribute("class");

            if (fallbackClasses == null || !fallbackClasses.contains("clicked")) {
                fallback.scrollIntoViewIfNeeded();
                page.waitForTimeout(300);
                fallback.click();

                Locator fallbackTitle = fallback.locator(".department-title");
                String fallbackName = fallbackTitle.count() > 0 ?
                        fallbackTitle.textContent().trim() : "Unknown";

                log.info("Selected first available location as fallback: '{}'", fallbackName);
                page.waitForTimeout(1000);
            }
        } catch (Exception e) {
            log.error("Failed to select fallback location: {}", e.getMessage());
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
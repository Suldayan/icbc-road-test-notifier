package com.example.icbc_road_test_notifier.navigation.internal;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.PlaywrightException;
import com.microsoft.playwright.options.AriaRole;
import com.microsoft.playwright.options.LoadState;
import com.microsoft.playwright.options.WaitForSelectorState;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.nio.file.Paths;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

@Service
@Slf4j
public class NavigationServiceImpl {
    public static final String LOGIN_URL = "https://onlinebusiness.icbc.com/webdeas-ui/login;type=driver";
    public static final int NAVIGATION_TIMEOUT = 60000;
    public static final int URL_WAIT_TIMEOUT = 30000;
    public static final int ELEMENT_WAIT_TIMEOUT = 15000;
    public static final int CONFIRMATION_TIMEOUT = 10000;
    public static final int CLICK_DELAY = 500;
    public static final String DEBUG_SCREENSHOT_FILENAME = "debug-reschedule-button.png";

    public void authenticate(Page page, String lastName, String licenseNumber, String keyword) {
        log.debug("Authenticating user");

        page.navigate(LOGIN_URL, new Page.NavigateOptions().setTimeout(NAVIGATION_TIMEOUT));

        fillLoginForm(page, lastName, licenseNumber, keyword);
        handleTermsCheckbox(page);
        submitLogin(page);

        page.waitForURL(url -> !url.equals(LOGIN_URL), new Page.WaitForURLOptions().setTimeout(URL_WAIT_TIMEOUT));
        assertThat(page).not().hasURL(LOGIN_URL);

        log.info("Authentication successful");
    }

    public void navigateToAppointmentSection(Page page) {
        log.debug("Navigating to appointment section");

        page.waitForLoadState(LoadState.NETWORKIDLE);

        Locator rescheduleButton = findRescheduleButton(page);
        clickRescheduleButton(page, rescheduleButton);
        handleRescheduleConfirmation(page);

        page.waitForLoadState(LoadState.NETWORKIDLE);
    }

    private void fillLoginForm(Page page, String lastName, String licenseNumber, String keyword) {
        page.getByLabel("Driver's last name")
                .or(page.locator("input[formcontrolname='drvrLastName']"))
                .fill(lastName);

        page.getByLabel("B.C. driver's or learner's licence number")
                .or(page.locator("input[formcontrolname='licenceNumber']"))
                .fill(licenseNumber);

        page.getByLabel("ICBC keyword")
                .or(page.locator("input[formcontrolname='keyword']"))
                .fill(keyword);
    }

    private void handleTermsCheckbox(Page page) {
        Locator termsCheckbox = page.locator("mat-checkbox[formcontrolname='cb']")
                .or(page.locator("label[for='mat-checkbox-1-input']"))
                .or(page.getByRole(AriaRole.CHECKBOX, new Page.GetByRoleOptions().setName("I have read and agree to the")));

        if (termsCheckbox.count() > 0) {
            termsCheckbox.first().click();
            log.debug("Clicked terms and conditions checkbox");
        }
    }

    private void submitLogin(Page page) {
        page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Sign in"))
                .or(page.locator("button[type='submit']:has-text('Sign in')"))
                .click();
    }

    private Locator findRescheduleButton(Page page) {
        return page.locator("button.raised-button.primary:has-text('Reschedule appointment')")
                .or(page.locator("button.raised-button:has-text('Reschedule appointment')"))
                .or(page.locator("button:has-text('Reschedule appointment')"))
                .or(page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Reschedule appointment")));
    }

    private void clickRescheduleButton(Page page, Locator rescheduleButton) {
        try {
            rescheduleButton.first().waitFor(new Locator.WaitForOptions()
                    .setState(WaitForSelectorState.VISIBLE)
                    .setTimeout(ELEMENT_WAIT_TIMEOUT));

            rescheduleButton.first().scrollIntoViewIfNeeded();
            page.waitForTimeout(CLICK_DELAY);

            try {
                rescheduleButton.first().click();
                log.debug("Successfully clicked reschedule appointment button");
            } catch (PlaywrightException e) {
                log.warn("Standard click failed, trying force click: {}", e.getMessage());
                rescheduleButton.first().click(new Locator.ClickOptions().setForce(true));
            }
        } catch (PlaywrightException e) {
            log.error("Failed to find reschedule button: {}", e.getMessage());
            takeDebugScreenshot(page);
        }
    }

    private void handleRescheduleConfirmation(Page page) {
        try {
            Locator confirmButton = page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Yes"))
                    .or(page.locator("button:has-text('Yes')"));

            confirmButton.first().waitFor(new Locator.WaitForOptions()
                    .setState(WaitForSelectorState.VISIBLE)
                    .setTimeout(CONFIRMATION_TIMEOUT));

            confirmButton.first().click();
            log.debug("Confirmed reschedule");
        } catch (PlaywrightException e) {
            log.warn("Could not find or click confirmation dialog: {}", e.getMessage());
        }
    }

    private void takeDebugScreenshot(Page page) {
        try {
            page.screenshot(new Page.ScreenshotOptions().setPath(Paths.get(DEBUG_SCREENSHOT_FILENAME)));
        } catch (Exception e) {
            log.warn("Could not take debug screenshot: {}", e.getMessage());
        }
    }
}
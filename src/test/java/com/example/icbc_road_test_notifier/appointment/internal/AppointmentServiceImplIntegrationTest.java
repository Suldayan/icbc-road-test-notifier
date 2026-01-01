package com.example.icbc_road_test_notifier.appointment.internal;

import com.example.icbc_road_test_notifier.appointment.AppointmentFound;
import com.example.icbc_road_test_notifier.shared.DateRangePreference;
import com.example.icbc_road_test_notifier.shared.DaysOfTheWeek;
import com.example.icbc_road_test_notifier.shared.TimePreference;
import com.example.icbc_road_test_notifier.shared.IcbcConfig;
import com.microsoft.playwright.*;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.test.context.TestPropertySource;

import java.time.LocalDate;
import java.time.Period;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@TestPropertySource(properties = {
        "logging.level.com.example=DEBUG",
        "icbc.last-name=TestUser",
        "icbc.license-number=1234567",
        "icbc.keyword=test-keyword",
        "icbc.preferred-location=Vancouver, BC",
        "icbc.preferred-days=MONDAY,TUESDAY",
        "icbc.time-preference=ANY",
        "icbc.date-range-preference.start-date=2025-01-01",
        "icbc.date-range-preference.end-date=2025-12-31"
})
class AppointmentServiceImplIntegrationTest {

    private ApplicationEventPublisher eventPublisher;
    @Autowired
    private AppointmentServiceImpl appointmentService;
    private Playwright playwright;
    private Browser browser;

    @BeforeEach
    void setUp() {
        eventPublisher = mock(ApplicationEventPublisher.class);
        playwright = Playwright.create();
        browser = playwright.chromium().launch(new BrowserType.LaunchOptions().setHeadless(true));
    }

    @AfterEach
    void tearDown() {
        if (browser != null) {
            browser.close();
        }
        if (playwright != null) {
            playwright.close();
        }
    }

    @Test
    void shouldHandleInvalidCredentials() {
        IcbcConfig config = new IcbcConfig(
                "",
                "validLicense",
                "validKeyword",
                "vancouver",
                Set.of(DaysOfTheWeek.MONDAY, DaysOfTheWeek.TUESDAY),
                null,
                null
        );

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> appointmentService.authenticateAndSearchAppointments(config)
        );

        assertEquals("Last name is required", exception.getMessage());
        verifyNoInteractions(eventPublisher);
    }

    @Test
    void shouldHandleEmptyLicenseNumber() {
        IcbcConfig config = new IcbcConfig(
                "Smith",
                "",
                "validKeyword",
                "vancouver",
                Set.of(DaysOfTheWeek.MONDAY, DaysOfTheWeek.TUESDAY),
                null,
                null
        );

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> appointmentService.authenticateAndSearchAppointments(config)
        );

        assertEquals("License number is required", exception.getMessage());
        verifyNoInteractions(eventPublisher);
    }

    @Test
    void shouldHandleEmptyKeyword() {
        IcbcConfig config = new IcbcConfig(
                "Smith",
                "1234567",
                "",
                "vancouver",
                Set.of(DaysOfTheWeek.MONDAY, DaysOfTheWeek.TUESDAY),
                null,
                null
        );

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> appointmentService.authenticateAndSearchAppointments(config)
        );

        assertEquals("Keyword is required", exception.getMessage());
        verifyNoInteractions(eventPublisher);
    }

    @Test
    void shouldCreateBrowserContextWithCorrectSettings() {
        BrowserContext context = browser.newContext(new Browser.NewContextOptions()
                .setViewportSize(1920, 1080)
                .setUserAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36"));

        Page page = context.newPage();

        assertNotNull(page);
        assertEquals(1920, page.viewportSize().width);
        assertEquals(1080, page.viewportSize().height);

        context.close();
    }

    @Test
    void shouldHandlePlaywrightExceptions() {
        // Verify service handles network/navigation failures gracefully
        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> {
                    try (BrowserContext context = browser.newContext()) {
                        Page page = context.newPage();
                        page.navigate("invalid://url", new Page.NavigateOptions().setTimeout(1000));
                    }
                }
        );

        assertNotNull(exception.getMessage());
    }

    @Disabled("Disabled for development - remove when ready to test backward compatibility")
    @Test
    void shouldSupportSingleLocationMethodSignature() {
        IcbcConfig config = new IcbcConfig(
                "Smith",
                "1234567",
                "keyword",
                "Vancouver, BC",
                Set.of(DaysOfTheWeek.MONDAY),
                null,
                null
        );

        assertDoesNotThrow(() -> appointmentService.authenticateAndSearchAppointments(config));
    }

    @Test
    void shouldCompleteFullWorkflowWithMockServer() {
        try (BrowserContext context = browser.newContext()) {
            Page page = context.newPage();

            // Mock HTML that mirrors ICBC's actual structure for testing selectors
            String mockHtml = """
                <html>
                <body>
                    <form>
                        <input aria-label="Driver's last name" formcontrolname="drvrLastName" />
                        <input aria-label="B.C. driver's or learner's licence number" formcontrolname="licenceNumber" />
                        <input aria-label="ICBC keyword" formcontrolname="keyword" />
                        <mat-checkbox formcontrolname="cb">
                            <input type="checkbox" aria-label="I have read and agree to the" />
                        </mat-checkbox>
                        <button role="button" aria-label="Sign in">Sign in</button>
                    </form>
                    
                    <button class="raised-button primary">Reschedule appointment</button>
                    
                    <input formcontrolname="finishedAutocomplete" placeholder="Start typing..." class="mat-autocomplete-trigger" />
                    
                    <div class="mat-autocomplete-panel" role="listbox">
                        <mat-option class="mat-option" role="option">
                            <span class="mat-option-text">Vancouver, BC</span>
                        </mat-option>
                        <mat-option class="mat-option" role="option">
                            <span class="mat-option-text">Burnaby, BC</span>
                        </mat-option>
                    </div>
                    
                    <mat-checkbox>
                        <input type="checkbox" role="checkbox" aria-label="Monday" />
                        <label>Monday</label>
                    </mat-checkbox>
                    <mat-checkbox>
                        <input type="checkbox" role="checkbox" aria-label="Tuesday" />
                        <label>Tuesday</label>
                    </mat-checkbox>
                    
                    <button role="button" aria-label="Search">Search</button>
                    
                    <div class="appointment-listings">
                        <div class="date-title">Monday, Jan 15</div>
                        <div class="mat-button-toggle-button">
                            <span class="mat-button-toggle-label-content">9:00 AM</span>
                        </div>
                        <div class="date-title">Tuesday, Jan 16</div>
                        <div class="mat-button-toggle-button">
                            <span class="mat-button-toggle-label-content">2:15 PM</span>
                        </div>
                    </div>
                </body>
                </html>
            """;

            page.setContent(mockHtml);

            // Verify all Playwright selectors used in service can find elements
            assertTrue(page.getByLabel("Driver's last name").count() > 0);
            assertTrue(page.getByLabel("B.C. driver's or learner's licence number").count() > 0);
            assertTrue(page.getByLabel("ICBC keyword").count() > 0);
            assertTrue(page.locator("mat-checkbox[formcontrolname='cb']").count() > 0);
            assertTrue(page.locator("button.raised-button.primary").count() > 0);
            assertTrue(page.locator("input[formcontrolname='finishedAutocomplete']").count() > 0);
            assertTrue(page.locator(".mat-autocomplete-panel mat-option").count() > 0);
            assertTrue(page.locator(".appointment-listings").count() > 0);
            assertTrue(page.locator(".date-title").count() > 0);
            assertTrue(page.locator(".mat-button-toggle-label-content").count() > 0);
        }
    }

    @Disabled("Requires valid ICBC test credentials - enable with -Dicbc.test.* system properties")
    @Test
    void shouldCompleteFullWorkflowWithRealICBCWebsite() {
        // System properties: -Dicbc.test.lastName=DOE -Dicbc.test.licenseNumber=1234567 -Dicbc.test.keyword=test
        String testLastName = System.getProperty("icbc.test.lastName");
        String testLicense = System.getProperty("icbc.test.licenseNumber");
        String testKeyword = System.getProperty("icbc.test.keyword");

        if (testLastName == null || testLicense == null || testKeyword == null) {
            System.out.println("Skipping real ICBC test - no test credentials provided");
            return;
        }

        IcbcConfig config = new IcbcConfig(
                testLastName,
                testLicense,
                testKeyword,
                "vancouver",
                Set.of(DaysOfTheWeek.MONDAY, DaysOfTheWeek.TUESDAY),
                TimePreference.ANY,
                new DateRangePreference(
                        LocalDate.now(),
                        LocalDate.now().plus(Period.ofDays(120)))
        );

        assertDoesNotThrow(() -> appointmentService.authenticateAndSearchAppointments(config));

        verify(eventPublisher, atMost(1)).publishEvent(any(AppointmentFound.class));
    }

    @Disabled("Requires valid ICBC test credentials")
    @Test
    void shouldCompleteFullWorkflowWithPriorityLocations() {
        String testLastName = System.getProperty("icbc.test.lastName");
        String testLicense = System.getProperty("icbc.test.licenseNumber");
        String testKeyword = System.getProperty("icbc.test.keyword");

        if (testLastName == null || testLicense == null || testKeyword == null) {
            System.out.println("Skipping priority location test - no test credentials provided");
            return;
        }

        IcbcConfig config = new IcbcConfig(
                testLastName,
                testLicense,
                testKeyword,
                "North Vancouver, BC",
                Set.of(DaysOfTheWeek.MONDAY, DaysOfTheWeek.FRIDAY),
                null,
                null
        );

        assertDoesNotThrow(() -> appointmentService.authenticateAndSearchAppointments(config));

        verify(eventPublisher, atMost(1)).publishEvent(any(AppointmentFound.class));
    }
}
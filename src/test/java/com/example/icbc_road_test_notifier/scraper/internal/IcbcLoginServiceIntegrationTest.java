package com.example.icbc_road_test_notifier.scraper.internal;

import com.example.icbc_road_test_notifier.scraper.IcbcScrapingService;
import com.microsoft.playwright.*;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

@SpringBootTest
@TestPropertySource(properties = {
        "icbc.scraper.timeout-seconds=15",
        "icbc.scraper.max-retry-attempts=1",
        "icbc.scraper.retry-delay=PT1S"
})
class IcbcLoginServiceIntegrationTest {

    @Autowired
    private IcbcScrapingService loginService;

    @Autowired
    private Validator validator;

    @Autowired
    private IcbcScraperProperties properties;

    private static Playwright playwright;
    private static Browser browser;
    private BrowserContext context;
    private Page page;

    @BeforeAll
    static void setUpClass() {
        playwright = Playwright.create();
        browser = playwright.chromium().launch(new BrowserType.LaunchOptions()
                .setHeadless(false)
                .setChannel("msedge")
                .setSlowMo(500));
    }

    @AfterAll
    static void tearDownClass() {
        if (browser != null) browser.close();
        if (playwright != null) playwright.close();
    }

    @BeforeEach
    void setUp() {
        context = browser.newContext(new Browser.NewContextOptions()
                .setViewportSize(1280, 720)
                .setUserAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36"));
        page = context.newPage();
    }

    @AfterEach
    void tearDown() {
        if (page != null) page.close();
        if (context != null) context.close();
    }

    @Nested
    @DisplayName("Page Navigation Tests")
    class NavigationTests {

        @Test
        @DisplayName("Should successfully load ICBC login page")
        void shouldLoadLoginPage() {
            assertDoesNotThrow(() -> {
                page.navigate(properties.loginUrl());
                page.waitForSelector("input[formcontrolname='drvrLastName']",
                        new Page.WaitForSelectorOptions().setTimeout(10000));
            });

            assertTrue(page.locator("input[formcontrolname='drvrLastName']").isVisible());
            assertTrue(page.locator("input[formcontrolname='licenceNumber']").isVisible());
            assertTrue(page.locator("input[formcontrolname='keyword']").isVisible());
            assertTrue(page.locator("#mat-checkbox-1-input").isVisible());
        }

        @Test
        @DisplayName("Should handle navigation timeout")
        void shouldHandleNavigationTimeout() {
            IcbcScraperProperties invalidProperties = new IcbcScraperProperties(
                    "https://invalid-url-that-does-not-exist.com",
                    5,
                    1,
                    properties.retryDelay()
            );

            IcbcScrapingServiceImpl serviceWithInvalidUrl = new IcbcScrapingServiceImpl(invalidProperties, validator);
            IcbcCredentials validCredentials = createValidCredentials();

            IcbcScrapingException exception = assertThrows(IcbcScrapingException.class, () -> {
                serviceWithInvalidUrl.login(page, validCredentials);
            });

            assertTrue(exception.getMessage().contains("timed out") ||
                    exception.getMessage().contains("load"));
        }

        @Test
        @DisplayName("Should respect timeout configuration")
        void shouldRespectTimeoutConfiguration() {
            IcbcCredentials validCredentials = createValidCredentials();
            page.navigate(properties.loginUrl());

            assertTimeout(java.time.Duration.ofSeconds(properties.timeoutSeconds() + 5), () -> {
                try {
                    page.waitForSelector("input[formcontrolname='drvrLastName']",
                            new Page.WaitForSelectorOptions().setTimeout(properties.timeoutSeconds() * 1000.0));
                } catch (Exception e) {
                    // Expected for actual login attempts
                }
            });
        }
    }

    @Nested
    @DisplayName("Form Interaction Tests")
    class FormInteractionTests {

        @Test
        @DisplayName("Should fill form fields correctly")
        void shouldFillFormFieldsCorrectly() {
            IcbcCredentials validCredentials = createValidCredentials();
            page.navigate(properties.loginUrl());
            page.waitForSelector("input[formcontrolname='drvrLastName']");

            page.fill("input[formcontrolname='drvrLastName']", validCredentials.lastName());
            page.fill("input[formcontrolname='licenceNumber']", validCredentials.driversLicenseNumber());
            page.fill("input[formcontrolname='keyword']", validCredentials.keyword());

            assertEquals(validCredentials.lastName(),
                    page.inputValue("input[formcontrolname='drvrLastName']"));
            assertEquals(validCredentials.driversLicenseNumber(),
                    page.inputValue("input[formcontrolname='licenceNumber']"));
            assertEquals(validCredentials.keyword(),
                    page.inputValue("input[formcontrolname='keyword']"));
        }
    }

    @Nested
    @DisplayName("Validation Tests")
    class ValidationTests {

        @Test
        @DisplayName("Should validate credentials and return violations for invalid data")
        void shouldValidateCredentials() {
            IcbcCredentials invalidLicense = new IcbcCredentials(
                    "ValidName",
                    "123",
                    "ValidKeyword"
            );

            Set<ConstraintViolation<IcbcCredentials>> violations = validator.validate(invalidLicense);

            assertFalse(violations.isEmpty());
            assertTrue(violations.stream()
                    .anyMatch(v -> v.getPropertyPath().toString().equals("driversLicenseNumber")));

            IcbcCredentials validCredentials = new IcbcCredentials(
                    "ValidName",
                    "12345678",
                    "ValidKeyword"
            );

            violations = validator.validate(validCredentials);
            assertTrue(violations.isEmpty(), "Valid credentials should have no violations");
        }

        @Test
        @DisplayName("Should throw exception for invalid credentials in service")
        void shouldThrowExceptionForInvalidCredentials() {
            IcbcCredentials invalidCredentials = new IcbcCredentials(
                    "ValidName",
                    "123",
                    "ValidKeyword"
            );

            Page mockPage = mock(Page.class);

            assertThrows(IcbcScrapingException.class, () -> {
                loginService.login(mockPage, invalidCredentials);
            });
        }
    }

    @Nested
    @DisplayName("Retry Behavior Tests")
    class RetryBehaviorTests {

        @Test
        @DisplayName("Should not retry on validation errors")
        void shouldNotRetryOnValidationErrors() {
            IcbcCredentials invalidCredentials = new IcbcCredentials(
                    "ValidName",
                    "123", // Invalid license number
                    "ValidKeyword"
            );

            long startTime = System.currentTimeMillis();

            IcbcScrapingException exception = assertThrows(IcbcScrapingException.class, () -> {
                loginService.login(page, invalidCredentials);
            });

            long endTime = System.currentTimeMillis();
            long duration = endTime - startTime;

            // Should fail immediately without retries (validation errors shouldn't be retried)
            assertTrue(duration < 500, "Validation errors should fail fast without retries");
            assertTrue(exception.getMessage().contains("Invalid credentials"));
        }
    }

    @Nested
    @DisplayName("Login Behavior Tests")
    class LoginBehaviorTests {

        @Test
        @DisplayName("Should handle invalid credentials gracefully")
        void shouldHandleInvalidCredentials() {
            IcbcCredentials invalidCredentials = new IcbcCredentials(
                    "TestUser",
                    "12345678",
                    "TestKeyword"
            );

            IcbcScrapingException exception = assertThrows(IcbcScrapingException.class, () -> {
                loginService.login(page, invalidCredentials);
            });

            assertNotNull(exception.getMessage());
            assertTrue(exception.getMessage().contains("Login") ||
                    exception.getMessage().contains("timed out") ||
                    exception.getMessage().contains("failed"));
        }

        @Test
        @Disabled("Only enable for manual testing with real credentials")
        @DisplayName("Manual test with real credentials")
        void manualTestWithRealCredentials() {
            IcbcCredentials realCredentials = new IcbcCredentials("", "", "");

            assertDoesNotThrow(() -> {
                loginService.login(page, realCredentials);
            });
        }
    }

    private IcbcCredentials createValidCredentials() {
        return new IcbcCredentials("LastName", "12345678", "TestKeyword");
    }
}
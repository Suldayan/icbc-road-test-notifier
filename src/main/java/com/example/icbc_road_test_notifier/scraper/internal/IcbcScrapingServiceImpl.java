package com.example.icbc_road_test_notifier.scraper.internal;

import com.example.icbc_road_test_notifier.scraper.IcbcScrapingService;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.TimeoutError;
import jakarta.validation.ConstraintViolation;

import jakarta.validation.Validator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;

import java.util.Set;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class IcbcScrapingServiceImpl implements IcbcScrapingService {
    private final IcbcScraperProperties properties;
    private final Validator validator;

    private static final class Selectors {
        static final String LAST_NAME_INPUT = "input[formcontrolname='drvrLastName']";
        static final String LICENSE_NUMBER_INPUT = "input[formcontrolname='licenceNumber']";
        static final String KEYWORD_INPUT = "input[formcontrolname='keyword']";
        static final String TERMS_CHECKBOX = "mat-checkbox";
        static final String TERMS_CHECKBOX_INPUT = "mat-checkbox input[type='checkbox']";
        static final String SIGN_IN_BUTTON = "button[type='submit']:has-text('Sign in')";
        static final String LOGIN_SUCCESS_INDICATOR = ".dashboard, .main-content, [data-test='login-success']";
        static final String ERROR_MESSAGE = ".error-message, .alert-danger, [data-test='login-error']";
    }

    private static final int CHECKBOX_TIMEOUT_MS = 5000;
    private static final int SUBMIT_TIMEOUT_MS = 10000;
    private static final int SUCCESS_VERIFICATION_TIMEOUT_MS = 5000;

    @Override
    @Retryable(
            retryFor = {IcbcScrapingException.class, TimeoutError.class},
            maxAttemptsExpression = "#{@icbcScraperProperties.maxRetryAttempts}",
            backoff = @Backoff(delayExpression = "#{@icbcScraperProperties.retryDelay.toMillis()}")
    )
    public void login(Page page, IcbcCredentials credentials) {
        validateCredentials(credentials);
        String maskedLastName = maskSensitiveData(credentials.lastName());
        log.info("Starting ICBC login process for user: {}", maskedLastName);

        try {
            navigateToLoginPage(page);
            fillCredentials(page, credentials);
            acceptTermsAndConditions(page);
            submitLogin(page);
            waitForLoginSuccess(page);

            log.info("ICBC login completed successfully for user: {}", maskedLastName);

        } catch (TimeoutError e) {
            String errorMsg = String.format("Login process timed out for user: %s", maskedLastName);
            log.error(errorMsg, e);
            throw new IcbcScrapingException(errorMsg, e);
        } catch (IcbcScrapingException e) {
            log.error("Login failed for user: {} - {}", maskedLastName, e.getMessage());
            throw e;
        } catch (Exception e) {
            String errorMsg = String.format("Login failed due to unexpected error for user: %s", maskedLastName);
            log.error(errorMsg, e);
            throw new IcbcScrapingException(errorMsg, e);
        }
    }

    private void validateCredentials(IcbcCredentials credentials) {
        Set<ConstraintViolation<IcbcCredentials>> violations = validator.validate(credentials);

        if (!violations.isEmpty()) {
            String errorMessages = violations.stream()
                    .map(ConstraintViolation::getMessage)
                    .collect(Collectors.joining(", "));

            log.error("Invalid credentials provided: {}", errorMessages);
            throw new IcbcScrapingException("Invalid credentials: " + errorMessages);
        }
    }

    private void navigateToLoginPage(Page page) {
        log.debug("Navigating to ICBC login page: {}", properties.loginUrl());

        try {
            page.navigate(properties.loginUrl());
            page.waitForSelector(Selectors.LAST_NAME_INPUT,
                    new Page.WaitForSelectorOptions()
                            .setTimeout(properties.timeoutSeconds() * 1000.0));

            log.debug("Login page loaded successfully");

        } catch (TimeoutError e) {
            throw new IcbcScrapingException("Login form did not load within expected time", e);
        }
    }

    private void fillCredentials(Page page, IcbcCredentials credentials) {
        log.debug("Filling login credentials");

        try {
            // Clear and fill last name
            page.fill(Selectors.LAST_NAME_INPUT, "");
            page.fill(Selectors.LAST_NAME_INPUT, credentials.lastName());
            log.debug("Last name field filled");

            // Clear and fill license number
            page.fill(Selectors.LICENSE_NUMBER_INPUT, "");
            page.fill(Selectors.LICENSE_NUMBER_INPUT, credentials.driversLicenseNumber());
            log.debug("License number field filled");

            // Clear and fill keyword
            page.fill(Selectors.KEYWORD_INPUT, "");
            page.fill(Selectors.KEYWORD_INPUT, credentials.keyword());
            log.debug("Keyword field filled");

            verifyFieldsFilled(page, credentials);

        } catch (Exception e) {
            throw new IcbcScrapingException("Failed to fill login credentials", e);
        }
    }

    private void verifyFieldsFilled(Page page, IcbcCredentials credentials) {
        String lastNameValue = page.inputValue(Selectors.LAST_NAME_INPUT);
        String licenseValue = page.inputValue(Selectors.LICENSE_NUMBER_INPUT);
        String keywordValue = page.inputValue(Selectors.KEYWORD_INPUT);

        if (!credentials.lastName().equals(lastNameValue) ||
                !credentials.driversLicenseNumber().equals(licenseValue) ||
                !credentials.keyword().equals(keywordValue)) {
            throw new IcbcScrapingException("Form fields were not filled correctly");
        }
    }

    private void acceptTermsAndConditions(Page page) {
        log.debug("Accepting terms and conditions");

        try {
            // Wait for the mat-checkbox container to be available
            page.waitForSelector(Selectors.TERMS_CHECKBOX,
                    new Page.WaitForSelectorOptions().setTimeout(CHECKBOX_TIMEOUT_MS));

            // Click the checkbox (Angular Material will handle the state)
            page.click(Selectors.TERMS_CHECKBOX);

            // Wait a moment for the state to update
            page.waitForTimeout(500);

            // Verify checkbox is checked using the hidden input
            if (!page.isChecked(Selectors.TERMS_CHECKBOX_INPUT)) {
                throw new IcbcScrapingException("Terms and conditions checkbox could not be checked");
            }

            log.debug("Terms and conditions accepted");

        } catch (TimeoutError e) {
            throw new IcbcScrapingException("Terms and conditions checkbox not found within expected time", e);
        } catch (Exception e) {
            throw new IcbcScrapingException("Failed to accept terms and conditions", e);
        }
    }

    private void submitLogin(Page page) {
        log.debug("Submitting login form");

        try {
            // Wait for the sign-in button to be available (doesn't take long to enable after all credentials are filled)
            page.waitForSelector(Selectors.SIGN_IN_BUTTON,
                    new Page.WaitForSelectorOptions().setTimeout(SUBMIT_TIMEOUT_MS));

            // Click the sign-in button
            page.click(Selectors.SIGN_IN_BUTTON);
            log.debug("Login form submitted");

        } catch (TimeoutError e) {
            throw new IcbcScrapingException("Sign in button not found within expected time", e);
        } catch (Exception e) {
            throw new IcbcScrapingException("Failed to submit login form", e);
        }
    }

    private void waitForLoginSuccess(Page page) {
        log.debug("Waiting for login success confirmation");

        try {
            // Wait for successful login indicators
            page.waitForSelector(Selectors.LOGIN_SUCCESS_INDICATOR,
                    new Page.WaitForSelectorOptions()
                            .setTimeout(properties.timeoutSeconds() * 1000.0));

            // Check URL change or specific elements
            page.waitForFunction(
                    "() => window.location.href.includes('dashboard') || document.querySelector('.main-content')",
                    new Page.WaitForFunctionOptions().setTimeout(SUCCESS_VERIFICATION_TIMEOUT_MS)
            );

            log.debug("Login success confirmed");

        } catch (TimeoutError e) {
            checkForLoginErrors(page);
            throw new IcbcScrapingException("Login success could not be confirmed within expected time", e);
        }
    }

    private void checkForLoginErrors(Page page) {
        if (page.locator(Selectors.ERROR_MESSAGE).count() > 0) {
            String errorText = page.locator(Selectors.ERROR_MESSAGE)
                    .first()
                    .textContent();
            throw new IcbcScrapingException("Login failed with error: " + errorText);
        }
    }

    private String maskSensitiveData(String data) {
        if (data == null || data.length() <= 3) {
            return "***";
        }
        return data.substring(0, 3) + "***";
    }
}
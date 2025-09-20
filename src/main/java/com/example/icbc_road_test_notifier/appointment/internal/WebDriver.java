package com.example.icbc_road_test_notifier.appointment.internal;

import com.microsoft.playwright.*;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class WebDriver {
    private static final int DEFAULT_TIMEOUT_MS = 30000;
    private static final int NAVIGATION_TIMEOUT_MS = 60000;

    public WebDriverSession createSession() {
        Playwright playwright = Playwright.create();
        Browser browser = playwright.chromium().launch(new BrowserType.LaunchOptions().setHeadless(false));

        Browser.NewContextOptions options = new Browser.NewContextOptions()
                .setViewportSize(1920, 1080)
                .setUserAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36");

        BrowserContext context = browser.newContext(options);
        context.setDefaultTimeout(DEFAULT_TIMEOUT_MS);
        context.setDefaultNavigationTimeout(NAVIGATION_TIMEOUT_MS);

        Page page = context.newPage();

        return new WebDriverSession(playwright, browser, context, page);
    }

    // Session wrapper for proper resource management
    @RequiredArgsConstructor
    public static class WebDriverSession implements AutoCloseable {
        private final Playwright playwright;
        private final Browser browser;
        private final BrowserContext context;
        @Getter private final Page page;

        @Override
        public void close() {
            try {
                if (context != null) context.close();
                if (browser != null) browser.close();
                if (playwright != null) playwright.close();
            } catch (Exception e) {
                log.warn("Error closing WebDriver session: {}", e.getMessage());
            }
        }
    }
}
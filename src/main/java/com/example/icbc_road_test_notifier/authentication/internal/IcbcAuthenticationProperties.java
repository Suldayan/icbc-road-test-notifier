package com.example.icbc_road_test_notifier.authentication.internal;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Min;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@ConfigurationProperties(prefix = "icbc.scraper")
public record IcbcAuthenticationProperties(
        @NotBlank(message = "Login URL cannot be blank")
        String loginUrl,

        @Positive(message = "Timeout seconds must be positive")
        @Min(value = 5, message = "Timeout should be at least 5 seconds")
        int timeoutSeconds,

        @Positive(message = "Max retry attempts must be positive")
        @Min(value = 1, message = "At least 1 retry attempt is required")
        int maxRetryAttempts,

        Duration retryDelay
) {
    public IcbcAuthenticationProperties {
        if (loginUrl == null || loginUrl.isBlank()) {
            loginUrl = "https://onlinebusiness.icbc.com/webdeas-ui/login;type=driver";
        }
        if (retryDelay == null) {
            retryDelay = Duration.ofSeconds(2);
        }
        if (retryDelay.isNegative() || retryDelay.isZero()) {
            throw new IllegalArgumentException("Retry delay must be positive");
        }
        if (retryDelay.toSeconds() > 30) {
            throw new IllegalArgumentException("Retry delay should not exceed 30 seconds");
        }
    }
}
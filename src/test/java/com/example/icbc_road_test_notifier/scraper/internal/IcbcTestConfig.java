package com.example.icbc_road_test_notifier.scraper.internal;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

@TestConfiguration
public class IcbcTestConfig {

    @Bean
    @Primary
    public IcbcScraperProperties testProperties() {
        return new IcbcScraperProperties(
                "https://onlinebusiness.icbc.com/webdeas-ui/login;type=driver",
                15, // Shorter timeout for tests
                1,  // Single retry for faster tests
                java.time.Duration.ofSeconds(1)
        );
    }
}
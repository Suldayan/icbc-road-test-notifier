package com.example.icbc_road_test_notifier.scraper.internal;

public class IcbcScrapingException extends RuntimeException {
    public IcbcScrapingException(String message) {
        super(message);
    }

    public IcbcScrapingException(String message, Throwable cause) {
        super(message, cause);
    }
}


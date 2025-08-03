package com.example.icbc_road_test_notifier.authentication.internal;

public class IcbcAuthenticationException extends RuntimeException {
    public IcbcAuthenticationException(String message) {
        super(message);
    }

    public IcbcAuthenticationException(String message, Throwable cause) {
        super(message, cause);
    }
}


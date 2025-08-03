package com.example.icbc_road_test_notifier.authentication;

import com.example.icbc_road_test_notifier.authentication.internal.IcbcCredentials;
import com.example.icbc_road_test_notifier.authentication.internal.IcbcAuthenticationException;
import com.microsoft.playwright.Page;

/**
 * Service for authenticating with ICBC web portal.
 */
public interface AuthenticationService {
    /**
     * Authenticates with ICBC using the provided credentials.
     *
     * @param page the Playwright page instance
     * @param credentials the ICBC login credentials
     * @throws IcbcAuthenticationException if login fails
     */
    void login(Page page, IcbcCredentials credentials) throws IcbcAuthenticationException;
}
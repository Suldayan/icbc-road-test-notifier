package com.example.icbc_road_test_notifier.scraper;

import com.example.icbc_road_test_notifier.scraper.internal.IcbcCredentials;
import com.example.icbc_road_test_notifier.scraper.internal.IcbcScrapingException;
import com.microsoft.playwright.Page;

/**
 * Service for authenticating with ICBC web portal.
 */
public interface IcbcScrapingService {
    /**
     * Authenticates with ICBC using the provided credentials.
     *
     * @param page the Playwright page instance
     * @param credentials the ICBC login credentials
     * @throws IcbcScrapingException if login fails
     */
    void login(Page page, IcbcCredentials credentials) throws IcbcScrapingException;
}
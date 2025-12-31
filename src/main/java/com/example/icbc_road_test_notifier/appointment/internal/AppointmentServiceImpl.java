package com.example.icbc_road_test_notifier.appointment.internal;

import com.example.icbc_road_test_notifier.appointment.*;
import com.example.icbc_road_test_notifier.navigation.NavigationService;
import com.example.icbc_road_test_notifier.shared.DaysOfTheWeek;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;

@Service
@Slf4j
@RequiredArgsConstructor
public class AppointmentServiceImpl implements AppointmentService {
    private final @NonNull WebDriver webDriver;
    private final @NonNull NavigationService navigationService;
    private final @NonNull AppointmentSearchService searchService;
    private final @NonNull ApplicationEventPublisher events;

    @Override
    public void authenticateAndSearchAppointments(
            String lastName,
            String licenseNumber,
            String keyword,
            String preferredLocation,
            Set<DaysOfTheWeek> preferredDays,
            TimePreference timePreference,
            DateRangePreference dateRangePreference) {

        validateInputs(lastName, licenseNumber, keyword);
        log.info("Starting ICBC appointment search for user: {} at location: {} with time preference: {} and date range: {}",
                lastName, preferredLocation,
                timePreference != null ? timePreference.getDisplayName() : "ANY",
                dateRangePreference != null ? "custom range" : "no restriction");

        try (WebDriver.WebDriverSession session = webDriver.createSession()) {
            navigationService.authenticate(session.getPage(), lastName, licenseNumber, keyword);
            navigationService.navigateToAppointmentSection(session.getPage());
            searchService.configureAndSearch(session.getPage(), preferredLocation, preferredDays, timePreference, dateRangePreference);

            AppointmentResults results = searchService.getLastResults();
            if (results.hasAvailableAppointments()) {
                publishAppointmentFoundEvent(results, timePreference, dateRangePreference);
            } else {
                log.info("No appointments found matching the specified criteria");
            }
        } catch (Exception e) {
            log.error("Appointment search failed: {}", e.getMessage());
            throw new RuntimeException("Appointment search failed", e);
        }
    }

    @Transactional
    public void publishAppointmentFoundEvent(AppointmentResults results, TimePreference timePreference, DateRangePreference dateRangePreference) {
        StringBuilder messageBuilder = new StringBuilder();
        messageBuilder.append(String.format("Available appointments found: %d dates with %d total time slots. %s",
                results.getDateCount(),
                results.getTotalSlots(),
                results.getSummary()));

        // Add filtering information to the message
        if (timePreference != null || dateRangePreference != null) {
            messageBuilder.append(" [Filtered by: ");
            if (timePreference != null) {
                messageBuilder.append("time=").append(timePreference.getDisplayName());
            }
            if (dateRangePreference != null) {
                if (timePreference != null) messageBuilder.append(", ");
                messageBuilder.append("date range");
            }
            messageBuilder.append("]");
        }

        String eventMessage = messageBuilder.toString();
        events.publishEvent(new AppointmentFound(eventMessage));

        log.info("Published appointment found event: {} dates, {} slots{}",
                results.getDateCount(),
                results.getTotalSlots(),
                (timePreference != null || dateRangePreference != null) ? " (filtered)" : "");
    }

    private void validateInputs(String lastName, String licenseNumber, String keyword) {
        if (lastName == null || lastName.trim().isEmpty()) {
            throw new IllegalArgumentException("Last name is required");
        }
        if (licenseNumber == null || licenseNumber.trim().isEmpty()) {
            throw new IllegalArgumentException("License number is required");
        }
        if (keyword == null || keyword.trim().isEmpty()) {
            throw new IllegalArgumentException("Keyword is required");
        }
    }
}
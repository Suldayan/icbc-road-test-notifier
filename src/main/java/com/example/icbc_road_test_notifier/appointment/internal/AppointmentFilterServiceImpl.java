package com.example.icbc_road_test_notifier.appointment.internal;

import com.example.icbc_road_test_notifier.appointment.AppointmentFilterService;
import com.example.icbc_road_test_notifier.appointment.AppointmentResults;
import com.example.icbc_road_test_notifier.shared.DateRangePreference;
import com.example.icbc_road_test_notifier.shared.TimePreference;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
@Slf4j
public class AppointmentFilterServiceImpl implements AppointmentFilterService {
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("MMMM d, yyyy", Locale.ENGLISH);
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("h:mm a", Locale.ENGLISH);
    private static final int DEFAULT_SLOTS_PER_DATE = 8;

    @Override
    public AppointmentResults filterByPreferences(
            AppointmentResults rawResults,
            TimePreference timePreference,
            DateRangePreference dateRangePreference) {
        if (rawResults.isEmpty()) {
            return rawResults;
        }

        log.debug("Filtering {} dates with {} slots using time preference: {}",
                rawResults.getDateCount(), rawResults.getTotalSlots(),
                timePreference != null ? timePreference.getDisplayName() : "ANY");

        Map<String, List<String>> dateToTimeSlots = buildDateToSlotsMapping(rawResults);
        Map<String, List<String>> filteredMapping = filterDateSlotMapping(dateToTimeSlots, timePreference, dateRangePreference);

        return buildFilteredResults(filteredMapping, rawResults);
    }

    private Map<String, List<String>> buildDateToSlotsMapping(AppointmentResults results) {
        // Use existing mapping if available, otherwise fall back to grouping logic for legacy support
        Map<String, List<String>> existingMapping = results.getDateToSlotsMap();
        if (!existingMapping.isEmpty()) {
            return existingMapping;
        }

        return groupTimeSlotsByDate(results.getDates(), results.getTimeSlots());
    }

    // Fallback heuristic: estimates slot distribution when proper date-to-slot mapping isn't available
    private Map<String, List<String>> groupTimeSlotsByDate(List<String> dates, List<String> timeSlots) {
        Map<String, List<String>> grouped = new LinkedHashMap<>();
        int timeSlotIndex = 0;
        int slotsPerDate = Math.max(1, timeSlots.size() / Math.max(1, dates.size()));

        log.debug("Grouping {} slots across {} dates (estimated {} slots per date)",
                timeSlots.size(), dates.size(), slotsPerDate);

        for (String date : dates) {
            List<String> slotsForThisDate = new ArrayList<>();
            int slotsToTake = Math.min(DEFAULT_SLOTS_PER_DATE, timeSlots.size() - timeSlotIndex);

            for (int i = 0; i < slotsToTake && timeSlotIndex < timeSlots.size(); i++) {
                slotsForThisDate.add(timeSlots.get(timeSlotIndex++));
            }

            grouped.put(date, slotsForThisDate);
        }

        return grouped;
    }

    private Map<String, List<String>> filterDateSlotMapping(Map<String, List<String>> dateToTimeSlots,
                                                            TimePreference timePreference,
                                                            DateRangePreference dateRangePreference) {
        Map<String, List<String>> filtered = new LinkedHashMap<>();

        for (Map.Entry<String, List<String>> entry : dateToTimeSlots.entrySet()) {
            String dateString = entry.getKey();
            List<String> timeSlotsForDate = entry.getValue();

            if (!isDateWithinRange(dateString, dateRangePreference)) {
                log.debug("Date {} is outside preferred range, skipping", dateString);
                continue;
            }

            List<String> matchingTimeSlots = filterTimeSlotsByPreference(timeSlotsForDate, timePreference);

            if (!matchingTimeSlots.isEmpty()) {
                filtered.put(dateString, matchingTimeSlots);
            }
        }

        return filtered;
    }

    private boolean isDateWithinRange(String dateString, DateRangePreference dateRangePreference) {
        if (dateRangePreference == null) {
            return true;
        }

        LocalDate appointmentDate = parseDateString(dateString);
        if (appointmentDate == null) {
            log.warn("Could not parse date string: {}", dateString);
            return false;
        }

        return dateRangePreference.isWithinRange(appointmentDate);
    }

    private List<String> filterTimeSlotsByPreference(List<String> timeSlots, TimePreference timePreference) {
        if (timePreference == null || timePreference == TimePreference.ANY) {
            return new ArrayList<>(timeSlots);
        }

        List<String> filtered = new ArrayList<>();

        for (String timeSlot : timeSlots) {
            LocalTime appointmentTime = parseTimeString(timeSlot);
            if (appointmentTime != null && isTimeInPreferredRange(appointmentTime, timePreference)) {
                filtered.add(timeSlot);
            }
        }

        log.debug("Filtered {} time slots to {} matching preference: {}",
                timeSlots.size(), filtered.size(), timePreference.getDisplayName());

        return filtered;
    }

    // Parse date format "Tuesday, January 6th, 2026" - removes day prefix and ordinal suffixes
    private LocalDate parseDateString(String dateString) {
        try {
            String cleaned = dateString
                    .replaceFirst("^\\w+,\\s*", "") // Remove "Tuesday, "
                    .replaceAll("(\\d+)(st|nd|rd|th)", "$1"); // Remove ordinal suffixes

            return LocalDate.parse(cleaned, DATE_FORMATTER);

        } catch (Exception e) {
            log.error("Failed to parse date string '{}': {}", dateString, e.getMessage());
            return null;
        }
    }

    private LocalTime parseTimeString(String timeString) {
        try {
            return LocalTime.parse(timeString.trim(), TIME_FORMATTER);
        } catch (Exception e) {
            log.warn("Could not parse time string '{}': {}", timeString, e.getMessage());
            return null;
        }
    }

    private boolean isTimeInPreferredRange(LocalTime appointmentTime, TimePreference timePreference) {
        int hour = appointmentTime.getHour();
        return hour >= timePreference.getStartHour() && hour < timePreference.getEndHour();
    }

    private AppointmentResults buildFilteredResults(Map<String, List<String>> filteredMapping,
                                                    AppointmentResults originalResults) {
        if (filteredMapping.isEmpty()) {
            return AppointmentResults.empty();
        }

        List<String> filteredDates = new ArrayList<>(filteredMapping.keySet());
        List<String> filteredTimeSlots = filteredMapping.values().stream()
                .flatMap(List::stream)
                .collect(ArrayList::new, ArrayList::add, ArrayList::addAll);

        log.info("Filtered results: {} dates with {} time slots (from {} original dates with {} slots)",
                filteredDates.size(), filteredTimeSlots.size(),
                originalResults.getDateCount(), originalResults.getTotalSlots());

        return new AppointmentResults(filteredDates, filteredTimeSlots, filteredMapping);
    }
}
package com.example.icbc_road_test_notifier.appointment;

import java.util.*;

public class AppointmentResults {
    private final List<String> dates;
    private final List<String> timeSlots;
    private final Map<String, List<String>> dateToSlotsMap;

    public AppointmentResults(List<String> dates, List<String> timeSlots) {
        this(dates, timeSlots, null);
    }

    public AppointmentResults(List<String> dates, List<String> timeSlots, Map<String, List<String>> dateToSlotsMap) {
        this.dates = new ArrayList<>(dates);
        this.timeSlots = new ArrayList<>(timeSlots);
        this.dateToSlotsMap = dateToSlotsMap != null ? new LinkedHashMap<>(dateToSlotsMap) : new LinkedHashMap<>();
    }

    public List<String> getDates() {
        return new ArrayList<>(dates);
    }

    public List<String> getTimeSlots() {
        return new ArrayList<>(timeSlots);
    }

    public Map<String, List<String>> getDateToSlotsMap() {
        return new LinkedHashMap<>(dateToSlotsMap);
    }

    public boolean hasAvailableAppointments() {
        return !dates.isEmpty() && !timeSlots.isEmpty();
    }

    public boolean isEmpty() {
        return dates.isEmpty() || timeSlots.isEmpty();
    }

    public int getDateCount() {
        return dates.size();
    }

    public int getTotalSlots() {
        return timeSlots.size();
    }

    public String getSummary() {
        if (isEmpty()) {
            return "No appointments available";
        }

        StringBuilder summary = new StringBuilder();
        if (!dateToSlotsMap.isEmpty()) {
            for (Map.Entry<String, List<String>> entry : dateToSlotsMap.entrySet()) {
                if (!summary.isEmpty()) {
                    summary.append("; ");
                }
                summary.append(entry.getKey())
                        .append(" (")
                        .append(entry.getValue().size())
                        .append(" slots)");
            }
        } else {
            summary.append(getDateCount()).append(" dates, ").append(getTotalSlots()).append(" total slots");
        }

        return summary.toString();
    }

    public static AppointmentResults empty() {
        return new AppointmentResults(Collections.emptyList(), Collections.emptyList());
    }
}
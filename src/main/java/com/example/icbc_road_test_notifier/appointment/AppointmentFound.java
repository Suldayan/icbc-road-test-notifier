package com.example.icbc_road_test_notifier.appointment;

import org.jmolecules.event.types.DomainEvent;

/**
 * Event published when available appointments are found.
 *
 * @param message Details about the found appointments
 */
public record AppointmentFound(String message) implements DomainEvent {
}
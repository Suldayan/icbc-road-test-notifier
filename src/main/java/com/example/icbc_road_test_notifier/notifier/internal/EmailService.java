package com.example.icbc_road_test_notifier.notifier.internal;

import com.example.icbc_road_test_notifier.appointment.AppointmentFound;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.modulith.events.ApplicationModuleListener;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@Slf4j
@RequiredArgsConstructor
public class EmailService {
    private final JavaMailSender emailSender;
    private AppointmentFound appointment;
    private static final String SUBJECT = "ICBC N-Test Appointment";

    @ApplicationModuleListener
    public void appointmentListener(@NonNull AppointmentFound appointment) {
        log.info("Appointment event has been received at: {}", LocalDateTime.now());
        this.appointment = appointment;
    }

    public void sendSimpleMessage(String to) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom("noreply@baeldung.com");
        message.setTo(to);
        message.setSubject(SUBJECT);
        message.setText(appointment.message());
        emailSender.send(message);
        log.info("Email message has been sent to: {} at {}", to, LocalDateTime.now());
    }
}
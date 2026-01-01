package com.example.icbc_road_test_notifier.notifier.internal;

import com.example.icbc_road_test_notifier.appointment.AppointmentFound;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.modulith.events.ApplicationModuleListener;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class EmailService {
    private final JavaMailSender emailSender;
    private final String senderEmail;
    private final String recipientEmail;

    private static final String SUBJECT = "ICBC N-Test Appointment Found!";

    public EmailService(
            JavaMailSender emailSender,
            @Value("${spring.mail.username}") String senderEmail,
            @Value("${icbc.notification-recipient:${spring.mail.username}}") String recipientEmail
    ) {
        this.emailSender = emailSender;
        this.senderEmail = senderEmail;
        this.recipientEmail = recipientEmail;
    }

    @ApplicationModuleListener
    public void appointmentListener(@NonNull AppointmentFound appointment) {
        log.info("Appointment event received. Notifying: {}", recipientEmail);
        sendSimpleMessage(recipientEmail, appointment.message());
    }

    private void sendSimpleMessage(String to, String content) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(senderEmail);
        message.setTo(to);
        message.setSubject(SUBJECT);
        message.setText(content);
        emailSender.send(message);
    }
}
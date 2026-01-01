package com.example.icbc_road_test_notifier.notifier.internal;

import com.example.icbc_road_test_notifier.appointment.AppointmentFound;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class EmailServiceUnitTest {

    @Mock
    private JavaMailSender emailSender;

    @InjectMocks
    private EmailService emailService;

    @Test
    void shouldSendEmail_WhenAppointmentEventIsReceived() {
        String expectedBody = "Available appointments found: 1 dates";
        AppointmentFound mockEvent = new AppointmentFound(expectedBody);

        emailService.appointmentListener(mockEvent);

        ArgumentCaptor<SimpleMailMessage> messageCaptor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(emailSender).send(messageCaptor.capture());

        SimpleMailMessage sentMessage = messageCaptor.getValue();
        assertEquals(expectedBody, sentMessage.getText());
        assertEquals("ICBC N-Test Appointment Found!", sentMessage.getSubject());
    }
}
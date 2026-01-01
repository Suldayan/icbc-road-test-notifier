package com.example.icbc_road_test_notifier.appointment.internal;

import com.example.icbc_road_test_notifier.appointment.AppointmentService;
import com.example.icbc_road_test_notifier.shared.IcbcConfig;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("api/v1/appointments")
public class AppointmentController {
    private final AppointmentService appointmentService;
    private final IcbcConfig icbcConfig;

    @PostMapping("/check")
    public void checkAppointments() {
        appointmentService.authenticateAndSearchAppointments(icbcConfig);
    }
}

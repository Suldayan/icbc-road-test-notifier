package com.example.icbc_road_test_notifier.scraper.internal;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record IcbcCredentials(
        @NotBlank(message = "Last name is required")
        @Size(max = 35, message = "Last name cannot exceed 35 characters")
        String lastName,

        @NotBlank(message = "Driver's license number is required")
        @Pattern(regexp = "\\d{8}", message = "Driver's license number must be exactly 8 digits")
        String driversLicenseNumber,

        @NotBlank(message = "Keyword is required")
        @Size(max = 22, message = "Keyword cannot exceed 22 characters")
        String keyword
) {}
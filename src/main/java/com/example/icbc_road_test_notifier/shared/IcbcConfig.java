package com.example.icbc_road_test_notifier.shared;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.util.Set;

@Validated
@ConfigurationProperties(prefix = "icbc")
public record IcbcConfig(
        @NotBlank
        String lastName,

        @NotBlank
        @Pattern(regexp = "^[0-9]{7}$", message = "License number must be 7 digits")
        String licenseNumber,

        @NotBlank
        String keyword,

        @NotBlank
        String preferredLocation,

        @NotEmpty
        Set<DaysOfTheWeek> preferredDays,

        @NotNull
        TimePreference timePreference,

        @NotNull
        DateRangePreference dateRangePreference

) {}


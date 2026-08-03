package com.clanmanager.clanmanager.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;

public record WatchReportRequest(
        @NotBlank @Size(max = 100) String targetId,
        @NotBlank @Pattern(regexp = "^\\d+/\\d+$") String oldValue,
        @NotBlank @Pattern(regexp = "^\\d+/\\d+$") String newValue,
        @NotNull LocalDateTime changedAt
) {
}

package com.clanmanager.clanmanager.controller;

import com.clanmanager.clanmanager.dto.WatchReportRequest;
import com.clanmanager.clanmanager.entity.WatchLog;
import com.clanmanager.clanmanager.service.WatchReportService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

@RestController
@RequestMapping("/api/watch")
@RequiredArgsConstructor
public class WatchController {

    private final WatchReportService watchReportService;

    @Value("${watch.api-key:}")
    private String apiKey;

    @PostMapping("/report")
    @ResponseStatus(HttpStatus.CREATED)
    public WatchLog report(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @Valid @RequestBody WatchReportRequest request
    ) {
        validateApiKey(authorization);
        return watchReportService.report(request);
    }

    private void validateApiKey(String authorization) {
        String provided = authorization != null && authorization.startsWith("Bearer ")
                ? authorization.substring(7).trim()
                : "";
        boolean configured = apiKey != null && !apiKey.isBlank();
        boolean matches = configured && MessageDigest.isEqual(
                apiKey.getBytes(StandardCharsets.UTF_8),
                provided.getBytes(StandardCharsets.UTF_8)
        );
        if (!matches) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid watch API key");
        }
    }
}

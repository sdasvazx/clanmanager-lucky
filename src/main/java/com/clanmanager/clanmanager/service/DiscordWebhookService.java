package com.clanmanager.clanmanager.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;

import java.time.Duration;
import java.time.format.DateTimeFormatter;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class DiscordWebhookService {

    private static final DateTimeFormatter MESSAGE_TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final RestTemplateBuilder restTemplateBuilder;

    @Value("${discord.webhook-url:}")
    private String webhookUrl;

    public void send(String targetId, String oldValue, String newValue, java.time.LocalDateTime changedAt) {
        sendMessage("[%s] %s → %s (%s)".formatted(targetId, oldValue, newValue, changedAt.format(MESSAGE_TIME)));
    }

    public void sendMessage(String content) {
        if (webhookUrl == null || webhookUrl.isBlank()) {
            log.warn("Discord webhook URL is not configured; watch alert was stored without Discord delivery.");
            return;
        }
        try {
            restTemplateBuilder
                    .connectTimeout(Duration.ofSeconds(5))
                    .readTimeout(Duration.ofSeconds(10))
                    .build()
                    .postForEntity(webhookUrl, Map.of("content", content), Void.class);
        } catch (RestClientException exception) {
            // A temporary Discord outage must not discard an already recognized game value.
            log.error("Failed to deliver Discord webhook message", exception);
        }
    }
}

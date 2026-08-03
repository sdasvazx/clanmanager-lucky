package com.clanmanager.clanmanager.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.ZoneId;

@Component
@RequiredArgsConstructor
@Slf4j
public class BossScheduleDiscordNotifier {

    private static final ZoneId KOREA = ZoneId.of("Asia/Seoul");

    private final DiscordWebhookService discordWebhookService;

    @Scheduled(cron = "0 55 11,12,16,19,20 * * *", zone = "Asia/Seoul")
    public void notifyFiveMinutesBefore() {
        int scheduleHour = LocalDateTime.now(KOREA).plusMinutes(5).getHour();
        String message = messageForHour(scheduleHour);
        log.info("Sending scheduled Discord reminder for {}:00", scheduleHour);
        discordWebhookService.sendMessage(message);
    }

    static String messageForHour(int scheduleHour) {
        String eventName = switch (scheduleHour) {
            case 12, 20 -> "월드보스";
            case 13, 17, 21 -> "게헨나";
            default -> "보스 일정";
        };
        return "⏰ %d시 %s 5분 전입니다. 참여 준비해 주세요!".formatted(scheduleHour, eventName);
    }
}

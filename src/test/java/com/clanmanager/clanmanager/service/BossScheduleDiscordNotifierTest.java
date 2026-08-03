package com.clanmanager.clanmanager.service;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class BossScheduleDiscordNotifierTest {

    @Test
    void scheduledReminderSendsFiveMinuteWarning() {
        DiscordWebhookService webhookService = mock(DiscordWebhookService.class);
        BossScheduleDiscordNotifier notifier = new BossScheduleDiscordNotifier(webhookService);

        notifier.notifyFiveMinutesBefore();

        verify(webhookService).sendMessage(contains("5분 전"));
    }

    @Test
    void eventNameMatchesEachScheduleHour() {
        assertThat(BossScheduleDiscordNotifier.messageForHour(12)).contains("12시 월드보스 5분 전");
        assertThat(BossScheduleDiscordNotifier.messageForHour(13)).contains("13시 게헨나 5분 전");
        assertThat(BossScheduleDiscordNotifier.messageForHour(17)).contains("17시 게헨나 5분 전");
        assertThat(BossScheduleDiscordNotifier.messageForHour(20)).contains("20시 월드보스 5분 전");
        assertThat(BossScheduleDiscordNotifier.messageForHour(21)).contains("21시 게헨나 5분 전");
    }
}

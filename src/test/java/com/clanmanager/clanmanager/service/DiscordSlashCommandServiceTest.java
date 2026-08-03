package com.clanmanager.clanmanager.service;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class DiscordSlashCommandServiceTest {

    @Test
    void scheduleContainsAllFourDailyBosses() {
        assertThat(DiscordSlashCommandService.scheduleMessage())
                .contains("12:00 월드보스", "13:00 게헨나", "20:00 월드보스", "21:00 게헨나");
    }

    @Test
    void nextBossChoosesTheNearestFutureSchedule() {
        String message = DiscordSlashCommandService.nextBossMessage(LocalDateTime.of(2026, 8, 3, 12, 30));

        assertThat(message).contains("게헨나", "08월 03일 13:00", "0시간 30분 후");
    }

    @Test
    void nextBossRollsOverToTomorrowAfterLastSchedule() {
        String message = DiscordSlashCommandService.nextBossMessage(LocalDateTime.of(2026, 8, 3, 21, 30));

        assertThat(message).contains("월드보스", "08월 04일 12:00");
    }
}

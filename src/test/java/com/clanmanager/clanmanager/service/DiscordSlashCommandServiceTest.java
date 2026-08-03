package com.clanmanager.clanmanager.service;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Random;

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

    @Test
    void drawSelectsRequestedNumberWithoutDuplicates() {
        List<String> members = List.of("인원1", "인원2", "인원3", "인원4");

        List<String> selected = DiscordSlashCommandService.drawMembers(members, 2, new Random(1));

        assertThat(selected).hasSize(2).doesNotHaveDuplicates().allMatch(members::contains);
    }

    @Test
    void helpListsNewCommands() {
        assertThat(DiscordSlashCommandService.helpMessage())
                .contains("/운세", "/뽑기", "/사이트", "/도움말", "/공지", "/공지등록");
    }
}

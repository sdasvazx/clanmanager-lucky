package com.clanmanager.clanmanager.service;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

class KoreanFortuneServiceTest {

    private final KoreanFortuneService service = new KoreanFortuneService();

    @Test
    void sameUserAndDateAlwaysReturnsSameFourCharacterFortune() {
        LocalDate date = LocalDate.of(2026, 8, 3);

        assertThat(service.getFortune(date, "1001"))
                .isEqualTo(service.getFortune(date, "1001"))
                .contains("**", "뜻:", "오늘의 흐름:");
    }

    @Test
    void differentUsersReceiveDifferentFortunesOnSameDate() {
        LocalDate date = LocalDate.of(2026, 8, 3);

        assertThat(service.getFortune(date, "1001"))
                .isNotEqualTo(service.getFortune(date, "1002"));
    }
}

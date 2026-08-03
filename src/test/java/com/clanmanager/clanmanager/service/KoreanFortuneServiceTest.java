package com.clanmanager.clanmanager.service;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

class KoreanFortuneServiceTest {

    private final KoreanFortuneService service = new KoreanFortuneService();

    @Test
    void sameDateAlwaysReturnsSameFourCharacterFortune() {
        LocalDate date = LocalDate.of(2026, 8, 3);

        assertThat(service.getFortune(date))
                .isEqualTo(service.getFortune(date))
                .contains("**", "뜻:", "오늘의 흐름:");
    }

    @Test
    void nextDateMovesToAnotherFortune() {
        assertThat(service.getFortune(LocalDate.of(2026, 8, 3)))
                .isNotEqualTo(service.getFortune(LocalDate.of(2026, 8, 4)));
    }
}

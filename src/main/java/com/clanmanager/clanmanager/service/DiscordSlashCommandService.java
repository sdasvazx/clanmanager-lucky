package com.clanmanager.clanmanager.service;

import com.clanmanager.clanmanager.entity.WatchLog;
import com.clanmanager.clanmanager.repository.WatchLogRepository;
import lombok.RequiredArgsConstructor;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Component
@RequiredArgsConstructor
public class DiscordSlashCommandService extends ListenerAdapter {

    private static final ZoneId KOREA = ZoneId.of("Asia/Seoul");
    private static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern("MM월 dd일 HH:mm");
    private static final List<BossTime> DAILY_BOSSES = List.of(
            new BossTime(LocalTime.of(12, 0), "월드보스"),
            new BossTime(LocalTime.of(13, 0), "게헨나"),
            new BossTime(LocalTime.of(20, 0), "월드보스"),
            new BossTime(LocalTime.of(21, 0), "게헨나")
    );

    private final WatchLogRepository watchLogRepository;

    @Override
    public void onSlashCommandInteraction(SlashCommandInteractionEvent event) {
        switch (event.getName()) {
            case "보스일정" -> event.reply(scheduleMessage()).queue();
            case "다음보스" -> event.reply(nextBossMessage(LocalDateTime.now(KOREA))).queue();
            case "감시상태" -> event.reply(watchStatusMessage()).setEphemeral(true).queue();
            case "알림테스트" -> event.reply("운좋은 알림봇이 정상 작동 중입니다.").setEphemeral(true).queue();
            default -> event.reply("지원하지 않는 명령어입니다.").setEphemeral(true).queue();
        }
    }

    static String scheduleMessage() {
        return "**매일 보스 일정**\n12:00 월드보스\n13:00 게헨나\n20:00 월드보스\n21:00 게헨나\n각 일정 5분 전에 자동 알림이 전송됩니다.";
    }

    static String nextBossMessage(LocalDateTime now) {
        for (BossTime boss : DAILY_BOSSES) {
            LocalDateTime candidate = now.toLocalDate().atTime(boss.time());
            if (candidate.isAfter(now)) {
                return formatNextBoss(now, candidate, boss.name());
            }
        }
        BossTime first = DAILY_BOSSES.get(0);
        LocalDateTime tomorrow = now.toLocalDate().plusDays(1).atTime(first.time());
        return formatNextBoss(now, tomorrow, first.name());
    }

    private static String formatNextBoss(LocalDateTime now, LocalDateTime at, String name) {
        Duration remaining = Duration.between(now, at);
        long hours = remaining.toHours();
        long minutes = remaining.minusHours(hours).toMinutes();
        return "다음 일정: **%s** · %s (약 %d시간 %d분 후)"
                .formatted(name, at.format(DATE_TIME), hours, minutes);
    }

    private String watchStatusMessage() {
        return watchLogRepository.findTopByOrderByChangedAtDesc()
                .map(this::formatWatchStatus)
                .orElse("아직 화면 숫자 감시 기록이 없습니다. PC 감시 프로그램을 실행해 주세요.");
    }

    private String formatWatchStatus(WatchLog log) {
        return "최근 감지: **%s** · %s → %s (%s)"
                .formatted(log.getTargetId(), log.getOldValue(), log.getNewValue(), log.getChangedAt().format(DATE_TIME));
    }

    private record BossTime(LocalTime time, String name) {
    }
}

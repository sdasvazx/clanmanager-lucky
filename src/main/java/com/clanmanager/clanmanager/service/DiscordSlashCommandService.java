package com.clanmanager.clanmanager.service;

import com.clanmanager.clanmanager.entity.WatchLog;
import com.clanmanager.clanmanager.repository.WatchLogRepository;
import lombok.RequiredArgsConstructor;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import org.springframework.stereotype.Component;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Component
@RequiredArgsConstructor
public class DiscordSlashCommandService extends ListenerAdapter {

    private static final ZoneId KOREA = ZoneId.of("Asia/Seoul");
    private static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern("MM월 dd일 HH:mm");
    private static final String CLAN_SITE = "https://clanmanager-lucky.vercel.app";
    private static final List<BossTime> DAILY_BOSSES = List.of(
            new BossTime(LocalTime.of(12, 0), "월드보스"),
            new BossTime(LocalTime.of(13, 0), "게헨나"),
            new BossTime(LocalTime.of(20, 0), "월드보스"),
            new BossTime(LocalTime.of(21, 0), "게헨나")
    );

    private final WatchLogRepository watchLogRepository;
    private final KoreanFortuneService koreanFortuneService;
    private final DiscordBotNoticeService discordBotNoticeService;
    private final SecureRandom secureRandom = new SecureRandom();

    @Override
    public void onSlashCommandInteraction(SlashCommandInteractionEvent event) {
        switch (event.getName()) {
            case "보스일정" -> event.reply(scheduleMessage()).queue();
            case "다음보스" -> event.reply(nextBossMessage(LocalDateTime.now(KOREA))).queue();
            case "감시상태" -> event.reply(watchStatusMessage()).queue();
            case "알림테스트" -> event.reply("운좋은 알림봇이 정상 작동 중입니다.").queue();
            case "운세" -> replyFortune(event);
            case "뽑기" -> replyDraw(event);
            case "사이트" -> event.reply("운좋은 클랜 사이트\n" + CLAN_SITE).queue();
            case "도움말" -> event.reply(helpMessage()).queue();
            case "공지" -> event.reply(discordBotNoticeService.getNotice()).queue();
            case "공지등록" -> updateNotice(event);
            default -> event.reply("지원하지 않는 명령어입니다. `/도움말`을 확인해 주세요.").queue();
        }
    }

    private void replyFortune(SlashCommandInteractionEvent event) {
        event.deferReply().queue(hook -> {
            String fortune = koreanFortuneService.getFortune();
            hook.editOriginal("🔮 **오늘의 운세**\n" + fortune).queue();
        });
    }

    private void replyDraw(SlashCommandInteractionEvent event) {
        OptionMapping countOption = event.getOption("숫자");
        if (countOption == null) {
            event.reply("뽑을 숫자를 입력해 주세요.").queue();
            return;
        }

        OptionMapping memberOption = event.getOption("인원목록");
        if (memberOption == null || memberOption.getAsString().isBlank()) {
            event.reply("인원목록을 쉼표로 구분해 입력해 주세요.").queue();
            return;
        }
        List<String> members = List.of(memberOption.getAsString().split("[,，\\n]"))
                .stream()
                .map(String::trim)
                .filter(name -> !name.isBlank())
                .toList();

        if (members.size() > 50) {
            event.reply("인원은 최대 50명까지 입력할 수 있습니다.").queue();
            return;
        }

        int drawCount = countOption.getAsInt();
        Set<String> uniqueMembers = new HashSet<>(members);
        if (uniqueMembers.size() != members.size()) {
            event.reply("같은 인원을 두 번 입력할 수 없습니다.").queue();
            return;
        }
        if (drawCount < 1 || drawCount > members.size()) {
            event.reply("숫자는 1부터 입력한 인원 수(" + members.size() + ") 사이로 입력해 주세요.").queue();
            return;
        }

        List<String> winners = drawMembers(members, drawCount, secureRandom);
        event.reply("🎉 **뽑기 결과**\n" + String.join("\n", winners.stream().map(name -> "- " + name).toList())).queue();
    }

    private void updateNotice(SlashCommandInteractionEvent event) {
        if (event.getMember() == null || !event.getMember().hasPermission(Permission.MANAGE_SERVER)) {
            event.reply("서버 관리 권한이 있는 사람만 공지를 등록할 수 있습니다.").queue();
            return;
        }
        OptionMapping content = event.getOption("내용");
        if (content == null || content.getAsString().isBlank()) {
            event.reply("공지 내용을 입력해 주세요.").queue();
            return;
        }
        discordBotNoticeService.updateNotice(content.getAsString().trim());
        event.reply("공지를 저장했습니다. `/공지`로 확인할 수 있습니다.").queue();
    }

    static List<String> drawMembers(List<String> members, int count, java.util.Random random) {
        List<String> shuffled = new ArrayList<>(members);
        Collections.shuffle(shuffled, random);
        return List.copyOf(shuffled.subList(0, count));
    }

    static String helpMessage() {
        return """
                **운좋은 알림봇 명령어**
                `/보스일정` 매일 보스 시간 확인
                `/다음보스` 가장 가까운 다음 보스 확인
                `/운세` 외부 한국어 API로 오늘의 운세 확인
                `/뽑기 숫자:2 인원목록:홍길동,김운좋,박행운` 쉼표로 구분한 최대 50명 중 숫자만큼 추첨
                `/사이트` 운좋은 클랜 사이트 주소 확인
                `/도움말` 명령어와 사용법 확인
                `/공지` 현재 클랜 공지 확인
                `/공지등록 내용:...` 공지 변경(서버 관리자 전용)
                `/감시상태` 게임 화면 숫자 감시의 최근 기록 확인
                `/알림테스트` 봇 연결 상태 확인
                """;
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

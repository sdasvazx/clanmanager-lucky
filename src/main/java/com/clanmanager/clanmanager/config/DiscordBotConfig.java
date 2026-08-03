package com.clanmanager.clanmanager.config;

import com.clanmanager.clanmanager.service.DiscordSlashCommandService;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.requests.GatewayIntent;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class DiscordBotConfig {

    private final DiscordSlashCommandService slashCommandService;

    @Value("${discord.bot-token:}")
    private String botToken;

    private JDA jda;

    @PostConstruct
    public void connect() {
        if (botToken == null || botToken.isBlank()) {
            log.info("DISCORD_BOT_TOKEN is empty; Discord slash commands are disabled.");
            return;
        }

        try {
            jda = JDABuilder.createLight(botToken)
                    .enableIntents(GatewayIntent.GUILD_VOICE_STATES)
                    .addEventListeners(slashCommandService)
                    .build()
                    .awaitReady();

            List<CommandData> commands = commandDefinitions();
            if (jda.getGuilds().isEmpty()) {
                jda.updateCommands().addCommands(commands).queue();
                log.info("Registered global Discord slash commands.");
            } else {
                jda.getGuilds().forEach(guild -> guild.updateCommands().addCommands(commands).queue());
                log.info("Registered Discord slash commands in {} guild(s).", jda.getGuilds().size());
            }
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            log.error("Interrupted while connecting Discord bot.", ex);
        } catch (RuntimeException ex) {
            log.error("Failed to connect Discord bot. Check DISCORD_BOT_TOKEN.", ex);
        }
    }

    static List<CommandData> commandDefinitions() {
        List<CommandData> commands = new ArrayList<>(List.of(
                Commands.slash("보스일정", "매일 보스 일정을 확인합니다."),
                Commands.slash("다음보스", "가장 가까운 다음 보스를 확인합니다."),
                Commands.slash("감시상태", "게임 화면 숫자 감시 상태를 확인합니다."),
                Commands.slash("알림테스트", "운좋은 알림봇 연결 상태를 확인합니다."),
                Commands.slash("운세", "오늘의 운세를 확인합니다."),
                Commands.slash("사이트", "운좋은 클랜 사이트 주소를 표시합니다."),
                Commands.slash("도움말", "사용 가능한 명령어와 사용법을 표시합니다."),
                Commands.slash("공지", "현재 클랜 공지를 표시합니다."),
                Commands.slash("노래", "유튜브에서 노래를 검색해 음성 채널에서 재생합니다.")
                        .addOption(OptionType.STRING, "검색어", "곡명, 가수 또는 유튜브 주소", true),
                Commands.slash("정지", "현재 노래를 멈추고 대기열을 비웁니다."),
                Commands.slash("스킵", "현재 노래를 건너뛰고 다음 곡을 재생합니다."),
                Commands.slash("나가기", "노래를 멈추고 봇을 음성 채널에서 내보냅니다."),
                Commands.slash("목록", "현재 노래와 신청 대기열을 확인합니다."),
                Commands.slash("공지등록", "클랜 공지를 변경합니다.")
                        .addOption(OptionType.STRING, "내용", "표시할 공지 내용", true)
        ));

        SlashCommandData draw = Commands.slash("뽑기", "입력한 인원 중 지정한 숫자만큼 무작위로 뽑습니다.")
                .addOption(OptionType.INTEGER, "숫자", "뽑을 사람 수", true)
                .addOption(OptionType.STRING, "인원목록", "쉼표로 구분한 인원 이름(최대 50명)", true);
        commands.add(draw);
        return List.copyOf(commands);
    }

    @PreDestroy
    public void disconnect() {
        if (jda != null) {
            jda.shutdown();
        }
    }
}

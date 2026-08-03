package com.clanmanager.clanmanager.config;

import com.clanmanager.clanmanager.service.DiscordSlashCommandService;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

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
                    .addEventListeners(slashCommandService)
                    .build()
                    .awaitReady();

            var commands = java.util.List.of(
                    Commands.slash("보스일정", "매일 보스 일정을 확인합니다."),
                    Commands.slash("다음보스", "가장 가까운 다음 보스를 확인합니다."),
                    Commands.slash("감시상태", "게임 화면 숫자 감시 상태를 확인합니다."),
                    Commands.slash("알림테스트", "운좋은 알림봇 연결 상태를 확인합니다.")
            );

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

    @PreDestroy
    public void disconnect() {
        if (jda != null) {
            jda.shutdown();
        }
    }
}

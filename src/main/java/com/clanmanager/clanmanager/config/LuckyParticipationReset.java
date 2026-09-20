package com.clanmanager.clanmanager.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * 던컨2 전환 시 기존 참여/보스 기록을 한 번만 초기화한다.
 * 클랜원 및 다른 업무 데이터는 절대 수정하지 않는다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class LuckyParticipationReset implements CommandLineRunner {

    private static final String RESET_KEY = "duncan2-participation-reset-20260920";

    private final JdbcTemplate jdbcTemplate;
    private final TransactionTemplate transactionTemplate;

    @Override
    public void run(String... args) {
        jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS app_data_resets (
                    reset_key VARCHAR(100) PRIMARY KEY,
                    executed_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                    deleted_attendances BIGINT NOT NULL,
                    deleted_boss_members BIGINT NOT NULL,
                    deleted_boss_records BIGINT NOT NULL
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
                """);

        Integer alreadyExecuted = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM app_data_resets WHERE reset_key = ?",
                Integer.class,
                RESET_KEY
        );
        if (alreadyExecuted != null && alreadyExecuted > 0) {
            log.info("Participation reset already completed: {}", RESET_KEY);
            return;
        }

        transactionTemplate.executeWithoutResult(status -> {
            Long memberCountBefore = count("members");
            Long attendanceCount = count("activity_attendances");
            Long bossMemberCount = count("boss_participation_members");
            Long bossRecordCount = count("boss_participation_records");

            jdbcTemplate.update("DELETE FROM activity_attendances");
            jdbcTemplate.update("DELETE FROM boss_participation_members");
            jdbcTemplate.update("DELETE FROM boss_participation_records");

            Long memberCountAfter = count("members");
            if (!memberCountBefore.equals(memberCountAfter)) {
                status.setRollbackOnly();
                throw new IllegalStateException("Member count changed during participation reset");
            }
            if (count("activity_attendances") != 0L
                    || count("boss_participation_members") != 0L
                    || count("boss_participation_records") != 0L) {
                status.setRollbackOnly();
                throw new IllegalStateException("Participation reset verification failed");
            }

            jdbcTemplate.update(
                    "INSERT INTO app_data_resets "
                            + "(reset_key, deleted_attendances, deleted_boss_members, deleted_boss_records) "
                            + "VALUES (?, ?, ?, ?)",
                    RESET_KEY,
                    attendanceCount,
                    bossMemberCount,
                    bossRecordCount
            );
            log.warn(
                    "Participation reset completed: attendances={}, bossMembers={}, bossRecords={}, membersPreserved={}",
                    attendanceCount,
                    bossMemberCount,
                    bossRecordCount,
                    memberCountAfter
            );
        });
    }

    private Long count(String tableName) {
        return jdbcTemplate.queryForObject("SELECT COUNT(*) FROM " + tableName, Long.class);
    }
}

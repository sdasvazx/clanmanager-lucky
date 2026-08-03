package com.clanmanager.clanmanager.repository;

import com.clanmanager.clanmanager.entity.DiscordBotNotice;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DiscordBotNoticeRepository extends JpaRepository<DiscordBotNotice, Long> {
}

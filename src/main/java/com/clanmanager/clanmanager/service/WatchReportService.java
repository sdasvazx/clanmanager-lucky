package com.clanmanager.clanmanager.service;

import com.clanmanager.clanmanager.dto.WatchReportRequest;
import com.clanmanager.clanmanager.entity.WatchLog;
import com.clanmanager.clanmanager.repository.WatchLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class WatchReportService {

    private final WatchLogRepository watchLogRepository;
    private final DiscordWebhookService discordWebhookService;

    @Transactional
    public WatchLog report(WatchReportRequest request) {
        WatchLog saved = watchLogRepository.save(WatchLog.builder()
                .targetId(request.targetId().trim())
                .oldValue(request.oldValue())
                .newValue(request.newValue())
                .changedAt(request.changedAt())
                .build());
        discordWebhookService.send(saved.getTargetId(), saved.getOldValue(), saved.getNewValue(), saved.getChangedAt());
        return saved;
    }
}

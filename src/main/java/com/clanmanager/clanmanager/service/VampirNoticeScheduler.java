package com.clanmanager.clanmanager.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class VampirNoticeScheduler {

    private final VampirNoticeCrawlerService crawlerService;
    private final VampirNoticeSseService sseService;

    // 넷마블 내부 API에 부담을 주지 않도록 10분 간격을 유지한다.
    @Scheduled(fixedDelay = 600_000L, initialDelay = 5_000L)
    public void refreshNotices() {
        try {
            var newNotices = crawlerService.crawlNewNotices();
            sseService.broadcastNewNotices(newNotices);
        } catch (Exception exception) {
            log.warn("뱀피르 공지 수집에 실패했습니다: {}", exception.getMessage());
        }
    }
}

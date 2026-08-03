package com.clanmanager.clanmanager.service;

import com.clanmanager.clanmanager.entity.DiscordBotNotice;
import com.clanmanager.clanmanager.repository.DiscordBotNoticeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneId;

@Service
@RequiredArgsConstructor
public class DiscordBotNoticeService {

    private static final long NOTICE_ID = 1L;
    private static final ZoneId KOREA = ZoneId.of("Asia/Seoul");

    private final DiscordBotNoticeRepository repository;

    @Transactional(readOnly = true)
    public String getNotice() {
        return repository.findById(NOTICE_ID)
                .map(DiscordBotNotice::getContent)
                .orElse("현재 등록된 공지가 없습니다. 서버 관리자는 `/공지등록`으로 공지를 입력할 수 있습니다.");
    }

    @Transactional
    public void updateNotice(String content) {
        LocalDateTime now = LocalDateTime.now(KOREA);
        DiscordBotNotice notice = repository.findById(NOTICE_ID)
                .orElseGet(() -> DiscordBotNotice.builder()
                        .id(NOTICE_ID)
                        .content(content)
                        .updatedAt(now)
                        .build());
        notice.update(content, now);
        repository.save(notice);
    }
}

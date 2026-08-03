package com.clanmanager.clanmanager.controller;

import com.clanmanager.clanmanager.dto.VampirNoticeResponse;
import com.clanmanager.clanmanager.service.VampirNoticeCrawlerService;
import com.clanmanager.clanmanager.service.VampirNoticeSseService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;

@RestController
@RequestMapping("/api/notice")
@RequiredArgsConstructor
public class VampirNoticeController {

    private final VampirNoticeCrawlerService crawlerService;
    private final VampirNoticeSseService sseService;

    @GetMapping
    public List<VampirNoticeResponse> latestNotices() {
        return crawlerService.getLatestNotices().stream().map(VampirNoticeResponse::from).toList();
    }

    @GetMapping(value = "/subscribe", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter subscribe() {
        return sseService.subscribe();
    }
}

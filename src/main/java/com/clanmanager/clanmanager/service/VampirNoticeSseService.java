package com.clanmanager.clanmanager.service;

import com.clanmanager.clanmanager.dto.VampirNoticeResponse;
import com.clanmanager.clanmanager.entity.VampirNotice;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

@Service
public class VampirNoticeSseService {

    private static final long SSE_TIMEOUT_MILLIS = 30L * 60L * 1000L;

    // 단일 서버용 emitter 목록이다. 서버를 여러 대로 확장할 때는 Redis Pub/Sub 등으로 이벤트를 공유해야 한다.
    private final CopyOnWriteArrayList<SseEmitter> emitters = new CopyOnWriteArrayList<>();

    public SseEmitter subscribe() {
        SseEmitter emitter = new SseEmitter(SSE_TIMEOUT_MILLIS);
        emitters.add(emitter);
        emitter.onCompletion(() -> emitters.remove(emitter));
        emitter.onTimeout(() -> emitters.remove(emitter));
        emitter.onError(error -> emitters.remove(emitter));

        try {
            emitter.send(SseEmitter.event().name("connected").data("connected"));
        } catch (IOException exception) {
            emitters.remove(emitter);
            emitter.completeWithError(exception);
        }
        return emitter;
    }

    public void broadcastNewNotices(List<VampirNotice> notices) {
        if (notices == null || notices.isEmpty()) {
            return;
        }
        List<VampirNoticeResponse> payload = notices.stream().map(VampirNoticeResponse::from).toList();
        for (SseEmitter emitter : emitters) {
            try {
                emitter.send(SseEmitter.event().name("newNotice").data(payload));
            } catch (Exception exception) {
                emitters.remove(emitter);
                emitter.complete();
            }
        }
    }
}

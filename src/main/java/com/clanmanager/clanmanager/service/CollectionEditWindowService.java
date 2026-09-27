package com.clanmanager.clanmanager.service;

import com.clanmanager.clanmanager.entity.CollectionEditWindow;
import com.clanmanager.clanmanager.repository.CollectionEditWindowRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.Duration;
import java.time.Instant;

@Service
@RequiredArgsConstructor
public class CollectionEditWindowService {
    private final CollectionEditWindowRepository repository;

    public WindowStatus status() {
        Instant now = Instant.now();
        Instant end = repository.findById(1L).map(CollectionEditWindow::getExpiresAt).orElse(null);
        return new WindowStatus(end != null && now.isBefore(end), end, now);
    }

    @Transactional
    public WindowStatus openFor24Hours(Long actorId) {
        CollectionEditWindow window = repository.findById(1L).orElseGet(CollectionEditWindow::new);
        // Retrying a request must not extend an already-open window.
        if (window.getExpiresAt() == null || !Instant.now().isBefore(window.getExpiresAt())) {
            window.setExpiresAt(Instant.now().plus(Duration.ofHours(24)));
            window.setOpenedByMemberId(actorId);
            repository.save(window);
        }
        return status();
    }

    public record WindowStatus(boolean active, Instant expiresAt, Instant serverNow) {}
}

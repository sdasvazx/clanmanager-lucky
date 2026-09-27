package com.clanmanager.clanmanager.service;

import com.clanmanager.clanmanager.entity.CollectionEditWindow;
import com.clanmanager.clanmanager.repository.CollectionEditWindowRepository;
import org.junit.jupiter.api.Test;
import java.time.Instant;
import java.time.Duration;
import java.util.Optional;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class CollectionEditWindowServiceTest {
    @Test void noWindowAndExpiredWindowAreClosed() {
        var repo = mock(CollectionEditWindowRepository.class);
        var service = new CollectionEditWindowService(repo);
        when(repo.findById(1L)).thenReturn(Optional.empty());
        assertThat(service.status().active()).isFalse();
        var expired = new CollectionEditWindow(); expired.setExpiresAt(Instant.now().minusSeconds(1));
        when(repo.findById(1L)).thenReturn(Optional.of(expired));
        assertThat(service.status().active()).isFalse();
    }
    @Test void openingPersists24HoursAndRetryOrRestartDoesNotExtendIt() {
        var repo = mock(CollectionEditWindowRepository.class);
        var window = new CollectionEditWindow();
        when(repo.findById(1L)).thenReturn(Optional.of(window));
        var service = new CollectionEditWindowService(repo);
        Instant before = Instant.now();
        var opened = service.openFor24Hours(1L);
        assertThat(opened.active()).isTrue();
        assertThat(opened.expiresAt()).isBetween(before.plus(Duration.ofHours(24)), Instant.now().plus(Duration.ofHours(24)));
        assertThat(service.openFor24Hours(1L).expiresAt()).isEqualTo(opened.expiresAt());
        assertThat(new CollectionEditWindowService(repo).status().expiresAt()).isEqualTo(opened.expiresAt());
        verify(repo, times(1)).save(window);
    }
}

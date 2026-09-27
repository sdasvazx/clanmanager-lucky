package com.clanmanager.clanmanager.controller;

import com.clanmanager.clanmanager.entity.*;
import com.clanmanager.clanmanager.repository.*;
import com.clanmanager.clanmanager.service.CollectionEditWindowService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CollectionEditPermissionsTest {
    @Mock MemberRepository members;
    @Mock CollectionItemRepository items;
    @Mock CollectionStatusRepository statuses;
    @Mock CollectionHistoryRepository histories;
    @Mock CollectionEditWindowService windows;
    @InjectMocks ManagementRecordController controller;
    Member member;
    CollectionItem item;
    CollectionStatus status;

    @BeforeEach void prepare() {
        member = Member.builder().memberId(1L).characterName("본인").role(MemberRole.MEMBER).active(true).build();
        item = CollectionItem.builder().collectionItemId(7L).itemName("스킬").build();
        status = CollectionStatus.builder().collectionStatusId(9L).member(member).item(item).state("완료").locked(true).build();
        when(members.findById(1L)).thenReturn(Optional.of(member));
    }
    ManagementRecordController.CollectionStatusRequest request(Long target, String state) {
        var request = new ManagementRecordController.CollectionStatusRequest();
        request.setActorMemberId(1L); request.setMemberId(target); request.setItemId(7L); request.setState(state);
        return request;
    }
    UsernamePasswordAuthenticationToken auth(String id) {
        return new UsernamePasswordAuthenticationToken(id, null, List.of());
    }
    void stateLookup(boolean open) {
        when(items.findById(7L)).thenReturn(Optional.of(item));
        when(statuses.findByMemberAndItem(member, item)).thenReturn(Optional.of(status));
        when(windows.status()).thenReturn(new CollectionEditWindowService.WindowStatus(open, Instant.now(), Instant.now()));
    }
    @Test void ownLockedCompletedStatusCanBeChangedDuringWindowWithoutClearingLock() {
        stateLookup(true);
        when(statuses.save(status)).thenReturn(status);
        controller.updateOwnCollectionStatus(request(1L, "미완료"), auth("1"));
        assertThat(status.getState()).isEqualTo("미완료");
        assertThat(status.getLocked()).isTrue();
        verify(histories).save(any());
    }
    @Test void anotherMemberIsRejectedEvenDuringWindow() {
        var other = Member.builder().memberId(2L).role(MemberRole.MEMBER).build();
        when(members.findById(2L)).thenReturn(Optional.of(other));
        assertThatThrownBy(() -> controller.updateCollectionStatus(request(2L, "완료"), auth("1"))).isInstanceOf(SecurityException.class);
        verifyNoInteractions(statuses);
    }
    @Test void spoofedActorAndAnonymousRequestsAreRejected() {
        assertThatThrownBy(() -> controller.updateCollectionStatus(request(1L, "완료"), auth("2"))).isInstanceOf(SecurityException.class);
        assertThatThrownBy(() -> controller.updateCollectionStatus(request(1L, "완료"), null)).isInstanceOf(SecurityException.class);
        verifyNoInteractions(statuses);
    }
    @Test void expirationRestoresCompletedRestriction() {
        stateLookup(false);
        assertThatThrownBy(() -> controller.updateOwnCollectionStatus(request(1L, "미완료"), auth("1"))).isInstanceOf(SecurityException.class);
        verify(statuses, never()).save(any());
    }
    @Test void expirationRestoresLockRestriction() {
        stateLookup(false); status.setState("미완료");
        assertThatThrownBy(() -> controller.updateOwnCollectionStatus(request(1L, "완료"), auth("1"))).isInstanceOf(SecurityException.class);
        verify(statuses, never()).save(any());
    }
    @Test void unlockedIncompleteSelfRemainsEditableAfterExpiration() {
        stateLookup(false); status.setState("미완료"); status.setLocked(false);
        when(statuses.save(status)).thenReturn(status);
        controller.updateOwnCollectionStatus(request(1L, "완료"), auth("1"));
        assertThat(status.getState()).isEqualTo("완료");
    }
    @Test void adminCanEditSomeoneElseOutsideWindow() {
        member.setRole(MemberRole.ADMIN);
        var other = Member.builder().memberId(2L).characterName("다른사람").role(MemberRole.MEMBER).build();
        when(members.findById(2L)).thenReturn(Optional.of(other));
        when(items.findById(7L)).thenReturn(Optional.of(item));
        when(statuses.findByMemberAndItem(other, item)).thenReturn(Optional.of(status));
        when(windows.status()).thenReturn(new CollectionEditWindowService.WindowStatus(false, null, Instant.now()));
        when(statuses.save(status)).thenReturn(status);
        controller.updateCollectionStatus(request(2L, "미완료"), auth("1"));
        assertThat(status.getState()).isEqualTo("미완료");
    }
    @Test void memberCannotOpenWindow() {
        assertThatThrownBy(() -> controller.openCollectionEditWindow(auth("1"))).isInstanceOf(SecurityException.class);
        verifyNoInteractions(windows);
    }
}

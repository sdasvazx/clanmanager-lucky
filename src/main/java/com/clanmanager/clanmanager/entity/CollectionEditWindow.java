package com.clanmanager.clanmanager.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import lombok.Getter;
import lombok.Setter;
import java.time.Instant;

@Entity
@Getter
@Setter
public class CollectionEditWindow {
    @Id
    private Long id = 1L;
    private Instant expiresAt;
    private Long openedByMemberId;
}

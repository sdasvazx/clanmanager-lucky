package com.clanmanager.clanmanager.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "watch_log")
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WatchLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "target_id", nullable = false, length = 100)
    private String targetId;

    @Column(name = "old_value", nullable = false, length = 50)
    private String oldValue;

    @Column(name = "new_value", nullable = false, length = 50)
    private String newValue;

    @Column(name = "changed_at", nullable = false)
    private LocalDateTime changedAt;
}

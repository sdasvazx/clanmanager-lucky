package com.clanmanager.clanmanager.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "vault_item_distributions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VaultItemDistribution {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long distributionId;

    @Column(nullable = false)
    private Long memberId;

    @Column(nullable = false, length = 50)
    private String memberName;

    @Column(nullable = false, length = 30)
    private String itemId;

    @Column(nullable = false, length = 50)
    private String itemName;

    @Column(nullable = false)
    private Integer quantity;

    @Column(nullable = false)
    private Long distributedByMemberId;

    @Column(nullable = false)
    private LocalDateTime distributedAt;
}

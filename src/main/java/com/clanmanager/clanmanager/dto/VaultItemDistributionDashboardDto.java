package com.clanmanager.clanmanager.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class VaultItemDistributionDashboardDto {
    private int activeMemberCount;
    private int weeklyGrantCount;
    private int unpaidMemberCount;
    private int todayQuantity;
    private List<MemberRow> members;
    private List<HistoryRow> history;

    @Data
    @Builder
    public static class MemberRow {
        private Long memberId;
        private String characterName;
        private int weeklyQuantity;
        private LocalDateTime recentDistributedAt;
        private String status;
    }

    @Data
    @Builder
    public static class HistoryRow {
        private Long distributionId;
        private LocalDateTime distributedAt;
        private String distributedByName;
        private String memberName;
        private String itemName;
        private int quantity;
    }
}

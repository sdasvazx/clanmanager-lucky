package com.clanmanager.clanmanager.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Data
@Builder
public class VaultItemDistributionDashboardDto {
    private int activeMemberCount;
    private int weeklyGrantCount;
    private int unpaidMemberCount;
    private int todayQuantity;
    private Long periodId;
    private String periodName;
    private LocalDate periodStartDate;
    private LocalDate periodEndDate;
    private Map<String, Integer> itemQuantities;
    private List<MemberRow> members;
    private List<HistoryRow> history;

    @Data
    @Builder
    public static class MemberRow {
        private Long memberId;
        private String characterName;
        private int weeklyQuantity;
        private String distributedItems;
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

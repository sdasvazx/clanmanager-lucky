package com.clanmanager.clanmanager.dto;

import lombok.Data;

import java.util.List;

@Data
public class VaultItemDistributionRequestDto {
    private Long adminMemberId;
    private String itemId;
    private Integer quantity;
    private List<Long> memberIds;
}

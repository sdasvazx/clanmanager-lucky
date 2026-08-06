package com.clanmanager.clanmanager.controller;

import com.clanmanager.clanmanager.dto.VaultItemDistributionDashboardDto;
import com.clanmanager.clanmanager.dto.VaultItemDistributionRequestDto;
import com.clanmanager.clanmanager.service.VaultItemDistributionService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/vault-item-distributions")
@RequiredArgsConstructor
public class VaultItemDistributionController {
    private final VaultItemDistributionService service;

    @GetMapping
    public VaultItemDistributionDashboardDto dashboard() {
        return service.dashboard();
    }

    @PostMapping
    public VaultItemDistributionDashboardDto distribute(@RequestBody VaultItemDistributionRequestDto request) {
        return service.distribute(request);
    }

    @DeleteMapping
    public VaultItemDistributionDashboardDto resetHistory(@RequestParam Long adminMemberId) {
        return service.resetHistory(adminMemberId);
    }
}

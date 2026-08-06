package com.clanmanager.clanmanager.repository;

import com.clanmanager.clanmanager.entity.VaultItemDistribution;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface VaultItemDistributionRepository extends JpaRepository<VaultItemDistribution, Long> {
    List<VaultItemDistribution> findByDistributedAtGreaterThanEqualOrderByDistributedAtDesc(LocalDateTime from);
}

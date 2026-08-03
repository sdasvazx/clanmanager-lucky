package com.clanmanager.clanmanager.repository;

import com.clanmanager.clanmanager.entity.VampirNotice;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;

public interface VampirNoticeRepository extends JpaRepository<VampirNotice, Long> {
    List<VampirNotice> findAllByArticleIdIn(Collection<Long> articleIds);

    List<VampirNotice> findTop20ByOrderByRegDateDescArticleIdDesc();
}

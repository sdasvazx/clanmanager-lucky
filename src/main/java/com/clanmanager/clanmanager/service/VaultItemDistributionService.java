package com.clanmanager.clanmanager.service;

import com.clanmanager.clanmanager.dto.VaultItemDistributionDashboardDto;
import com.clanmanager.clanmanager.dto.VaultItemDistributionRequestDto;
import com.clanmanager.clanmanager.entity.Member;
import com.clanmanager.clanmanager.entity.MemberRole;
import com.clanmanager.clanmanager.entity.VaultItemDistribution;
import com.clanmanager.clanmanager.repository.MemberRepository;
import com.clanmanager.clanmanager.repository.VaultItemDistributionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.*;
import java.time.temporal.TemporalAdjusters;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class VaultItemDistributionService {
    private static final ZoneId SEOUL = ZoneId.of("Asia/Seoul");
    private static final Map<String, String> ITEMS = Map.of(
            "weapon", "영무 1티어",
            "armor", "영방 1티어",
            "portrait", "초상화"
    );

    private final VaultItemDistributionRepository repository;
    private final MemberRepository memberRepository;

    @Transactional(readOnly = true)
    public VaultItemDistributionDashboardDto dashboard() {
        LocalDate today = LocalDate.now(SEOUL);
        LocalDateTime weekStart = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)).atStartOfDay();
        LocalDateTime todayStart = today.atStartOfDay();
        List<Member> members = memberRepository.findByActiveTrueOrderByMemberIdAsc().stream()
                .sorted(Comparator.comparing(Member::getCharacterName, Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER)))
                .toList();
        List<VaultItemDistribution> all = repository.findAll();
        List<VaultItemDistribution> weekly = all.stream().filter(row -> !row.getDistributedAt().isBefore(weekStart)).toList();
        Map<Long, Integer> weeklyQuantity = weekly.stream().collect(Collectors.groupingBy(
                VaultItemDistribution::getMemberId,
                Collectors.summingInt(VaultItemDistribution::getQuantity)
        ));
        Map<Long, LocalDateTime> recent = all.stream().collect(Collectors.toMap(
                VaultItemDistribution::getMemberId,
                VaultItemDistribution::getDistributedAt,
                (left, right) -> left.isAfter(right) ? left : right
        ));
        List<VaultItemDistributionDashboardDto.MemberRow> rows = members.stream().map(target -> {
            int quantity = weeklyQuantity.getOrDefault(target.getMemberId(), 0);
            return VaultItemDistributionDashboardDto.MemberRow.builder()
                    .memberId(target.getMemberId())
                    .characterName(target.getCharacterName())
                    .weeklyQuantity(quantity)
                    .recentDistributedAt(recent.get(target.getMemberId()))
                    .status(quantity > 0 ? "지급 완료" : "미지급")
                    .build();
        }).toList();
        int todayQuantity = all.stream()
                .filter(row -> !row.getDistributedAt().isBefore(todayStart))
                .mapToInt(VaultItemDistribution::getQuantity)
                .sum();
        return VaultItemDistributionDashboardDto.builder()
                .activeMemberCount(members.size())
                .weeklyGrantCount(weekly.size())
                .unpaidMemberCount((int) rows.stream().filter(row -> "미지급".equals(row.getStatus())).count())
                .todayQuantity(todayQuantity)
                .members(rows)
                .build();
    }

    @Transactional
    public VaultItemDistributionDashboardDto distribute(VaultItemDistributionRequestDto request) {
        Member admin = memberRepository.findById(request.getAdminMemberId())
                .orElseThrow(() -> new IllegalArgumentException("운영자 정보를 찾을 수 없습니다."));
        if (admin.getRole() != MemberRole.ADMIN) throw new IllegalArgumentException("운영자만 지급할 수 있습니다.");
        String itemName = ITEMS.get(request.getItemId());
        if (itemName == null) throw new IllegalArgumentException("지원하지 않는 분배 아이템입니다.");
        int quantity = request.getQuantity() == null ? 0 : request.getQuantity();
        if (quantity < 1) throw new IllegalArgumentException("지급 수량은 1개 이상이어야 합니다.");
        List<Long> ids = request.getMemberIds() == null ? List.of() : request.getMemberIds().stream().distinct().toList();
        if (ids.isEmpty()) throw new IllegalArgumentException("지급할 클랜원을 선택해 주세요.");
        Map<Long, Member> members = memberRepository.findAllById(ids).stream()
                .filter(member -> Boolean.TRUE.equals(member.getActive()))
                .collect(Collectors.toMap(Member::getMemberId, Function.identity()));
        if (members.size() != ids.size()) throw new IllegalArgumentException("선택한 클랜원 중 유효하지 않은 인원이 있습니다.");
        LocalDateTime now = LocalDateTime.now(SEOUL);
        repository.saveAll(ids.stream().map(id -> {
            Member target = members.get(id);
            return VaultItemDistribution.builder()
                    .memberId(id)
                    .memberName(target.getCharacterName())
                    .itemId(request.getItemId())
                    .itemName(itemName)
                    .quantity(quantity)
                    .distributedByMemberId(admin.getMemberId())
                    .distributedAt(now)
                    .build();
        }).toList());
        return dashboard();
    }
}

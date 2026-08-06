package com.clanmanager.clanmanager.service;

import com.clanmanager.clanmanager.dto.VaultItemDistributionDashboardDto;
import com.clanmanager.clanmanager.dto.VaultItemDistributionRequestDto;
import com.clanmanager.clanmanager.entity.Member;
import com.clanmanager.clanmanager.entity.MemberRole;
import com.clanmanager.clanmanager.entity.ParticipationPeriod;
import com.clanmanager.clanmanager.entity.VaultItemDistribution;
import com.clanmanager.clanmanager.repository.MemberRepository;
import com.clanmanager.clanmanager.repository.ParticipationPeriodRepository;
import com.clanmanager.clanmanager.repository.VaultItemDistributionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.*;
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
            "portrait", "초상화",
            "will", "유언"
    );

    private final VaultItemDistributionRepository repository;
    private final MemberRepository memberRepository;
    private final ParticipationPeriodRepository participationPeriodRepository;

    @Transactional(readOnly = true)
    public VaultItemDistributionDashboardDto dashboard() {
        LocalDate today = LocalDate.now(SEOUL);
        LocalDateTime todayStart = today.atStartOfDay();
        ParticipationPeriod currentPeriod = participationPeriodRepository.findAllByOrderByPeriodIndexAsc().stream()
                .filter(period -> !today.isBefore(period.getStartDate()) && !today.isAfter(period.getEndDate()))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("현재 날짜에 해당하는 회차 기간이 없습니다."));
        List<Member> members = memberRepository.findByActiveTrueOrderByMemberIdAsc().stream()
                .sorted(Comparator.comparing(Member::getCharacterName, Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER)))
                .toList();
        List<VaultItemDistribution> all = repository.findAll();
        Map<Long, String> memberNames = memberRepository.findAll().stream().collect(Collectors.toMap(
                Member::getMemberId,
                Member::getCharacterName,
                (left, right) -> left
        ));
        List<VaultItemDistribution> periodDistributions = all.stream().filter(row -> {
            LocalDate distributedDate = row.getDistributedAt().toLocalDate();
            return !distributedDate.isBefore(currentPeriod.getStartDate()) && !distributedDate.isAfter(currentPeriod.getEndDate());
        }).toList();
        Map<Long, Integer> weeklyQuantity = periodDistributions.stream().collect(Collectors.groupingBy(
                VaultItemDistribution::getMemberId,
                Collectors.summingInt(VaultItemDistribution::getQuantity)
        ));
        Map<Long, String> distributedItemsByMember = periodDistributions.stream()
                .collect(Collectors.groupingBy(VaultItemDistribution::getMemberId))
                .entrySet().stream().collect(Collectors.toMap(Map.Entry::getKey, entry -> entry.getValue().stream()
                        .collect(Collectors.groupingBy(VaultItemDistribution::getItemName, LinkedHashMap::new, Collectors.summingInt(VaultItemDistribution::getQuantity)))
                        .entrySet().stream()
                        .map(item -> item.getKey() + " " + item.getValue() + "개")
                        .collect(Collectors.joining(", "))));
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
                    .distributedItems(distributedItemsByMember.getOrDefault(target.getMemberId(), "-"))
                    .recentDistributedAt(recent.get(target.getMemberId()))
                    .status(quantity > 0 ? "지급 완료" : "미지급")
                    .build();
        }).toList();
        int todayQuantity = all.stream()
                .filter(row -> !row.getDistributedAt().isBefore(todayStart))
                .mapToInt(VaultItemDistribution::getQuantity)
                .sum();
        Map<String, Integer> itemQuantities = new LinkedHashMap<>();
        ITEMS.keySet().forEach(itemId -> itemQuantities.put(itemId, 0));
        periodDistributions.forEach(row -> itemQuantities.merge(row.getItemId(), row.getQuantity(), Integer::sum));
        List<VaultItemDistributionDashboardDto.HistoryRow> history = all.stream()
                .sorted(Comparator.comparing(VaultItemDistribution::getDistributedAt).reversed())
                .limit(100)
                .map(row -> VaultItemDistributionDashboardDto.HistoryRow.builder()
                        .distributionId(row.getDistributionId())
                        .distributedAt(row.getDistributedAt())
                        .distributedByName(memberNames.getOrDefault(row.getDistributedByMemberId(), "탈퇴한 운영자"))
                        .memberName(row.getMemberName())
                        .itemName(row.getItemName())
                        .quantity(row.getQuantity())
                        .build())
                .toList();
        return VaultItemDistributionDashboardDto.builder()
                .activeMemberCount(members.size())
                .weeklyGrantCount(periodDistributions.size())
                .unpaidMemberCount((int) rows.stream().filter(row -> "미지급".equals(row.getStatus())).count())
                .todayQuantity(todayQuantity)
                .periodId(currentPeriod.getPeriodId())
                .periodName(currentPeriod.getPeriodName())
                .periodStartDate(currentPeriod.getStartDate())
                .periodEndDate(currentPeriod.getEndDate())
                .itemQuantities(itemQuantities)
                .members(rows)
                .history(history)
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
        LocalDate today = now.toLocalDate();
        ParticipationPeriod currentPeriod = participationPeriodRepository.findAllByOrderByPeriodIndexAsc().stream()
                .filter(period -> !today.isBefore(period.getStartDate()) && !today.isAfter(period.getEndDate()))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("현재 날짜에 해당하는 회차 기간이 없습니다."));
        repository.saveAll(ids.stream().map(id -> {
            Member target = members.get(id);
            return VaultItemDistribution.builder()
                    .memberId(id)
                    .memberName(target.getCharacterName())
                    .itemId(request.getItemId())
                    .itemName(itemName)
                    .quantity(quantity)
                    .distributedByMemberId(admin.getMemberId())
                    .periodId(currentPeriod.getPeriodId())
                    .periodName(currentPeriod.getPeriodName())
                    .distributedAt(now)
                    .build();
        }).toList());
        return dashboard();
    }
}

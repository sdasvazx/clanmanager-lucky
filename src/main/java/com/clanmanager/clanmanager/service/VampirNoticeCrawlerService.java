package com.clanmanager.clanmanager.service;

import com.clanmanager.clanmanager.dto.VampirNoticeApiResponse;
import com.clanmanager.clanmanager.entity.VampirNotice;
import com.clanmanager.clanmanager.repository.VampirNoticeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class VampirNoticeCrawlerService {

    static final String NOTICE_API_URL = "https://forum.netmarble.com/api/game/thered/official/forum/vampir/article/list"
            + "?rows=20&start=0&viewType=pv&menuSeq=2&sort=NEW";
    private static final String USER_AGENT = "ClanManagerLucky/1.0 (+https://clanmanager-lucky.vercel.app)";
    private static final ZoneId SEOUL_ZONE = ZoneId.of("Asia/Seoul");

    private final VampirNoticeRepository noticeRepository;
    private final RestTemplateBuilder restTemplateBuilder;

    @Transactional
    public List<VampirNotice> crawlNewNotices() {
        RestTemplate restTemplate = restTemplateBuilder.build();
        HttpHeaders headers = new HttpHeaders();
        headers.set(HttpHeaders.USER_AGENT, USER_AGENT);

        VampirNoticeApiResponse response = restTemplate.exchange(
                NOTICE_API_URL,
                HttpMethod.GET,
                new HttpEntity<>(headers),
                VampirNoticeApiResponse.class
        ).getBody();

        if (response == null || !Objects.equals(response.code(), 0) || response.articleList() == null) {
            throw new IllegalStateException("뱀피르 공지 API가 정상 응답을 반환하지 않았습니다.");
        }

        List<VampirNoticeApiResponse.Article> articles = response.articleList().stream()
                .filter(article -> article.id() != null && article.regDate() != null)
                .toList();
        List<Long> articleIds = articles.stream().map(VampirNoticeApiResponse.Article::id).toList();
        Set<Long> existingIds = new HashSet<>(noticeRepository.findAllByArticleIdIn(articleIds).stream()
                .map(VampirNotice::getArticleId)
                .toList());
        LocalDateTime crawledAt = LocalDateTime.now(SEOUL_ZONE);

        List<VampirNotice> newNotices = articles.stream()
                .filter(article -> !existingIds.contains(article.id()))
                .map(article -> toEntity(article, crawledAt))
                .toList();

        return newNotices.isEmpty() ? List.of() : noticeRepository.saveAll(newNotices);
    }

    @Transactional(readOnly = true)
    public List<VampirNotice> getLatestNotices() {
        return noticeRepository.findTop20ByOrderByRegDateDescArticleIdDesc();
    }

    private VampirNotice toEntity(VampirNoticeApiResponse.Article article, LocalDateTime crawledAt) {
        return VampirNotice.builder()
                .articleId(article.id())
                .title(article.title() == null ? "" : article.title())
                .content(article.content())
                .regDate(LocalDateTime.ofInstant(Instant.ofEpochMilli(article.regDate()), SEOUL_ZONE))
                .type(article.type())
                .thumbnailUrl(article.thumbnailUrl())
                .crawledAt(crawledAt)
                .build();
    }
}

package com.clanmanager.clanmanager.service;

import com.clanmanager.clanmanager.dto.VampirNoticeApiResponse;
import com.clanmanager.clanmanager.entity.VampirNotice;
import com.clanmanager.clanmanager.repository.VampirNoticeRepository;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class VampirNoticeCrawlerServiceTest {

    @Test
    void savesOnlyNewArticlesAndConvertsEpochMillisToSeoulTime() {
        VampirNoticeRepository repository = mock(VampirNoticeRepository.class);
        RestTemplateBuilder builder = mock(RestTemplateBuilder.class);
        RestTemplate restTemplate = mock(RestTemplate.class);
        when(builder.build()).thenReturn(restTemplate);

        var existing = new VampirNoticeApiResponse.Article(100L, "기존", "", 0L, "NOTICE", null, 1L);
        var fresh = new VampirNoticeApiResponse.Article(101L, "신규", "내용", 1_725_187_200_000L, "NOTICE", "https://img", 2L);
        var apiResponse = new VampirNoticeApiResponse(0, "OK", List.of(existing, fresh), 2);
        when(restTemplate.exchange(
                eq(VampirNoticeCrawlerService.NOTICE_API_URL),
                eq(HttpMethod.GET),
                any(HttpEntity.class),
                eq(VampirNoticeApiResponse.class)
        )).thenReturn(ResponseEntity.ok(apiResponse));
        when(repository.findAllByArticleIdIn(List.of(100L, 101L))).thenReturn(List.of(VampirNotice.builder().articleId(100L).build()));
        when(repository.saveAll(any())).thenAnswer(invocation -> invocation.getArgument(0));

        var service = new VampirNoticeCrawlerService(repository, builder);
        List<VampirNotice> saved = service.crawlNewNotices();

        assertThat(saved).hasSize(1);
        assertThat(saved.getFirst().getArticleId()).isEqualTo(101L);
        assertThat(saved.getFirst().getRegDate()).isEqualTo(LocalDateTime.of(2024, 9, 1, 19, 40));
        ArgumentCaptor<Iterable<VampirNotice>> captor = ArgumentCaptor.forClass(Iterable.class);
        verify(repository).saveAll(captor.capture());
        assertThat(captor.getValue()).hasSize(1);
    }
}

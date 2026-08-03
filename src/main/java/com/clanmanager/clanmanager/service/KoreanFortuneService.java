package com.clanmanager.clanmanager.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

@Service
@Slf4j
public class KoreanFortuneService {

    private static final String API_URL = "https://korean-advice-open-api.vercel.app/api/advice";
    private static final List<String> FALLBACK_FORTUNES = List.of(
            "오늘은 서두르기보다 차근차근 움직이면 좋은 결과가 따라옵니다.",
            "뜻밖의 도움을 받을 수 있는 날입니다. 주변 사람의 말을 잘 들어보세요.",
            "작은 선택이 큰 행운으로 이어질 수 있습니다. 자신 있게 결정해 보세요.",
            "새로운 일을 시작하기 좋은 흐름입니다. 미뤄둔 일에 도전해 보세요."
    );

    private final RestTemplateBuilder restTemplateBuilder;

    public KoreanFortuneService(RestTemplateBuilder restTemplateBuilder) {
        this.restTemplateBuilder = restTemplateBuilder;
    }

    public String getFortune() {
        try {
            KoreanAdviceResponse response = restTemplateBuilder
                    .connectTimeout(Duration.ofSeconds(3))
                    .readTimeout(Duration.ofSeconds(3))
                    .build()
                    .getForObject(API_URL, KoreanAdviceResponse.class);
            if (response != null && response.message() != null && !response.message().isBlank()) {
                String author = response.author() == null || response.author().isBlank()
                        ? "오늘의 조언"
                        : response.author();
                return "%s\n— %s".formatted(response.message().trim(), author.trim());
            }
        } catch (RestClientException exception) {
            log.warn("Korean fortune API request failed; using fallback fortune.", exception);
        }
        return FALLBACK_FORTUNES.get(ThreadLocalRandom.current().nextInt(FALLBACK_FORTUNES.size()));
    }

    private record KoreanAdviceResponse(String author, String authorProfile, String message) {
    }
}

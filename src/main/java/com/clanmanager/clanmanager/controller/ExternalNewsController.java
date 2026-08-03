package com.clanmanager.clanmanager.controller;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@RestController
@RequestMapping("/api/external-news")
public class ExternalNewsController {

    private static final String FORUM_ORIGIN = "https://forum.netmarble.com";
    private static final String VAMPIR_NEWS_URL = FORUM_ORIGIN + "/vampir/list/2?nm_to=gallery";
    private static final Pattern DATE_PATTERN = Pattern.compile("20\\d{2}\\.\\s*\\d{1,2}\\.\\s*\\d{1,2}\\.");
    private static final Duration CACHE_TTL = Duration.ofHours(1);

    private volatile List<ExternalNewsItem> cachedItems = List.of();
    private volatile Instant cachedAt = Instant.EPOCH;

    @GetMapping("/vampir")
    public List<ExternalNewsItem> vampirNews() {
        if (!cachedItems.isEmpty() && cachedAt.plus(CACHE_TTL).isAfter(Instant.now())) {
            return cachedItems;
        }

        try {
            Document document = Jsoup.connect(VAMPIR_NEWS_URL)
                    .userAgent("Mozilla/5.0 (compatible; Googlebot/2.1; +http://www.google.com/bot.html)")
                    .timeout(8_000)
                    .get();
            List<ExternalNewsItem> latest = parseItems(document);
            if (!latest.isEmpty()) {
                cachedItems = List.copyOf(latest);
                cachedAt = Instant.now();
            }
            return latest.isEmpty() ? cachedItems : latest;
        } catch (Exception exception) {
            if (!cachedItems.isEmpty()) {
                return cachedItems;
            }
            throw new IllegalStateException("공식 포럼 소식을 불러오지 못했습니다.", exception);
        }
    }

    static List<ExternalNewsItem> parseItems(Document document) {
        List<ExternalNewsItem> items = new ArrayList<>();
        Set<String> seenUrls = new LinkedHashSet<>();

        for (Element link : document.select("a[href^=/vampir/view/2/]")) {
            String title = link.text().trim();
            String path = link.attr("href").trim();
            if (title.isBlank() || path.isBlank()) {
                continue;
            }
            String url = FORUM_ORIGIN + path;
            if (!seenUrls.add(url)) {
                continue;
            }

            String date = findDate(link);
            items.add(new ExternalNewsItem(title, date, url));
            if (items.size() == 6) {
                break;
            }
        }
        return items;
    }

    private static String findDate(Element link) {
        Element current = link;
        for (int depth = 0; depth < 4 && current != null; depth++, current = current.parent()) {
            Matcher matcher = DATE_PATTERN.matcher(current.text());
            if (matcher.find()) {
                return matcher.group().replaceAll("\\s+", " ");
            }
        }
        return "";
    }

    public record ExternalNewsItem(String title, String date, String url) {
    }
}

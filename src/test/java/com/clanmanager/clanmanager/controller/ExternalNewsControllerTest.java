package com.clanmanager.clanmanager.controller;

import org.jsoup.Jsoup;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ExternalNewsControllerTest {

    @Test
    void parsesUniqueLatestVampirNews() {
        var document = Jsoup.parse("""
                <ul>
                  <li><a href="/vampir/view/2/100">업데이트 안내</a><em>2026. 8. 3.</em></li>
                  <li><a href="/vampir/view/2/100">업데이트 안내</a><em>2026. 8. 3.</em></li>
                  <li><a href="/vampir/view/2/101">점검 안내</a><em>2026. 8. 2.</em></li>
                </ul>
                """);

        var items = ExternalNewsController.parseItems(document);

        assertThat(items).hasSize(2);
        assertThat(items.getFirst().title()).isEqualTo("업데이트 안내");
        assertThat(items.getFirst().date()).isEqualTo("2026. 8. 3.");
        assertThat(items.getFirst().url()).isEqualTo("https://forum.netmarble.com/vampir/view/2/100");
    }
}

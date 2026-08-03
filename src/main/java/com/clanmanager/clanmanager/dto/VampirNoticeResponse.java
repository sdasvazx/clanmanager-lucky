package com.clanmanager.clanmanager.dto;

import com.clanmanager.clanmanager.entity.VampirNotice;

import java.time.LocalDateTime;

public record VampirNoticeResponse(
        Long articleId,
        String title,
        String content,
        LocalDateTime regDate,
        String type,
        String thumbnailUrl,
        LocalDateTime crawledAt,
        String url
) {
    public static VampirNoticeResponse from(VampirNotice notice) {
        return new VampirNoticeResponse(
                notice.getArticleId(),
                notice.getTitle(),
                notice.getContent(),
                notice.getRegDate(),
                notice.getType(),
                notice.getThumbnailUrl(),
                notice.getCrawledAt(),
                "https://forum.netmarble.com/vampir/view/2/" + notice.getArticleId()
        );
    }
}

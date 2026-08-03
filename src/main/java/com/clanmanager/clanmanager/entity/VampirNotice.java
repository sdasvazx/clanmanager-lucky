package com.clanmanager.clanmanager.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "vampir_notice")
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VampirNotice {

    @Id
    @Column(name = "article_id", nullable = false)
    private Long articleId;

    @Column(nullable = false, length = 500)
    private String title;

    @Lob
    @Column(columnDefinition = "LONGTEXT")
    private String content;

    @Column(name = "reg_date", nullable = false)
    private LocalDateTime regDate;

    @Column(length = 50)
    private String type;

    @Column(name = "thumbnail_url", length = 2048)
    private String thumbnailUrl;

    @Column(name = "crawled_at", nullable = false)
    private LocalDateTime crawledAt;
}

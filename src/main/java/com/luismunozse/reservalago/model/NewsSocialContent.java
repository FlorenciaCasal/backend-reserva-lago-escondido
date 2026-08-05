package com.luismunozse.reservalago.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(
        name = "news_social_contents",
        uniqueConstraints = @UniqueConstraint(name = "uk_news_social_contents_news_platform", columnNames = {"news_id", "platform"})
)
@Getter
@Setter
public class NewsSocialContent {

    @Id
    @GeneratedValue
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "news_id", nullable = false)
    private News news;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private SocialPlatform platform;

    @Column(columnDefinition = "text")
    private String caption;

    @Column(columnDefinition = "text")
    private String body;

    @Column(columnDefinition = "text")
    private String hashtags;

    @Column(name = "call_to_action", length = 300)
    private String callToAction;

    @Column(name = "alt_text", columnDefinition = "text")
    private String altText;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    @PreUpdate
    void touch() {
        this.updatedAt = Instant.now();
    }
}

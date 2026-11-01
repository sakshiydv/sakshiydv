package com.example.urlshortener.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;

import java.time.Instant;

/**
 * One recorded click on a short link. Written on the async analytics path,
 * never on the redirect path.
 *
 * In a scaled system this table would live in a columnar / time-series store
 * (ClickHouse, Timescale) and be fed from Kafka. Postgres is fine to start.
 */
@Entity
@Table(
        name = "click_event",
        indexes = @Index(name = "idx_click_event_short_code", columnList = "shortCode")
)
public class ClickEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 16)
    private String shortCode;

    @Column(nullable = false)
    private Instant clickedAt;

    @Column(length = 64)
    private String ipAddress;

    @Column(length = 512)
    private String userAgent;

    @Column(length = 2048)
    private String referrer;

    protected ClickEvent() {
        // for JPA
    }

    public ClickEvent(String shortCode, Instant clickedAt, String ipAddress,
                      String userAgent, String referrer) {
        this.shortCode = shortCode;
        this.clickedAt = clickedAt;
        this.ipAddress = ipAddress;
        this.userAgent = userAgent;
        this.referrer = referrer;
    }

    public Long getId() {
        return id;
    }

    public String getShortCode() {
        return shortCode;
    }

    public Instant getClickedAt() {
        return clickedAt;
    }
}

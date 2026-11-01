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
 * Source-of-truth record for one short link. Lives in Postgres.
 * The short code is derived from the id via Base62 (see UrlService).
 */
@Entity
@Table(
        name = "url_mapping",
        indexes = @Index(name = "idx_url_mapping_short_code", columnList = "shortCode", unique = true)
)
public class UrlMapping {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 16)
    private String shortCode;

    @Column(nullable = false, length = 2048)
    private String longUrl;

    @Column(nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    protected UrlMapping() {
        // for JPA
    }

    public UrlMapping(String longUrl) {
        this.longUrl = longUrl;
    }

    public Long getId() {
        return id;
    }

    public String getShortCode() {
        return shortCode;
    }

    public void setShortCode(String shortCode) {
        this.shortCode = shortCode;
    }

    public String getLongUrl() {
        return longUrl;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}

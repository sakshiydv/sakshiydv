package com.example.urlshortener.web.dto;

import java.util.List;

public record StatsResponse(
        String shortCode,
        long totalClicks,
        List<ReferrerStat> topReferrers
) {
    public record ReferrerStat(String referrer, long clicks) {
    }
}

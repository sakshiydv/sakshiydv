package com.example.urlshortener.analytics;

import java.time.Instant;

/**
 * Published on the redirect path, consumed asynchronously.
 *
 * This is the seam between the hot read path and the analytics write path.
 * Today it's a Spring application event drained by an @Async listener; swapping
 * to Kafka means publishing this same payload to a topic instead — the redirect
 * code does not change.
 */
public record ClickRecordedEvent(
        String shortCode,
        Instant clickedAt,
        String ipAddress,
        String userAgent,
        String referrer
) {
}

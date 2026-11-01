package com.example.urlshortener.analytics;

import com.example.urlshortener.config.AsyncConfig;
import com.example.urlshortener.domain.ClickEvent;
import com.example.urlshortener.repository.ClickEventRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

/**
 * The analytics "consumer". Runs on the analytics thread pool, off the request
 * thread, so a slow DB insert can never delay a redirect.
 *
 * This is the one class that changes when we move to Kafka: instead of an
 * @EventListener draining an in-memory queue, it becomes a @KafkaListener
 * draining a topic. The published payload (ClickRecordedEvent) stays the same.
 */
@Component
public class ClickAnalyticsListener {

    private static final Logger log = LoggerFactory.getLogger(ClickAnalyticsListener.class);

    private final ClickEventRepository repository;

    public ClickAnalyticsListener(ClickEventRepository repository) {
        this.repository = repository;
    }

    @Async(AsyncConfig.ANALYTICS_EXECUTOR)
    @EventListener
    public void onClick(ClickRecordedEvent event) {
        try {
            repository.save(new ClickEvent(
                    event.shortCode(),
                    event.clickedAt(),
                    event.ipAddress(),
                    event.userAgent(),
                    event.referrer()));
        } catch (Exception e) {
            // Analytics is latency-tolerant AND loss-tolerant relative to redirects:
            // never let a failure here surface to the user. Log and move on.
            log.warn("failed to record click for {}", event.shortCode(), e);
        }
    }
}

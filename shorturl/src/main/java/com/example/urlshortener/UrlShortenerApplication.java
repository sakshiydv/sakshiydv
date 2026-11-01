package com.example.urlshortener;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

/**
 * URL Shortener + Analytics.
 *
 * Core lesson (see README): a redirect is latency-critical and read-heavy, while
 * click analytics is high-volume and latency-tolerant. We keep those two paths
 * separate — the redirect reads from a Redis cache and never blocks on an
 * analytics write, which is handed off asynchronously.
 */
@SpringBootApplication
@EnableAsync
public class UrlShortenerApplication {

    public static void main(String[] args) {
        SpringApplication.run(UrlShortenerApplication.class, args);
    }
}

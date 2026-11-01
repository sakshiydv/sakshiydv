package com.example.urlshortener.web;

import com.example.urlshortener.analytics.ClickRecordedEvent;
import com.example.urlshortener.domain.UrlMapping;
import com.example.urlshortener.service.UrlService;
import com.example.urlshortener.web.dto.ShortenRequest;
import com.example.urlshortener.web.dto.ShortenResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.net.URI;
import java.time.Instant;
import java.util.Optional;

@RestController
public class UrlController {

    private final UrlService urlService;
    private final ApplicationEventPublisher events;

    public UrlController(UrlService urlService, ApplicationEventPublisher events) {
        this.urlService = urlService;
        this.events = events;
    }

    /** WRITE path (rare). Create a short link. */
    @PostMapping("/api/shorten")
    public ResponseEntity<ShortenResponse> shorten(@Valid @RequestBody ShortenRequest request) {
        UrlMapping mapping = urlService.shorten(request.url(), request.customAlias());
        ShortenResponse body = new ShortenResponse(
                mapping.getShortCode(),
                urlService.buildShortUrl(mapping.getShortCode()),
                mapping.getLongUrl());
        return ResponseEntity.status(HttpStatus.CREATED).body(body);
    }

    /**
     * READ path (the hot path). Resolve a code and 302-redirect.
     *
     * We use 302 FOUND, not 301 MOVED PERMANENTLY: 301 lets browsers cache the
     * redirect and skip our server on repeat clicks, which would hide analytics.
     * 302 keeps every click flowing through us. Cost: higher redirect load —
     * which is exactly why the read path is cached and the analytics write is async.
     *
     * The click event is published and returns immediately; recording it happens
     * off-thread and never delays the redirect.
     */
    @GetMapping("/{shortCode:[0-9A-Za-z]{3,16}}")
    public ResponseEntity<Void> redirect(@PathVariable String shortCode,
                                         HttpServletRequest httpRequest) {
        Optional<String> longUrl = urlService.resolve(shortCode);
        if (longUrl.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "unknown short code");
        }

        events.publishEvent(new ClickRecordedEvent(
                shortCode,
                Instant.now(),
                httpRequest.getRemoteAddr(),
                httpRequest.getHeader("User-Agent"),
                httpRequest.getHeader("Referer")));

        return ResponseEntity.status(HttpStatus.FOUND)
                .location(URI.create(longUrl.get()))
                .build();
    }
}

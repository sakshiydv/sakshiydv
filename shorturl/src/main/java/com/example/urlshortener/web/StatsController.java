package com.example.urlshortener.web;

import com.example.urlshortener.repository.ClickEventRepository;
import com.example.urlshortener.repository.UrlMappingRepository;
import com.example.urlshortener.web.dto.StatsResponse;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
public class StatsController {

    private final ClickEventRepository clickRepository;
    private final UrlMappingRepository urlRepository;

    public StatsController(ClickEventRepository clickRepository,
                           UrlMappingRepository urlRepository) {
        this.clickRepository = clickRepository;
        this.urlRepository = urlRepository;
    }

    /** Analytics read. Not on the hot path, so it may query Postgres directly. */
    @GetMapping("/api/stats/{shortCode}")
    public StatsResponse stats(@PathVariable String shortCode) {
        if (!urlRepository.existsByShortCode(shortCode)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "unknown short code");
        }

        long total = clickRepository.countByShortCode(shortCode);
        List<StatsResponse.ReferrerStat> referrers = clickRepository.topReferrers(shortCode).stream()
                .limit(10)
                .map(r -> new StatsResponse.ReferrerStat(r.getReferrer(), r.getClicks()))
                .toList();

        return new StatsResponse(shortCode, total, referrers);
    }
}

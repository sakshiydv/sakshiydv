package com.example.urlshortener.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * @param url         the long URL to shorten (required)
 * @param customAlias optional caller-chosen code; must be unique
 */
public record ShortenRequest(
        @NotBlank
        @Pattern(regexp = "^https?://.+", message = "url must start with http:// or https://")
        @Size(max = 2048)
        String url,

        @Pattern(regexp = "^[0-9A-Za-z]{3,16}$", message = "alias must be 3-16 chars, [0-9A-Za-z]")
        String customAlias
) {
}

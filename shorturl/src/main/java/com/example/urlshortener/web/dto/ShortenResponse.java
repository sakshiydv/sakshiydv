package com.example.urlshortener.web.dto;

public record ShortenResponse(String shortCode, String shortUrl, String longUrl) {
}

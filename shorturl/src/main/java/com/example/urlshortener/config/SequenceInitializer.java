package com.example.urlshortener.config;

import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * Ensures the dedicated short-code sequence exists on startup.
 *
 * We use a sequence separate from the row's primary key so the public short code
 * can be generated BEFORE the row is inserted (letting us write a complete row in
 * one INSERT) and so the public code is not coupled to the internal PK.
 */
@Configuration
public class SequenceInitializer {

    @Bean
    public CommandLineRunner createCodeSequence(JdbcTemplate jdbcTemplate) {
        return args -> jdbcTemplate.execute("CREATE SEQUENCE IF NOT EXISTS code_seq START 1");
    }
}

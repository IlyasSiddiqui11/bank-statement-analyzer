package com.example.bank_statement_analyzer.config;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class DatabaseConfigurationTest {

    @Test
    void convertsRenderPostgresUrlsToJdbcUrls() {
        assertEquals(
                "jdbc:postgresql://example.com:5432/bankdb",
                DatabaseConfiguration.normalizeDatabaseUrl("postgres://example.com:5432/bankdb"));
        assertEquals(
                "jdbc:postgresql://example.com:5432/bankdb",
                DatabaseConfiguration.normalizeDatabaseUrl("postgresql://example.com:5432/bankdb"));
    }

    @Test
    void leavesJdbcUrlsUnchanged() {
        String jdbcUrl = "jdbc:postgresql://example.com:5432/bankdb";

        assertEquals(jdbcUrl, DatabaseConfiguration.normalizeDatabaseUrl(jdbcUrl));
    }
}

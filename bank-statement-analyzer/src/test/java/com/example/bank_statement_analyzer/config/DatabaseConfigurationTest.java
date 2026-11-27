package com.example.bank_statement_analyzer.config;

import com.zaxxer.hikari.HikariDataSource;
import org.springframework.boot.jdbc.autoconfigure.DataSourceProperties;
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

    @Test
    void normalizesUrlBeforeBuildingDataSource() {
        DataSourceProperties properties = new DataSourceProperties();
        properties.setUrl("postgres://example.com:5432/bankdb");

        HikariDataSource dataSource =
                (HikariDataSource) new DatabaseConfiguration().dataSource(properties);
        try {
            assertEquals("jdbc:postgresql://example.com:5432/bankdb", dataSource.getJdbcUrl());
        } finally {
            dataSource.close();
        }
    }
}

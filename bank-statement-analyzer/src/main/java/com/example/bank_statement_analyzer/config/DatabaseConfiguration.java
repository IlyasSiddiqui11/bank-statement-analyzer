package com.example.bank_statement_analyzer.config;

import javax.sql.DataSource;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.jdbc.autoconfigure.DataSourceProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
public class DatabaseConfiguration {

    @Bean
    @ConfigurationProperties("spring.datasource.hikari")
    DataSource dataSource(DataSourceProperties properties) {
        String url = normalizeDatabaseUrl(properties.determineUrl());
        return properties.initializeDataSourceBuilder()
                .url(url)
                .build();
    }

    static String normalizeDatabaseUrl(String url) {
        if (url.startsWith("postgres://")) {
            return "jdbc:postgresql://" + url.substring("postgres://".length());
        }
        if (url.startsWith("postgresql://")) {
            return "jdbc:postgresql://" + url.substring("postgresql://".length());
        }
        return url;
    }
}

package com.rexchain.cinema.repository;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.sql.Connection;

/** Resolves the schema visible to the current JDBC connection without hardcoding a database name. */
@Component
public class DatabaseSchemaResolver {
    private final JdbcTemplate jdbc;

    public DatabaseSchemaResolver(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public String currentSchema() {
        return jdbc.execute((Connection connection) -> {
            String schema = connection.getSchema();
            if (schema != null && !schema.isBlank()) return schema;
            String catalog = connection.getCatalog();
            if (catalog != null && !catalog.isBlank()) return catalog;
            throw new IllegalStateException("Không xác định được schema hiện tại của kết nối JDBC");
        });
    }
}

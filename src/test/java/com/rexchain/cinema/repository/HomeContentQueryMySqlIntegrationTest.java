package com.rexchain.cinema.repository;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/** Opt-in, read-only measurement against the local MySQL development schema. */
@EnabledIfSystemProperty(named = "rex.home.mysql-it", matches = "true")
class HomeContentQueryMySqlIntegrationTest {
    private static final String COLUMNS = "id, active, created_at, description, image_url, label, link_url, sort_order, title";
    private static final String BASELINE = "SELECT " + COLUMNS
            + " FROM cinema_events WHERE active = 1 ORDER BY sort_order ASC, created_at DESC";
    private static final String LIMITED = BASELINE + " LIMIT 4";

    @Test
    void homeContentLimitIsMeasuredOnLocalMySqlWithoutWritingData() throws Exception {
        String url = System.getenv("DB_URL");
        String username = System.getenv("DB_USERNAME");
        String password = System.getenv("DB_PASSWORD");
        assumeTrue(url != null && username != null && password != null,
                "MySQL DEV environment was not loaded");
        assumeTrue(url.matches("jdbc:mysql://(?:localhost|127\\.0\\.0\\.1)(?::3306)?/rex_chain_cinema(?:\\?.*)?"),
                "Only the local rex_chain_cinema development schema is allowed");

        try (Connection connection = DriverManager.getConnection(url, username, password)) {
            assertThat(scalar(connection, "SELECT DATABASE()")).isEqualTo("rex_chain_cinema");
            long activeRows = scalarLong(connection, "SELECT COUNT(*) FROM cinema_events WHERE active = 1");

            // Warm connection/query caches before measuring equivalent projections.
            rows(connection, BASELINE);
            rows(connection, LIMITED);
            Measurement baseline = measure(connection, BASELINE);
            Measurement limited = measure(connection, LIMITED);
            assertThat(baseline.rows()).isEqualTo(activeRows);
            assertThat(limited.rows()).isLessThanOrEqualTo(4);
            assertThat(explainRows(connection, LIMITED)).isGreaterThan(0);

            System.out.printf("MYSQL_HOME_QUERY activeRows=%d withoutLimitMs=%d top4Ms=%d explain=true%n",
                    activeRows, baseline.milliseconds(), limited.milliseconds());
        }
    }

    private static Measurement measure(Connection connection, String query) throws Exception {
        long started = System.nanoTime();
        long rows = rows(connection, query);
        return new Measurement(rows, (System.nanoTime() - started) / 1_000_000);
    }

    private static long rows(Connection connection, String query) throws Exception {
        long rows = 0;
        try (Statement statement = connection.createStatement(); ResultSet result = statement.executeQuery(query)) {
            while (result.next()) rows++;
        }
        return rows;
    }

    private static long explainRows(Connection connection, String query) throws Exception {
        long rows = 0;
        try (Statement statement = connection.createStatement(); ResultSet result = statement.executeQuery("EXPLAIN " + query)) {
            while (result.next()) rows++;
        }
        return rows;
    }

    private static String scalar(Connection connection, String query) throws Exception {
        try (Statement statement = connection.createStatement(); ResultSet result = statement.executeQuery(query)) {
            result.next();
            return result.getString(1);
        }
    }

    private static long scalarLong(Connection connection, String query) throws Exception {
        try (Statement statement = connection.createStatement(); ResultSet result = statement.executeQuery(query)) {
            result.next();
            return result.getLong(1);
        }
    }

    private record Measurement(long rows, long milliseconds) { }
}

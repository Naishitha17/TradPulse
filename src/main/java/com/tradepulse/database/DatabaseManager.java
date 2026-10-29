package com.tradepulse.database;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.stream.Collectors;

public class DatabaseManager {
    private static final Logger log = LoggerFactory.getLogger(DatabaseManager.class);
    private static DatabaseManager instance;

    private final DatabaseConfig config;
    private final HikariDataSource dataSource;

    private DatabaseManager(DatabaseConfig config) {
        this.config = config;
        HikariConfig hikariConfig = new HikariConfig();
        hikariConfig.setJdbcUrl(config.getJdbcUrl());
        if (config.isSqlite()) {
            hikariConfig.setDriverClassName("org.sqlite.JDBC");
            // SQLite connection settings
            hikariConfig.addDataSourceProperty("foreign_keys", "true");
            hikariConfig.addDataSourceProperty("journal_mode", "WAL");
            hikariConfig.addDataSourceProperty("synchronous", "NORMAL");
        } else {
            hikariConfig.setDriverClassName("org.postgresql.Driver");
            hikariConfig.setUsername(config.getUsername());
            hikariConfig.setPassword(config.getPassword());
        }

        hikariConfig.setMaximumPoolSize(config.getMaxPoolSize());
        hikariConfig.setMinimumIdle(1);
        hikariConfig.setIdleTimeout(30000);
        hikariConfig.setConnectionTimeout(10000);
        hikariConfig.setPoolName("TradePulsePool");

        this.dataSource = new HikariDataSource(hikariConfig);
        log.info("Database connection pool initialized with URL: {}", config.getJdbcUrl());

        initSchema();
        DatabaseSeeder.seed(this);
    }

    public static synchronized DatabaseManager getInstance() {
        if (instance == null) {
            instance = new DatabaseManager(new DatabaseConfig());
        }
        return instance;
    }

    public static synchronized void initialize(DatabaseConfig config) {
        if (instance != null) {
            instance.shutdown();
        }
        instance = new DatabaseManager(config);
    }

    public Connection getConnection() throws SQLException {
        Connection conn = dataSource.getConnection();
        if (config.isSqlite()) {
            try (Statement stmt = conn.createStatement()) {
                stmt.execute("PRAGMA foreign_keys = ON;");
            }
        }
        return conn;
    }

    private void initSchema() {
        String schemaFile = config.isSqlite() ? "schema-sqlite.sql" : "schema-postgres.sql";
        String resourcePath = "/com/tradepulse/schema/" + schemaFile;
        log.info("Loading schema migration from {}", resourcePath);

        try (InputStream is = getClass().getResourceAsStream(resourcePath)) {
            if (is == null) {
                log.warn("Schema file {} not found on classpath", resourcePath);
                return;
            }

            String sql = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))
                    .lines()
                    .collect(Collectors.joining("\n"));

            String[] statements = sql.split(";");
            try (Connection conn = getConnection()) {
                conn.setAutoCommit(false);
                try (Statement stmt = conn.createStatement()) {
                    for (String singleSql : statements) {
                        String trimmed = singleSql.trim();
                        if (!trimmed.isEmpty()) {
                            stmt.execute(trimmed);
                        }
                    }
                    conn.commit();
                    log.info("Schema migrations executed successfully.");
                } catch (SQLException e) {
                    conn.rollback();
                    log.error("Failed to execute schema migration: {}", e.getMessage(), e);
                    throw e;
                } finally {
                    conn.setAutoCommit(true);
                }
            }
        } catch (Exception e) {
            log.error("Error initializing database schema", e);
            throw new RuntimeException("Failed to initialize database schema", e);
        }
    }

    public void shutdown() {
        if (dataSource != null && !dataSource.isClosed()) {
            dataSource.close();
            log.info("Database connection pool shut down.");
        }
    }
}

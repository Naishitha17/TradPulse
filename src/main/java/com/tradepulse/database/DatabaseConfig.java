package com.tradepulse.database;

public class DatabaseConfig {
    private final String dbType;
    private final String jdbcUrl;
    private final String username;
    private final String password;
    private final int maxPoolSize;

    public DatabaseConfig() {
        String type = System.getenv("DB_TYPE");
        if (type == null || type.isBlank()) {
            this.dbType = "SQLITE";
            String path = System.getenv("DB_PATH");
            if (path == null || path.isBlank()) {
                path = "tradepulse.db";
            }
            this.jdbcUrl = "jdbc:sqlite:" + path;
            this.username = "";
            this.password = "";
            this.maxPoolSize = 5;
        } else if (type.equalsIgnoreCase("POSTGRES")) {
            this.dbType = "POSTGRES";
            String url = System.getenv("DB_URL");
            this.jdbcUrl = (url != null && !url.isBlank()) ? url : "jdbc:postgresql://localhost:5432/tradepulse";
            this.username = System.getenv("DB_USER") != null ? System.getenv("DB_USER") : "postgres";
            this.password = System.getenv("DB_PASSWORD") != null ? System.getenv("DB_PASSWORD") : "postgres";
            this.maxPoolSize = 10;
        } else {
            this.dbType = "SQLITE";
            this.jdbcUrl = "jdbc:sqlite:tradepulse.db";
            this.username = "";
            this.password = "";
            this.maxPoolSize = 5;
        }
    }

    public DatabaseConfig(String dbType, String jdbcUrl, String username, String password, int maxPoolSize) {
        this.dbType = dbType;
        this.jdbcUrl = jdbcUrl;
        this.username = username;
        this.password = password;
        this.maxPoolSize = maxPoolSize;
    }

    public String getDbType() { return dbType; }
    public String getJdbcUrl() { return jdbcUrl; }
    public String getUsername() { return username; }
    public String getPassword() { return password; }
    public int getMaxPoolSize() { return maxPoolSize; }
    public boolean isSqlite() { return "SQLITE".equalsIgnoreCase(dbType); }
}

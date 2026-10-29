package com.tradepulse;

import com.tradepulse.database.DatabaseConfig;
import com.tradepulse.database.DatabaseManager;

import java.io.File;

public class TestDbHelper {
    public static DatabaseManager createTestDatabase() {
        String testDbPath = "test-tradepulse-" + System.currentTimeMillis() + ".db";
        File dbFile = new File(testDbPath);
        if (dbFile.exists()) {
            dbFile.delete();
        }
        dbFile.deleteOnExit();

        DatabaseConfig config = new DatabaseConfig("SQLITE", "jdbc:sqlite:" + testDbPath, "", "", 5);
        DatabaseManager.initialize(config);
        return DatabaseManager.getInstance();
    }
}

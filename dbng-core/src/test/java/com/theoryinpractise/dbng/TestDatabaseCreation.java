package com.theoryinpractise.dbng;

import org.apache.commons.dbcp.BasicDataSource;
import org.apache.log4j.BasicConfigurator;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.BeforeSuite;
import org.testng.annotations.Test;

import java.sql.SQLException;

public class TestDatabaseCreation {

    String groupId = "com.theoryinpractise.dbng";
    String artifactId = "dbng-examples";
    public static final String DB_ENGINE = "pgsql";
    public static final String DB_NAME = "mytest";
    public static final String DB_USERNAME = "postgres";
    public static final String DB_PASSWORD = "8avana";

    private MigrationManager migrationManager;

    @BeforeSuite
    public void initializeLog4j() {
        BasicConfigurator.configure();
    }

    @BeforeMethod
    public void createFreshDatabase() throws SQLException {
        migrationManager = DatabaseInitializationManagerFactory
                .getInstance(DB_ENGINE)
                .createDatabase(DB_NAME, "", DB_USERNAME, DB_PASSWORD);

        long currentVersion = migrationManager.getCurrentVersion();
        assert currentVersion == 0 : "A Fresh Database should have a version of 0, found: " + currentVersion;
    }

    @AfterMethod
    public void closeTheDatasource() throws SQLException {
        ((BasicDataSource) migrationManager.getDataSource()).close();
    }

    @DataMigration(groupId = "com.theoryinpractise.dbng", artifactId = "test1", version = 1)
    public void createUsersTable(MigrationManager m) {
        m.update("CREATE TABLE users (id serial, username varchar(256))");
    }

    @Test
    public void testTableCreation() throws SQLException, MigrationException {

        long currentVersion = migrationManager.getCurrentVersion();
        assert currentVersion == 0 : "A Fresh Database should have a version of 0, found: " + currentVersion;

        migrationManager.processMigrations("com.theoryinpractise.dbng", "test1", "com.theoryinpractise.*");


        currentVersion = migrationManager.getCurrentVersion();
        assert currentVersion == 1 : "A clean database with test1 migrations should be at version 1, found: " + currentVersion;

    }

    @Test
    public void testRepeatingMigrations() throws SQLException, MigrationException {

        long currentVersion = migrationManager.getCurrentVersion();
        assert currentVersion == 0 : "A Fresh Database should have a version of 0, found: " + currentVersion;

        migrationManager.processMigrations("com.theoryinpractise.dbng", "test1", "com.theoryinpractise.*");

        currentVersion = migrationManager.getCurrentVersion();
        assert currentVersion == 1 : "A clean database with test1 migrations should be at version 1, found: " + currentVersion;

        // Run the migrations again... shouldn't cause any problems as the existing migrations should be skipped
        migrationManager.processMigrations("com.theoryinpractise.dbng", "test1", "com.theoryinpractise.*");

        currentVersion = migrationManager.getCurrentVersion();
        assert currentVersion == 1 : "A database with test1 migrations should be at version 1 regardless of how often the migrations are run, found: " + currentVersion;
    }

    @DataMigration(groupId = "com.theoryinpractise.dbng", artifactId = "createUsersTableWithBadSql", version = 1)
    public void createUsersTableWithBadSql(MigrationManager m) {
        m.update("CREATE TABLE users (id serial, username varchar(256)");
    }

    @Test
    public void testCreateUsersTableWithBadSql() throws SQLException {

        try {
            migrationManager.processMigrations("com.theoryinpractise.dbng", "createUsersTableWithBadSql", "com.theoryinpractise.*");
            assert false : "The SQL in one of the migrations is bad, we should get an exception";
        } catch (MigrationException e) {
            // expecting a rollback
        }

    }

    @DataMigration(groupId = "com.theoryinpractise.dbng", artifactId = "MigrationsWithVersionGaps", version = 1)
    public void createUsersTableForVersionGapTest(MigrationManager m) {
        m.update("CREATE TABLE users (id serial, username varchar(256))");
    }

    @DataMigration(groupId = "com.theoryinpractise.dbng", artifactId = "MigrationsWithVersionGaps", version = 3)
    public void createAccountsTableForVersionGapTest(MigrationManager m) {
        m.update("CREATE TABLE account (id serial, user_id integer, username varchar(256))");
    }

    @Test
    public void testMigrationsWithVersionGap() throws SQLException {
        try {
            migrationManager.processMigrations("com.theoryinpractise.dbng", "MigrationsWithVersionGaps", "com.theoryinpractise.*");
            assert false : "Migration should not occur as theres a version gap for the artifact.";
        } catch (MigrationException e) {
            // expecting an exception due to version gaps
        }

    }

    @DataMigration(groupId = "com.theoryinpractise.dbng", artifactId = "MigrationsWithDuplicateVersion", version = 1)
    public void createUsersTableForMigrationsWithDuplicateVersionTest(MigrationManager m) {
        m.update("CREATE TABLE users (id serial, username varchar(256))");
    }

    @DataMigration(groupId = "com.theoryinpractise.dbng", artifactId = "MigrationsWithDuplicateVersion", version = 1)
    public void createAccountsTableForMigrationsWithDuplicateVersionTest(MigrationManager m) {
        m.update("CREATE TABLE account (id serial, user_id integer, username varchar(256))");
    }

    @Test
    public void testMigrationsWithDuplicateVersion() throws SQLException {
        try {
            migrationManager.processMigrations("com.theoryinpractise.dbng", "MigrationsWithDuplicateVersion", "com.theoryinpractise.*");
            assert false : "Migration should not occur as theres a duplicte version artifact.";
        } catch (MigrationException e) {
            // expecting an exception due to version gaps
        }

    }


}

package com.theoryinpractise.dbng;

public interface DatabaseInitializationManager {
    MigrationManager openDatabase(String dbname, String hostname, String username, String password) throws MigrationException;
    MigrationManager openDatabase(String dbname, String hostname, Integer port, String username, String password) throws MigrationException;
    MigrationManager createDatabase(String dbname, String hostname, String username, String password) throws MigrationException;
    MigrationManager createDatabase(String dbname, String hostname, Integer port, String username, String password) throws MigrationException;
}

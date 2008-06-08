package com.theoryinpractise.dbng;

import java.sql.SQLException;

/**
 * Created by IntelliJ IDEA.
 * User: Mark Derricutt
 * Date: 25/03/2007
 * Time: 14:49:18
 * To change this template use File | Settings | File Templates.
 */
public interface DatabaseInitializationManager {
    MigrationManager createDatabase(String dbname, String hostname, String username, String password) throws MigrationException;
    MigrationManager createDatabase(String dbname, String hostname, Integer port, String username, String password) throws MigrationException;
}

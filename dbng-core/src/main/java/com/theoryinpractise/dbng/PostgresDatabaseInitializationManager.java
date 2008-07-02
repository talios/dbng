package com.theoryinpractise.dbng;

import org.apache.commons.dbcp.BasicDataSource;
import org.apache.log4j.Logger;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.ResultSet;
import java.sql.PreparedStatement;

/**
 * Created by IntelliJ IDEA.
 * User: Mark Derricutt
 * Date: 25/03/2007
 * Time: 14:50:53
 * To change this template use File | Settings | File Templates.
 */
public class PostgresDatabaseInitializationManager implements DatabaseInitializationManager {
    private static final Logger LOG = Logger.getLogger(PostgresDatabaseInitializationManager.class);


    public MigrationManager createDatabase(String dbname, String hostname, String username, String password) throws MigrationException {
        return createDatabase(dbname, hostname, null, username, password);
    }

    public MigrationManager createDatabase(String dbname, String hostname, Integer port, String username, String password) throws MigrationException {


        String driver = "org.postgresql.Driver";
        String url = "jdbc:postgresql:";
        if (hostname != null && !"".equals(hostname)) {
            url += "//" + hostname;

            if (port != null) {
                url += ":" + port;
            }

            url += "/";
        }


        try {
            // Create the database and load up default schema
            BasicDataSource ds = getConnection(driver, url + "template1", username, password);
            Connection conn = ds.getConnection();
            try {
                LOG.info("Creating integration testing database " + dbname + "...");
                conn.prepareStatement("CREATE DATABASE " + dbname + " WITH ENCODING 'UTF-8'").executeUpdate();
            } catch (SQLException e) {
                // Rollback any struck transactions
                PreparedStatement statement = conn.prepareStatement("SELECT gid FROM pg_prepared_xacts WHERE database = ?");
                statement.setString(1, dbname);                    
                ResultSet transactions = statement.executeQuery();
                while (transactions.next()) {
                    String gid = transactions.getString("gid");
                    LOG.info("Rolling back transaction " + gid);
                    conn.prepareStatement("ROLLBACK PREPARED '" + gid + "'").executeUpdate();
                }

                LOG.info("Database exists, dropping and recreating " + dbname + "...");
                conn.prepareStatement("DROP DATABASE " + dbname).executeUpdate();
                conn.prepareStatement("CREATE DATABASE " + dbname + " WITH ENCODING 'UTF-8'").executeUpdate();
            } finally {
                conn.close();
                ds.close();
            }


            MigrationManager migrationManager = new AnnotationMigrationManager(
                    getConnection(driver, url + dbname, username, password)
            );


            migrationManager.update("CREATE TABLE version ( group_id text, artifact_id text, version text, migration_date timestamp )");

            return migrationManager;
        } catch (SQLException e) {
            throw new MigrationException(e.toString());
        } catch (ClassNotFoundException e) {
            throw new MigrationException(e.toString());
        }

    }


    private BasicDataSource getConnection(String driver, String url, String username, String password) throws ClassNotFoundException {
        Class.forName(driver);

        BasicDataSource ds = new BasicDataSource();
        ds.setDriverClassName(driver);
        ds.setUrl(url);
        ds.setUsername(username);
        ds.setPassword(password);

        return ds;
    }


}

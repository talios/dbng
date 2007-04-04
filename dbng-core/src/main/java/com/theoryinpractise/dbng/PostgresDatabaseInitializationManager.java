package com.theoryinpractise.dbng;

import org.apache.commons.dbcp.BasicDataSource;
import org.apache.log4j.Logger;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Date;

/**
 * Created by IntelliJ IDEA.
 * User: Mark Derricutt
 * Date: 25/03/2007
 * Time: 14:50:53
 * To change this template use File | Settings | File Templates.
 */
public class PostgresDatabaseInitializationManager implements DatabaseInitializationManager {
    private static final Logger LOG = Logger.getLogger(PostgresDatabaseInitializationManager.class);


    public MigrationManager createDatabase(String dbname, String hostname, String username, String password) throws SQLException {


        String driver = "org.postgresql.Driver";
        String url = "jdbc:postgresql:";
        if (hostname != null && !"".equals(hostname)) {
            url += "//" + hostname + "/";
        }


        try {
            // Create the database and load up default schema
            BasicDataSource ds = getConnection(driver, url + "template1", username, password);
            Connection conn = ds.getConnection();
            try {
                LOG.info("Creating integration testing database...");
                conn.prepareStatement("CREATE DATABASE " + dbname + " WITH ENCODING 'UTF-8'").executeUpdate();
            } catch (SQLException e) {
                LOG.info("Database exists, dropping and recreating...");

                conn.prepareStatement("DROP DATABASE " + dbname).executeUpdate();
                conn.prepareStatement("CREATE DATABASE " + dbname + " WITH ENCODING 'UTF-8'").executeUpdate();
            } finally {
                conn.close();
                ds.close();
            }


            MigrationManager migrationManager = new AnnotationMigrationManager(
                    getConnection(driver, url + dbname, username, password)
            );


            migrationManager.update("CREATE TABLE version ( version integer, migration_date timestamp )");
            migrationManager.update("INSERT INTO version VALUES (?, ?)", new Object[]{0, new Date()});


            return migrationManager;
        } catch (ClassNotFoundException e) {
            throw new SQLException(e.toString());
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

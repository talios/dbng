package com.theoryinpractise.dbng.migrations;

import com.theoryinpractise.dbng.DataMigration;
import com.theoryinpractise.dbng.MigrationManager;
import org.apache.log4j.Logger;

import java.io.IOException;

/**
 * Hello world!
 */
public class DatabaseCreation {
    private static final Logger LOG = Logger.getLogger(DatabaseCreation.class);

    @DataMigration(groupId = "com.theoryinpractise.dbng", artifactId = "dbng-examples", version = 0)
    public void updateForSomething(MigrationManager manager) throws IOException {
        LOG.info("Creating initial database schema");


        manager.executeSqlFile(
                DatabaseCreation.class.getResourceAsStream("/schema-3.2.4.sql"));


    }


}
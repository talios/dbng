package com.theoryinpractise.dbng;

import org.apache.log4j.BasicConfigurator;

import java.lang.reflect.InvocationTargetException;
import java.sql.SQLException;

/**
 * Created by IntelliJ IDEA.
 * User: Mark Derricutt
 * Date: 24/03/2007
 * Time: 18:51:45
 * To change this template use File | Settings | File Templates.
 */
public class ProcessMigrations {


    public static void main(String[] args) throws IllegalAccessException, InstantiationException, InvocationTargetException, ClassNotFoundException, SQLException, MigrationException {


        BasicConfigurator.configure();

        final String groupId = "com.theoryinpractise.dbng";
        final String artifactId = "dbng-examples";


        DatabaseInitializationManagerFactory
                .getInstance("pgsql")
                .createDatabase("mytest", "", "postgres", "8avana")
                .processMigrations(groupId, artifactId, "com.theoryinpractise.*");


    }

}

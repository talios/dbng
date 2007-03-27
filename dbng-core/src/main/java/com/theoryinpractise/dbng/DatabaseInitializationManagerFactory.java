package com.theoryinpractise.dbng;

/**
 * Created by IntelliJ IDEA.
 * User: Mark Derricutt
 * Date: 25/03/2007
 * Time: 14:48:58
 * To change this template use File | Settings | File Templates.
 */
public class DatabaseInitializationManagerFactory {


    public static DatabaseInitializationManager getInstance(String engine) {

        if ("pgsql".equals(engine)) {
            return new PostgresDatabaseInitializationManager();
        } else {
            throw new UnsupportedOperationException("Unsupported Database Engine");
        }


    }


}

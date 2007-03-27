package com.theoryinpractise.dbng;

/**
 * Created by IntelliJ IDEA.
 * User: Mark Derricutt
 * Date: 27/03/2007
 * Time: 23:03:38
 * To change this template use File | Settings | File Templates.
 */
public class MigrationException extends Exception {
    public MigrationException() {
    }

    public MigrationException(String message) {
        super(message);
    }

    public MigrationException(String message, Throwable cause) {
        super(message, cause);
    }

    public MigrationException(Throwable cause) {
        super(cause);
    }
}

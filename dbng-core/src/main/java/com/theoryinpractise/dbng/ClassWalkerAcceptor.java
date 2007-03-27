package com.theoryinpractise.dbng;

/**
 * Created by IntelliJ IDEA.
 * User: Mark Derricutt
 * Date: 24/03/2007
 * Time: 19:29:38
 * To change this template use File | Settings | File Templates.
 */
public interface ClassWalkerAcceptor {
    boolean accept(Class classInstance);
}

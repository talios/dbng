package com.theoryinpractise.dbng;

import org.apache.maven.artifact.versioning.ArtifactVersion;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.ResultSetExtractor;

import javax.sql.DataSource;
import java.io.IOException;
import java.io.InputStream;

/**
 * Created by IntelliJ IDEA.
 * User: Mark Derricutt
 * Date: 24/03/2007
 * Time: 17:43:27
 * To change this template use File | Settings | File Templates.
 */
public interface MigrationManager {

    int update(String string) throws DataAccessException;

    int update(java.lang.String string, java.lang.Object[] objects) throws DataAccessException;

    Object query(String s, ResultSetExtractor resultSetExtractor) throws DataAccessException;

    Object query(String s, Object[] objects, ResultSetExtractor resultSetExtractor) throws DataAccessException;

    ArtifactVersion getCurrentVersion();

    DataSource getDataSource();

    MigrationManager executeSqlFile(InputStream inputStream) throws DataAccessException, IOException;

    MigrationManager processMigrations(final String groupId, final String artifactId, final String initialPackage) throws MigrationException;

    void info(String string);
}

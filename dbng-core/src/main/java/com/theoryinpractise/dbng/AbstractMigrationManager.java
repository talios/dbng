package com.theoryinpractise.dbng;

import org.apache.log4j.Logger;
import org.apache.maven.artifact.versioning.ArtifactVersion;
import org.apache.maven.artifact.versioning.DefaultArtifactVersion;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.ResultSetExtractor;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import javax.sql.DataSource;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

/**
 * Created by IntelliJ IDEA.
 * User: Mark Derricutt
 * Date: 24/03/2007
 * Time: 20:11:47
 * To change this template use File | Settings | File Templates.
 */
public abstract class AbstractMigrationManager implements MigrationManager {
    private static final Logger LOG = Logger.getLogger(AbstractMigrationManager.class);

    protected JdbcTemplate jdbcTemplate;
    protected TransactionTemplate transactionTemplate;
    protected DataSource dataSource;

    public AbstractMigrationManager(DataSource dataSource) {

        this.dataSource = dataSource;
        this.jdbcTemplate = new JdbcTemplate(dataSource);
        this.transactionTemplate = new TransactionTemplate(
                new DataSourceTransactionManager(dataSource));


    }

    public int update(String string) throws DataAccessException {
        return jdbcTemplate.update(string);
    }

    public int update(String string, Object[] objects) throws DataAccessException {
        return jdbcTemplate.update(string, objects);
    }

    public Object query(String s, ResultSetExtractor resultSetExtractor) throws DataAccessException {
        return jdbcTemplate.query(s, resultSetExtractor);
    }

    public Object query(String s, Object[] objects, ResultSetExtractor resultSetExtractor) throws DataAccessException {
        return jdbcTemplate.query(s, objects, resultSetExtractor);
    }

    public ArtifactVersion getCurrentVersion() {
        return new DefaultArtifactVersion((String) jdbcTemplate.queryForObject("SELECT version FROM version", String.class));
    }

    public DataSource getDataSource() {
        return dataSource;
    }

    public abstract MigrationManager processMigrations(String groupId, String artifactId, String initialPackage) throws MigrationException;

    public MigrationManager executeSqlFile(InputStream inputStream) throws DataAccessException, IOException {

        String schemaSql = getStreamAsString(inputStream);

        String[] schemaStatements = schemaSql.split(";");

        for (String schemaStatement : schemaStatements) {
            jdbcTemplate.update(schemaStatement);
        }

        return this;
    }

    private String getStreamAsString(InputStream is) throws IOException {
        if (is == null) {
            throw new IllegalArgumentException("InputStream should not be null");
        }

        BufferedReader in = new BufferedReader(new InputStreamReader(is));

        String inputLine;
        StringBuilder buffer = new StringBuilder();

        while ((inputLine = in.readLine()) != null) {
            buffer.append(inputLine);
            buffer.append("\n");
        }

        in.close();
        return buffer.toString().trim();

    }

    public void info(String string) {
        LOG.info("    * " + string);
    }
}

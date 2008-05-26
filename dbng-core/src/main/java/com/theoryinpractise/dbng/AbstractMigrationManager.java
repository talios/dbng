package com.theoryinpractise.dbng;

import org.apache.log4j.Logger;
import org.apache.maven.artifact.versioning.ArtifactVersion;
import org.apache.maven.artifact.versioning.DefaultArtifactVersion;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.InvalidDataAccessResourceUsageException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.ResultSetExtractor;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import javax.sql.DataSource;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.LineNumberReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

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

    public ArtifactVersion getCurrentVersion(String groupId, String artifactId) {
        List<String> versionNumbers = jdbcTemplate.queryForList(
                "SELECT version FROM version WHERE group_id = ? AND artifact_id = ?",
                new Object[]{groupId, artifactId}, String.class);

        List<ArtifactVersion> versions = new ArrayList<ArtifactVersion>();
        for (String versionNumber : versionNumbers) {
            versions.add(new DefaultArtifactVersion(versionNumber));
        }

        Collections.sort(versions);

        return versions.isEmpty()
                ? new DefaultArtifactVersion("0")
                : versions.get(versions.size() - 1);
    }

    public DataSource getDataSource() {
        return dataSource;
    }

    public abstract MigrationManager processMigrations(String groupId, String artifactId, String initialPackage) throws MigrationException;

    public MigrationManager executeSqlFile(InputStream inputStream) throws DataAccessException, IOException {

        processStreamByLine(inputStream, new LineReader() {
            public void run(int lineNumber, String line) {
                try {
                    jdbcTemplate.update(line);
                } catch (Exception e) {
                    throw new InvalidDataAccessResourceUsageException("Error on line " + lineNumber + ": " + e.getMessage());
                }
            }
        });


        return this;
    }

    private interface LineReader {
        void run(int lineNumber, String line);
    }

    private void processStreamByLine(InputStream is, LineReader runner) throws IOException {
        if (is == null) {
            throw new IllegalArgumentException("InputStream should not be null");
        }

        LineNumberReader in = new LineNumberReader(new InputStreamReader(is));

        String inputLine;
        int startingLineNumber = 1;
        StringBuilder buffer = new StringBuilder();

        while ((inputLine = in.readLine()) != null) {
            if (!inputLine.startsWith("--") && !"".equals(inputLine.trim())) {

                if (buffer.length() == 0) {
                    startingLineNumber = in.getLineNumber();
                }

                buffer.append(inputLine);
                buffer.append("\n");

            }

            if (buffer.toString().trim().endsWith(";")) {
                runner.run(startingLineNumber, buffer.toString().trim());
                buffer = new StringBuilder();
            }

        }

        runner.run(startingLineNumber, buffer.toString().trim());
        in.close();

    }

    public void info(String string) {
        LOG.info("    * " + string);
    }
}

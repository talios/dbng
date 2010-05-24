package com.theoryinpractise.dbng;

import org.apache.log4j.Logger;
import org.apache.maven.artifact.versioning.ArtifactVersion;
import org.apache.maven.artifact.versioning.DefaultArtifactVersion;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.InvalidDataAccessResourceUsageException;
import org.springframework.jdbc.core.ConnectionCallback;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.ResultSetExtractor;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import javax.sql.DataSource;
import java.io.*;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
                "SELECT version FROM public.version WHERE group_id = ? AND artifact_id = ?",
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

    public MigrationManager executeSqlFile(final File file) throws DataAccessException {
        try {
            return executeSqlFile(file.getName(), new FileInputStream(file));
        } catch (FileNotFoundException e) {
            throw new InvalidDataAccessResourceUsageException(e.getMessage());
        }
    }

    public MigrationManager executeSqlFile(final InputStream inputStream) throws DataAccessException {
        return executeSqlFile("unknown", inputStream);
    }

    public MigrationManager executeSqlFile(final String id, final InputStream inputStream) throws DataAccessException {
        jdbcTemplate.execute(new ConnectionCallback() {
            public Object doInConnection(final Connection con) {
                try {
                    boolean autoCommit = con.getAutoCommit();
                    con.setAutoCommit(false);
                    processStreamByLine(inputStream, new LineReader() {
                        public void run(int lineNumber, String line) {
                            try {
                                con.prepareStatement(line).execute();
                            } catch (Exception e) {
                                throw new InvalidDataAccessResourceUsageException(id + ":" + lineNumber + ": " + e.getMessage());
                            }
                        }
                    });
                    con.setAutoCommit(autoCommit);
                    return null;
                } catch (IOException e) {
                    throw new InvalidDataAccessResourceUsageException(e.getMessage());
                } catch (SQLException e) {
                    throw new InvalidDataAccessResourceUsageException(e.getMessage());
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

            if (hasMatchingQuotes(buffer) && buffer.toString().trim().endsWith(";")) {
                runner.run(startingLineNumber, buffer.toString().trim());
                buffer = new StringBuilder();
            }

        }

        runner.run(startingLineNumber, buffer.toString().trim());
        in.close();

    }

    public boolean hasMatchingQuotes(CharSequence s) {
        return countQuotes(s) % 2 == 0;
    }

    public static int countQuotes(CharSequence s) {
        int count = 0;
        final Matcher matcher = Pattern.compile("(\\$\\$|'|\")").matcher(s);
        while (matcher.find()) {
            count++;
        }
        return count;
    }

    public static void main(String[] args) {

        System.out.println(  countQuotes("Hello $$ world $$"));

    }


    public void info(String string) {
        LOG.info("    * " + string);
    }
}

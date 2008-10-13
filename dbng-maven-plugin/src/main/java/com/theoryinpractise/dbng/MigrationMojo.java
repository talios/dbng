package com.theoryinpractise.dbng;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;

import java.io.File;
import java.io.IOException;
import static java.text.MessageFormat.format;

/**
 * Creates and migrates a database through a series of sql files, or migration classes
 *
 * @goal migrate
 * @phase test
 */
public class MigrationMojo extends AbstractMojo {

    /**
     * The groupId to use for processing migrations, this defaults to the projects groupId
     *
     * @parameter expression="${project.groupId}"
     * @required
     */
    private String groupId;

    /**
     * The artifactId to use for processing migrations, this defaults to the projects artifactId
     *
     * @parameter expression="${project.artifactId}"
     * @required
     */
    private String artifactId;

    /**
     * The version to use for processing migrations, this defaults to the projects version
     *
     * @parameter expression="${project.version}"
     * @required
     */
    private String version;

    /**
     * A list of SQL files to execute after creating a fresh database
     *
     * @parameter
     */
    private File[] files;

    /**
     * Should we execute any migration classes found in the project?
     *
     * @parameter default-value="false"
     */
    private boolean processMigrations;

    /**
     * Should we drop/create the database whenever we run?
     *
     * @parameter default-value="false"
     */
    private boolean createDatabase;

    /**
     * What database engine are we using?
     *
     * @parameter default-value="pgsql"
     */
    private String engine;

    /**
     * What username to use?
     *
     * @parameter
     * @required
     */
    private String username;

    /**
     * What password to use?
     *
     * @parameter
     * @required
     */
    private String password;

    /**
     * What database name to use?
     *
     * @parameter expression="${project.artifactId}
     * @required
     */
    private String databaseName;

    /**
     * What hostname name to use?
     *
     * @parameter default-value=""
     *
     */
    private String hostName;

    /**
     * @parameter expression="${project.groupId}     
     */
    private String basePackage;

    public void execute() throws MojoExecutionException {

        try {
            System.out.println(format("Getting {0} database factory", engine));

            DatabaseInitializationManager factory = DatabaseInitializationManagerFactory.getInstance(engine);

            if (createDatabase) {

                String translatedDatabaseName = databaseName.replaceAll("-", "_");
                getLog().info(format("Dropping and recreating {0} database", translatedDatabaseName));
                MigrationManager database = factory.createDatabase(translatedDatabaseName, hostName, username, password);

                if (files != null) {
                    getLog().info(format("Executing {0} files.", files.length));
                    for (File file : files) {
                        getLog().info(format("Executing {0}", file.getPath()));
                        database.executeSqlFile(file);
                    }
                }

                if (processMigrations) {
                    getLog().info(format("Dropping and recreating {0} database", translatedDatabaseName));
                    database.processMigrations(groupId, artifactId, basePackage);
                }

            } else {
                getLog().info("Skipping database creation.");
            }
        } catch (MigrationException e) {
            throw new MojoExecutionException(e.getMessage());
        } catch (IOException e) {
            throw new MojoExecutionException(e.getMessage());
        }
    }
}

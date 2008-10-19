package com.theoryinpractise.dbng;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.codehaus.classworlds.ClassWorld;
import org.codehaus.classworlds.DuplicateRealmException;
import org.codehaus.classworlds.ClassRealm;

import java.io.File;
import java.io.IOException;
import static java.text.MessageFormat.format;
import java.util.Iterator;
import java.net.URL;
import java.net.MalformedURLException;

/**
 * Creates and migrates a database through a series of sql files, or migration classes
 *
 * @goal migrate
 * @phase generate-test-resources
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
     */
    private String hostName;

    /**
     * @parameter expression="${project.groupId}
     */
    private String basePackage;

    /**
     * @parameter expression="${project.build.outputDirectory}
     */
    private File outputDirectory;

    public void execute() throws MojoExecutionException {

        extendRealmClasspath();

        try {
            getLog().info(format("Getting {0} database factory", engine));

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
                    getLog().info(format("Processing migrations for {0}:{1}/{2}", groupId, artifactId, basePackage));
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

    /**
     * This will prepare the current ClassLoader and add all jars and local
     * classpaths (e.g. target/classes) needed by the dbng task.
     *
     * @throws MojoExecutionException
     */
    protected void extendRealmClasspath()
        throws MojoExecutionException {
        ClassWorld world = new ClassWorld();
        ClassRealm realm;

        try {
            realm = world.newRealm("dbng.maven.plugin", Thread.currentThread().getContextClassLoader());
        } catch (DuplicateRealmException e) {
            throw new MojoExecutionException("problem while creating new ClassRealm", e);
        }


        getLog().debug("adding classpathElement " + outputDirectory.getPath());
        try {
            // we need to use 3 slashes to prevent windoof from interpreting 'file://D:/path' as server 'D'
            // we also have to add a trailing slash after directory paths
            URL url = new URL("file:///" + outputDirectory.getPath() + (outputDirectory.isDirectory() ? "/" : ""));
            realm.addConstituent(url);
        } catch (MalformedURLException e) {
            throw new MojoExecutionException("error in adding the classpath " + outputDirectory, e);
        }

        // set the new ClassLoader as default for this Thread
        Thread.currentThread().setContextClassLoader(realm.getClassLoader());
    }
}

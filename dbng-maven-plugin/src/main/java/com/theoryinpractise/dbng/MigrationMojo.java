package com.theoryinpractise.dbng;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import org.apache.log4j.BasicConfigurator;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.codehaus.classworlds.ClassRealm;
import org.codehaus.classworlds.ClassWorld;
import org.codehaus.classworlds.DuplicateRealmException;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;

import static java.text.MessageFormat.format;

/**
 * Creates and migrates a database through a series of sql files, or migration classes
 */
@Mojo(name = "migrate", defaultPhase = LifecyclePhase.PROCESS_CLASSES, requiresDependencyResolution = ResolutionScope.COMPILE)
public class MigrationMojo extends AbstractMojo {

    /**
     * The groupId to use for processing migrations, this defaults to the projects groupId
     */
    @Parameter(defaultValue = "${project.groupId}", required = true)
    private String groupId;

    /**
     * The artifactId to use for processing migrations, this defaults to the projects artifactId
     */
    @Parameter(defaultValue = "${project.artifactId}", required = true)
    private String artifactId;

    /**
     * The version to use for processing migrations, this defaults to the projects version
     */
    @Parameter(defaultValue = "${project.version}", required = true)
    private String version;

    /**
     * A list of SQL files to execute after creating a fresh database
     */
    @Parameter
    private File[] files;

    /**
     * A YAML file containing a list of SQL files to execute after creating a fresh database
     */
    @Parameter
    private File manifestFile;

    /**
     * Should we execute any migration classes found in the project?
     */
    @Parameter(defaultValue = "false")
    private boolean processMigrations;

    /**
     * Should we drop/create the database whenever we run?
     */
    @Parameter(defaultValue = "false")
    private boolean createDatabase;

    /**
     * What database engine are we using?
     */
    @Parameter(defaultValue = "pgsql")
    private String engine;

    /**
     * What username to use?
     */
    @Parameter(required = true)
    private String username;

    /**
     * What password to use?
     */
    @Parameter(required = true)
    private String password;

    /**
     * What database name to use?
     */
    @Parameter(defaultValue = "${project.artifactId}", required = true)
    private String databaseName;

    /**
     * What hostname name to use?
     */
    @Parameter(defaultValue = "")
    private String hostName;

    @Parameter(defaultValue = "${project.groupId}.*")
    private String basePackage;

    @Parameter(defaultValue = "${project.build.outputDirectory}")
    private File outputDirectory;

    public void execute() throws MojoExecutionException {

        BasicConfigurator.configure();

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

                if (manifestFile != null) {
                    if (manifestFile.exists()) {
                        getLog().info(format("Loading manifest file {0}", manifestFile.getPath()));
                        ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
                        List<String> files = mapper.readValue(manifestFile, List.class);
                        for (String fileName : files) {
                            File file = new File(manifestFile.getParentFile(), fileName);
                            getLog().info(format("Executing {0}", file.getPath()));
                            database.executeSqlFile(file);
                        }
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

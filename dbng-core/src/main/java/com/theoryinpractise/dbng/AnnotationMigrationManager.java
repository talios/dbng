package com.theoryinpractise.dbng;

import org.apache.log4j.Logger;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;

import javax.sql.DataSource;
import java.lang.reflect.Method;
import java.util.*;

/**
 * Created by IntelliJ IDEA.
 * User: Mark Derricutt
 * Date: 24/03/2007
 * Time: 20:00:29
 * To change this template use File | Settings | File Templates.
 */
public class AnnotationMigrationManager extends AbstractMigrationManager {
    private static final Logger LOG = Logger.getLogger(AnnotationMigrationManager.class);

    public AnnotationMigrationManager(DataSource dataSource) {
        super(dataSource);
    }

    public MigrationManager processMigrations(final String groupId, final String artifactId, final String initialPackage) throws MigrationException {

        Set<Class> classes = ClassWalker.findMigrationClassesInPackage(initialPackage, new ClassWalkerAcceptor() {
            public boolean accept(Class classInstance) {
                for (Method classMethod : classInstance.getMethods()) {
                    if (classMethod.isAnnotationPresent(DataMigration.class)) {
                        DataMigration dataMigration = classMethod.getAnnotation(DataMigration.class);
                        if (groupId.equals(dataMigration.groupId()) && artifactId.equals(dataMigration.artifactId())) {
                            return true;
                        }
                    }
                }

                return false;
            }
        });

        final Set<MethodMigrationContainer> migrationContainers = new TreeSet<MethodMigrationContainer>(new Comparator<MethodMigrationContainer>() {
            public int compare(MethodMigrationContainer m1, MethodMigrationContainer m2) {
                return new Long(m1.getDataMigration().version()).compareTo(m2.getDataMigration().version());
            }
        });

        // Find current version number
        final long initialVersion = jdbcTemplate.queryForLong("SELECT version FROM version");

        final Map<Class, Object> instances = new HashMap<Class, Object>();

        for (Class aClass : classes) {
            for (Method classMethod : aClass.getMethods()) {
                if (classMethod.isAnnotationPresent(DataMigration.class)) {
                    DataMigration dataMigration = classMethod.getAnnotation(DataMigration.class);
                    if (groupId.equals(dataMigration.groupId()) && artifactId.equals(dataMigration.artifactId()) && initialVersion < dataMigration.version()) {
                        try {
                            if (!instances.containsKey(aClass)) {
                                instances.put(aClass, aClass.newInstance());
                            }


                            MethodMigrationContainer migrationContainer = new MethodMigrationContainer(aClass, classMethod, dataMigration);
                            if (!migrationContainers.contains(migrationContainer)) {
                                migrationContainers.add(migrationContainer);
                            } else {
                                throw new MigrationException("Invalid migration found: " + migrationContainer.toString());
                            }

                        } catch (InstantiationException e) {
                            LOG.error(e.getMessage(), e);
                        } catch (IllegalAccessException e) {
                            LOG.error(e.getMessage(), e);
                        }
                    }
                }
            }
        }

        checkContiguousMigrations(migrationContainers);

        LOG.info("Current database version is " + initialVersion + ", found " + migrationContainers.size() + " migrations to process.");

        Long newVersion = (Long) transactionTemplate.execute(new TransactionCallback() {
            public Object doInTransaction(TransactionStatus transactionStatus) {

                long currentVersion = initialVersion;

                try {
                    for (MethodMigrationContainer container : migrationContainers) {
                        LOG.info("Processing migration " + container.toString());

                        Object o = instances.get(container.getClazz());
                        container.getMethod().invoke(o, AnnotationMigrationManager.this);
                        currentVersion = container.getDataMigration().version();
                        jdbcTemplate.update("UPDATE version SET version = ?, migration_date = ?", new Object[]{currentVersion, new Date()});
                    }

                    return currentVersion;
                } catch (Exception e) {
                    LOG.warn("Data migration failed - all migrations will be rolled back", e);
                    transactionStatus.setRollbackOnly();
                    return null;
                }
            }
        });

        if (newVersion == null) {
            throw new MigrationException("Transaction was rolled back due to errors");
        } else {
            LOG.info("Migration complete, database version is now: " + newVersion);
        }

        return this;
    }

    /**
     * checkContiguousMigrations does a simple linear check of migration version numbers to check that the available
     * migrations don't skip, or duplicate any version numbers.
     *
     * @param migrationContainers
     * @throws MigrationException
     */
    private void checkContiguousMigrations(Set<MethodMigrationContainer> migrationContainers) throws MigrationException {
        long currentVersion = getCurrentVersion();
        System.out.println("checking " + migrationContainers.size() + " migrations");
        for (MethodMigrationContainer migrationContainer : migrationContainers) {
            currentVersion += 1;
            System.out.println(currentVersion + " --- " + migrationContainer.getDataMigration().version());
            if (currentVersion != migrationContainer.getDataMigration().version()) {
                throw new MigrationException("Unexpected version number found ("
                        + migrationContainer.getDataMigration().version() + ") in method "
                        + migrationContainer.getClazz().getName() + "#" + migrationContainer.getMethod().getName());
            }
        }
    }

    public class MethodMigrationContainer {
        private Class clazz;
        private Method method;
        private DataMigration dataMigration;

        public MethodMigrationContainer(Class clazz, Method method, DataMigration dataMigration) {
            this.clazz = clazz;
            this.method = method;
            this.dataMigration = dataMigration;
        }

        public Class getClazz() {
            return clazz;
        }

        public void setClazz(Class clazz) {
            this.clazz = clazz;
        }

        public Method getMethod() {
            return method;
        }

        public void setMethod(Method method) {
            this.method = method;
        }

        public DataMigration getDataMigration() {
            return dataMigration;
        }

        public void setDataMigration(DataMigration dataMigration) {
            this.dataMigration = dataMigration;
        }

        @Override
        public String toString() {
            return dataMigration.version() + ":" + clazz.getName() + "#" + method.getName();

        }
    }

}

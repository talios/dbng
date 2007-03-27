package com.theoryinpractise.dbng;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.RUNTIME)
public @interface DataMigration {
    long version();

    String groupId();

    String artifactId();

    String description() default "";
}

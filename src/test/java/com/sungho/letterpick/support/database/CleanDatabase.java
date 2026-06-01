package com.sungho.letterpick.support.database;

import org.junit.jupiter.api.extension.ExtendWith;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@ExtendWith(CleanDatabaseExtension.class)
public @interface CleanDatabase {

    String[] value() default {};

    String[] exclude() default {
            "flyway_schema_history"
    };
}

package com.sungho.letterpick;

import com.sungho.letterpick.common.config.JpaAuditingConfig;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

@Target(TYPE)
@Retention(RUNTIME)
@DataJpaTest
@Import(JpaAuditingConfig.class)
public @interface LetterPickDataJpaTest {
}

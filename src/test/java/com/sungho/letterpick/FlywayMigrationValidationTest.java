package com.sungho.letterpick;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

@Import(LetterPickTestConfiguration.class)
@SpringBootTest
@ActiveProfiles("test")
@TestPropertySource(properties = {
        "spring.flyway.enabled=true",
        "spring.flyway.locations=classpath:db/migration",
        "spring.jpa.hibernate.ddl-auto=validate"
})
class FlywayMigrationValidationTest {

    @Test
    void contextLoadsWithFlywayMigrationAndHibernateValidation() {
    }
}

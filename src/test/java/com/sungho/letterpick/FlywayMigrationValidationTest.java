package com.sungho.letterpick;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import static org.assertj.core.api.Assertions.assertThat;

@Import(LetterPickTestConfiguration.class)
@SpringBootTest
@ActiveProfiles("test")
@TestPropertySource(properties = {
        "spring.flyway.enabled=true",
        "spring.flyway.locations=classpath:db/migration",
        "spring.jpa.hibernate.ddl-auto=validate"
})
class FlywayMigrationValidationTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void contextLoadsWithFlywayMigrationAndHibernateValidation() {
        Integer newsletterCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM newsletter",
                Integer.class
        );
        String moneyNeverSleepImageUrl = jdbcTemplate.queryForObject(
                "SELECT image_url FROM newsletter WHERE email = ?",
                String.class,
                "snowballlabs.official-gmail.com@send.stibee.com"
        );

        assertThat(newsletterCount).isEqualTo(53);
        assertThat(moneyNeverSleepImageUrl).isEqualTo("https://img.stibee.com/a91f6d85-092d-41b0-ba28-a16bf0857990.jpg");
    }
}

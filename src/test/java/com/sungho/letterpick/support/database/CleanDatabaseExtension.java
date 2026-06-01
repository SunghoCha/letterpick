package com.sungho.letterpick.support.database;

import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

public class CleanDatabaseExtension implements BeforeEachCallback {

    private static final Pattern TABLE_NAME_PATTERN = Pattern.compile("[A-Za-z0-9_]+");

    @Override
    public void beforeEach(ExtensionContext context) {
        CleanDatabase cleanDatabase = getCleanDatabase(context);
        JdbcTemplate jdbcTemplate = SpringExtension.getApplicationContext(context).getBean(JdbcTemplate.class);
        List<String> tableNames = resolveTableNames(jdbcTemplate, cleanDatabase);

        jdbcTemplate.execute("SET FOREIGN_KEY_CHECKS = 0");
        try {
            tableNames.stream()
                    .map(this::quoteTableName)
                    .forEach(tableName -> jdbcTemplate.execute("TRUNCATE TABLE " + tableName));
        } finally {
            jdbcTemplate.execute("SET FOREIGN_KEY_CHECKS = 1");
        }
    }

    private List<String> resolveTableNames(JdbcTemplate jdbcTemplate, CleanDatabase cleanDatabase) {
        if (cleanDatabase.value().length > 0) {
            return Arrays.asList(cleanDatabase.value());
        }

        List<String> excludedTableNames = Arrays.asList(cleanDatabase.exclude());

        return jdbcTemplate.queryForList("""
                SELECT table_name
                FROM information_schema.tables
                WHERE table_schema = DATABASE()
                    AND table_type = 'BASE TABLE'
                ORDER BY table_name
                """, String.class)
                .stream()
                .filter(tableName -> !excludedTableNames.contains(tableName))
                .toList();
    }

    private CleanDatabase getCleanDatabase(ExtensionContext context) {
        CleanDatabase cleanDatabase = context.getRequiredTestClass().getAnnotation(CleanDatabase.class);
        if (cleanDatabase == null) {
            throw new IllegalStateException("@CleanDatabase annotation is missing.");
        }

        return cleanDatabase;
    }

    private String quoteTableName(String tableName) {
        if (!TABLE_NAME_PATTERN.matcher(tableName).matches()) {
            throw new IllegalArgumentException("Unsupported table name: " + tableName);
        }

        return "`" + tableName + "`";
    }
}

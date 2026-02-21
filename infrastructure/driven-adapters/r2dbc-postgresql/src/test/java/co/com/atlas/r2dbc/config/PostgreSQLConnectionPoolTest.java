package co.com.atlas.r2dbc.config;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

/**
 * DISABLED: PostgreSQLConnectionPool and PostgresqlConnectionProperties classes
 * were removed during migration from PostgreSQL to MySQL R2DBC.
 * This test references non-existent classes and needs to be rewritten
 * for the current MySQL-based connection pool configuration.
 */
@Disabled("Classes PostgreSQLConnectionPool and PostgresqlConnectionProperties no longer exist after MySQL migration")
class PostgreSQLConnectionPoolTest {

    @Test
    void placeholderTest() {
        // Placeholder to prevent empty test class warning
    }
}

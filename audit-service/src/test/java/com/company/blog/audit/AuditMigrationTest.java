package com.company.blog.audit;

import static org.assertj.core.api.Assertions.assertThat;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;

class AuditMigrationTest {
    @Test
    void createsAuditRecordTable() throws Exception {
        String url = "jdbc:h2:mem:audit_migration;MODE=PostgreSQL;DB_CLOSE_DELAY=-1";
        Flyway.configure().dataSource(url, "sa", "").locations("classpath:db/migration").load().migrate();

        try (Connection connection = DriverManager.getConnection(url, "sa", "");
             ResultSet columns = connection.getMetaData().getColumns(null, null, "AUDIT_RECORD", null)) {
            java.util.Set<String> names = new java.util.HashSet<>();
            while (columns.next()) {
                names.add(columns.getString("COLUMN_NAME"));
            }
            assertThat(names).contains("EVENT_ID", "ACTOR_ID", "ACTION", "RESOURCE_ID", "OCCURRED_AT");
        }
    }
}

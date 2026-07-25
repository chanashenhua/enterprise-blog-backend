package com.company.blog.tag;

import static org.assertj.core.api.Assertions.assertThat;

import com.company.blog.tag.api.JdbcCatalogRepository;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

class JdbcCatalogRepositoryTest {
    @Test
    void persistsUpdatesAndDeactivatesCatalogItems() {
        DriverManagerDataSource dataSource = new DriverManagerDataSource(
                "jdbc:h2:mem:catalog_repository;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
                "sa",
                ""
        );
        Flyway.configure()
                .dataSource(dataSource)
                .locations("classpath:db/migration")
                .load()
                .migrate();
        JdbcCatalogRepository repository = new JdbcCatalogRepository(new JdbcTemplate(dataSource));

        repository.create(CatalogType.TAG, "kafka", "Kafka");
        assertThat(repository.update(CatalogType.TAG, "kafka", "Apache Kafka")).get()
                .extracting(CatalogItem::name)
                .isEqualTo("Apache Kafka");
        assertThat(repository.deactivate(CatalogType.TAG, "kafka")).get()
                .extracting(CatalogItem::active)
                .isEqualTo(false);
        assertThat(repository.findAll(CatalogType.TAG, false))
                .extracting(CatalogItem::id)
                .doesNotContain("kafka");
        assertThat(repository.findAll(CatalogType.TAG, true))
                .extracting(CatalogItem::id)
                .contains("kafka");
    }
}

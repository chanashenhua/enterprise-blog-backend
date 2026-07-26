package com.company.blog.stats;

import static org.assertj.core.api.Assertions.assertThat;

import com.company.blog.stats.api.InteractionSnapshot;
import com.company.blog.stats.api.JdbcInteractionRepository;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

class JdbcInteractionRepositoryTest {
    @Test
    void migrationAndRepositoryPersistDeduplicatedInteractions() {
        DriverManagerDataSource dataSource = new DriverManagerDataSource(
                "jdbc:h2:mem:stats_repository;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
                "sa",
                ""
        );
        Flyway.configure()
                .dataSource(dataSource)
                .locations("classpath:db/migration")
                .load()
                .migrate();
        JdbcInteractionRepository repository = new JdbcInteractionRepository(
                new org.springframework.jdbc.core.JdbcTemplate(dataSource)
        );

        assertThat(repository.add("a-1", "u-reader", InteractionType.VIEW)).isTrue();
        assertThat(repository.add("a-1", "u-reader", InteractionType.VIEW)).isFalse();
        repository.add("a-1", "u-author", InteractionType.VIEW);
        repository.add("a-1", "u-reader", InteractionType.LIKE);
        repository.add("a-1", "u-reader", InteractionType.FAVORITE);

        InteractionSnapshot initial = repository.snapshot("a-1", "u-reader");
        assertThat(initial.viewCount()).isEqualTo(2);
        assertThat(initial.likeCount()).isEqualTo(1);
        assertThat(initial.favoriteCount()).isEqualTo(1);
        assertThat(initial.liked()).isTrue();
        assertThat(initial.favorited()).isTrue();

        assertThat(repository.remove("a-1", "u-reader", InteractionType.LIKE)).isTrue();
        assertThat(repository.remove("a-1", "u-reader", InteractionType.LIKE)).isFalse();
        assertThat(repository.snapshot("a-1", "u-reader").liked()).isFalse();
    }
}

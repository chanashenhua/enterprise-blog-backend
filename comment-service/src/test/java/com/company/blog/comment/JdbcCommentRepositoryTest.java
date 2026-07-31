package com.company.blog.comment;

import static org.assertj.core.api.Assertions.assertThat;

import com.company.blog.comment.api.JdbcCommentRepository;
import java.time.Instant;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

class JdbcCommentRepositoryTest {
    @Test
    void migrationAndRepositoryPreserveRepliesAfterSoftDelete() {
        DriverManagerDataSource dataSource = new DriverManagerDataSource(
                "jdbc:h2:mem:comment_repository;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
                "sa",
                ""
        );
        Flyway.configure()
                .dataSource(dataSource)
                .locations("classpath:db/migration")
                .load()
                .migrate();
        JdbcTemplate jdbc = new JdbcTemplate(dataSource);
        JdbcCommentRepository repository = new JdbcCommentRepository(jdbc);
        Instant now = Instant.now();
        Comment root = repository.save(new Comment(
                "c-root",
                "a-1",
                null,
                "u-author",
                "Root",
                CommentStatus.ACTIVE,
                now,
                now
        ));
        repository.save(new Comment(
                "c-reply",
                "a-1",
                root.id(),
                "u-reader",
                "Reply",
                CommentStatus.ACTIVE,
                now.plusSeconds(1),
                now.plusSeconds(1)
        ));

        assertThat(jdbc.queryForObject("select count(*) from blog_comment", Integer.class)).isEqualTo(2);
        assertThat(repository.updateContent("c-reply", "Edited")).get()
                .extracting(Comment::content)
                .isEqualTo("Edited");
        assertThat(repository.softDelete(root.id())).get()
                .extracting(Comment::deleted)
                .isEqualTo(true);
        assertThat(repository.findByArticleId("a-1"))
                .extracting(Comment::id)
                .containsExactly("c-root", "c-reply");

        assertThat(repository.hide("c-reply")).get().extracting(Comment::hidden).isEqualTo(true);
        assertThat(repository.search(new com.company.blog.comment.api.AdminCommentQuery(
                "a-1", "u-reader", CommentStatus.HIDDEN, 20
        ))).extracting(Comment::id).containsExactly("c-reply");
        assertThat(repository.restore("c-reply")).get().extracting(Comment::active).isEqualTo(true);
    }
}

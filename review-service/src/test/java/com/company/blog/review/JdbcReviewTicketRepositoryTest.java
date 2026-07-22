package com.company.blog.review;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.company.blog.review.api.CreateReviewTicketRequest;
import com.company.blog.review.api.JdbcReviewTicketRepository;
import com.company.blog.review.api.ReviewTicketRepository;
import com.company.blog.review.api.ReviewTicketStateConflictException;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

class JdbcReviewTicketRepositoryTest {
    @Test
    void persistsAndUpdatesReviewTicketInDatabase() {
        DriverManagerDataSource dataSource = new DriverManagerDataSource(
                "jdbc:h2:mem:review_ticket_repository;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
                "sa",
                ""
        );
        Flyway.configure()
                .dataSource(dataSource)
                .locations("classpath:db/migration")
                .load()
                .migrate();
        JdbcReviewTicketRepository repository = new JdbcReviewTicketRepository(new JdbcTemplate(dataSource));

        ReviewTicket saved = repository.save(ReviewTicketRepository.create(new CreateReviewTicketRequest(
                "a-1",
                "rr-a-1",
                "u-author",
                "team",
                Set.of("t-search")
        )));
        ReviewTicket approved = repository.updateStatus(saved.id(), ReviewTicketStatus.APPROVED);

        assertThat(repository.findById(saved.id())).contains(approved);
        assertThat(approved.status()).isEqualTo(ReviewTicketStatus.APPROVED);
    }

    @Test
    void locksOnlyPendingTicketForTransition() {
        DriverManagerDataSource dataSource = new DriverManagerDataSource(
                "jdbc:h2:mem:review_ticket_lock;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
                "sa",
                ""
        );
        Flyway.configure()
                .dataSource(dataSource)
                .locations("classpath:db/migration")
                .load()
                .migrate();
        JdbcReviewTicketRepository repository = new JdbcReviewTicketRepository(new JdbcTemplate(dataSource));

        ReviewTicket saved = repository.save(ReviewTicketRepository.create(new CreateReviewTicketRequest(
                "a-2",
                "rr-a-2",
                "u-author",
                "team",
                Set.of("t-search")
        )));

        assertThat(repository.findPendingForTransition(saved.id())).isEqualTo(saved);

        repository.updateStatus(saved.id(), ReviewTicketStatus.APPROVED);

        assertThatThrownBy(() -> repository.findPendingForTransition(saved.id()))
                .isInstanceOf(ReviewTicketStateConflictException.class);
    }

    @Test
    void retryingTicketCreationReturnsTheExistingTicketForAnArticle() {
        DriverManagerDataSource dataSource = new DriverManagerDataSource(
                "jdbc:h2:mem:review_ticket_unique_article;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
                "sa",
                ""
        );
        Flyway.configure()
                .dataSource(dataSource)
                .locations("classpath:db/migration")
                .load()
                .migrate();
        JdbcReviewTicketRepository repository = new JdbcReviewTicketRepository(new JdbcTemplate(dataSource));

        ReviewTicket first = repository.save(ReviewTicketRepository.create(new CreateReviewTicketRequest(
                "a-once",
                "rr-a-once",
                "u-author",
                "team",
                Set.of("t-search")
        )));
        ReviewTicket retried = repository.save(ReviewTicketRepository.create(new CreateReviewTicketRequest(
                "a-once",
                "rr-a-once",
                "u-author",
                "team",
                Set.of("t-search")
        )));

        assertThat(retried.id()).isEqualTo(first.id());
    }

    @Test
    void concurrentTicketCreationReturnsTheSameTicketForAnArticle() throws Exception {
        DriverManagerDataSource dataSource = new DriverManagerDataSource(
                "jdbc:h2:mem:review_ticket_concurrent_article;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
                "sa",
                ""
        );
        Flyway.configure()
                .dataSource(dataSource)
                .locations("classpath:db/migration")
                .load()
                .migrate();
        JdbcReviewTicketRepository repository = new JdbcReviewTicketRepository(new JdbcTemplate(dataSource));
        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);

        try {
            Future<ReviewTicket> first = executor.submit(() -> createAtTheSameTime(repository, ready, start));
            Future<ReviewTicket> second = executor.submit(() -> createAtTheSameTime(repository, ready, start));

            assertThat(ready.await(5, TimeUnit.SECONDS)).isTrue();
            start.countDown();

            assertThat(first.get(5, TimeUnit.SECONDS).id()).isEqualTo(second.get(5, TimeUnit.SECONDS).id());
        } finally {
            executor.shutdownNow();
        }
    }

    @Test
    void uncertainApprovalBlocksRejectionUntilApprovalCompletes() {
        DriverManagerDataSource dataSource = new DriverManagerDataSource(
                "jdbc:h2:mem:review_ticket_approval_recovery;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
                "sa",
                ""
        );
        Flyway.configure()
                .dataSource(dataSource)
                .locations("classpath:db/migration")
                .load()
                .migrate();
        JdbcReviewTicketRepository repository = new JdbcReviewTicketRepository(new JdbcTemplate(dataSource));
        ReviewTicket saved = repository.save(ReviewTicketRepository.create(new CreateReviewTicketRequest(
                "a-approval-recovery",
                "rr-approval-recovery",
                "u-author",
                "team",
                Set.of("t-search")
        )));

        assertThat(repository.beginApproval(saved.id()).status()).isEqualTo(ReviewTicketStatus.APPROVING);
        assertThatThrownBy(() -> repository.beginRejection(saved.id()))
                .isInstanceOf(ReviewTicketStateConflictException.class);
        assertThat(repository.completeApproval(saved.id()).status()).isEqualTo(ReviewTicketStatus.APPROVED);
    }

    @Test
    void rejectedTicketIsNotReopenedByAnOldReviewRequest() {
        DriverManagerDataSource dataSource = new DriverManagerDataSource(
                "jdbc:h2:mem:review_ticket_reopen;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
                "sa",
                ""
        );
        Flyway.configure()
                .dataSource(dataSource)
                .locations("classpath:db/migration")
                .load()
                .migrate();
        JdbcReviewTicketRepository repository = new JdbcReviewTicketRepository(new JdbcTemplate(dataSource));
        ReviewTicket first = repository.save(ReviewTicketRepository.create(new CreateReviewTicketRequest(
                "a-reopen",
                "rr-first",
                "u-author",
                "TEAM",
                Set.of("t-search")
        )));

        repository.beginRejection(first.id());
        assertThat(repository.completeRejection(first.id()).status()).isEqualTo(ReviewTicketStatus.REJECTED);

        ReviewTicket oldRetry = repository.save(ReviewTicketRepository.create(new CreateReviewTicketRequest(
                "a-reopen",
                "rr-first",
                "u-author",
                "TEAM",
                Set.of("t-search")
        )));
        ReviewTicket nextRound = repository.save(ReviewTicketRepository.create(new CreateReviewTicketRequest(
                "a-reopen",
                "rr-second",
                "u-author",
                "TEAM",
                Set.of("t-platform")
        )));

        assertThat(oldRetry.id()).isEqualTo(first.id());
        assertThat(oldRetry.status()).isEqualTo(ReviewTicketStatus.REJECTED);
        assertThat(nextRound.id()).isNotEqualTo(first.id());
        assertThat(nextRound.status()).isEqualTo(ReviewTicketStatus.PENDING);
        assertThat(nextRound.targetOrgIds()).containsExactly("t-platform");
    }

    private static ReviewTicket createAtTheSameTime(
            JdbcReviewTicketRepository repository,
            CountDownLatch ready,
            CountDownLatch start
    ) throws InterruptedException {
        ready.countDown();
        start.await();
        return repository.save(ReviewTicketRepository.create(new CreateReviewTicketRequest(
                "a-concurrent",
                "rr-concurrent",
                "u-author",
                "team",
                Set.of("t-search")
        )));
    }
}

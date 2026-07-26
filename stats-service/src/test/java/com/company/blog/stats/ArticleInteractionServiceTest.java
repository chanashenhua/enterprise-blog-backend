package com.company.blog.stats;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.company.blog.stats.api.ArticleInteractionResponse;
import com.company.blog.stats.api.ArticleInteractionService;
import com.company.blog.stats.api.InteractionRepository;
import com.company.blog.stats.api.InteractionSnapshot;
import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

class ArticleInteractionServiceTest {
    @Test
    void recordsEachUsersViewLikeAndFavoriteOnlyOnce() {
        InMemoryRepository repository = new InMemoryRepository();
        ArticleInteractionService service = new ArticleInteractionService(repository, (articleId, headers) -> {
        });

        service.recordView("a-1", caller("u-reader"));
        service.recordView("a-1", caller("u-reader"));
        service.recordView("a-1", caller("u-author"));
        service.like("a-1", caller("u-reader"));
        service.like("a-1", caller("u-reader"));
        ArticleInteractionResponse response = service.favorite("a-1", caller("u-reader"));

        assertThat(response.viewCount()).isEqualTo(2);
        assertThat(response.likeCount()).isEqualTo(1);
        assertThat(response.favoriteCount()).isEqualTo(1);
        assertThat(response.liked()).isTrue();
        assertThat(response.favorited()).isTrue();
    }

    @Test
    void cancellationIsIdempotentAndPreservesOtherUsersInteractions() {
        InMemoryRepository repository = new InMemoryRepository();
        ArticleInteractionService service = new ArticleInteractionService(repository, (articleId, headers) -> {
        });
        service.like("a-1", caller("u-reader"));
        service.like("a-1", caller("u-author"));

        service.unlike("a-1", caller("u-reader"));
        ArticleInteractionResponse response = service.unlike("a-1", caller("u-reader"));

        assertThat(response.likeCount()).isEqualTo(1);
        assertThat(response.liked()).isFalse();
    }

    @Test
    void rejectsAnonymousUsersAndUnreadableArticles() {
        ArticleInteractionService service = new ArticleInteractionService(
                new InMemoryRepository(),
                (articleId, headers) -> {
                    if ("a-hidden".equals(articleId)) {
                        throw new ResponseStatusException(HttpStatus.FORBIDDEN);
                    }
                }
        );

        assertThatThrownBy(() -> service.get("a-1", new HttpHeaders()))
                .isInstanceOfSatisfying(
                        ResponseStatusException.class,
                        ex -> assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED)
                );
        assertThatThrownBy(() -> service.recordView("a-hidden", caller("u-reader")))
                .isInstanceOfSatisfying(
                        ResponseStatusException.class,
                        ex -> assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN)
                );
    }

    private static HttpHeaders caller(String userId) {
        HttpHeaders headers = new HttpHeaders();
        headers.set("X-User-Id", userId);
        return headers;
    }

    private static final class InMemoryRepository implements InteractionRepository {
        private final Set<Key> interactions = new HashSet<>();

        @Override
        public boolean add(String articleId, String userId, InteractionType type) {
            return interactions.add(new Key(articleId, userId, type));
        }

        @Override
        public boolean remove(String articleId, String userId, InteractionType type) {
            return interactions.remove(new Key(articleId, userId, type));
        }

        @Override
        public InteractionSnapshot snapshot(String articleId, String userId) {
            return new InteractionSnapshot(
                    count(articleId, InteractionType.VIEW),
                    count(articleId, InteractionType.LIKE),
                    count(articleId, InteractionType.FAVORITE),
                    interactions.contains(new Key(articleId, userId, InteractionType.LIKE)),
                    interactions.contains(new Key(articleId, userId, InteractionType.FAVORITE))
            );
        }

        private long count(String articleId, InteractionType type) {
            return interactions.stream()
                    .filter(key -> key.articleId().equals(articleId) && key.type() == type)
                    .count();
        }
    }

    private record Key(String articleId, String userId, InteractionType type) {
    }
}

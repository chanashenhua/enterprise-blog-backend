package com.company.blog.comment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.company.blog.comment.api.AdminCommentQuery;
import com.company.blog.comment.api.AdminCommentRepository;
import com.company.blog.comment.api.AdminCommentService;
import com.company.blog.comment.api.CommentGovernanceActionRequest;
import com.company.blog.comment.api.CommentRepository;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

class AdminCommentServiceTest {
    @Test
    void adminCanFilterHideAndRestoreWithAuditEvents() {
        InMemoryRepository repository = new InMemoryRepository();
        repository.save(comment("c-1", "a-1", "u-reader"));
        repository.save(comment("c-2", "a-2", "u-author"));
        List<String> actions = new ArrayList<>();
        AdminCommentService service = new AdminCommentService(
                repository,
                repository,
                (actorId, roles, action, comment, reason) -> actions.add(action + ":" + reason)
        );

        assertThat(service.search(admin(), "a-1", null, "ACTIVE", 100))
                .extracting(record -> record.id())
                .containsExactly("c-1");
        assertThat(service.hide(admin(), "c-1", new CommentGovernanceActionRequest("不当内容")).status())
                .isEqualTo(CommentStatus.HIDDEN);
        assertThat(service.restore(admin(), "c-1", new CommentGovernanceActionRequest("复核通过")).status())
                .isEqualTo(CommentStatus.ACTIVE);
        assertThat(actions).containsExactly("COMMENT_HIDE:不当内容", "COMMENT_RESTORE:复核通过");
    }

    @Test
    void nonAdminCannotSearchOrGovernComments() {
        InMemoryRepository repository = new InMemoryRepository();
        repository.save(comment("c-1", "a-1", "u-reader"));
        AdminCommentService service = new AdminCommentService(repository, repository, (a, r, action, c, reason) -> { });
        HttpHeaders reader = new HttpHeaders();
        reader.set("X-User-Id", "u-reader");
        reader.set("X-User-Roles", "READER");

        assertThatThrownBy(() -> service.hide(reader, "c-1", null))
                .isInstanceOfSatisfying(ResponseStatusException.class,
                        ex -> assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN));
    }

    private static Comment comment(String id, String articleId, String authorId) {
        Instant now = Instant.now();
        return new Comment(id, articleId, null, authorId, "评论内容", CommentStatus.ACTIVE, now, now);
    }

    private static HttpHeaders admin() {
        HttpHeaders headers = new HttpHeaders();
        headers.set("X-User-Id", "u-admin");
        headers.set("X-User-Roles", "ADMIN");
        return headers;
    }

    private static final class InMemoryRepository implements CommentRepository, AdminCommentRepository {
        private final Map<String, Comment> values = new LinkedHashMap<>();

        @Override
        public Comment save(Comment comment) {
            values.put(comment.id(), comment);
            return comment;
        }

        @Override
        public Optional<Comment> findById(String id) {
            return Optional.ofNullable(values.get(id));
        }

        @Override
        public List<Comment> findByArticleId(String articleId) {
            return values.values().stream().filter(value -> value.articleId().equals(articleId)).toList();
        }

        @Override
        public Optional<Comment> updateContent(String id, String content) {
            return Optional.empty();
        }

        @Override
        public Optional<Comment> softDelete(String id) {
            return Optional.empty();
        }

        @Override
        public List<Comment> search(AdminCommentQuery query) {
            return values.values().stream()
                    .filter(value -> query.articleId() == null || value.articleId().equals(query.articleId()))
                    .filter(value -> query.authorId() == null || value.authorId().equals(query.authorId()))
                    .filter(value -> query.status() == null || value.status() == query.status())
                    .limit(query.limit())
                    .toList();
        }

        @Override
        public Optional<Comment> hide(String commentId) {
            return replace(commentId, CommentStatus.HIDDEN);
        }

        @Override
        public Optional<Comment> restore(String commentId) {
            return replace(commentId, CommentStatus.ACTIVE);
        }

        private Optional<Comment> replace(String id, CommentStatus status) {
            return findById(id).map(value -> {
                Comment replaced = new Comment(
                        value.id(), value.articleId(), value.parentId(), value.authorId(), value.content(),
                        status, value.createdAt(), Instant.now()
                );
                values.put(id, replaced);
                return replaced;
            });
        }
    }
}

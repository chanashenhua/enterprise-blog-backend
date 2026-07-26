package com.company.blog.comment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.company.blog.comment.api.CommentRepository;
import com.company.blog.comment.api.CommentResponse;
import com.company.blog.comment.api.CommentService;
import com.company.blog.comment.api.CreateCommentRequest;
import com.company.blog.comment.api.UpdateCommentRequest;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

class CommentServiceTest {
    @Test
    void createsRepliesEditsAndSoftDeletesWithoutBreakingTheThread() {
        InMemoryRepository repository = new InMemoryRepository();
        CommentService service = new CommentService(repository, (articleId, headers) -> {
        });
        HttpHeaders author = caller("u-author", "AUTHOR");

        CommentResponse root = service.create("a-1", new CreateCommentRequest(" First comment ", null), author);
        CommentResponse reply = service.create(
                "a-1",
                new CreateCommentRequest("A reply", root.id()),
                caller("u-reader", "READER")
        );
        CommentResponse edited = service.update(
                "a-1",
                reply.id(),
                new UpdateCommentRequest("Updated reply"),
                caller("u-reader", "READER")
        );
        service.delete("a-1", root.id(), author);

        List<CommentResponse> thread = service.list("a-1", caller("u-reader", "READER"));
        assertThat(root.content()).isEqualTo("First comment");
        assertThat(edited.content()).isEqualTo("Updated reply");
        assertThat(thread).hasSize(2);
        assertThat(thread.get(0).deleted()).isTrue();
        assertThat(thread.get(0).content()).isNull();
        assertThat(thread.get(1).parentId()).isEqualTo(root.id());
    }

    @Test
    void onlyTheAuthorOrAnAdminMayChangeAComment() {
        InMemoryRepository repository = new InMemoryRepository();
        CommentService service = new CommentService(repository, (articleId, headers) -> {
        });
        CommentResponse comment = service.create(
                "a-1",
                new CreateCommentRequest("Protected", null),
                caller("u-author", "AUTHOR")
        );

        assertThatThrownBy(() -> service.update(
                "a-1",
                comment.id(),
                new UpdateCommentRequest("Hijacked"),
                caller("u-reader", "READER")
        ))
                .isInstanceOfSatisfying(
                        ResponseStatusException.class,
                        ex -> assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN)
                );

        service.delete("a-1", comment.id(), caller("u-admin", "ADMIN"));
        assertThat(repository.findById(comment.id())).get().extracting(Comment::deleted).isEqualTo(true);
    }

    @Test
    void rejectsNestedRepliesAndUnreadableArticles() {
        InMemoryRepository repository = new InMemoryRepository();
        CommentService service = new CommentService(repository, (articleId, headers) -> {
            if ("a-hidden".equals(articleId)) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN);
            }
        });
        HttpHeaders caller = caller("u-author", "AUTHOR");
        CommentResponse root = service.create("a-1", new CreateCommentRequest("Root", null), caller);
        CommentResponse reply = service.create("a-1", new CreateCommentRequest("Reply", root.id()), caller);

        assertThatThrownBy(() -> service.create(
                "a-1",
                new CreateCommentRequest("Nested", reply.id()),
                caller
        ))
                .isInstanceOfSatisfying(
                        ResponseStatusException.class,
                        ex -> assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.CONFLICT)
                );
        assertThatThrownBy(() -> service.list("a-hidden", caller))
                .isInstanceOfSatisfying(
                        ResponseStatusException.class,
                        ex -> assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN)
                );
    }

    private static HttpHeaders caller(String userId, String roles) {
        HttpHeaders headers = new HttpHeaders();
        headers.set("X-User-Id", userId);
        headers.set("X-User-Roles", roles);
        return headers;
    }

    private static final class InMemoryRepository implements CommentRepository {
        private final Map<String, Comment> comments = new LinkedHashMap<>();

        @Override
        public Comment save(Comment comment) {
            comments.put(comment.id(), comment);
            return comment;
        }

        @Override
        public Optional<Comment> findById(String id) {
            return Optional.ofNullable(comments.get(id));
        }

        @Override
        public List<Comment> findByArticleId(String articleId) {
            return comments.values().stream()
                    .filter(comment -> articleId.equals(comment.articleId()))
                    .toList();
        }

        @Override
        public Optional<Comment> updateContent(String id, String content) {
            return findById(id)
                    .filter(comment -> !comment.deleted())
                    .map(comment -> replace(comment, content, CommentStatus.ACTIVE));
        }

        @Override
        public Optional<Comment> softDelete(String id) {
            return findById(id).map(comment -> replace(comment, "", CommentStatus.DELETED));
        }

        private Comment replace(Comment comment, String content, CommentStatus status) {
            Comment replacement = new Comment(
                    comment.id(),
                    comment.articleId(),
                    comment.parentId(),
                    comment.authorId(),
                    content,
                    status,
                    comment.createdAt(),
                    Instant.now()
            );
            comments.put(replacement.id(), replacement);
            return replacement;
        }
    }
}

package com.company.blog.stats;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.company.blog.stats.api.AdminInteractionOverview;
import com.company.blog.stats.api.AdminInteractionService;
import com.company.blog.stats.api.ArticleInteractionRanking;
import com.company.blog.stats.api.InternalArticleRankingController;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

class InternalArticleRankingControllerTest {
    @Test
    void returnsRankingsOnlyForTheSharedFeedToken() {
        ArticleInteractionRanking ranking = new ArticleInteractionRanking("article-1", 9, 3, 2);
        AdminInteractionService service = new AdminInteractionService(limit -> new AdminInteractionOverview(
                9,
                3,
                2,
                1,
                2,
                List.of(ranking)
        ));
        InternalArticleRankingController controller = new InternalArticleRankingController(service, "feed-token");

        assertThat(controller.rankings("feed-token", 10)).containsExactly(ranking);
        assertThatThrownBy(() -> controller.rankings("wrong-token", 10))
                .isInstanceOfSatisfying(ResponseStatusException.class, exception ->
                        assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN));
    }
}

package com.sungho.letterpick.newsletter.adapter.persistence;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class LikePublicFeedSearchReaderConditionTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withBean(NewsletterIssueRepository.class, () -> mock(NewsletterIssueRepository.class))
            .withUserConfiguration(LikePublicFeedSearchReader.class);

    @Test
    @DisplayName("검색 strategy 설정이 없으면 LIKE 공개 피드 검색 reader bean을 등록한다")
    void registerLikeReaderWhenStrategyMissing() {
        // when
        contextRunner.run(context -> {
            // then
            assertThat(context).hasSingleBean(LikePublicFeedSearchReader.class);
        });
    }

    @Test
    @DisplayName("검색 strategy가 like이면 LIKE 공개 피드 검색 reader bean을 등록한다")
    void registerLikeReaderWhenStrategyLike() {
        // when
        contextRunner
                .withPropertyValues("letterpick.search.public-feed.strategy=like")
                .run(context -> {
                    // then
                    assertThat(context).hasSingleBean(LikePublicFeedSearchReader.class);
                });
    }

    @Test
    @DisplayName("검색 strategy가 like가 아니면 LIKE 공개 피드 검색 reader bean을 등록하지 않는다")
    void doesNotRegisterLikeReaderWhenStrategyIsNotLike() {
        // when
        contextRunner
                .withPropertyValues("letterpick.search.public-feed.strategy=fulltext")
                .run(context -> {
                    // then
                    assertThat(context).doesNotHaveBean(LikePublicFeedSearchReader.class);
                });
    }
}

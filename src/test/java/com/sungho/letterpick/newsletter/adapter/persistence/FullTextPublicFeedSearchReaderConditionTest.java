package com.sungho.letterpick.newsletter.adapter.persistence;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class FullTextPublicFeedSearchReaderConditionTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withBean(NewsletterIssueRepository.class, () -> mock(NewsletterIssueRepository.class))
            .withUserConfiguration(FullTextPublicFeedSearchReader.class);

    @Test
    @DisplayName("검색 strategy가 fulltext이면 FULLTEXT 공개 피드 검색 reader bean을 등록한다")
    void registerFullTextReaderWhenStrategyFullText() {
        // when
        contextRunner
                .withPropertyValues("letterpick.search.public-feed.strategy=fulltext")
                .run(context -> {
                    // then
                    assertThat(context).hasSingleBean(FullTextPublicFeedSearchReader.class);
                });
    }

    @Test
    @DisplayName("검색 strategy가 fulltext가 아니면 FULLTEXT 공개 피드 검색 reader bean을 등록하지 않는다")
    void doesNotRegisterFullTextReaderWhenStrategyIsNotFullText() {
        // when
        contextRunner
                .withPropertyValues("letterpick.search.public-feed.strategy=like")
                .run(context -> {
                    // then
                    assertThat(context).doesNotHaveBean(FullTextPublicFeedSearchReader.class);
                });
    }
}

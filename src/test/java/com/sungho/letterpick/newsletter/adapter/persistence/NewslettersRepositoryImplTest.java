package com.sungho.letterpick.newsletter.adapter.persistence;

import com.sungho.letterpick.LetterPickTestConfiguration;
import com.sungho.letterpick.newsletter.application.provided.NewsletterListItem;
import com.sungho.letterpick.newsletter.application.provided.NewsletterSearchCondition;
import com.sungho.letterpick.newsletter.domain.NewsletterCategory;
import com.sungho.letterpick.newsletter.domain.NewsletterFixture;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import com.sungho.letterpick.LetterPickDataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Slice;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@LetterPickDataJpaTest
@ActiveProfiles("test")
@Import({LetterPickTestConfiguration.class})
class NewslettersRepositoryImplTest {

    @Autowired
    NewslettersRepository newslettersRepository;

    @Autowired
    EntityManager entityManager;

    @Test
    @DisplayName("카테고리 조건으로 뉴스레터를 조회하면 해당 카테고리만 반환되고 다음 페이지 여부를 계산한다")
    void findAllBySearchCondition_filters_by_category_and_calculates_hasNext() {
        newslettersRepository.saveAll(List.of(
                NewsletterFixture.createNewsletter("비즈 레터", NewsletterCategory.BIZ),
                NewsletterFixture.createNewsletter("테크 레터 1", NewsletterCategory.TECH),
                NewsletterFixture.createNewsletter("테크 레터 2", NewsletterCategory.TECH)
        ));
        entityManager.flush();
        entityManager.clear();

        Slice<NewsletterListItem> result = newslettersRepository.findAllBySearchCondition(
                new NewsletterSearchCondition(NewsletterCategory.TECH),
                PageRequest.of(0, 1)
        );

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).name()).isEqualTo("테크 레터 1");
        assertThat(result.getContent().get(0).category().code()).isEqualTo("TECH");
        assertThat(result.getSize()).isEqualTo(1);
        assertThat(result.hasNext()).isTrue();
    }

}

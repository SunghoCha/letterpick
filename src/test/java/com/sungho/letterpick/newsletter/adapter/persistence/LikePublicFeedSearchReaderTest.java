package com.sungho.letterpick.newsletter.adapter.persistence;

import com.sungho.letterpick.newsletter.application.provided.NewsletterIssueItem;
import com.sungho.letterpick.newsletter.application.provided.PublicNewsletterIssueSearchCondition;
import com.sungho.letterpick.newsletter.domain.NewsletterCategory;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.SliceImpl;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class LikePublicFeedSearchReaderTest {

    @Mock
    private NewsletterIssueRepository newsletterIssueRepository;

    @Test
    @DisplayName("LIKE 공개 피드 검색 reader는 기존 공개 피드 repository 쿼리에 위임한다")
    void findIssuesDelegatesToRepository() {
        // given
        LikePublicFeedSearchReader reader = new LikePublicFeedSearchReader(newsletterIssueRepository);
        Long memberId = 10L;
        PublicNewsletterIssueSearchCondition condition = new PublicNewsletterIssueSearchCondition(
                NewsletterCategory.TECH,
                "redis"
        );
        Pageable pageable = PageRequest.of(0, 20);
        Slice<NewsletterIssueItem> expected = new SliceImpl<>(
                List.of(new NewsletterIssueItem(
                        1L,
                        2L,
                        "기술 뉴스레터",
                        "https://example.com/newsletter.png",
                        NewsletterCategory.TECH,
                        "공개 피드 테스트",
                        "공개 피드 미리보기",
                        Instant.parse("2050-05-12T03:00:00Z"),
                        false
                )),
                pageable,
                false
        );

        given(newsletterIssueRepository.findPublicIssuesByMemberId(memberId, condition, pageable))
                .willReturn(expected);

        // when
        Slice<NewsletterIssueItem> result = reader.findIssues(memberId, condition, pageable);

        // then
        assertThat(result).isSameAs(expected);
        verify(newsletterIssueRepository).findPublicIssuesByMemberId(memberId, condition, pageable);
    }
}

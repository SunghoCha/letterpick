package com.sungho.letterpick.newsletter.adapter.trending;

import com.sungho.letterpick.newsletter.application.exception.PublicIssueRankingReadException;
import com.sungho.letterpick.newsletter.application.provided.PublicIssueRankingItem;
import com.sungho.letterpick.newsletter.application.provided.PublicIssueRankingWindowType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;
import static org.springframework.http.HttpMethod.GET;

class RestClientPublicIssueRankingReaderTest {

    @Test
    @DisplayName("trending-service 내부 API에서 인기 이슈 랭킹을 조회한다")
    void findTop_reads_public_issue_rankings_from_trending_service() {
        // given
        RestClient.Builder builder = RestClient.builder()
                .baseUrl("https://trending.example.com");
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        RestClientPublicIssueRankingReader reader = new RestClientPublicIssueRankingReader(builder.build());

        server.expect(requestTo("https://trending.example.com/internal/api/v1/public-issue-rankings?windowType=DAILY&limit=3"))
                .andExpect(method(GET))
                .andRespond(withSuccess("""
                        {
                          "items": [
                            { "issueId": 10, "score": 123 },
                            { "issueId": 20, "score": 99 }
                          ]
                        }
                        """, MediaType.APPLICATION_JSON));

        // when
        List<PublicIssueRankingItem> result = reader.findTop(PublicIssueRankingWindowType.DAILY, 3);

        // then
        assertThat(result)
                .containsExactly(
                        new PublicIssueRankingItem(10L, 123),
                        new PublicIssueRankingItem(20L, 99)
                );
        server.verify();
    }

    @Test
    @DisplayName("trending-service 호출 실패는 랭킹 조회 실패 예외로 변환한다")
    void findTop_throws_when_trending_service_fails() {
        // given
        RestClient.Builder builder = RestClient.builder()
                .baseUrl("https://trending.example.com");
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        RestClientPublicIssueRankingReader reader = new RestClientPublicIssueRankingReader(builder.build());

        server.expect(requestTo("https://trending.example.com/internal/api/v1/public-issue-rankings?windowType=WEEKLY&limit=20"))
                .andExpect(method(GET))
                .andRespond(withServerError());

        // when & then
        assertThatThrownBy(() -> reader.findTop(PublicIssueRankingWindowType.WEEKLY, 20))
                .isInstanceOf(PublicIssueRankingReadException.class);
        server.verify();
    }

    @Test
    @DisplayName("trending-service 응답 매핑 실패는 랭킹 조회 실패 예외로 변환한다")
    void findTop_throws_when_trending_service_response_is_invalid() {
        // given
        RestClient.Builder builder = RestClient.builder()
                .baseUrl("https://trending.example.com");
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        RestClientPublicIssueRankingReader reader = new RestClientPublicIssueRankingReader(builder.build());

        server.expect(requestTo("https://trending.example.com/internal/api/v1/public-issue-rankings?windowType=DAILY&limit=3"))
                .andExpect(method(GET))
                .andRespond(withSuccess("""
                        {
                          "items": [
                            { "issueId": null, "score": 123 }
                          ]
                        }
                        """, MediaType.APPLICATION_JSON));

        // when & then
        assertThatThrownBy(() -> reader.findTop(PublicIssueRankingWindowType.DAILY, 3))
                .isInstanceOf(PublicIssueRankingReadException.class);
        server.verify();
    }
}

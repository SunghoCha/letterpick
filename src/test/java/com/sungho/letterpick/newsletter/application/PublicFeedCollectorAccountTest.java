package com.sungho.letterpick.newsletter.application;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.times;

@ExtendWith(MockitoExtension.class)
class PublicFeedCollectorAccountTest {

    private static final String COLLECTOR_INBOX_ADDRESS = "collector@inbound.letterpick.test";

    @Mock
    private RecipientAddressResolver recipientAddressResolver;

    private PublicFeedCollectorAccount publicFeedCollectorAccount;

    @BeforeEach
    void setUp() {
        publicFeedCollectorAccount = new PublicFeedCollectorAccount(
                COLLECTOR_INBOX_ADDRESS,
                recipientAddressResolver
        );
    }

    @Test
    @DisplayName("공개 피드 컬렉터 회원 ID를 조회한다")
    void collectorMemberId() {
        given(recipientAddressResolver.resolve(COLLECTOR_INBOX_ADDRESS))
                .willReturn(RecipientAddressResolution.found(10L));

        Long collectorMemberId = publicFeedCollectorAccount.collectorMemberId();

        assertThat(collectorMemberId).isEqualTo(10L);
    }

    @Test
    @DisplayName("공개 피드 컬렉터 회원 ID는 한 번 조회한 뒤 캐시한다")
    void collectorMemberIdCachesResolvedMemberId() {
        given(recipientAddressResolver.resolve(COLLECTOR_INBOX_ADDRESS))
                .willReturn(RecipientAddressResolution.found(10L));

        assertThat(publicFeedCollectorAccount.collectorMemberId()).isEqualTo(10L);
        assertThat(publicFeedCollectorAccount.collectorMemberId()).isEqualTo(10L);

        then(recipientAddressResolver).should(times(1)).resolve(COLLECTOR_INBOX_ADDRESS);
    }

    @Test
    @DisplayName("공개 피드 컬렉터 회원을 찾을 수 없으면 실패한다")
    void collectorMemberIdFailsWhenCollectorMemberNotFound() {
        given(recipientAddressResolver.resolve(COLLECTOR_INBOX_ADDRESS))
                .willReturn(RecipientAddressResolution.notFound());

        assertThatThrownBy(publicFeedCollectorAccount::collectorMemberId)
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("공개 피드 컬렉터 회원을 찾을 수 없습니다.");
    }

    @Test
    @DisplayName("공개 피드 컬렉터 회원 조회 실패는 캐시하지 않는다")
    void collectorMemberIdDoesNotCacheFailedResolution() {
        given(recipientAddressResolver.resolve(COLLECTOR_INBOX_ADDRESS))
                .willReturn(RecipientAddressResolution.notFound())
                .willReturn(RecipientAddressResolution.found(10L));

        assertThatThrownBy(publicFeedCollectorAccount::collectorMemberId)
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("공개 피드 컬렉터 회원을 찾을 수 없습니다.");
        assertThat(publicFeedCollectorAccount.collectorMemberId()).isEqualTo(10L);

        then(recipientAddressResolver).should(times(2)).resolve(COLLECTOR_INBOX_ADDRESS);
    }

    @Test
    @DisplayName("주어진 회원 ID가 공개 피드 컬렉터 회원인지 판단한다")
    void isCollectorMemberId() {
        given(recipientAddressResolver.resolve(COLLECTOR_INBOX_ADDRESS))
                .willReturn(RecipientAddressResolution.found(10L));

        assertThat(publicFeedCollectorAccount.isCollectorMemberId(10L)).isTrue();
        assertThat(publicFeedCollectorAccount.isCollectorMemberId(20L)).isFalse();

        then(recipientAddressResolver).should(times(1)).resolve(COLLECTOR_INBOX_ADDRESS);
    }

    @Test
    @DisplayName("주어진 수신자 주소가 공개 피드 컬렉터 주소인지 판단한다")
    void isCollectorInboxAddress() {
        assertThat(publicFeedCollectorAccount.isCollectorInboxAddress(COLLECTOR_INBOX_ADDRESS)).isTrue();
        assertThat(publicFeedCollectorAccount.isCollectorInboxAddress("member@inbound.letterpick.test")).isFalse();
    }
}

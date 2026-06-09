package com.sungho.letterpick.newsletter.adapter.webapi;

import static com.sungho.letterpick.common.auth.SecurityAuthorities.ROLE_USER;
import static org.assertj.core.api.Assertions.assertThat;

import com.sungho.letterpick.common.auth.SocialPrincipal;
import com.sungho.letterpick.common.auth.SocialProvider;
import com.sungho.letterpick.common.auth.SocialUserInfo;
import com.sungho.letterpick.member.adapter.security.CustomOAuth2Principal;
import com.sungho.letterpick.member.domain.MemberFixture;
import com.sungho.letterpick.newsletter.application.PublicIssueViewCountProperties;
import jakarta.servlet.http.Cookie;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;

class PublicIssueViewActorResolverTest {

    private static final String COOKIE_NAME = "letterpick_anonymous_id";

    @Test
    @DisplayName("가입 완료 로그인 사용자는 member actorKey를 사용한다")
    void resolveActorKey_uses_member_actor_key_for_registered_user() {
        // given
        PublicIssueViewActorResolver resolver = resolver();
        Authentication authentication = authentication(CustomOAuth2Principal.existing(
                MemberFixture.createMemberWithId(42L),
                oauth2User()
        ));
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        // when
        String actorKey = resolver.resolveActorKey(authentication, request, response);

        // then
        assertThat(actorKey).isEqualTo("member:42");
        assertThat(response.getHeaders(HttpHeaders.SET_COOKIE)).isEmpty();
    }

    @Test
    @DisplayName("익명 사용자가 anonymousId 쿠키를 가지고 있으면 해당 cookie value로 actorKey를 만든다")
    void resolveActorKey_uses_existing_anonymous_cookie() {
        // given
        PublicIssueViewActorResolver resolver = resolver();
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setCookies(new Cookie(COOKIE_NAME, "visitor-1"));
        MockHttpServletResponse response = new MockHttpServletResponse();

        // when
        String actorKey = resolver.resolveActorKey(null, request, response);

        // then
        assertThat(actorKey).isEqualTo("anonymous:visitor-1");
        assertThat(response.getHeaders(HttpHeaders.SET_COOKIE)).isEmpty();
    }

    @Test
    @DisplayName("익명 사용자가 anonymousId 쿠키를 가지고 있지 않으면 새 anonymousId 쿠키를 발급한다")
    void resolveActorKey_sets_anonymous_cookie_when_missing() {
        // given
        PublicIssueViewActorResolver resolver = resolver();
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        // when
        String actorKey = resolver.resolveActorKey(null, request, response);

        // then
        assertThat(actorKey).isEqualTo("anonymous:generated-anonymous-id");
        assertThat(response.getHeader(HttpHeaders.SET_COOKIE))
                .contains(COOKIE_NAME + "=generated-anonymous-id")
                .contains("Max-Age=7776000")
                .contains("Path=/")
                .contains("HttpOnly")
                .contains("SameSite=Lax");
    }

    @Test
    @DisplayName("가입 대기 사용자는 memberId가 없으므로 anonymous cookie 기반 actorKey를 사용한다")
    void resolveActorKey_uses_anonymous_cookie_for_pending_user() {
        // given
        PublicIssueViewActorResolver resolver = resolver();
        Authentication authentication = authentication(CustomOAuth2Principal.pending(
                new SocialUserInfo(SocialProvider.GOOGLE, "provider-1", "user@example.com", "user", null),
                oauth2User()
        ));
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setCookies(new Cookie(COOKIE_NAME, "visitor-1"));
        MockHttpServletResponse response = new MockHttpServletResponse();

        // when
        String actorKey = resolver.resolveActorKey(authentication, request, response);

        // then
        assertThat(actorKey).isEqualTo("anonymous:visitor-1");
        assertThat(response.getHeaders(HttpHeaders.SET_COOKIE)).isEmpty();
    }

    private PublicIssueViewActorResolver resolver() {
        return new PublicIssueViewActorResolver(
                properties(),
                () -> "generated-anonymous-id"
        );
    }

    private PublicIssueViewCountProperties properties() {
        return new PublicIssueViewCountProperties(
                50,
                Duration.ofMinutes(30),
                "letterpick:public-issue",
                COOKIE_NAME,
                Duration.ofDays(90)
        );
    }

    private Authentication authentication(SocialPrincipal principal) {
        return new TestingAuthenticationToken(
                principal,
                null,
                List.of(new SimpleGrantedAuthority(ROLE_USER))
        );
    }

    private OAuth2User oauth2User() {
        return new DefaultOAuth2User(
                List.of(new SimpleGrantedAuthority(ROLE_USER)),
                Map.of("sub", "test-sub"),
                "sub"
        );
    }
}

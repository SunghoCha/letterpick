package com.sungho.letterpick.newsletter.adapter.webapi;

import com.sungho.letterpick.common.auth.SocialPrincipal;
import com.sungho.letterpick.newsletter.application.PublicIssueViewCountProperties;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.Objects;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.util.WebUtils;

@Component
public class PublicIssueViewActorResolver {

    private static final String MEMBER_ACTOR_KEY_PREFIX = "member:";
    private static final String ANONYMOUS_ACTOR_KEY_PREFIX = "anonymous:";

    private final PublicIssueViewCountProperties properties;
    private final AnonymousIdGenerator anonymousIdGenerator;

    public PublicIssueViewActorResolver(PublicIssueViewCountProperties properties,
                                        AnonymousIdGenerator anonymousIdGenerator) {
        this.properties = Objects.requireNonNull(properties, "properties must not be null");
        this.anonymousIdGenerator = Objects.requireNonNull(anonymousIdGenerator, "anonymousIdGenerator must not be null");
    }

    public String resolveActorKey(Authentication authentication,
                                  HttpServletRequest request,
                                  HttpServletResponse response) {
        Objects.requireNonNull(request, "request must not be null");
        Objects.requireNonNull(response, "response must not be null");

        if (authentication != null
                && authentication.getPrincipal() instanceof SocialPrincipal principal
                && !principal.isPending()) {
            return MEMBER_ACTOR_KEY_PREFIX + principal.getMember().getId();
        }

        return ANONYMOUS_ACTOR_KEY_PREFIX + resolveAnonymousId(request, response);
    }

    private String resolveAnonymousId(HttpServletRequest request, HttpServletResponse response) {
        Cookie cookie = WebUtils.getCookie(request, properties.anonymousCookieName());
        if (cookie != null && StringUtils.hasText(cookie.getValue())) {
            return cookie.getValue();
        }

        String anonymousId = anonymousIdGenerator.generate();
        if (!StringUtils.hasText(anonymousId)) {
            throw new IllegalStateException("generated anonymousId must not be blank");
        }

        ResponseCookie responseCookie = ResponseCookie.from(properties.anonymousCookieName(), anonymousId)
                .path("/")
                .maxAge(properties.anonymousCookieMaxAge())
                .httpOnly(true)
                .sameSite("Lax")
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, responseCookie.toString());
        return anonymousId;
    }
}

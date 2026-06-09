package com.sungho.letterpick.newsletter.adapter.webapi;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.MethodParameter;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

public class PublicIssueViewActorArgumentResolver implements HandlerMethodArgumentResolver {

    private final PublicIssueViewActorResolver publicIssueViewActorResolver;

    public PublicIssueViewActorArgumentResolver(PublicIssueViewActorResolver publicIssueViewActorResolver) {
        this.publicIssueViewActorResolver = publicIssueViewActorResolver;
    }

    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        return parameter.hasParameterAnnotation(CurrentPublicIssueViewActor.class)
                && PublicIssueViewActor.class.equals(parameter.getParameterType());
    }

    @Override
    public Object resolveArgument(MethodParameter parameter,
                                  ModelAndViewContainer mavContainer,
                                  NativeWebRequest webRequest,
                                  WebDataBinderFactory binderFactory) {
        HttpServletRequest request = webRequest.getNativeRequest(HttpServletRequest.class);
        HttpServletResponse response = webRequest.getNativeResponse(HttpServletResponse.class);
        if (request == null || response == null) {
            throw new IllegalStateException("HttpServletRequest and HttpServletResponse are required");
        }

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String actorKey = publicIssueViewActorResolver.resolveActorKey(authentication, request, response);
        return new PublicIssueViewActor(actorKey);
    }
}

package com.sungho.letterpick.newsletter.adapter.webapi;

import com.sungho.letterpick.common.config.HandlerMethodArgumentResolverRegistrar;
import java.util.List;
import org.springframework.stereotype.Component;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;

@Component
public class PublicIssueViewActorArgumentResolverRegistrar implements HandlerMethodArgumentResolverRegistrar {

    private final PublicIssueViewActorArgumentResolver publicIssueViewActorArgumentResolver;

    public PublicIssueViewActorArgumentResolverRegistrar(PublicIssueViewActorResolver publicIssueViewActorResolver) {
        this.publicIssueViewActorArgumentResolver = new PublicIssueViewActorArgumentResolver(
                publicIssueViewActorResolver
        );
    }

    @Override
    public void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers) {
        resolvers.add(publicIssueViewActorArgumentResolver);
    }
}

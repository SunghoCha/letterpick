package com.sungho.letterpick.common.config;

import java.util.List;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;

public interface HandlerMethodArgumentResolverRegistrar {

    void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers);
}

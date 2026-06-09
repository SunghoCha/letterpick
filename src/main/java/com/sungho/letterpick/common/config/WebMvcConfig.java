package com.sungho.letterpick.common.config;

import com.sungho.letterpick.common.auth.CurrentUserArgumentResolver;
import com.sungho.letterpick.common.logging.AccessLogInterceptor;
import com.sungho.letterpick.common.logging.MdcInterceptor;
import java.util.List;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    private static final String[] LOGGING_EXCLUDE_PATHS = {
            "/actuator/**",
            "/swagger-ui/**",
            "/swagger-ui.html",
            "/v3/api-docs/**",
            "/favicon.ico"
    };

    private final MdcInterceptor mdcInterceptor;

    private final AccessLogInterceptor accessLogInterceptor;
    private final List<HandlerMethodArgumentResolverRegistrar> argumentResolverRegistrars;

    public WebMvcConfig(MdcInterceptor mdcInterceptor,
                        AccessLogInterceptor accessLogInterceptor,
                        List<HandlerMethodArgumentResolverRegistrar> argumentResolverRegistrars) {
        this.mdcInterceptor = mdcInterceptor;
        this.accessLogInterceptor = accessLogInterceptor;
        this.argumentResolverRegistrars = argumentResolverRegistrars;
    }

    @Override
    public void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers) {
        resolvers.add(new CurrentUserArgumentResolver());
        argumentResolverRegistrars.forEach(registrar -> registrar.addArgumentResolvers(resolvers));
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(mdcInterceptor)
                .excludePathPatterns(LOGGING_EXCLUDE_PATHS);
        registry.addInterceptor(accessLogInterceptor)
                .excludePathPatterns(LOGGING_EXCLUDE_PATHS);
    }
}

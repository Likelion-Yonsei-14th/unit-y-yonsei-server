package com.likelion.yonsei.daedongje.config;

import com.likelion.yonsei.daedongje.common.auth.AdminRoleInterceptor;
import com.likelion.yonsei.daedongje.common.auth.CurrentAdminArgumentResolver;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.List;

@Configuration
@RequiredArgsConstructor
public class WebMvcConfig implements WebMvcConfigurer {

    private final AdminRoleInterceptor adminRoleInterceptor;
    private final CurrentAdminArgumentResolver currentAdminArgumentResolver;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(adminRoleInterceptor)
                .addPathPatterns("/api/admin/**");
    }

    @Override
    public void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers) {
        resolvers.add(currentAdminArgumentResolver);
    }
}
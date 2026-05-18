package com.likelion.yonsei.daedongje.domain.auth.support;

import com.likelion.yonsei.daedongje.common.exception.BusinessException;
import com.likelion.yonsei.daedongje.domain.auth.session.AdminSessionConst;
import com.likelion.yonsei.daedongje.domain.auth.exception.AuthErrorCode;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
@RequiredArgsConstructor
public class AdminRoleInterceptor implements HandlerInterceptor {

    private final AdminAuthContextService adminAuthContextService;

    @Override
    public boolean preHandle(
            HttpServletRequest request,
            HttpServletResponse response,
            Object handler
    ) {
        if (!(handler instanceof HandlerMethod handlerMethod)) {
            return true;
        }

        RequireAdminRole requireAdminRole = findRequireAdminRole(handlerMethod);

        if (requireAdminRole == null) {
            return true;
        }

        AdminSessionUser currentAdmin = adminAuthContextService.getCurrentAdmin(request);

        if (!currentAdmin.hasAnyRole(requireAdminRole.value())) {
            throw new BusinessException(AuthErrorCode.FORBIDDEN);
        }

        request.setAttribute(AdminSessionConst.CURRENT_ADMIN, currentAdmin);

        return true;
    }

    private RequireAdminRole findRequireAdminRole(HandlerMethod handlerMethod) {
        RequireAdminRole methodAnnotation = handlerMethod.getMethodAnnotation(RequireAdminRole.class);

        if (methodAnnotation != null) {
            return methodAnnotation;
        }

        return AnnotationUtils.findAnnotation(handlerMethod.getBeanType(), RequireAdminRole.class);
    }
}
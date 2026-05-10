package com.likelion.yonsei.daedongje.domain.auth.support;

import com.likelion.yonsei.daedongje.domain.auth.session.AdminSessionConst;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.core.MethodParameter;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

@Component
@RequiredArgsConstructor
public class CurrentAdminArgumentResolver implements HandlerMethodArgumentResolver {

    private final AdminAuthContextService adminAuthContextService;

    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        return parameter.hasParameterAnnotation(CurrentAdmin.class)
                && AdminSessionUser.class.isAssignableFrom(parameter.getParameterType());
    }

    @Override
    public Object resolveArgument(
            MethodParameter parameter,
            ModelAndViewContainer mavContainer,
            NativeWebRequest webRequest,
            WebDataBinderFactory binderFactory
    ) {
        HttpServletRequest request = webRequest.getNativeRequest(HttpServletRequest.class);

        Object currentAdmin = request.getAttribute(AdminSessionConst.CURRENT_ADMIN);

        if (currentAdmin instanceof AdminSessionUser adminSessionUser) {
            return adminSessionUser;
        }

        AdminSessionUser adminSessionUser = adminAuthContextService.getCurrentAdmin(request);
        request.setAttribute(AdminSessionConst.CURRENT_ADMIN, adminSessionUser);

        return adminSessionUser;
    }
}
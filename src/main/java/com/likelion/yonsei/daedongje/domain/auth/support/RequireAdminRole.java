package com.likelion.yonsei.daedongje.domain.auth.support;

import com.likelion.yonsei.daedongje.domain.auth.entity.AdminRole;

import java.lang.annotation.*;

@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RequireAdminRole {

    AdminRole[] value() default {};
}
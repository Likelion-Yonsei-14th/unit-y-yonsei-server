package com.likelion.yonsei.daedongje.domain.auth.repository;

import com.likelion.yonsei.daedongje.domain.auth.entity.AdminUser;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AdminUserRepository extends JpaRepository<AdminUser, Long> {

    boolean existsByLoginId(String loginId);
}
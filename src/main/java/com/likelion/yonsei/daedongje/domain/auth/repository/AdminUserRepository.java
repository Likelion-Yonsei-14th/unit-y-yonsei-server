package com.likelion.yonsei.daedongje.domain.auth.repository;

import com.likelion.yonsei.daedongje.domain.auth.entity.AdminUser;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AdminUserRepository extends JpaRepository<AdminUser, Long> {

    boolean existsByLoginId(String loginId);

    Optional<AdminUser> findByLoginId(String loginId);
}
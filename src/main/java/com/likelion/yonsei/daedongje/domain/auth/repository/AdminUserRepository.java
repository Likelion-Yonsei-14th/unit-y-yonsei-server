package com.likelion.yonsei.daedongje.domain.auth.repository;

import com.likelion.yonsei.daedongje.domain.auth.entity.AdminRole;
import com.likelion.yonsei.daedongje.domain.auth.entity.AdminUser;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Sort;

import java.util.List;
import java.util.Optional;

public interface AdminUserRepository extends JpaRepository<AdminUser, Long> {

    boolean existsByLoginId(String loginId);

    Optional<AdminUser> findByLoginId(String loginId);

    List<AdminUser> findAllByRole(AdminRole role, Sort sort);
}
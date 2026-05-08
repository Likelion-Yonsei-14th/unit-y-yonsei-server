package com.likelion.yonsei.daedongje.domain.auth.controller;

import com.likelion.yonsei.daedongje.domain.auth.dto.AdminUserCreateRequest;
import com.likelion.yonsei.daedongje.domain.auth.dto.AdminUserCreateResponse;
import com.likelion.yonsei.daedongje.domain.auth.service.AdminUserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/users")
@RequiredArgsConstructor
public class AdminUserController {

    private final AdminUserService adminUserService;

    @PostMapping
    public ResponseEntity<AdminUserCreateResponse> createAdminUser(
            @Valid @RequestBody AdminUserCreateRequest request
    ) {
        AdminUserCreateResponse response = adminUserService.createAdminUser(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}
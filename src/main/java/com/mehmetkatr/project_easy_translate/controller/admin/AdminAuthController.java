package com.mehmetkatr.project_easy_translate.controller.admin;

import com.mehmetkatr.project_easy_translate.dto.admin.AdminAuthResponse;
import com.mehmetkatr.project_easy_translate.dto.admin.AdminAuthStatusResponse;
import com.mehmetkatr.project_easy_translate.dto.admin.AdminBootstrapRequest;
import com.mehmetkatr.project_easy_translate.dto.admin.AdminLoginRequest;
import com.mehmetkatr.project_easy_translate.entity.Admin;
import com.mehmetkatr.project_easy_translate.exception.ResourceConflictException;
import com.mehmetkatr.project_easy_translate.service.AdminService;
import com.mehmetkatr.project_easy_translate.service.TokenService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/auth")
@RequiredArgsConstructor
public class AdminAuthController {

    private final AdminService adminService;
    private final TokenService tokenService;

    @Value("${app.admin.bootstrap-key:}")
    private String adminBootstrapKey;

    @GetMapping("/status")
    public ResponseEntity<AdminAuthStatusResponse> status() {
        return ResponseEntity.ok(
                AdminAuthStatusResponse.builder()
                        .hasAdmin(adminService.hasAnyAdmin())
                        .build()
        );
    }

    @PostMapping("/bootstrap")
    public ResponseEntity<AdminAuthResponse> bootstrap(@Valid @RequestBody AdminBootstrapRequest request) {
        if (adminService.hasAnyAdmin()) {
            throw new ResourceConflictException("ADMIN_ALREADY_EXISTS", "Bootstrap is disabled because an admin already exists");
        }

        if (adminBootstrapKey == null || adminBootstrapKey.isBlank()) {
            throw new IllegalArgumentException("Admin bootstrap key is not configured on the server");
        }

        if (!adminBootstrapKey.equals(request.getBootstrapKey())) {
            throw new IllegalArgumentException("Invalid bootstrap key");
        }

        Admin admin = adminService.registerAdmin(request.getUsername(), request.getEmail(), request.getPassword());
        String token = tokenService.generateToken(admin.getUsername(), "ROLE_ADMIN");

        return ResponseEntity.ok(
                AdminAuthResponse.builder()
                        .adminId(admin.getId())
                        .username(admin.getUsername())
                        .token(token)
                        .tokenType("Bearer")
                        .build()
        );
    }

    @PostMapping("/login")
    public ResponseEntity<AdminAuthResponse> login(@Valid @RequestBody AdminLoginRequest request) {
        Admin admin = adminService.authenticate(request.getIdentifier(), request.getPassword());
        String token = tokenService.generateToken(admin.getUsername(), "ROLE_ADMIN");

        return ResponseEntity.ok(
                AdminAuthResponse.builder()
                        .adminId(admin.getId())
                        .username(admin.getUsername())
                        .token(token)
                        .tokenType("Bearer")
                        .build()
        );
    }
}

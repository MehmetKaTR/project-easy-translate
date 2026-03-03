package com.mehmetkatr.project_easy_translate.service;

import com.mehmetkatr.project_easy_translate.entity.Admin;
import com.mehmetkatr.project_easy_translate.exception.AuthenticationFailedException;
import com.mehmetkatr.project_easy_translate.exception.ResourceConflictException;
import com.mehmetkatr.project_easy_translate.repository.AdminRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional
public class AdminService {

    private final AdminRepository adminRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional(readOnly = true)
    public List<Admin> findAll() {
        return adminRepository.findAll();
    }

    @Transactional(readOnly = true)
    public Optional<Admin> findByEmail(String email) {
        if (email == null) return Optional.empty();
        return adminRepository.findFirstByEmailIgnoreCase(email.trim().toLowerCase(Locale.ROOT));
    }

    @Transactional(readOnly = true)
    public Optional<Admin> findByUsername(String username) {
        if (username == null) return Optional.empty();
        return adminRepository.findFirstByUsernameIgnoreCase(username.trim());
    }

    @Transactional(readOnly = true)
    public Optional<Admin> findByIdentifier(String identifier) {
        if (identifier == null) return Optional.empty();
        String normalized = identifier.trim();
        if (normalized.isBlank()) return Optional.empty();

        if (normalized.contains("@")) {
            Optional<Admin> byEmail = adminRepository.findFirstByEmailIgnoreCase(normalized.toLowerCase(Locale.ROOT));
            if (byEmail.isPresent()) {
                return byEmail;
            }
        }

        Optional<Admin> byUsername = adminRepository.findFirstByUsernameIgnoreCase(normalized);
        if (byUsername.isPresent()) {
            return byUsername;
        }

        return adminRepository.findFirstByEmailIgnoreCase(normalized.toLowerCase(Locale.ROOT));
    }

    @Transactional(readOnly = true)
    public Admin getByIdentifierOrThrow(String identifier) {
        return findByIdentifier(identifier)
                .orElseThrow(() -> new AuthenticationFailedException("Invalid admin credentials"));
    }

    @Transactional(readOnly = true)
    public boolean hasAnyAdmin() {
        return adminRepository.count() > 0;
    }

    @Transactional(readOnly = true)
    public Admin authenticate(String identifier, String rawPassword) {
        Admin admin = findByIdentifier(identifier)
                .orElseThrow(() -> new AuthenticationFailedException("Invalid admin credentials"));

        if (!passwordEncoder.matches(rawPassword, admin.getPasswordHash())) {
            throw new AuthenticationFailedException("Invalid admin credentials");
        }

        return admin;
    }

    public Admin registerAdmin(String username, String email, String rawPassword) {
        String normalizedUsername = normalizeUsername(username);
        String normalizedEmail = normalizeEmail(email);

        adminRepository.findFirstByEmailIgnoreCase(normalizedEmail)
                .ifPresent(existing -> {
                    throw new ResourceConflictException("ADMIN_EMAIL_ALREADY_EXISTS", "Admin email already exists");
                });

        adminRepository.findFirstByUsernameIgnoreCase(normalizedUsername)
                .ifPresent(existing -> {
                    throw new ResourceConflictException("ADMIN_USERNAME_ALREADY_EXISTS", "Admin username already exists");
                });

        Admin admin = Admin.builder()
                .username(normalizedUsername)
                .email(normalizedEmail)
                .passwordHash(passwordEncoder.encode(rawPassword))
                .build();

        return adminRepository.save(admin);
    }

    private String normalizeUsername(String username) {
        if (username == null) return "";
        return username.trim();
    }

    private String normalizeEmail(String email) {
        if (email == null) return "";
        return email.trim().toLowerCase(Locale.ROOT);
    }
}

package com.mehmetkatr.project_easy_translate.service;

import com.mehmetkatr.project_easy_translate.entity.Admin;
import com.mehmetkatr.project_easy_translate.repository.AdminRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional
public class AdminService {

    private final AdminRepository adminRepository;
    private final PasswordEncoder passwordEncoder;

    public List<Admin> findAll() {
        return adminRepository.findAll();
    }

    public Optional<Admin> findByEmail(String email) {
        return adminRepository.findByEmail(email);
    }

    public Optional<Admin> findByUsername(String username) {
        return adminRepository.findByUsername(username);
    }

    @Transactional
    public Admin registerAdmin(String username, String email, String rawPassword) {
        if (adminRepository.findByEmail(email).isPresent()) {
            throw new RuntimeException("Email already exists");
        }
        if (adminRepository.findByUsername(username).isPresent()) {
            throw new RuntimeException("Username already exists");
        }

        Admin admin = Admin.builder()
                .username(username)
                .email(email)
                .passwordHash(passwordEncoder.encode(rawPassword))
                .build();

        return adminRepository.save(admin);
    }
}

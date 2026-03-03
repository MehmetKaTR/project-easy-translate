package com.mehmetkatr.project_easy_translate.repository;

import com.mehmetkatr.project_easy_translate.entity.Admin;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AdminRepository extends JpaRepository<Admin, Long> {

    Optional<Admin> findByEmail(String email);

    Optional<Admin> findByUsername(String username);

    Optional<Admin> findFirstByEmailIgnoreCase(String email);

    Optional<Admin> findFirstByUsernameIgnoreCase(String username);
}

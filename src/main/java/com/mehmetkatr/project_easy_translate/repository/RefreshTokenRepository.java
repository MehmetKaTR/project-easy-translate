package com.mehmetkatr.project_easy_translate.repository;

import com.mehmetkatr.project_easy_translate.entity.RefreshToken;
import com.mehmetkatr.project_easy_translate.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {

    Optional<RefreshToken> findFirstByTokenHash(String tokenHash);

    List<RefreshToken> findByUserAndRevokedAtIsNullAndExpiresAtAfter(User user, LocalDateTime now);

    @Modifying
    @Query(value = "DELETE FROM refresh_tokens WHERE user_id = :userId", nativeQuery = true)
    int deleteByUserIdNative(@Param("userId") Long userId);
}

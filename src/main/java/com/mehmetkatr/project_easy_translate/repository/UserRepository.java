package com.mehmetkatr.project_easy_translate.repository;

import com.mehmetkatr.project_easy_translate.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByUsername(String username);

    Optional<User> findByEmail(String email);

    Optional<User> findFirstByUsernameIgnoreCase(String username);

    Optional<User> findFirstByEmailIgnoreCase(String email);

    List<User> findBySubscriptionLevel(User.SubscriptionLevel subscriptionLevel);

    List<User> findByPendingDeletionTrueAndDeletionScheduledAtBefore(LocalDateTime now);
}

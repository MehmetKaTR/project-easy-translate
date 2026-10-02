package com.mehmetkatr.project_easy_translate.repository;

import com.mehmetkatr.project_easy_translate.entity.Admin;
import com.mehmetkatr.project_easy_translate.entity.AdminActionLog;
import com.mehmetkatr.project_easy_translate.entity.User;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface AdminActionLogRepository extends JpaRepository<AdminActionLog, Long> {

    // admin + targetUser'i birlikte getir: mapping transaction disinda yapildigi icin
    // lazy yuklenemiyordu (LazyInitializationException -> 500).
    @Query("SELECT l FROM AdminActionLog l LEFT JOIN FETCH l.admin LEFT JOIN FETCH l.targetUser ORDER BY l.createdAt DESC")
    List<AdminActionLog> findRecentWithRefs(Pageable pageable);

    List<AdminActionLog> findByAdmin(Admin admin);

    List<AdminActionLog> findByTargetUser(User targetUser);

    List<AdminActionLog> findByAdminAndTargetUser(Admin admin, User targetUser);

    List<AdminActionLog> findByActionType(AdminActionLog.AdminActionType actionType);

    @Modifying
    @Query(value = "DELETE FROM admin_action_log WHERE target_user_id = :userId", nativeQuery = true)
    int deleteByTargetUserIdNative(@Param("userId") Long userId);
}

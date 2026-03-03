package com.mehmetkatr.project_easy_translate.repository;

import com.mehmetkatr.project_easy_translate.entity.Admin;
import com.mehmetkatr.project_easy_translate.entity.AdminActionLog;
import com.mehmetkatr.project_easy_translate.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AdminActionLogRepository extends JpaRepository<AdminActionLog, Long> {

    List<AdminActionLog> findByAdmin(Admin admin);

    List<AdminActionLog> findByTargetUser(User targetUser);

    List<AdminActionLog> findByAdminAndTargetUser(Admin admin, User targetUser);

    List<AdminActionLog> findByActionType(AdminActionLog.AdminActionType actionType);
}

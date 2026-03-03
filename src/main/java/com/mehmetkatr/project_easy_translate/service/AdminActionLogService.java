package com.mehmetkatr.project_easy_translate.service;

import com.mehmetkatr.project_easy_translate.entity.Admin;
import com.mehmetkatr.project_easy_translate.entity.AdminActionLog;
import com.mehmetkatr.project_easy_translate.entity.User;
import com.mehmetkatr.project_easy_translate.repository.AdminActionLogRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class AdminActionLogService {

    private final AdminActionLogRepository adminActionLogRepository;

    public AdminActionLog saveLog(AdminActionLog log) {
        return adminActionLogRepository.save(log);
    }

    public AdminActionLog logAction(Admin admin, User targetUser, AdminActionLog.AdminActionType actionType) {
        AdminActionLog log = AdminActionLog.builder()
                .admin(admin)
                .targetUser(targetUser)
                .actionType(actionType)
                .build();
        return adminActionLogRepository.save(log);
    }

    public List<AdminActionLog> findByAdmin(Admin admin) {
        return adminActionLogRepository.findByAdmin(admin);
    }

    public List<AdminActionLog> findByTargetUser(User targetUser) {
        return adminActionLogRepository.findByTargetUser(targetUser);
    }

    public List<AdminActionLog> findByAdminAndTargetUser(Admin admin, User targetUser) {
        return adminActionLogRepository.findByAdminAndTargetUser(admin, targetUser);
    }

    public List<AdminActionLog> findByActionType(AdminActionLog.AdminActionType actionType) {
        return adminActionLogRepository.findByActionType(actionType);
    }

    public List<AdminActionLog> findRecent(int limit) {
        int safeLimit = Math.max(1, Math.min(limit, 200));
        List<AdminActionLog> all = adminActionLogRepository.findAll(Sort.by(Sort.Direction.DESC, "createdAt"));
        if (all.size() <= safeLimit) {
            return all;
        }
        return all.subList(0, safeLimit);
    }
}

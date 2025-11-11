package com.mehmetkatr.project_easy_translate.service;

import com.mehmetkatr.project_easy_translate.entity.AdminActionLog;
import com.mehmetkatr.project_easy_translate.entity.User;
import com.mehmetkatr.project_easy_translate.repository.AdminActionLogRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
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

    public List<AdminActionLog> findByAdmin(User admin) {
        return adminActionLogRepository.findByAdmin(admin);
    }

    public List<AdminActionLog> findByTargetUser(User targetUser) {
        return adminActionLogRepository.findByTargetUser(targetUser);
    }

    public List<AdminActionLog> findByAdminAndTargetUser(User admin, User targetUser) {
        return adminActionLogRepository.findByAdminAndTargetUser(admin, targetUser);
    }

    public List<AdminActionLog> findByActionType(AdminActionLog.AdminActionType actionType) {
        return adminActionLogRepository.findByActionType(actionType);
    }
}

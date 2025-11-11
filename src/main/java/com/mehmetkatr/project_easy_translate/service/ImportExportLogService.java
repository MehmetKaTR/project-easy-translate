package com.mehmetkatr.project_easy_translate.service;

import com.mehmetkatr.project_easy_translate.entity.ImportExportLog;
import com.mehmetkatr.project_easy_translate.entity.User;
import com.mehmetkatr.project_easy_translate.repository.ImportExportLogRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class ImportExportLogService {

    private final ImportExportLogRepository importExportLogRepository;

    public void save(ImportExportLog importExportLog) {
        importExportLogRepository.save(importExportLog);
    }

    public List<ImportExportLog> findAll() {
        return importExportLogRepository.findAll();
    }

    public List<ImportExportLog> findByUser(User targetUser) {
        return importExportLogRepository.findByUser(targetUser);
    }

    public List<ImportExportLog> findBySource(String source) {
        return importExportLogRepository.findBySource(source);
    }

    public List<ImportExportLog> findByUserAndSource(User user, String source) {
        return importExportLogRepository.findByUserAndSource(user, source);
    }
}

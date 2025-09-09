package com.mehmetkatr.project_easy_translate.repository;

import com.mehmetkatr.project_easy_translate.entity.ImportExportLog;
import com.mehmetkatr.project_easy_translate.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ImportExportLogRepository extends JpaRepository<ImportExportLog, Long> {

    List<ImportExportLog> findByUser(User user);

    List<ImportExportLog> findBySource(String source);

}

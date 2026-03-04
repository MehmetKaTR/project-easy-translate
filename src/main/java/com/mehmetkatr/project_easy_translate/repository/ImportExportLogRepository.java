package com.mehmetkatr.project_easy_translate.repository;

import com.mehmetkatr.project_easy_translate.entity.ImportExportLog;
import com.mehmetkatr.project_easy_translate.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ImportExportLogRepository extends JpaRepository<ImportExportLog, Long> {

    List<ImportExportLog> findByUser(User user);

    List<ImportExportLog> findBySource(String source);

    List<ImportExportLog> findByUserAndSource(User user, String source);

    @Modifying
    @Query(value = "DELETE FROM import_export_log WHERE user_id = :userId", nativeQuery = true)
    int deleteByUserIdNative(@Param("userId") Long userId);
}

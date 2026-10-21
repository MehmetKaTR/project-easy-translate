package com.mehmetkatr.project_easy_translate.repository;

import com.mehmetkatr.project_easy_translate.entity.UserStudyProgress;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface UserStudyProgressRepository extends JpaRepository<UserStudyProgress, Long> {

    Optional<UserStudyProgress> findByUserIdAndEntryId(Long userId, Long entryId);

    /** Zamani gelmis (due) tekrarlar, en gec kalmistan basla. */
    @Query("""
            SELECT p FROM UserStudyProgress p
            WHERE p.userId = :uid AND p.dueDate IS NOT NULL AND p.dueDate <= :today
              AND (:exclude IS NULL OR p.entryId NOT IN :exclude)
            ORDER BY p.dueDate ASC, p.id ASC
            """)
    List<UserStudyProgress> findDue(@Param("uid") Long uid,
                                    @Param("today") LocalDate today,
                                    @Param("exclude") List<Long> exclude,
                                    Pageable pageable);

    long countByUserIdAndDueDateLessThanEqual(Long userId, LocalDate date);

    long countByUserId(Long userId);

    /** Yanlislarim: lapses>0 veya son sonuc AGAIN. */
    @Query("""
            SELECT p.entryId FROM UserStudyProgress p
            WHERE p.userId = :uid AND (p.lapses > 0 OR p.lastResult = 'AGAIN')
            ORDER BY p.dueDate ASC, p.id ASC
            """)
    List<Long> findMistakeEntryIds(@Param("uid") Long uid, Pageable pageable);
}

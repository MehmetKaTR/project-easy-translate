package com.mehmetkatr.project_easy_translate.repository;

import com.mehmetkatr.project_easy_translate.entity.UserWord;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface UserWordRepository extends JpaRepository<UserWord, Long> {

    // Not: bloklanmis global kelimeler kullanici listelerinde gizlenir (uw.word.blocked = false).
    @Query("SELECT uw FROM UserWord uw WHERE uw.user.id = :userId AND uw.word.blocked = false")
    Page<UserWord> findByUserId(@Param("userId") Long userId, Pageable pageable);

    @Query("SELECT uw FROM UserWord uw WHERE uw.user.id = :userId AND uw.starred = :starred AND uw.word.blocked = false")
    Page<UserWord> findByUserIdAndStarred(@Param("userId") Long userId, @Param("starred") boolean starred, Pageable pageable);

    Optional<UserWord> findByIdAndUserId(Long id, Long userId);

    Optional<UserWord> findByUserIdAndWordId(Long userId, Long wordId);

    long deleteByUserId(Long userId);

    @Query("SELECT uw FROM UserWord uw JOIN uw.wordLists wl WHERE wl.id = :wordListId AND uw.user.id = :userId AND uw.word.blocked = false")
    Page<UserWord> findByWordListId(@Param("wordListId") Long wordListId, @Param("userId") Long userId, Pageable pageable);

}

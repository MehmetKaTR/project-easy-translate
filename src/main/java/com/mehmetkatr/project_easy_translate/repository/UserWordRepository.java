package com.mehmetkatr.project_easy_translate.repository;

import com.mehmetkatr.project_easy_translate.dto.response.AdminWordUserResponse;
import com.mehmetkatr.project_easy_translate.entity.UserWord;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface UserWordRepository extends JpaRepository<UserWord, Long> {

    // Admin: bir global kelimeye sahip kullanicilar + cevirileri.
    @Query("""
            SELECT new com.mehmetkatr.project_easy_translate.dto.response.AdminWordUserResponse(
                u.id, u.username, u.email, uw.translated)
            FROM UserWord uw JOIN uw.user u
            WHERE uw.word.id = :wordId
            ORDER BY u.username ASC
            """)
    List<AdminWordUserResponse> findUsersForWord(@Param("wordId") Long wordId);

    // Admin: bir global kelimeyi tamamen silmek icin (blok DEGIL; sonra yeniden eklenebilir).
    @Modifying
    @Query(value = "DELETE FROM user_word_wordlist WHERE user_word_id IN (SELECT id FROM user_words WHERE word_id = :wordId)", nativeQuery = true)
    int deleteWordlistLinksByWordId(@Param("wordId") Long wordId);

    @Modifying
    @Query(value = "DELETE FROM user_words WHERE word_id = :wordId", nativeQuery = true)
    int deleteByWordIdNative(@Param("wordId") Long wordId);

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

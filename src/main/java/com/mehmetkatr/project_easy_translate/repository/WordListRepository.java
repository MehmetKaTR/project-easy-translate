package com.mehmetkatr.project_easy_translate.repository;

import com.mehmetkatr.project_easy_translate.entity.User;
import com.mehmetkatr.project_easy_translate.entity.WordList;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface WordListRepository extends JpaRepository<WordList, Long> {

    Optional<WordList> findByName(String name);

    List<WordList> findByUser(User user);

    Optional<WordList> findByUserAndName(User user, String name);

    Optional<WordList> findByUserAndId(User user, Long id);

    List<WordList> findByUserAndNameContainingIgnoreCase(User user, String name);

    @Query("SELECT wl FROM WordList wl LEFT JOIN FETCH wl.words WHERE wl.user.id = :userId")
    List<WordList> findAllByUserIdWithWords(@Param("userId") Long userId);

}

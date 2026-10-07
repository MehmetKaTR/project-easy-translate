package com.mehmetkatr.project_easy_translate.repository;

import com.mehmetkatr.project_easy_translate.entity.DictionarySenseTr;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface DictionarySenseTrRepository extends JpaRepository<DictionarySenseTr, Long> {

    Optional<DictionarySenseTr> findBySense_IdAndTargetLang(Long senseId, String targetLang);
}

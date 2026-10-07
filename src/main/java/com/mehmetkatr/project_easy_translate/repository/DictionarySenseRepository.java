package com.mehmetkatr.project_easy_translate.repository;

import com.mehmetkatr.project_easy_translate.entity.DictionarySense;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DictionarySenseRepository extends JpaRepository<DictionarySense, Long> {
}

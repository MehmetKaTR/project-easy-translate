package com.mehmetkatr.project_easy_translate.repository;

import com.mehmetkatr.project_easy_translate.entity.LlmModel;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface LlmModelRepository extends JpaRepository<LlmModel, Long> {

    List<LlmModel> findByName(String name);

    List<LlmModel> findByStatus(LlmModel.Status status);

}

package com.mehmetkatr.project_easy_translate.repository;

import com.mehmetkatr.project_easy_translate.entity.LlmModel;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.ui.Model;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface LlmModelRepository extends MongoRepository<LlmModel, String> {

    Optional<LlmModel> findByName(String name);

    List<Model> findByStatus(LlmModel.Status status);

}

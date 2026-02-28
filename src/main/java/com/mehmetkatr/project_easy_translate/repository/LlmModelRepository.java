package com.mehmetkatr.project_easy_translate.repository;

import com.mehmetkatr.project_easy_translate.entity.LlmModel;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface LlmModelRepository extends MongoRepository<LlmModel, String> {

    List<LlmModel> findByName(String name);

    List<LlmModel> findByStatus(LlmModel.Status status);

}

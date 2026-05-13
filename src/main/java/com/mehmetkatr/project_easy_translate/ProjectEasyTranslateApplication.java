package com.mehmetkatr.project_easy_translate;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableScheduling
@EnableAsync
public class ProjectEasyTranslateApplication {

    public static void main(String[] args) {
        SpringApplication.run(ProjectEasyTranslateApplication.class, args);
    }
}

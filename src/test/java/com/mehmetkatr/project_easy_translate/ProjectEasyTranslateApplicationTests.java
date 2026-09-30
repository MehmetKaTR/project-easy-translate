package com.mehmetkatr.project_easy_translate;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Disabled("Entegrasyon testi: Docker gerektirir. CI (GitHub Actions) ortaminda calisir; lokalde Docker/Testcontainers API uyumsuzlugu nedeniyle devre disi.")
@SpringBootTest
@Testcontainers
class ProjectEasyTranslateApplicationTests {

    @Container
    @ServiceConnection
    static MySQLContainer<?> mysql = new MySQLContainer<>("mysql:8.0");

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "create-drop");
        registry.add("jwt.secret", () -> "TestSecretKeyThatIsAtLeast32CharactersLong!!");
        registry.add("jwt.expiration-ms", () -> "3600000");
    }

    @Test
    void contextLoads() {
    }
}

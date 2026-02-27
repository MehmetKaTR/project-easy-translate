package com.mehmetkatr.project_easy_translate;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = {
		"spring.autoconfigure.exclude="
				+ "org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration,"
				+ "org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration,"
				+ "org.springframework.boot.autoconfigure.mongo.MongoAutoConfiguration,"
				+ "org.springframework.boot.autoconfigure.data.mongo.MongoDataAutoConfiguration",
		"jwt.secret=TestSecretKeyAtLeast32CharactersLong!",
		"jwt.expiration-ms=3600000"
})
class ProjectEasyTranslateApplicationTests {

	@Test
	void contextLoads() {
	}

}

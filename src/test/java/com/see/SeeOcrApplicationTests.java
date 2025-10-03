package com.see;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@TestPropertySource(
    properties = {
        "spring.datasource.url=jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=password",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.jpa.show-sql=false",
        "spring.jpa.properties.hibernate.format_sql=false",
        "logging.level.org.springframework.web=WARN",
        "logging.level.com.see=INFO",
        "logging.level.org.springframework.security=WARN",
        "logging.level.org.hibernate.SQL=WARN",
        "logging.level.org.hibernate.type=WARN",
        "logging.config=classpath:logback-test.xml",
        "app.admin.username=testadmin",
        "app.admin.email=test@example.com",
        "app.admin.password=TestPassword123",
        "jwt.secret=testSecretKeyForJWTTokenGenerationInTestEnvironment12345",
        "jwt.expiration=3600000",
    }
)
class SeeOcrApplicationTests {

    @Test
    void contextLoads() {
        // This test will pass if the application context loads successfully
    }

    @Test
    void applicationStarts() {
        // Basic test to ensure the application can start
        assert true;
    }
}

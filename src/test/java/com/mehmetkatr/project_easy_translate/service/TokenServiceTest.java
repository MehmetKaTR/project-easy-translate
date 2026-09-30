package com.mehmetkatr.project_easy_translate.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;

class TokenServiceTest {

    private TokenService tokenService;

    @BeforeEach
    void setUp() {
        tokenService = new TokenService();
        ReflectionTestUtils.setField(tokenService, "jwtSecret", "TestSecretKeyThatIsAtLeast32CharactersLong!!");
        ReflectionTestUtils.setField(tokenService, "jwtExpirationMs", 3_600_000L);
        ReflectionTestUtils.setField(tokenService, "adminJwtExpirationMs", 86_400_000L);
    }

    @Test
    void generatedTokenIsValid() {
        String token = tokenService.generateToken("mehmet", "ROLE_USER");

        assertThat(tokenService.validateToken(token)).isTrue();
    }

    @Test
    void usernameCanBeReadFromToken() {
        String token = tokenService.generateToken("mehmet", "ROLE_USER");

        assertThat(tokenService.getUsernameFromToken(token)).isEqualTo("mehmet");
    }

    @Test
    void roleCanBeReadFromToken() {
        String token = tokenService.generateToken("mehmet", "ROLE_USER");

        assertThat(tokenService.getRoleFromToken(token)).isEqualTo("ROLE_USER");
    }

    @Test
    void adminTokenCarriesAdminRole() {
        String token = tokenService.generateAdminToken("root");

        assertThat(tokenService.getRoleFromToken(token)).isEqualTo("ROLE_ADMIN");
    }

    @Test
    void tamperedTokenIsRejected() {
        String token = tokenService.generateToken("mehmet", "ROLE_USER");
        String tampered = token.substring(0, token.length() - 2) + "xx";

        assertThat(tokenService.validateToken(tampered)).isFalse();
    }

    @Test
    void malformedTokenIsRejected() {
        assertThat(tokenService.validateToken("not.a.jwt")).isFalse();
    }

    @Test
    void accessTokenExpiresInSecondsIsPositive() {
        assertThat(tokenService.getAccessTokenExpiresInSeconds()).isPositive();
    }
}

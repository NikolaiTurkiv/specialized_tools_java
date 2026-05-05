package org.example.security;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TokenServiceTest {

    @Test
    void shouldGenerateAndVerifyToken() {
        TokenService tokenService = new TokenService("secret-for-test", 3600);

        String token = tokenService.generate(42L, "user1", "USER");
        TokenPayload payload = tokenService.verify(token);

        assertEquals(42L, payload.userId());
        assertEquals("user1", payload.username());
        assertEquals("USER", payload.role());
    }
}

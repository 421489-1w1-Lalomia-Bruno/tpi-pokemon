package ar.edu.utn.frc.tup.piii.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.Authentication;

import static org.junit.jupiter.api.Assertions.*;

class JwtTokenProviderTest {

    private JwtTokenProvider jwtTokenProvider;

    @BeforeEach
    void setUp() {
        jwtTokenProvider = new JwtTokenProvider(
                "test-secret-key-which-is-long-enough-for-hmac-sha256",
                3600000);
    }

    @Test
    void generateToken_returnsValidToken() {
        String token = jwtTokenProvider.generateToken("player-123");
        assertNotNull(token);
        assertTrue(jwtTokenProvider.validate(token));
    }

    @Test
    void getUserIdFromToken_returnsCorrectSubject() {
        String token = jwtTokenProvider.generateToken("player-123");
        assertEquals("player-123", jwtTokenProvider.getUserIdFromToken(token));
    }

    @Test
    void validate_invalidToken_returnsFalse() {
        assertFalse(jwtTokenProvider.validate("invalid-token"));
    }

    @Test
    void validate_expiredToken_returnsFalse() {
        jwtTokenProvider = new JwtTokenProvider(
                "test-secret-key-which-is-long-enough-for-hmac-sha256",
                -1);
        String token = jwtTokenProvider.generateToken("player-123");
        assertFalse(jwtTokenProvider.validate(token));
    }

    @Test
    void getAuthentication_returnsAuthenticationWithPlayerId() {
        String token = jwtTokenProvider.generateToken("player-123");
        Authentication auth = jwtTokenProvider.getAuthentication(token);
        assertEquals("player-123", auth.getPrincipal());
        assertTrue(auth.getAuthorities().isEmpty());
    }
}

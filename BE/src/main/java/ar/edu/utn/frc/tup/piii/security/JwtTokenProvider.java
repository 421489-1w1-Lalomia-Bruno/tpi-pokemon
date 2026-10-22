package ar.edu.utn.frc.tup.piii.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.time.Instant;
import java.util.Base64;
import java.util.Date;
import java.util.List;

@Component
public class JwtTokenProvider {

    private final SecretKey secretKey;
    private final long expirationMs;

    public JwtTokenProvider(
            @Value("${jwt.secret:default-secret-key-change-in-production-must-be-256-bits-minimum}") String secret,
            @Value("${jwt.expiration-ms:3600000}") long expirationMs) {
        byte[] keyBytes = getSecretBytes(secret);
        this.secretKey = Keys.hmacShaKeyFor(keyBytes);
        this.expirationMs = expirationMs;
    }

    private byte[] getSecretBytes(String secret) {
        try {
            return Base64.getDecoder().decode(secret);
        } catch (IllegalArgumentException e) {
            return secret.getBytes();
        }
    }

    @SuppressWarnings("PMD.ReplaceJavaUtilDate")
    public String generateToken(String playerId) {
        Date now = Date.from(Instant.now());
        return Jwts.builder()
                .subject(playerId)
                .issuedAt(now)
                .expiration(Date.from(Instant.now().plusMillis(expirationMs)))
                .signWith(secretKey)
                .compact();
    }

    public boolean validate(String token) {
        try {
            Jwts.parser().verifyWith(secretKey).build().parseSignedClaims(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }

    public String getUserIdFromToken(String token) {
        return getClaims(token).getSubject();
    }

    public Authentication getAuthentication(String token) {
        String playerId = getUserIdFromToken(token);
        return new UsernamePasswordAuthenticationToken(playerId, null, List.of());
    }

    private Claims getClaims(String token) {
        return Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}

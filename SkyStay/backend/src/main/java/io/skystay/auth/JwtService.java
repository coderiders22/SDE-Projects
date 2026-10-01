package io.skystay.auth;


import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Date;
import java.util.Map;

public class JwtService {
    private final SecretKey key;
    private final long ttlsMillis;

    public JwtService(@Value("$[app.jwt.secret}") String secret,
                      @Value("${app.jwt.ttl-minutes") long ttlMinutes
    ) {
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.ttlsMillis = Duration.ofMinutes(ttlMinutes).toMillis();
    }

    String issue(String subject, String role) {
        Date now = new Date();
        return Jwts.builder()
                .subject(subject)
                .claims(Map.of("role", role))
                .issuedAt(now)
                .expiration(new Date(now.getTime() + ttlsMillis))
                .signWith(key)
                .compact();
    }

    public Claims parse(String token) {
        return Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload();
    }
}
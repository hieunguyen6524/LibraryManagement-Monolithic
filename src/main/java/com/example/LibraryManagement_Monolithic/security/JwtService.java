package com.example.LibraryManagement_Monolithic.security;

import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class JwtService {

    private final SecretKey key;
    private final long jwtExpirationMs;

    public JwtService(
            @Value("${LibraryManagement.app.jwtSecret}") String secret,
            @Value("${LibraryManagement.app.jwtExpirationMs}") long jwtExpirationMs) {

        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.jwtExpirationMs = jwtExpirationMs;
    }

    public String generateToken(String username, List<String> roles) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + jwtExpirationMs);

        return Jwts.builder()
                .subject(username)
                .issuedAt(now)
                .expiration(expiry)
                .claim("roles", roles)
                .signWith(key, Jwts.SIG.HS256)
                .compact();
    }

    public boolean validateToken(String token) {
        try {
            Jwts.parser()
                    .verifyWith(key)
                    .build()
                    .parseSignedClaims(token);
            return true;
        } catch (JwtException | IllegalArgumentException ex) {
            return false;
        }
    }

    public String getUsernameFromToken(String token) {
        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload()
                .getSubject();
    }

    //    @SuppressWarnings("unchecked")
    public List<String> getRolesFromToken(String token) {
        Object roles = Jwts.parser().verifyWith(key).build()
                .parseSignedClaims(token)
                .getPayload().get("roles");
        if (roles instanceof List) {
            return ((List<?>) roles).stream().map(Object::toString).collect(Collectors.toList());
        }
        return List.of();
    }

    public Date getExpirationDate(String token) {
        return Jwts.parser().verifyWith(key).build()
                .parseSignedClaims(token)
                .getPayload()
                .getExpiration();
    }
}

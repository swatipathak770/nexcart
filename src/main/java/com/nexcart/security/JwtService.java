package com.nexcart.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.util.Date;

@Service
public class JwtService {

    // Secret key used for signing JWT
    private static final SecretKey SECRET_KEY =
            Keys.secretKeyFor(SignatureAlgorithm.HS256);

    // Token validity: 24 hours
    private static final long EXPIRATION = 1000 * 60 * 60 * 24;

    // Generate JWT Token
    public String generateToken(String email) {

        return Jwts.builder()
                .subject(email)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + EXPIRATION))
                .signWith(SECRET_KEY)
                .compact();
    }

    // Extract email from JWT
    public String extractEmail(String token) {

        return getClaims(token).getSubject();
    }

    // Validate JWT
    public boolean isTokenValid(String token, String email) {

        return extractEmail(token).equals(email)
                && !isTokenExpired(token);
    }

    // Check expiration
    private boolean isTokenExpired(String token) {

        return getClaims(token)
                .getExpiration()
                .before(new Date());
    }

    // Read Claims
    private Claims getClaims(String token) {

        return Jwts.parser()
                .verifyWith(SECRET_KEY)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

}

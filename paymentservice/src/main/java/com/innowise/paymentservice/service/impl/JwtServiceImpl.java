package com.innowise.paymentservice.service.impl;

import com.innowise.paymentservice.service.JwtService;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;

@Service
public class JwtServiceImpl implements JwtService {

    private final SecretKey secretKey;

    public JwtServiceImpl(
            @Value("${jwt.secret}") String secret
    ) {
        this.secretKey = Keys.hmacShaKeyFor(secret.getBytes());
    }

    private Claims getClaims(String token) {
        return parse(token).getPayload();
    }

    private Jws<Claims> parse(String token) {
        return Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token);
    }

    public Long getUserId(String token) {
        return Long.valueOf(getClaims(token).getSubject());
    }

    public String getRole(String token) {
        return getClaims(token).get("role", String.class);
    }
}

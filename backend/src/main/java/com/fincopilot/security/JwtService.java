package com.fincopilot.security;

import com.fincopilot.entity.auth.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.UUID;

//jwt creare + validare
//security context e un per-request-container tinut in LocalThread storage de SecurityContextHolder
@Service
public class JwtService {

    private final SecretKey key;
    private final long ttlMillis;

    // timp de expirare 7 zile, sa il bag in yaml
    public JwtService(@Value("${jwt.secret}") String secret, @Value("${jwt.ttl-seconds:604800}") long ttlSeconds) {

        // symmetric encryption, creaza cheia
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.ttlMillis = ttlSeconds * 1000;
    }

    public String generateToken(User user) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + ttlMillis);

        return Jwts.builder().subject(user.getId().toString())
                .claim("org_id", user.getOrganization().getId().toString())
                .claim("email", user.getEmail()).claim("role", user.getRole()).issuedAt(now).expiration(expiry)
                .signWith(key).compact();
    } // header.payload.signature

    // validare + extragere claims
    public Claims validateAndParse(String token) {
        try {
            return Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload();
        } catch (Exception e) {
            return null;
        }
    }

    // claims -> principal
    public AuthPrincipal toPrincipal(Claims claims) {
        return new AuthPrincipal(
                UUID.fromString(claims.getSubject()),
                UUID.fromString(claims.get("org_id", String.class)),
                claims.get("email", String.class),
                claims.get("role", String.class));
    }
}

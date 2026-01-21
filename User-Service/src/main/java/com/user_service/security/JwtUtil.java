package com.user_service.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

@Component
public class JwtUtil {

    private final JwtProperties properties;

    public JwtUtil(JwtProperties properties) {
        this.properties = properties;
    }

    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    public <T> T extractClaim(String token, Function<Claims, T> resolver) {
        return resolver.apply(extractAllClaims(token));
    }

    public String generateToken(UserDetails userDetails, String role, String userId) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("role", role);
        claims.put("userId", userId);
        return buildToken(claims, userDetails, properties.getExpirationMs());
    }

    public String generateRefreshToken(UserDetails userDetails, String role, String userId) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("role", role);
        claims.put("userId", userId);
        claims.put("type", "refresh");
        return buildToken(claims, userDetails, properties.getRefreshExpirationMs());
    }

    private String buildToken(Map<String, Object> claims, UserDetails userDetails, long expiration) {
        return Jwts.builder()
                .claims(claims)
                .subject(userDetails.getUsername())
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + expiration))
                .signWith(getSignInKey())
                .compact();
    }

    public boolean isTokenValid(String token, UserDetails userDetails) {
        return extractUsername(token).equals(userDetails.getUsername())
                && !isTokenExpired(token);
    }

    private boolean isTokenExpired(String token) {
        return extractClaim(token, Claims::getExpiration).before(new Date());
    }

    private Claims extractAllClaims(String token) {
        return Jwts.parser()
                .verifyWith(getSignInKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    private SecretKey getSignInKey() {
        return Keys.hmacShaKeyFor(
                Decoders.BASE64.decode(properties.getSecret())
        );
    }

    public String extractRole(String token) {
        return extractClaim(token, c -> c.get("role", String.class));
    }

    public String extractUserId(String token) {
        return extractClaim(token, c -> c.get("userId", String.class));
    }

    public boolean isRefreshToken(String token) {
        return "refresh".equals(extractClaim(token, c -> c.get("type", String.class)));
    }
}

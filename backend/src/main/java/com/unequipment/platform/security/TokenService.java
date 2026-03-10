package com.unequipment.platform.security;

import com.unequipment.platform.modules.system.entity.SysUser;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.Date;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class TokenService {

    private final Key signingKey;
    private final long expireMillis;

    public TokenService(@Value("${app.security.jwt-secret}") String secret,
                        @Value("${app.security.jwt-expire-seconds}") long expireSeconds) {
        this.signingKey = Keys.hmacShaKeyFor(normalizeSecret(secret).getBytes(StandardCharsets.UTF_8));
        this.expireMillis = Math.max(expireSeconds, 60) * 1000L;
    }

    public String generate(SysUser user) {
        String roleCode = user.getPrimaryRoleCode() == null ? "INTERNAL_USER" : user.getPrimaryRoleCode();
        Date now = new Date();
        Date expireAt = new Date(now.getTime() + expireMillis);
        return Jwts.builder()
            .setSubject(String.valueOf(user.getId()))
            .claim("username", user.getUsername())
            .claim("role", roleCode)
            .setIssuedAt(now)
            .setExpiration(expireAt)
            .signWith(signingKey, SignatureAlgorithm.HS256)
            .compact();
    }

    public Long parseUserId(String token) {
        return Long.parseLong(parseClaims(token).getSubject());
    }

    public String parseRole(String token) {
        Object role = parseClaims(token).get("role");
        return role == null ? null : role.toString();
    }

    private Claims parseClaims(String token) {
        return Jwts.parserBuilder()
            .setSigningKey(signingKey)
            .build()
            .parseClaimsJws(token)
            .getBody();
    }

    private String normalizeSecret(String secret) {
        if (secret == null) {
            secret = "";
        }
        StringBuilder builder = new StringBuilder(secret);
        while (builder.length() < 32) {
            builder.append('0');
        }
        return builder.toString();
    }
}

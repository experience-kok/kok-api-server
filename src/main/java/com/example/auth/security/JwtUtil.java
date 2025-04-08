package com.example.auth.security;

import com.example.auth.exception.JwtValidationException;
import com.example.auth.exception.TokenErrorType;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.SignatureException;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.time.Instant;
import java.util.Date;

@Component
public class JwtUtil {

    @Value("${jwt.secret}")
    private String secret;

    @Value("${jwt.access-expiration}")
    private long accessExpiration;

    @Value("${jwt.refresh-expiration}")
    private long refreshExpiration;

    private Key key;

    @PostConstruct
    public void init() {
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    public String createAccessToken(Long userId) {
        return createToken(userId, accessExpiration);
    }

    public String createRefreshToken(Long userId) {
        return createToken(userId, refreshExpiration);
    }

    private String createToken(Long userId, long expiration) {
        Instant now = Instant.now();
        Instant expiry = now.plusMillis(expiration);

        return Jwts.builder()
                .setSubject(String.valueOf(userId))
                .setIssuedAt(Date.from(now))
                .setExpiration(Date.from(expiry))
                .signWith(key)  // 수정된 부분: SignatureAlgorithm 매개변수 제거
                .compact();
    }

    public Claims validateToken(String token) {
        try {
            return Jwts.parserBuilder()  // 수정된 부분: parser() 대신 parserBuilder() 사용
                    .setSigningKey(key)
                    .build()
                    .parseClaimsJws(token)
                    .getBody();
        } catch (ExpiredJwtException e) {
            throw new JwtValidationException("토큰이 만료되었습니다.", TokenErrorType.EXPIRED);
        } catch (UnsupportedJwtException | MalformedJwtException | SignatureException e) {
            throw new JwtValidationException("잘못되었거나 위조된 토큰입니다.", TokenErrorType.INVALID);
        } catch (Exception e) {
            throw new JwtValidationException("알 수 없는 JWT 오류", TokenErrorType.UNKNOWN);
        }
    }

    public Claims getClaimsIgnoreExpiration(String token) {
        try {
            return Jwts.parserBuilder()  // 수정된 부분: parser() 대신 parserBuilder() 사용
                    .setSigningKey(key)
                    .build()
                    .parseClaimsJws(token)
                    .getBody();
        } catch (ExpiredJwtException e) {
            // 만료된 토큰이어도 클레임 반환
            return e.getClaims();
        } catch (JwtException e) {
            throw new JwtValidationException("토큰 파싱 실패", TokenErrorType.INVALID);
        }
    }

    public Claims getClaims(String token) {
        return validateToken(token); // 중복 제거용 리팩토링
    }
}
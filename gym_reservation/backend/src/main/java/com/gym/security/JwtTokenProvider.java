package com.gym.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.security.Key;
import java.util.Date;
import java.util.List;

/** JWT 토큰 발급/검증 유틸(HS256) */
@Component
public class JwtTokenProvider {

    private final Key key;			// 서명키(32바이트 이상)
    private final long accessMs;	// 만료(ms)
    private final String issuer;	// 발급자

    public JwtTokenProvider(
            @Value("${jwt.secret}") String secret,
            @Value("${jwt.access-token-validity-seconds}") long seconds,
            @Value("${jwt.issuer}") String issuer) {
        this.key = Keys.hmacShaKeyFor(secret.getBytes()); // 길이 부족 시 예외
        this.accessMs = seconds * 1000L;
        this.issuer = issuer;
    }

    /** 토큰 생성: sub=회원ID, roles=권한배열 */
    public String createToken(String memberId, List<String> roles){
        long now = System.currentTimeMillis();
        return Jwts.builder()
                .setSubject(memberId)	// sub
                .setIssuer(issuer)		// iss
                .claim("roles", roles)	// ex) ["ROLE_USER","ROLE_ADMIN"]
                .setIssuedAt(new Date(now))	// iat
                .setExpiration(new Date(now + accessMs)) // exp
                .signWith(key, SignatureAlgorithm.HS256) // HS256
                .compact();
    }

    public boolean validate(String token){
        try { parse(token); return true; }
        catch (JwtException | IllegalArgumentException e) { return false; }
    }

    public String getMemberId(String token){ return parse(token).getBody().getSubject(); }

    @SuppressWarnings("unchecked")
    public List<String> getRoles(String token){ return (List<String>) parse(token).getBody().get("roles"); }

    private Jws<Claims> parse(String token){
        return Jwts.parserBuilder().setSigningKey(key).build().parseClaimsJws(token);
    }
}


// src/main/java/com/gym/security/TokenMaker.java
package com.gym.security;

import java.util.List;

public class TokenMaker {
    public static void main(String[] args) {
        String secret = "9PqZK5rX2tY7uAeH4mBvQ1sD8wCjR6LfT0NqU3xY"; // 서버랑 동일
        long validitySeconds = 3600L;
        String issuer = "gym-reservation";

        JwtTokenProvider provider = new JwtTokenProvider(secret, validitySeconds, issuer);
        String token = provider.createToken("hong8", List.of("ROLE_ADMIN"));
        System.out.println("JWT=" + token);
    }
}

/* ============================================================
[임시 설정] 개발 단계 전용(SecurityConfig)
- 목적: /health, /health/db, /v3/api-docs/**, /swagger-ui/** 만 permitAll
- 임시 허용: csrf.disable()  ← 개발 단계에서만 허용
- 금지: 운영(prod)에서 csrf.disable() 유지 금지, /health/db 외부 공개 금지
- 실전 전 TODO(반드시 수행):
  1) csrf.enable()로 복구
  2) /health/db 삭제 또는 내부망/IP 제한
  3) Swagger UI 외부 비공개(문서 JSON은 CI에서만 수집)
- 스택/규칙: STS4 + Spring Boot 3.4.9 + MyBatis + Log4j2 + Oracle + Gradle
- 금기: 파워셸, 임의 확장/리팩토링, 불필요한 엔드포인트 추가
============================================================ */

package com.gym.config;                                           // ⚙️ 설정 패키지

import org.springframework.context.annotation.Bean;                // ⚙️ @Bean 등록용
import org.springframework.context.annotation.Configuration;        // ⚙️ 설정 클래스 표시
import org.springframework.security.config.annotation.web.builders.HttpSecurity; // 🔐 보안 빌더
import org.springframework.security.web.SecurityFilterChain;         // 🔐 필터체인
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder; // 🔐 BCrypt 구현
import org.springframework.security.crypto.password.PasswordEncoder;     // 🔐 패스워드 인코더

/**
 * Spring Security 최소 뼈대 설정
 * - 목적: 헬스체크/Swagger만 공개, 나머지는 인증 필요(추후 정책 강화 전제)
 * - 주의: 개발 초기엔 CSRF 비활성(폼로그인/세션 정책 확립 전)
 */
@Configuration
public class SecurityConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {                    // 🔐 비밀번호 해싱(회원 가입/로그인 대비)
        return new BCryptPasswordEncoder();                       // 🔐 BCrypt 권장
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())                         // ⚠️ 개발 초기 임시: CSRF 비활성
            .authorizeHttpRequests(auth -> auth
                .requestMatchers(
                    "/health", "/health/db",			// ✅ 헬스 체크(앱/DB)
                    "/v3/api-docs/**", "/swagger-ui/**"	// ✅ Swagger(OpenAPI) 문서/화면
                    
                    /*--------- 임시 환경세팅 검증 테스트용---------*/
                    ,"/api/member/**",					// ✅ 임시 허용: 회원 단건 조회
                    "/api/facilities"  					// ✅ 임시 허용: 시설 목록
                    /*--------- 임시 환경세팅 검증 테스트용---------*/
                    
                ).permitAll()
                .anyRequest().authenticated()                     // 🔐 그 외는 인증 필요(기본 정책)
            );
        return http.build();
    }
}

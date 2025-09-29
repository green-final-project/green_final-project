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

package com.gym.config;

import org.springframework.context.annotation.Bean; // @Bean 등록용
import org.springframework.context.annotation.Configuration; // 설정 클래스 표시
import org.springframework.security.config.annotation.web.builders.HttpSecurity; // 보안 빌더
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityCustomizer;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder; // BCrypt 구현
import org.springframework.security.crypto.password.PasswordEncoder; // 패스워드 인코더
import org.springframework.security.web.SecurityFilterChain; // 필터체인

import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
// import com.gym.security.JwtTokenProvider; [250916 삭제]
import com.gym.security.NewJwtTokenProvider; // [250916 추가]
import com.gym.security.JwtAuthenticationFilter;
import org.springframework.http.HttpMethod; //[250917 추가]

@Configuration
@EnableWebSecurity // ★★★ 시큐리티 인식을 위해 꼭 필요함, 안그러면 에러는 403만 나옴 ★★★  
public class SecurityConfig {

    @Bean
    public PasswordEncoder passwordEncoder() { // 비밀번호 해싱(회원 가입/로그인 대비)
        return new BCryptPasswordEncoder(); // BCrypt
    }

    // security 적용 예외 URL 등록 (Swagger 등)
    @Bean
    WebSecurityCustomizer webSecurityCustomizer() {
        return (web) -> web.ignoring().requestMatchers("/",
                "/v3/api-docs/**",
                "/favicon.ico",
                "/swagger-ui/**",
                "/swagger-resources/**",
                "/webjars/**",
                "/sign-api/exception",
                "/__authprobe");
    }

    // [추가] JWT 토큰 유틸 주입자
    /*
     * private final JwtTokenProvider jwtTokenProvider;
     * public SecurityConfig(JwtTokenProvider jwtTokenProvider) {
     *   this.jwtTokenProvider = jwtTokenProvider;
     * }
     */
    // 수정
    private final NewJwtTokenProvider jwtTokenProvider;

    public SecurityConfig(NewJwtTokenProvider jwtTokenProvider) {
        this.jwtTokenProvider = jwtTokenProvider;
    }

    // [수정] 필터체인 메서드 1개로 통합(세션 무상태 + JWT 필터 등록 추가)
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        // [추가] 세션 완전 무상태: JWT 기반 인증
        http.sessionManagement(m -> m.sessionCreationPolicy(SessionCreationPolicy.STATELESS)); // [추가]

        // 기존 개발 초기 정책 유지
        http.csrf(csrf -> csrf.disable()) // 개발 초기 임시: CSRF 비활성(운영 전 복구)
            .authorizeHttpRequests(auth -> auth

            	    .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()   // ← 프리플라이트 허용(최상단)

            	    /* ========= 무인증 공개 영역(permitAll) ========= */
            	    .requestMatchers(
            	        "/health",    // 헬스 체크 → [GET]
            	        "/health/db", // DB 헬스 체크 → [GET]
            	        "/v3/api-docs/**",    // Swagger JSON → [GET]
            	        "/swagger-ui/**",     // Swagger UI → [GET]
            	        "/sign-api/**"        // 로그인/회원가입 → [POST/GET]
            	    ).permitAll()

            	    // 사용자 공개 조회 -------------------------
            	    .requestMatchers(
            	        "/api/facilities",    // 시설 목록 → [GET]
            	        "/api/facilities/*",  // 시설 단건 → [GET]
            	        "/api/boards/*/posts",    // 게시글 목록 → [GET]
            	        "/api/boards/*/posts/*"   // 게시글 상세 → [GET]
            	    ).permitAll()
            	                	    
            	    /* =========================== 콘텐츠 권한 분리  =========================== */
            	    // --- 콘텐츠 단건 조회: 누구나(permitAll) - 링크 클릭 시 단건 조회 허용 ---
            	    .requestMatchers(HttpMethod.GET, "/api/contents/**").permitAll() // GET: 콘텐츠 단건 조회 (뷰/링크)
            	    /* =========================== 콘텐츠 권한 분리  =========================== */
            	    
            	    /* =========================== 계좌 권한 분리 =========================== */
            	    // --- 계좌 등록/목록/대표지정/삭제: 로그인 본인만 허용 ---
            	    .requestMatchers(HttpMethod.POST, "/api/accounts").authenticated()           // POST: 계좌 등록
            	    .requestMatchers(HttpMethod.GET, "/api/members/*/accounts").authenticated() // GET: 회원별 계좌 목록 조회
            	    .requestMatchers(HttpMethod.PATCH, "/api/accounts/*/main").authenticated()  // PATCH: 대표계좌 설정
            	    .requestMatchers(HttpMethod.DELETE, "/api/accounts/*").authenticated()      // DELETE: 계좌 삭제
            	    /* =========================== 계좌 권한 분리 =========================== */

            	    /* =========================== 카드 권한 분리 =========================== */
            	    // --- 카드 등록/목록/대표지정/삭제: 로그인 본인만 허용 ---
            	    .requestMatchers(HttpMethod.POST, "/api/cards").authenticated()            // POST: 카드 등록
            	    .requestMatchers(HttpMethod.GET, "/api/members/*/cards").authenticated()   // GET: 회원별 카드 목록 조회
            	    .requestMatchers(HttpMethod.PATCH, "/api/cards/*/main").authenticated()    // PATCH: 대표카드 설정
            	    .requestMatchers(HttpMethod.DELETE, "/api/cards/*").authenticated()        // DELETE: 카드 삭제
            	    /* =========================== 카드 권한 분리 =========================== */

            	    /* ============================= CMS 관리 =========================== */
            	    // 계좌
            	    .requestMatchers("/api/cms/accounts/**").hasAnyAuthority("관리자","책임자","ROLE_ADMIN","admin")
            	    // 카드
            	    .requestMatchers("/api/cms/cards/**").hasAnyAuthority("관리자","책임자","ROLE_ADMIN","admin")
            	    // 콘텐츠 
            	    .requestMatchers("/api/cms/contents/**").hasAnyRole("ADMIN")
            	    // 시설
            	    .requestMatchers("/api/cms/facilities/**").hasAnyAuthority("강사","책임자","ROLE_ADMIN","admin")
            	    // 게시판
            	    .requestMatchers("/api/cms/boards/**").hasAnyAuthority("관리자","책임자","ROLE_ADMIN","admin")
            	    
            	    /* ======================= CMS 계좌/카드 관리 =========================== */
            	    
            	    /* ====================== 파일 권한 분리 [250923파일권한] ====================== */
            	    // ✅ 비로그인 허용: 목록/미리보기/다운로드(GET)
            	    .requestMatchers(HttpMethod.GET, "/api/files").permitAll()                   // 파일 목록
            	    .requestMatchers(HttpMethod.GET, "/api/files/*/preview").permitAll()        // 미리보기
            	    .requestMatchers(HttpMethod.GET, "/api/files/download").permitAll()         // 다운로드

            	    // 🔒 로그인 필요: 업로드/삭제
            	    .requestMatchers(HttpMethod.POST,   "/api/files/upload").authenticated()    // 업로드
            	    .requestMatchers(HttpMethod.DELETE, "/api/files/*").authenticated()         // 삭제
            	    /* ====================== 파일 권한 분리 [250923파일권한] ====================== */
            	    
            	    /* ====================== 공휴일 권한 분리 [250924권한] ====================== */
            	    // ✅ 비로그인 허용: 목록(GET)
            	    .requestMatchers(HttpMethod.GET, "/api/closed-days/**").permitAll() // 목록 조회
            	    
            	    // 🔒 로그인 필요: 등록/수정/삭제
            	    .requestMatchers("/api/cms/closed-days/**").hasAnyAuthority("강사","책임자","ROLE_ADMIN","admin")
            	    /* ====================== 파일 권한 분리 [250924권한] ====================== */
            	    
            	    /* ====================== 게시글 권한 분리 [250924게시글권한] ====================== */
            	    // ✅ 비로그인 허용: 목록/상세 조회(GET) — 사용자 화면용
            	    .requestMatchers(HttpMethod.GET, "/api/posts").permitAll() // 게시글 목록 조회(비로그인 허용)
            	    .requestMatchers(HttpMethod.GET, "/api/posts/*").permitAll()// 게시글 단건 조회(비로그인 허용)

            	    // 🔒 로그인 필요: 등록/수정/삭제 — 작성자 본인 여부는 컨트롤러에서 검사(관리자·최고관리자는 예외 허용)
            	    .requestMatchers(HttpMethod.POST,   "/api/posts").authenticated()      // 게시글 등록(로그인 필요)
            	    .requestMatchers(HttpMethod.PUT,    "/api/posts/*").authenticated()    // 게시글 수정(로그인 필요)
            	    .requestMatchers(HttpMethod.DELETE, "/api/posts/*").authenticated()    // 게시글 삭제(로그인 필요)

            	    // 🔒 CMS 전용: 관리자 권한만 접근 가능 — 담당자/관리자/책임자
            	    .requestMatchers("/api/cms/posts/**")
            	    .hasAnyAuthority("담당자","관리자","책임자") // CMS 게시글 관리(권한 계정만 허용)
            	    /* ====================== 게시글 권한 분리 [250924게시글권한] ====================== */
            	    
            	    /* ====================== 댓글 권한 분리 [250925 댓글 권한] ====================== */
            	    .requestMatchers(HttpMethod.GET, "/api/posts/*/comments").permitAll()
            	    .requestMatchers(HttpMethod.GET, "/api/posts/*/comments/**").permitAll()
            	    /* ====================== 댓글 권한 분리 [250925 댓글 권한] ====================== */
            	    
            	    
            	    /* ====================== 회원(사용자) API ====================== */
	            	// 회원가입(등록): 비로그인 허용 — 입력폼(application/x-www-form-urlencoded)으로 구현 예정
	            	.requestMatchers(HttpMethod.POST, "/api/members").permitAll()
	            	// 사용자 단건조회/수정: 로그인 필요(본인 확인은 컨트롤러/서비스에서 비밀번호로 재검증)
	            	.requestMatchers(HttpMethod.GET,  "/api/members/*").authenticated()
	            	.requestMatchers(HttpMethod.PUT,  "/api/members/*").authenticated()
	            	// 사용자 '목록/삭제'는 사용자 컨트롤러에선 제공하지 않으므로 차단(혹시 유입되어도 방어)
	            	.requestMatchers(HttpMethod.GET,    "/api/members").denyAll()
	            	.requestMatchers(HttpMethod.DELETE, "/api/members/*").denyAll()
	            	/* ====================== 회원(사용자) API ====================== */
	
	            	/* ====================== 회원(CMS) API ====================== */
	            	// CMS 회원 관리: ROLE_ADMIN만 1차 허용
	            	// ※ 최종 등급 검증은 컨트롤러에서 adminType == "책임자"로만 진행 가능
	            	.requestMatchers("/api/cms/closed-days/**").hasAnyAuthority("책임자", "ROLE_ADMIN", "admin")
	            	/* ====================== 회원(CMS) API ====================== */
            	    
            	    
            	    
            	    
            	    
            	    
            	    
            	    
            	    /* ========= 로그인 사용자(일반회원 이상) ========= */
            	    .requestMatchers(
            	        "/api/members/*",    // 내 정보 조회/수정/삭제 → [GET/PUT/DELETE]
            	        "/api/reservations/**",   // 예약 신청/변경/조회/삭제 → [POST/PUT/GET/DELETE]
            	        "/api/boards/*/posts",    // 게시글 등록 → [POST]
            	        "/api/boards/*/posts/*",  // 게시글 수정/삭제 → [PUT/DELETE]
            	        // "/api/comments/**",       // 댓글 등록/수정/삭제 → [POST/PUT/DELETE]
            	        "/api/payments",          // 결제 등록 → [POST]
            	        "/api/payments/search"    // 결제 목록/검색 → [GET]
            	    ).authenticated()

            	    /* ========= 담당자/최고관리자 ========= */
            	    
//            	    .requestMatchers(
//            	        "/api/facilities",    // 시설 생성 → [POST]
//            	        "/api/facilities/*",  // 시설 수정/삭제 → [PUT/DELETE]
//            	        "/api/facilities/*/use" // 시설 사용여부 변경 → [PATCH]
//            	    ).hasAnyAuthority("담당자","책임자")


            	    /* ========= 관리자/최고관리자 (기타 영역) ========= */
//            	    .requestMatchers(
//            	        "/api/cms/boards/**",// CMS 게시판 관리 → [GET/POST/PUT/DELETE]
//            	        "/api/payments/*/status" // 결제 상태 변경 → [PUT]
//            	    ).hasAnyAuthority("관리자","최고관리자")

            	    /* ========= 최고관리자 전용 ========= */
//            	    .requestMatchers(
//            	        "/api/members",      // 회원 목록(관리) → [GET]
//            	        "/api/paymentlogs/**" // 결제 로그 조회 → [GET]
//            	        
//            	    ).hasAuthority("최고관리자")

            	    /* ========= 기타 ========= */
            	    .anyRequest().authenticated()
            	);

        // JWT 인증 필터 등록 (스프링의 UsernamePasswordAuthenticationFilter 앞에 삽입)
        http.addFilterBefore(new JwtAuthenticationFilter(jwtTokenProvider), UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

}

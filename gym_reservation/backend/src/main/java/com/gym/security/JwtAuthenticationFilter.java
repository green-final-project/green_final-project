package com.gym.security;

import java.io.IOException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

public class JwtAuthenticationFilter extends OncePerRequestFilter {

	private final Logger LOGGER = LoggerFactory.getLogger(JwtAuthenticationFilter.class);

//  private final JwtTokenProvider jwtTokenProvider;

	private final NewJwtTokenProvider newJwtTokenProvider;

//  public JwtAuthenticationFilter(JwtTokenProvider jwtTokenProvider) {
//      this.jwtTokenProvider = jwtTokenProvider;
//  }

	public JwtAuthenticationFilter(NewJwtTokenProvider jwtTokenProvider) {
		this.newJwtTokenProvider = jwtTokenProvider;
	}

	// 필터 사이클
	@Override
	protected void doFilterInternal(HttpServletRequest servletRequest, HttpServletResponse servletResponse,
			FilterChain filterChain) throws ServletException, IOException {

		// String token = jwtTokenProvider.resolveToken(servletRequest);
		// String token = newJwtTokenProvider.resolveToken(servletRequest);
		String token = resolveToken(servletRequest);  // ← 필터 내부 메서드 사용(X-AUTH-TOKEN 우선, Bearer도 허용)

		LOGGER.info("[doFilterInternal] token 값 추출 완료. token : {}", token);

		LOGGER.info("[doFilterInternal] token 값 유효성 체크 시작");

		// if (token != null && jwtTokenProvider.validateToken(token)) {
		if (token != null && newJwtTokenProvider.validateToken(token)) {

			// Authentication authentication = jwtTokenProvider.getAuthentication(token);
			Authentication authentication = newJwtTokenProvider.getAuthentication(token);
			SecurityContextHolder.getContext().setAuthentication(authentication);

			LOGGER.info("[doFilterInternal] token 값 유효성 체크 완료");
		}

		filterChain.doFilter(servletRequest, servletResponse); //필터 체인지
	}

	// JwtAuthenticationFilter 내부 (패키지: com.gym.security)
	// 목적: 요청 헤더에서 JWT 토큰을 안전하게 추출(X-AUTH-TOKEN 우선, 없으면 Authorization: Bearer)
	private String resolveToken(HttpServletRequest request) {
	    // 1) Swagger에서 설정한 헤더명: X-AUTH-TOKEN (apiKey) → 여기서 먼저 읽음
	    String token = request.getHeader("X-AUTH-TOKEN"); // 헤더에서 토큰 직접 추출
	    if (token != null && !token.isBlank()) {
	        return token; // 값이 있으면 그대로 반환
	    }

	    // 2) 대안: Authorization: Bearer <JWT> 형식도 허용 (포스트맨/기타 클라이언트 호환)
	    String auth = request.getHeader("Authorization"); // Authorization 헤더 추출
	    if (auth != null && auth.startsWith("Bearer ")) { // "Bearer " 접두 확인
	        return auth.substring(7); // "Bearer " 이후 문자열(JWT)만 반환
	    }

	    // 3) 둘 다 없으면 null (필터 상위에서 null 처리)
	    return null; // 토큰 없음
	}


}
package com.gym.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Bean;

import io.swagger.v3.oas.models.OpenAPI;                    // OpenAPI 스펙 루트
import io.swagger.v3.oas.models.Components;                // 컴포넌트 컨테이너
import io.swagger.v3.oas.models.security.SecurityScheme;   // 보안 스키마 정의
import io.swagger.v3.oas.models.security.SecurityRequirement; // 보안 요구사항(전역 적용)

@Configuration
public class OpenApiSecurityConfig {

    @Bean
    public OpenAPI openAPI() {
        // 1) 보안 스키마 등록: X-AUTH-TOKEN (헤더, apiKey)
        String schemeName = "X-AUTH-TOKEN";
        SecurityScheme apiKeyHeader = new SecurityScheme()
                .type(SecurityScheme.Type.APIKEY)     // API Key 방식
                .in(SecurityScheme.In.HEADER)         // 헤더로 보냄
                .name("X-AUTH-TOKEN");                // 헤더명 정확히 일치

        // 2) 전역 보안 요구사항 추가: 모든 API에 X-AUTH-TOKEN 붙이기
        SecurityRequirement globalAuth = new SecurityRequirement().addList(schemeName);

        return new OpenAPI()
                .components(new Components().addSecuritySchemes(schemeName, apiKeyHeader))
                .addSecurityItem(globalAuth);
    }
}

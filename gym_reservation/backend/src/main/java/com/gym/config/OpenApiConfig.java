/* ============================================================
[Swagger 문서 설정] OpenApiConfig
- 목적: Swagger UI 문서 제목/설명/버전 표기
- 임시 허용: dev에서 UI 공개
- 금지: prod에서 UI 외부 공개
- 실전 전 TODO:
  1) prod 환경에선 Swagger UI 차단(문서 JSON은 CI에서만 수집)
============================================================ */
package com.gym.config;                                         // ⚙️ 설정 패키지

import io.swagger.v3.oas.models.OpenAPI;                        // 📖 OpenAPI 모델
import io.swagger.v3.oas.models.info.Info;                      // 📖 문서 정보
import org.springframework.context.annotation.*;                 // ⚙️ 스프링 설정

@Configuration
public class OpenApiConfig {                                    // 📖 Swagger 문서 설정

    @Bean
    public OpenAPI gymOpenAPI(){                                 // 🧩 OpenAPI 빈 등록
        return new OpenAPI().info(new Info()
            .title("Gym Reservation API")                        // 문서 제목
            .description("체육관 예약 시스템 API")                 // 문서 설명
            .version("v1"));                                     // 문서 버전
    }
}
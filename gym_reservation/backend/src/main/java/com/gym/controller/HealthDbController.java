/* ============================================================
[임시 헬스체크(DB)] HealthDbController
- 목적: DB 커넥션 실쿼리 확인(SELECT 1 FROM DUAL)
- 임시 허용: dev에서만 공개
- 실전 전 TODO:
  1) 이 파일 삭제 또는 내부망/IP 제한
============================================================ */
package com.gym.controller;                                     // 📦 컨트롤러 패키지

import org.springframework.http.*;                               // 🌐 응답 타입
import org.springframework.jdbc.core.JdbcTemplate;               // 🧰 간단 쿼리 실행
import org.springframework.web.bind.annotation.*;                // 🌐 REST 어노테이션

@RestController
public class HealthDbController {                                // ❤️ DB 헬스(임시)
    private final JdbcTemplate jt;                               // 💉 JdbcTemplate 주입
    public HealthDbController(JdbcTemplate jt){ this.jt = jt; }  // 🧩 생성자 주입

    @GetMapping("/health/db")                                    // 🌐 GET /health/db
    public ResponseEntity<String> db(){                          // 🚦 DB 상태 응답
        Integer one = jt.queryForObject("SELECT 1 FROM DUAL", Integer.class); // 🔎 최소 쿼리
        return ResponseEntity.ok("DB_OK:" + one);                // ✅ "DB_OK:1" 기대
    }
}


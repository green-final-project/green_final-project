package com.gym.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 서버 생존/상태 확인용 컨트롤러
 * - GET /health → "OK" 문자열 반환
 */
@RestController
public class HealthController {
	/*
	 * GET : 서버에서 데이터를 조회(Read) 할 때 사용
	 * /health : 서버 상태를 확인하는 엔드포인트 (브라우저에서 http://localhost:8080/health 입력)
	 */
    @GetMapping("/health")  // 🌐 GET /health 요청을 처리 → 서버 활성화 확인 용도
    public String health() { return "OK"; } // 단순 상태 반환 → "OK"라는 메시지가 나오면 활성화 상태 
}
// src/main/java/com/gym/api/AuthProbeController.java
package com.gym.api;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;

import java.security.Principal;

/** 인증 성공 시 200 + 로그인 아이디를 그대로 돌려주는 초간단 확인용 API */
@RestController
public class AuthProbeController {
	@Operation(
	        summary = "인증 확인 API",
	        description = "인증 성공 시 200 OK와 함께 로그인 아이디 또는 토큰을 반환합니다."
	    )
    @GetMapping("/AuthProbeController")
    public String probe(@Parameter(description = "token", required = true)
    					@RequestHeader(value = "x-auth-token", required = true) String xAuthToken,
    					Principal principal) {
        // return "OK:" + (principal == null ? "ANON" : principal.getName());
    	return "OK;" + xAuthToken; // 성공하면 OK 접두사가 나오도록 세팅
    	// 토큰 전달이 되었다면, 다음에는 인증 검사를 해야 함
    }
}

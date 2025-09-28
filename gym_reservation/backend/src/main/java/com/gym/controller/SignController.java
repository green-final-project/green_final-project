package com.gym.controller;

import com.gym.security.NewJwtTokenProvider; // JWT 토큰 발급/검증 유틸 클래스
import com.gym.security.mapper.LoginQueryMapper; // DB에서 회원 정보/권한을 조회하는 매퍼

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

import org.springframework.http.ResponseEntity; // HTTP 응답을 만들 때 사용하는 클래스
import org.springframework.security.crypto.password.PasswordEncoder; // 비밀번호 해시 검증용(BCrypt)
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Tag(name = "00.로그인", description = "로그인 후, 발행되는 토큰을 상단 Authorize에 입력")
@RestController // REST API 컨트롤러임을 표시
@RequestMapping("/sign-api") // 이 컨트롤러의 기본 URL 경로(prefix)
public class SignController {

    private final LoginQueryMapper loginQueryMapper; // 회원 아이디/비밀번호/권한 조회 담당
    private final PasswordEncoder passwordEncoder; // 비밀번호를 BCrypt 방식으로 비교
    private final NewJwtTokenProvider jwt; // JWT 토큰 발급기

    public SignController(LoginQueryMapper loginQueryMapper,
                          PasswordEncoder passwordEncoder,
                          NewJwtTokenProvider jwt) { // 생성자 주입
        this.loginQueryMapper = loginQueryMapper;
        this.passwordEncoder = passwordEncoder;
        this.jwt = jwt;
    }

    @Operation(
	        summary = "로그인 토큰 테스트",
	        description = "계정 ID 및 패스워드 입력 후 발행되는 토큰 확인 목적"
	)
    @PostMapping("/sign-in") // POST /sign-api/sign-in 호출 시 실행
    public ResponseEntity<?> signIn(@RequestParam("userId") String userId, // 요청 파라미터 userId
                                    @RequestParam("password") String password) { // 요청 파라미터 password
        var row = loginQueryMapper.findUser(userId); // DB에서 해당 회원 아이디 조회
        if (row == null) { return ResponseEntity.status(400).body(Map.of("code","400","message","회원 없음")); } // 회원 없음

        String dbPw = row.getPasswordHash(); // DB 저장 비밀번호(해시 또는 평문)
        boolean ok = false; // 비밀번호 일치 여부
        if (dbPw != null && dbPw.length() >= 60) { ok = passwordEncoder.matches(password, dbPw); } // 해시면 matches
        else { ok = password.equals(dbPw); } // 평문이면 equals(이행기간 임시 허용)

        if (!ok) { return ResponseEntity.status(401).body(Map.of("code","401","message","비밀번호 불일치")); } // 불일치 시 401

        List<String> rawRoles = loginQueryMapper.findRoles(userId); // 회원 권한 조회
        List<String> secuRoles = rawRoles.stream().map(this::toSecRole).toList(); // 권한 변환

        String token = jwt.createToken(userId, secuRoles); // 토큰 발급

        return ResponseEntity.ok() // 응답 반환
                .header("X-AUTH-TOKEN", token) // 응답 헤더에 토큰 추가
                .body(Map.of("userId", userId, "roles", secuRoles, "token", token)); // JSON 응답에 토큰 포함
    }

    private String toSecRole(String raw) {
        return switch (raw) {
            case "admin","관리자","최고관리자","담당자" -> "ROLE_ADMIN"; // 관리자 계열은 ROLE_ADMIN
            case "user","회원" -> "ROLE_USER"; // 일반 회원은 ROLE_USER
            default -> "ROLE_USER"; // 알 수 없는 값은 기본 ROLE_USER
        };
    }
    
    // 비밀번호 암호화
    // [250917] 암호화 테스트는 완료되었음으로 주석처리
    /*@Operation(
	        summary = "비밀번호 암호화",
	        description = "쿼리 파라미터 raw에 전달한 문자열을 BCrypt 알고리즘으로 암호화하여 반환."
	)
    @GetMapping("/_bcrypt") // GET /sign-api/_bcrypt?raw=1234  // 작업 끝나면 삭제
    public ResponseEntity<String> bcrypt(@RequestParam("raw") String raw) { // 평문 비번(raw)을 해시로 변환
        return ResponseEntity.ok(passwordEncoder.encode(raw)); // BCrypt 해시 문자열 그대로 반환
    }*/

}

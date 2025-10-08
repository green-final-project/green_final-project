// [1] 패키지 및 import
package com.gym.controller;

import com.gym.security.NewJwtTokenProvider; // [1-1] JWT 토큰 발급/검증
import com.gym.security.mapper.LoginQueryMapper; // [1-2] 회원 로그인 전용 매퍼
import com.gym.domain.member.Member; // [1-3] member_tbl과 매핑된 DTO

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Tag(name = "00.로그인", description = "로그인 후, 발행되는 토큰을 상단 Authorize에 입력")
@RestController
@RequestMapping("/sign-api")
@CrossOrigin("*") // [1-4] CORS 허용
public class SignController {

    private final LoginQueryMapper loginQueryMapper; // [2-1] 로그인 쿼리 매퍼
    private final PasswordEncoder passwordEncoder;   // [2-2] 비밀번호 검증기(BCrypt)
    private final NewJwtTokenProvider jwt;           // [2-3] JWT 토큰 제공자

    public SignController(LoginQueryMapper loginQueryMapper,
                          PasswordEncoder passwordEncoder,
                          NewJwtTokenProvider jwt) {
        this.loginQueryMapper = loginQueryMapper;
        this.passwordEncoder = passwordEncoder;
        this.jwt = jwt;
    }

    @Operation(summary = "로그인", description = "아이디/비밀번호 확인 후 토큰과 전체 회원정보 반환")
    @PostMapping("/sign-in")
    public ResponseEntity<?> signIn(@RequestParam("userId") String userId,
                                    @RequestParam("password") String password) {
        // [3-1] DB에서 userId로 회원 조회
        var row = loginQueryMapper.findUser(userId);
        if (row == null) {
            return ResponseEntity.status(400).body(Map.of("code","400","message","회원 없음"));
        }

        // [3-2] 비밀번호 검증 (BCrypt 해시 또는 평문)
        String dbPw = row.getPasswordHash();
        boolean ok = (dbPw != null && dbPw.length() >= 60)
                ? passwordEncoder.matches(password, dbPw)
                : password.equals(dbPw);
        if (!ok) {
            return ResponseEntity.status(401).body(Map.of("code","401","message","비밀번호 불일치"));
        }

        // [3-3] 권한 조회 후 ROLE 변환
        List<String> rawRoles = loginQueryMapper.findRoles(userId);
        List<String> secuRoles = rawRoles.stream().map(this::toSecRole).toList();

        // [3-4] JWT 토큰 발급
        String token = jwt.createToken(userId, secuRoles);

        // [3-5⚠️] 회원 전체 정보 조회 [250930]
        // ⚠️ old: 원래는 userId/roles/token만 반환했음
		    /*
		    return ResponseEntity.ok()
		            .header("X-AUTH-TOKEN", token)
		            .body(Map.of("userId", userId, "roles", secuRoles, "token", token));
		    */
        // Member member = loginQueryMapper.selectMemberById(userId); //⚠️ old: 결과적으로 회원 전체 정보 조회 후 user + token 반환했음
        
        // ------------------------------------------- ⚠️ [251006] 관리자 전용 필터 (CMS 로그인용) -------------------------------------------
        Member member = loginQueryMapper.selectMemberById(userId); // 로그인한 회원의 정보 조회 실행
        if (member == null) {	// 회원 값이 DB에 존재하지 않을 경우
            return ResponseEntity.status(404).body(Map.of( // 404에러 발생(회원 데이터가 없을 경우) 
            		"code","404", // 응답 코드
            		"message","회원 데이터 없음"	// 응답 메시지
            ));
        }
        String requestPath = "";  // 현재 요청 경로(request URI)를 확인하기
        try {
        	// RequestContextHolder를 통해 현재 요청 정보를 가져옴
            requestPath = java.util.Optional.ofNullable(
                org.springframework.web.context.request.RequestContextHolder 
                    .getRequestAttributes()
            )
            // ServletRequestAttributes로 캐스팅 후 실제 요청 URI 추출
            .map(attr -> ((org.springframework.web.context.request.ServletRequestAttributes) attr).getRequest().getRequestURI())
            .orElse("");
        // 예외 또는 null일 경우 빈 문자열로 대체
        } catch (Exception ignore) {}
    
        // CMS 경로 요청일 때만 관리자 필터 실행
        if (requestPath.startsWith("/cms") && ("user".equalsIgnoreCase(member.getMemberRole()))) 
            return ResponseEntity.status(403).body(Map.of(
                "code","403",
                "message","관리자 전용 계정만 로그인 가능합니다."
            ));

        // ------------------------------------------------------------------------------------------------------------------------------
        
        // [3-6] 응답 반환 : 토큰 + 전체 회원 정보
        return ResponseEntity.ok(Map.of(
                "user", member,
                "token", token
        ));
    }

    // [4] 원본 권한값을 Spring Security ROLE로 변환
    private String toSecRole(String raw) {
        return switch (raw) {
            //case "admin","관리자","최고관리자","담당자" -> "ROLE_ADMIN"; // old
            case "admin","관리자","책임자","강사" -> "ROLE_ADMIN"; // new
            case "user","회원" -> "ROLE_USER";
            default -> "ROLE_USER";
        };
    }
}


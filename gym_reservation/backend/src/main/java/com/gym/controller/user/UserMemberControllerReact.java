package com.gym.controller.user;

import com.gym.common.ApiResponse;
import com.gym.domain.member.Member;
import com.gym.domain.member.MemberResponse; // ✅ 조회용 DTO
import com.gym.service.MemberService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.bind.annotation.*;

import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

import java.security.Principal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

/**
 * 회원 API (단건 조회/등록/수정)
 * UI → Controller → Service → Mapper → Oracle
 *
 * [250916 암호화기능 추가]
 * [250926 수정] 단건조회/수정 시 로그인ID + 비밀번호 검증 방식 적용
 */
@Tag(name = "01.Member-USER", description = "회원 API (등록/단건조회/수정)")
@RestController
@RequestMapping("/api/membersTEMP")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin("*") // [250929] 클라이언트와 연동하는 목적 (*로 하면, 모든 클라이언트와 접속이 됨)
public class UserMemberControllerReact {
	
	private final MemberService memberService;

    //============================================================
    // 회원 조회 테스트
    //============================================================
    @GetMapping("/test")
    @CrossOrigin("*") // [250929] 클라이언트와 연동하는 목적 (*로 하면, 모든 클라이언트와 접속이 됨)
    public String test(
    		@RequestParam("memberId") String memberId,
    		@RequestHeader(value = "X-AUTH-TOKEN", required = true) String xAuthToken
    		) {
    	log.info("ID 입력 결과:{}", memberId);
    	log.info("토큰 결과:{}", xAuthToken);
    	return "테스트";
    }
    
    // ============================================================
    // 내 회원 정보 조회250928 개선형 (/me)
    // ============================================================
    /*
    @GetMapping("/me") // ← 동일 경로를 갖는 메서드는 이 하나만 존재해야 함
    @Operation(summary = "내 회원 정보 조회", description = "로그인한 사용자의 토큰 정보를 기반으로 자기 자신의 회원 정보를 조회한다.")
    public ApiResponse<MemberResponse> getMyInfo1(Authentication authentication) { //Authentication 토큰의 보안 검증용
        // [인증검증] 토큰이 없거나, 토큰에서 사용자명을 못 읽으면 401
        if (authentication == null || authentication.getName() == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "인증 정보가 없습니다.");
        }

        // [회원ID확정] JWT subject(username)
        final String memberId = authentication.getName();
        log.info("현재 로그인 사용자 ID = {}", memberId); // [로그]

        // [DB조회] 서비스 통해 단건 조회
        final Member member = memberService.getMemberById(memberId);
        if (member == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "회원 정보를 찾을 수 없습니다.");
        }

        // [핵심] Entity → DTO에서 조회를 원하는 값들 매핑
        final MemberResponse response = MemberResponse.builder()
                .memberId(member.getMemberId())                 // 회원ID
                .memberName(member.getMemberName())             // 이름
                .memberGender(member.getMemberGender())         // 성별
                .memberEmail(member.getMemberEmail())           // 이메일
                .memberMobile(member.getMemberMobile())         // 휴대폰
                .memberPhone(member.getMemberPhone())           // 전화
                .zip(member.getZip())                           // 우편번호
                .roadAddress(member.getRoadAddress())           // 도로명주소
                .jibunAddress(member.getJibunAddress())         // 지번주소
                .detailAddress(member.getDetailAddress())       // 상세주소
                .memberBirthday(member.getMemberBirthday())     // 생년월일
                .memberManipay(member.getMemberManipay())       // 회비/관리비
                .memberJoindate(member.getMemberJoindate())     // 가입일
                .memberRole(member.getMemberRole())             // 권한
                .adminType(member.getAdminType())               // 관리자유형
                .build();

        return ApiResponse.ok(response); // [응답]
    }
    */
    
}

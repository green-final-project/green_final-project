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
@RequestMapping("/api/members")
@RequiredArgsConstructor
@Slf4j
public class UserMemberController {

    private final MemberService memberService;

    @Autowired
    BCryptPasswordEncoder bCryptPasswordEncoder;

    // ============================================================
    // 회원 등록
    // ============================================================
    @Operation(summary = "회원 등록", description = "member_tbl INSERT (폼 입력, application/x-www-form-urlencoded)")
    @PostMapping(consumes = "application/x-www-form-urlencoded")
    public ApiResponse<Integer> createMember(
            @Parameter(description = "회원ID(필수)") @RequestParam String memberId,
            @Parameter(description = "비밀번호(필수)") @RequestParam String memberPw,
            @Parameter(description = "회원이름(필수)") @RequestParam String memberName,
            @Parameter(description = "성별(필수)", schema = @io.swagger.v3.oas.annotations.media.Schema(allowableValues = {"m", "f"}))
            @RequestParam String memberGender,
            @Parameter(description = "이메일(필수)") @RequestParam String memberEmail,
            @Parameter(description = "휴대폰(필수)") @RequestParam String memberMobile,
            @Parameter(description = "생년월일(필수, YYYY-MM-DD)") @RequestParam String memberBirthday,
            @Parameter(description = "우편번호(선택, 5자리)") @RequestParam(required = false) String zip,
            @Parameter(description = "전화번호(선택)") @RequestParam(required = false) String memberPhone,
            @Parameter(description = "도로명주소(선택)") @RequestParam(required = false) String roadAddress,
            @Parameter(description = "지번주소(선택)") @RequestParam(required = false) String jibunAddress,
            @Parameter(description = "상세주소(선택)") @RequestParam(required = false) String detailAddress
    ) {
        log.info("[POST]/api/members req memberId={}", memberId);

        Member req = new Member();
        req.setMemberId(memberId);
        req.setMemberPw(bCryptPasswordEncoder.encode(memberPw)); // [비번암호화] 신규 등록 시 BCrypt로 해시 저장
        req.setMemberName(memberName);
        req.setMemberGender(memberGender);
        req.setMemberEmail(memberEmail);
        req.setMemberMobile(memberMobile);
        req.setMemberPhone(memberPhone);
        req.setRoadAddress(roadAddress);
        req.setJibunAddress(jibunAddress);
        req.setDetailAddress(detailAddress);

        // 생년월일 파싱
        try {
            LocalDate birthday = LocalDate.parse(memberBirthday, DateTimeFormatter.ISO_DATE); // [형식검증]
            req.setMemberBirthday(birthday);
        } catch (DateTimeParseException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "memberBirthday는 YYYY-MM-DD 형식이어야 합니다.");
        }

        // ZIP 보정
        String zipFix = (zip == null || zip.isBlank() || "string".equalsIgnoreCase(zip)) ? null : zip; // [값보정]
        if (zipFix != null && zipFix.length() > 5) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "우편번호는 최대 5자리까지 입력 가능합니다."); // [형식검증]
        }
        req.setZip(zipFix);

        // 회원권한 자동 user
        req.setMemberRole("user"); // [기본값설정]

        int affected = memberService.createMember(req); // [DB처리]
        return ApiResponse.ok(affected);
    }

    // ============================================================
    // 내 회원 정보 조회250928 개선형 (/me)
    // ============================================================
    @GetMapping("/me") // ← 동일 경로를 갖는 메서드는 이 하나만 존재해야 함
    @Operation(summary = "내 회원 정보 조회", description = "로그인한 사용자의 토큰 정보를 기반으로 자기 자신의 회원 정보를 조회한다.")
    public ApiResponse<MemberResponse> getMyInfo1(Authentication authentication) {
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
    
    

    // ============================================================
    // 내 회원 정보 수정 (/me)
    // ============================================================
    @Operation(
        summary = "회원 수정",
        description = "로그인 계정 본인 확인 + 현재 비밀번호 검증 후 수정."
    )
    @PutMapping(value = "/me", consumes = "application/x-www-form-urlencoded")
    public ApiResponse<Integer> updateMemberMe(
            Principal principal,
            @Parameter(description = "현재 비밀번호(필수)") @RequestParam("currentPw") String currentPw,
            @Parameter(description = "변경할 새 비밀번호(선택). 미입력 시 기존 유지. 기존이 평문이면 이번 요청에서 해시 업그레이드") @RequestParam(value = "newPw",         required = false) String newPw,
            @Parameter(description = "이메일(선택)") @RequestParam(value = "memberEmail",   required = false) String memberEmail,
            @Parameter(description = "휴대폰(선택)") @RequestParam(value = "memberMobile",  required = false) String memberMobile,
            @Parameter(description = "전화번호(선택)") @RequestParam(value = "memberPhone",   required = false) String memberPhone,
            @Parameter(description = "도로명주소(선택)") @RequestParam(value = "roadAddress",   required = false) String roadAddress,
            @Parameter(description = "상세주소(선택)") @RequestParam(value = "detailAddress", required = false) String detailAddress
    ) {
        if (principal == null) // [인증검증]
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "인증 정보가 없습니다.");
        String loginId = principal.getName(); // [아이디확정]

        Member existing = memberService.getMemberById(loginId); // [DB조회]
        if (existing == null) // [예외처리]
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "회원 정보를 찾을 수 없습니다.");

        String stored = existing.getMemberPw();          // [비번검증-준비] DB 저장된 비번(평문 또는 해시)
        boolean isBCrypt = looksLikeBCrypt(stored);      // [비번검증] 저장 포맷 판별
        boolean ok = isBCrypt
                ? bCryptPasswordEncoder.matches(currentPw, stored)  // [비번검증-해시매칭]
                : stored != null && stored.equals(currentPw);       // [비번검증-평문비교]
        if (!ok) // [예외처리]
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "비밀번호가 일치하지 않습니다.");

        newPw         = sanitize(newPw);          // [값보정]
        memberEmail   = sanitize(memberEmail);    // [값보정]
        memberMobile  = sanitize(memberMobile);   // [값보정]
        memberPhone   = sanitize(memberPhone);    // [값보정]
        roadAddress   = sanitize(roadAddress);    // [값보정]
        detailAddress = sanitize(detailAddress);  // [값보정]

        Member req = new Member();                // [부분수정] null이 아닌 필드만 Mapper가 업데이트하도록 전달

        if (newPw != null) { // [비번암호화] 새 비번이 오면 해시하여 저장
            req.setMemberPw(bCryptPasswordEncoder.encode(newPw));
        } else if (!isBCrypt && stored != null) { // [비번암호화-업그레이드] 기존이 평문이면 이번 요청에서 해시 업그레이드
            req.setMemberPw(bCryptPasswordEncoder.encode(stored));
        }

        if (memberEmail   != null) req.setMemberEmail(memberEmail);     // [부분수정-이메일]
        if (memberMobile  != null) req.setMemberMobile(memberMobile);   // [부분수정-휴대폰]
        if (memberPhone   != null) req.setMemberPhone(memberPhone);     // [부분수정-전화]
        if (roadAddress   != null) req.setRoadAddress(roadAddress);     // [부분수정-주소]
        if (detailAddress != null) req.setDetailAddress(detailAddress); // [부분수정-상세주소]

        int affected = memberService.updateMember(loginId, req); // [DB처리-업데이트]
        return ApiResponse.ok(affected); // [응답]
    }

    // [250926] 값 보정; "string"/"" → null
    private static String sanitize(String v) {
        if (v == null) return null;        // [값보정]
        String t = v.trim();               // [값보정]
        if (t.isEmpty()) return null;      // [값보정]
        if ("string".equalsIgnoreCase(t)) return null; // [값보정]
        return t;                          // [값보정]
    }

    // [250926] BCrypt 포맷 검출(평문 대비)
    private static boolean looksLikeBCrypt(String s) {
        return s != null && (s.startsWith("$2a$") || s.startsWith("$2b$") || s.startsWith("$2y$")); // [비번검증-포맷판별]
    }
}

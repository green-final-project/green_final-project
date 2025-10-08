package com.gym.controller.cms;

import com.gym.common.ApiResponse;
import com.gym.domain.member.Member;
import com.gym.domain.member.MemberResponse; // [DTO-조회용]
import com.gym.mapper.annotation.AccountMapper;
import com.gym.mapper.annotation.CardMapper;
import com.gym.service.MemberService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Map;


/**
 * CMS 회원 관리 API UI → Controller → Service → Mapper → Oracle
 *
 * [250927 신규] CMS 책임자 전용 컨트롤러 - 책임자(admin_type='책임자')만 접근 가능 - 등록/수정/삭제/목록/단건조회
 * 제공
 */
@CrossOrigin("*")
@Tag(name = "01.Member-CMS", description = "CMS 회원 관리 API (등록/수정/삭제/목록/단건조회)")
@RestController
@RequestMapping("/api/cms/members")
//@RequestMapping({"/sign-api", "/api/cms/sign-api"}) // ✅ 두 경로 모두 허용
@RequiredArgsConstructor
@Slf4j
//@PreAuthorize("hasAuthority('책임자')") // [접근제어] 책임자만 접근 가능
@PreAuthorize("hasAnyAuthority('ROLE_ADMIN','책임자')")
public class CmsMemberController {

	private final MemberService memberService;

	// [250927] CMS 컨트롤러 전용 인스턴스 
	private final BCryptPasswordEncoder bCryptPasswordEncoder = new BCryptPasswordEncoder();
	// [250927] 계좌/카드 Mapper 불러오기...(계정 삭제할때,연결된 FK도 삭제 처리해야함)
	private final AccountMapper accountMapper;
	private final CardMapper cardMapper;
	
	// ============================================================
	// 회원 등록 (CMS)
	// ============================================================
	// --- (memberRole: 기본 user, adminType: 선택) ---
	@CrossOrigin("*")
    @Operation(summary = "회원 등록(CMS)", description = "관리자 권한으로 회원 등록 (폼 입력, application/x-www-form-urlencoded)")
    @PostMapping(consumes = "application/x-www-form-urlencoded")
    public ApiResponse<Integer> createMemberCms(
    		// @Parameter(description = "회원ID(필수)") String memberId, // Old방식,@RequestParam()로 다이렉트로 값을 입력하게 해야지, 마이바티스에 확실히 전송됨 
            @Parameter(description = "회원ID(필수)") @RequestParam("memberId") String memberId,
            @Parameter(description = "비밀번호(필수)") @RequestParam("memberPw") String memberPw,
            @Parameter(description = "회원이름(필수)") @RequestParam("memberName") String memberName,
            @Parameter(description = "성별(필수)", schema = @io.swagger.v3.oas.annotations.media.Schema(allowableValues = {"m", "f"}))
            @RequestParam("memberGender") String memberGender,
            @Parameter(description = "이메일(필수)") @RequestParam("memberEmail") String memberEmail,
            @Parameter(description = "휴대폰(필수)") @RequestParam("memberMobile") String memberMobile,
            @Parameter(description = "생년월일(필수, YYYY-MM-DD)") @RequestParam("memberBirthday") String memberBirthday,
            @Parameter(description = "우편번호(선택, 5자리)") @RequestParam(value = "zip", required = false) String zip,
            @Parameter(description = "전화번호(선택)") @RequestParam(value = "memberPhone", required = false) String memberPhone,
            @Parameter(description = "도로명주소(선택)") @RequestParam(value = "roadAddress", required = false) String roadAddress,
            @Parameter(description = "지번주소(선택)") @RequestParam(value = "jibunAddress", required = false) String jibunAddress,
            @Parameter(description = "상세주소(선택)") @RequestParam(value = "detailAddress", required = false) String detailAddress,

            // memberRole을 셀렉트로(스웨거 메타에 허용값 명시, 기본값 user) 
            @Parameter(
                description = "회원권한(선택, 기본 user)",
                schema = @io.swagger.v3.oas.annotations.media.Schema(allowableValues = {"user", "admin"})
            )
            @RequestParam(value = "memberRole", required = false, defaultValue = "user") String memberRole,
            
            // adminType을 셀렉트로(스웨거 메타에 허용값 명시), required=false 유지
            @Parameter(
                description = "관리자유형(선택, admin 계정일 경우)",
                schema = @io.swagger.v3.oas.annotations.media.Schema(allowableValues = {"책임자", "관리자", "강사"})
            )
            @RequestParam(value = "adminType", required = false) String adminType
            
            
            
            /* 추후 프론트에선 이렇게 함
            <select name="memberRole">
			  <option value="user" selected>user</option>
			  <option value="admin">admin</option>
			 </select>
			 <select name="adminType">
			  <option value="">--선택--</option>
			  <option value="책임자">책임자</option>
			  <option value="관리자">관리자</option>
			  <option value="강사">강사</option>
			</select>
            */
            
    ) {
        log.info("[POST]/api/cms/members req memberId={}", memberId);

        Member req = new Member();
        req.setMemberId(memberId);
        req.setMemberPw(bCryptPasswordEncoder.encode(memberPw)); // [비번암호화]
        req.setMemberName(memberName);
        req.setMemberGender(memberGender);
        req.setMemberEmail(memberEmail);
        req.setMemberMobile(memberMobile);
        req.setMemberPhone(memberPhone);
        req.setRoadAddress(roadAddress);
        req.setJibunAddress(jibunAddress);
        req.setDetailAddress(detailAddress);

        // 생년월일 파싱 (기존 로직 그대로)
        try {
            LocalDate birthday = LocalDate.parse(memberBirthday, DateTimeFormatter.ISO_DATE); // [형식검증]
            req.setMemberBirthday(birthday);
        } catch (DateTimeParseException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "memberBirthday는 YYYY-MM-DD 형식이어야 합니다.");
        }

        // ZIP 보정 (기존 로직 그대로)
        String zipFix = (zip == null || zip.isBlank() || "string".equalsIgnoreCase(zip)) ? null : zip;
        if (zipFix != null && zipFix.length() > 5) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "우편번호는 최대 5자리까지 입력 가능합니다.");
        }
        req.setZip(zipFix);

        // ----------------- 런타임 안전검증(추가, 매우 최소한) -----------------
        // memberRole은 'user' 또는 'admin'만 허용 (기본값은 이미 "user")
        if (!"user".equals(memberRole) && !"admin".equals(memberRole)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "memberRole은 'user' 또는 'admin'만 허용됩니다.");
        }
        req.setMemberRole(memberRole);

        // adminType은 admin 계정일 때만 의미가 있으므로, 값이 있으면 허용값 검증
        if (adminType != null && !adminType.isBlank()) {
            if (!("책임자".equals(adminType) || "관리자".equals(adminType) || "강사".equals(adminType))) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "adminType은 책임자/관리자/강사 중 하나여야 합니다.");
            }
            req.setAdminType(adminType);
        } else {
            req.setAdminType(null);
        }
        // -----------------------------------------------------------------

        int affected = memberService.createMember(req); // [DB처리, 기존 로직 그대로]
        return ApiResponse.ok(affected);
        
        
    }

	


	// ============================================================
	// 회원 단건 조회 (CMS)
	// ============================================================
	@CrossOrigin("*")
	@Operation(summary = "회원 단건 조회(CMS)", description = "회원ID를 PathVariable로 입력하면 해당 회원의 정보를 단건 조회한다.")
	@GetMapping("/{memberId}")
	
	public ApiResponse<Member> getMemberByIdCms(
			@Parameter(description = "회원ID") @PathVariable("memberId") String memberId) {
		log.info("[CMS][GET]/api/cms/members/{}", memberId); // 로그 출력
		Member m = memberService.getMemberById(memberId); // 서비스 호출
		if (m == null) {
			throw new ResponseStatusException(HttpStatus.NOT_FOUND, "해당 회원을 찾을 수 없습니다.");
		}
		return ApiResponse.ok(m); // 성공 응답
	}

	// ============================================================
	// 회원 목록 조회 (CMS)
	// ============================================================
	@CrossOrigin("*")
	@Operation(summary = "회원 목록 조회(CMS)", description = "페이지/사이즈 입력폼. 둘 다 빈값이면 전체조회")
	@GetMapping
	public ApiResponse<List<Member>> listMembersCms(
			@Parameter(description = "페이지번호(선택)") @RequestParam(value = "page", required = false) Integer page,
			@Parameter(description = "사이즈(선택)") @RequestParam(value = "size", required = false) Integer size,
			@Parameter(description = "검색어(선택)") @RequestParam(value = "keyword", required = false) String keyword,
			@Parameter(description = "역할필터(선택: user/admin)") @RequestParam(value = "role", required = false) String role) {
		List<Member> list = memberService.listMembers(page, size, keyword, role); // [DB조회-목록]
		return ApiResponse.ok(list); // [응답]
	}

	// ============================================================
	// 회원 수정 (CMS) — user와 차이: 역할/등급도 수정 가능
	// ============================================================
	@CrossOrigin("*")
	@Operation(summary = "회원 수정(CMS)", description = "책임자가 회원 정보를 수정. 입력폼(application/x-www-form-urlencoded). "
			+ "user와 달리 memberRole, adminType도 수정 가능")
	@PutMapping(value = "/{memberId}", consumes = "application/x-www-form-urlencoded")
	public ApiResponse<Integer> updateMemberCms(
			@Parameter(description = "수정할 회원ID") @PathVariable("memberId") String memberId, // ✅ PathVariable 명시
			@Parameter(description = "새 비밀번호(선택)") @RequestParam(value = "newPw", required = false) String newPw,
			@Parameter(description = "회원이름(필수)") @RequestParam("memberName") String memberName, // [251007] 파리미터 추가
            @Parameter(description = "성별(필수)", schema = @io.swagger.v3.oas.annotations.media.Schema(allowableValues = {"m", "f"}))
            @RequestParam("memberGender") String memberGender, // [251007] 파리미터 추가
			@Parameter(description = "생년월일") @RequestParam(value = "memberBirthday", required = false) String memberBirthday, // [251007] 파리미터 추가 
			@Parameter(description = "이메일(선택)") @RequestParam(value = "memberEmail", required = false) String memberEmail,
			@Parameter(description = "휴대폰(선택)") @RequestParam(value = "memberMobile", required = false) String memberMobile,
			@Parameter(description = "전화번호(선택)") @RequestParam(value = "memberPhone", required = false) String memberPhone,
			@Parameter(description = "우편번호(선택)") @RequestParam(value = "zip", required = false) String zip, // [251007] 파리미터 추가
			@Parameter(description = "도로명주소(선택)") @RequestParam(value = "roadAddress", required = false) String roadAddress,
			@Parameter(description = "지번주소(선택)") @RequestParam(value = "jibunAddress", required = false) String jibunAddress, // [251007] 파리미터 추가
			@Parameter(description = "상세주소(선택)") @RequestParam(value = "detailAddress", required = false) String detailAddress,
			 // select 형태(허용값 명시)
	        @Parameter(
	            description = "회원권한(선택: user/admin)",
	            schema = @io.swagger.v3.oas.annotations.media.Schema(allowableValues = {"user","admin"})
	        )
	        @RequestParam(value = "memberRole", required = false) String memberRole,

	        // select 형태(허용값 명시, 공란이면 NULL 처리)
	        @Parameter(
	            description = "관리자유형(선택: 책임자/관리자/강사, '--선택--' 또는 공란이면 미설정)",
	            schema = @io.swagger.v3.oas.annotations.media.Schema(allowableValues = {"책임자","관리자","강사"})
	        )
	        @RequestParam(value = "adminType", required = false) String adminType)
		{
		Member req = new Member();

		if (nz(newPw))
			req.setMemberPw(bCryptPasswordEncoder.encode(newPw));
		if (nz(memberEmail))
			req.setMemberEmail(memberEmail);
		if (nz(memberMobile))
			req.setMemberMobile(memberMobile);
		if (nz(memberPhone))
			req.setMemberPhone(memberPhone);
		if (nz(roadAddress))
			req.setRoadAddress(roadAddress);
		if (nz(detailAddress))
			req.setDetailAddress(detailAddress);
		if (nz(memberRole))
			req.setMemberRole(memberRole);
		if (nz(adminType))
			req.setAdminType(adminType);

		int affected = memberService.updateMember(memberId, req);
		return ApiResponse.ok(affected);
		
		
	}

	// ============================================================
	// 회원 삭제 (CMS)
	// ============================================================
	
	@Operation(
		    summary = "회원 삭제(CMS)",
		    description = "회원ID(PathVariable) 기준으로 해당 회원을 삭제한다. " +
		                  "계좌/카드 정보는 DB 트리거(TRG_MEMBER_CASCADE_DELETE)에 의해 자동 삭제됨"
		)
		@DeleteMapping("/{memberId}")
		@Transactional(rollbackFor = Exception.class)
		public ApiResponse<Integer> deleteMemberCms(
		        @Parameter(description = "회원ID") @PathVariable("memberId") String memberId) {

		    final String mid = memberId.trim(); // ✅ 앞뒤 공백 제거
		    log.info("[CMS][DELETE]/api/cms/members/{}", mid);

		    // 회원 삭제 (트리거에 의해 account_tbl, card_tbl도 같이 삭제됨)
		    int deleted = memberService.deleteMember(mid);

		    if (deleted == 0) {
		        throw new ResponseStatusException(HttpStatus.NOT_FOUND, "해당 회원을 찾을 수 없습니다.");
		    }

		    log.info("[CMS][DELETE] done member={}", mid);
		    return ApiResponse.ok(deleted);
		}

	// ------------------------------------------------------------
	// 내부 헬퍼 — "string"/공백 → false (값 없음)
	// ------------------------------------------------------------
	private static boolean nz(String v) { // [값보정-체크]
		if (v == null)
			return false;
		String t = v.trim();
		return !t.isEmpty() && !"string".equalsIgnoreCase(t);
	}
	
	// ------------------------------------------------------------
	// [251007] 회원중복 확인
	// ------------------------------------------------------------
	@CrossOrigin("*")
	@Operation(summary = "회원 ID 중복 확인", description = "회원ID 존재 여부 확인 (exists: true/false 반환)")
	@GetMapping("/check-id")
	public ApiResponse<?> checkId(@RequestParam("memberId") String memberId) {
	    log.info("[CMS][GET]/api/cms/members/check-id?memberId={}", memberId);
	    boolean exists = memberService.existsById(memberId);
	    return ApiResponse.ok(Map.of("exists", exists));
	}
}

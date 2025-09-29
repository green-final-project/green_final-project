package com.gym.controller.user;

import com.gym.common.ApiResponse; // 공통 응답 래퍼
import com.gym.domain.account.*; // DTO/도메인
import com.gym.service.AccountService; // 서비스
import io.swagger.v3.oas.annotations.Operation; // Swagger 요약/설명
import io.swagger.v3.oas.annotations.tags.Tag; // Swagger 태그
import io.swagger.v3.oas.annotations.Parameter; // Swagger 파라미터 설명
import io.swagger.v3.oas.annotations.media.Schema; // Swagger 스키마(allowableValues 등)
import lombok.RequiredArgsConstructor; // 롬복: 생성자 주입
import lombok.extern.slf4j.Slf4j; // 로깅
import org.springframework.http.MediaType; // consumes 지정
import org.springframework.web.bind.annotation.*; // REST 애노테이션
import org.springframework.security.core.Authentication; // [본인검증]
import org.springframework.security.access.AccessDeniedException; // [접근차단]

import java.util.List; // 목록 타입

// 계좌 API (등록/목록/대표지정/삭제)
@Tag(name = "05.Account-User", description = "계좌 API (등록/목록/대표지정/삭제)")
@RestController
@RequiredArgsConstructor
@Slf4j
public class UserAccountController {

    private final AccountService accountService; // 서비스

    /* =========================== [old] JSON 입력방식 ===========================
    @Operation(summary = "계좌 등록", description = "account_tbl INSERT (시퀀스/유니크 제약 준수)")
    @PostMapping("/api/accounts")
    public ApiResponse<Long> createAccount(@RequestBody AccountCreateRequest req) {
        log.info("[POST]/api/accounts req={}", req);
        Long pk = accountService.createAccount(req);
        return ApiResponse.ok(pk);
    }
    ============================================================================= */

    /**
     * 1) 등록(POST /api/accounts) — 입력폼(form-urlencoded)
     * - 작성자ID는 로그인ID로 자동 설정
     * - 중복계좌 입력하면 메시지 출력
     */
    @Operation(
    	    summary = "계좌 등록",
    	    description = "account_tbl INSERT (계좌정보 등록)"
    	)
    	@PostMapping(value = "/api/accounts", consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    	public ApiResponse<Long> createAccountForm(
    	        @Parameter(name = "accountBank", description = "은행명", required = true)
    	        @RequestParam("accountBank") String accountBank, // 필수 입력: 은행명

    	        @Parameter(name = "accountNumber", description = "계좌번호", required = true)
    	        @RequestParam("accountNumber") String accountNumber, // 필수 입력: 계좌번호(UNIQUE 등 제약 가능)

    	        @Parameter(
    	            name = "accountMain",
    	            description = "대표 여부(true/false)",
    	            schema = @Schema(type = "string", allowableValues = {"true","false"}, example = "false")
    	        )
    	        @RequestParam(name = "accountMain", defaultValue = "false") boolean accountMain, // 선택 입력: 대표여부

    	        Authentication auth // 인증 정보(필터에서 이미 보장)
    	) {
    	    // 1) 작성자ID는 로그인ID로 고정
    	    final String loginId = auth.getName();

    	    // 2) 운영 로그(민감정보 마스킹은 운영 정책에 따름)
    	    log.info("[POST]/api/accounts loginId={}, bank={}, number={}, main={}",
    	            loginId, accountBank, accountNumber, accountMain);

    	    // 3) 폼 파라미터 → DTO 변환(서비스는 DTO만 처리)
    	    AccountCreateRequest req = new AccountCreateRequest();
    	    req.setMemberId(loginId);            // 소유자ID = 로그인ID
    	    req.setAccountBank(accountBank);     // 은행명
    	    req.setAccountNumber(accountNumber); // 계좌번호
    	    req.setAccountMain(accountMain);     // 대표여부(등록 직후 대표 전환 로직이 별도면 서비스에서 처리)

    	    try {
    	        // 4) 등록 시도(서비스 내부에서 매퍼 호출 → INSERT)
    	        Long pk = accountService.createAccount(req);

    	        // 5) 정상 처리 응답
    	        return ApiResponse.ok(pk);

    	    } catch (org.springframework.dao.DataIntegrityViolationException ex) {
    	        // 6-A) DB 제약(UNIQUE 등) 충돌: 본인 소유 목록에서 동일 계좌번호 존재 여부로 메시지 분기
    	        boolean sameUserHasAccount = false;
    	        try {
    	            // 본인 계좌 목록 조회(매퍼 수정 없이 기존 서비스 메서드 사용 가정)
    	            java.util.List<AccountResponse> myAccounts = accountService.listAccountsByMember(loginId);
    	            if (myAccounts != null) {
    	                for (AccountResponse a : myAccounts) {
    	                    if (accountNumber.equals(a.getAccountNumber())) {
    	                        sameUserHasAccount = true; // 본인 ID로 이미 등록됨
    	                        break;
    	                    }
    	                }
    	            }
    	        } catch (Exception ignore) {
    	            // 조회 실패해도 아래 분기 로직은 수행
    	        }
    	        return sameUserHasAccount
    	                ? ApiResponse.fail(409, "이미 등록된 계좌입니다.") // 본인 ID 기준 중복
    	                : ApiResponse.fail(409, "본인 계좌인지 확인 부탁드립니다.");// 타인 ID의 계좌번호와 중복

    	    } catch (IllegalArgumentException ex) {
    	        // 6-B) 서비스 사전검사에서 던진 경우(예: "accountNumber already exists")
    	        boolean sameUserHasAccount = false;
    	        try {
    	            java.util.List<AccountResponse> myAccounts = accountService.listAccountsByMember(loginId);
    	            if (myAccounts != null) {
    	                for (AccountResponse a : myAccounts) {
    	                    if (accountNumber.equals(a.getAccountNumber())) {
    	                        sameUserHasAccount = true;
    	                        break;
    	                    }
    	                }
    	            }
    	        } catch (Exception ignore) { }
    	        return sameUserHasAccount
    	                ? ApiResponse.fail(409, "이미 등록된 계좌입니다.")
    	                : ApiResponse.fail(409, "본인 계좌인지 확인 부탁드립니다.");
    	    }
    	}


    /* =========================== [old] JSON 입력 ===========================
    @Operation(summary = "회원별 계좌 목록", description = "memberId 기준 SELECT")
    @GetMapping("/api/members/{memberId}/accounts")
    public ApiResponse<List<AccountResponse>> listByMember(
            @Parameter(description = "회원ID") @PathVariable("memberId") String memberId) {
        log.info("[GET]/api/members/{}/accounts", memberId);
        return ApiResponse.ok(accountService.listAccountsByMember(memberId));
    }
    ======================================================================== */

    /** 2) 회원별 목록(GET /api/members/{memberId}/accounts) — 본인만 조회 */
    @Operation(summary = "회원별 계좌 목록", description = "본인계좌만 조회 가능")
    @GetMapping("/api/members/{memberId}/accounts")
    public ApiResponse<List<AccountResponse>> listByMember(
            @Parameter(description = "회원ID") @PathVariable("memberId") String memberId,
            Authentication auth
    ) {
        log.info("[GET]/api/members/{}/accounts", memberId);
        if (!auth.getName().equals(memberId)) {
            throw new AccessDeniedException("본인 계좌만 조회할 수 있습니다.");
        }
        return ApiResponse.ok(accountService.listAccountsByMember(memberId));
    }

    /* =========================== [old] JSON 입력 ===========================
    @Operation(summary = "대표계좌 설정", description = "해당 회원의 다른 계좌는 자동으로 'N' 처리")
    @PatchMapping("/api/accounts/{accountId}/main")
    public ApiResponse<Void> setMainAccount(
            @Parameter(description = "대표로 지정할 계좌 PK") @PathVariable("accountId") Long accountId,
            @Parameter(description = "계좌 소유 회원ID") @RequestParam("memberId") String memberId) {
        log.info("[PATCH]/api/accounts/{}/main?memberId={}", accountId, memberId);
        accountService.setMainAccount(accountId, memberId);
        return ApiResponse.ok();
    }
    ========================================================================== */

    /**
     * 3) 대표계좌 설정(PATCH /api/accounts/{accountId}/main) — 본인만
     */
    @Operation(summary = "대표계좌 설정", description = "대상만 'Y', 나머지 자동 'N'")
    @PatchMapping("/api/accounts/{accountId}/main")
    public ApiResponse<Void> setMainAccount(
            @Parameter(description = "대표로 지정할 계좌 PK")
            @PathVariable("accountId") Long accountId,
            Authentication auth // 인증 정보(필터에서 이미 인증 보장)
    ) {
        // 로그인ID 추출(폼으로 받지 않음, 스푸핑 불가)
        final String loginId = auth.getName();

        // 로깅(민감정보 제외)
        log.info("[PATCH]/api/accounts/{}/main loginId={}", accountId, loginId);

        // 서비스 호출: 서비스 내부에서 해당 계좌가 loginId 소유인지 검증 및 대표계좌 전환 처리
        accountService.setMainAccount(accountId, loginId);

        // 표준 응답
        return ApiResponse.ok();
    }

    /* =========================== [old] JSON 입력 ===========================
    @Operation(summary = "계좌 삭제", description = "PK로 단건 삭제(트리거/참조 제약 주의)")
    @DeleteMapping("/api/accounts/{accountId}")
    public ApiResponse<Void> deleteAccountById(
            @Parameter(description = "삭제할 계좌 PK") @PathVariable("accountId") Long accountId) {
        log.info("[DELETE]/api/accounts/{}", accountId);
        accountService.deleteAccountById(accountId);
        return ApiResponse.ok();
    }
    ========================================================================== */

    /** 4) 삭제(DELETE /api/accounts/{accountId}) — 본인 소유만 삭제 가능 */
    @Operation(summary = "계좌 삭제", description = "계좌ID 입력한 단건 삭제")
    @DeleteMapping("/api/accounts/{accountId}")
    public ApiResponse<Void> deleteAccountById(
            @Parameter(description = "삭제할 계좌 PK") @PathVariable("accountId") Long accountId,
            Authentication auth
    ) {
        log.info("[DELETE]/api/accounts/{}", accountId);
        try {
            // 서비스에서 소유자 검증까지 수행(매퍼 변경 없이 구현)
            accountService.deleteAccountByIdForOwner(accountId, auth.getName());
            return ApiResponse.ok();
        } catch (RuntimeException e) {
            // 트리거나 제약에 의해 대표계좌 삭제 금지 메시지가 올라오는 경우 사용자 친화적으로 치환
            String msg = String.valueOf(e.getMessage());
            if (msg.contains("대표계좌") || msg.contains("TRG_ACCOUNT_REQUIRE_MAIN")) {
                return ApiResponse.fail(400, "대표계좌는 삭제할 수 없습니다.");
            }
            throw e; // 그 외는 그대로 전파
        }
    }
}

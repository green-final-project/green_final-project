package com.gym.controller.cms;

import com.gym.common.ApiResponse;                      // 공통 응답
import com.gym.domain.account.*;                        // DTO/응답
import com.gym.service.AccountService;                  // 서비스
import io.swagger.v3.oas.annotations.Operation;         // Swagger 요약/설명
import io.swagger.v3.oas.annotations.tags.Tag;          // Swagger 태그
import io.swagger.v3.oas.annotations.Parameter;         // Swagger 파라미터
import io.swagger.v3.oas.annotations.media.Schema;      // Swagger 스키마
import lombok.RequiredArgsConstructor;                  // 생성자 주입
import lombok.extern.slf4j.Slf4j;                       // 로그
import org.springframework.http.MediaType;              // consumes
import org.springframework.web.bind.annotation.*;       // REST
import org.springframework.dao.DataIntegrityViolationException;
import java.util.*;                                     // Map/List

/**
 * CMS 계좌 API (관리자/최고관리자)
 * - SecurityConfig: "/api/cms/accounts/**" -> hasAnyAuthority("관리자","최고관리자")
 * - 입력은 form-urlencoded (JSON 금지, 카드와 동일 정책)
 */
@Tag(name = "05.Account-CMS", description = "CMS 계좌 API (관리자/최고관리자)")
@RestController
@RequiredArgsConstructor
@Slf4j
@RequestMapping("/api/cms/accounts")
public class CmsAccountController {

    private final AccountService accountService;

    /** 1) 등록(POST /api/cms/accounts) — 임의 회원 대상, 폼 입력 */
    @Operation(summary = "CMS 계좌 등록", description = "임의 회원 대상 등록 (폼 입력)")
    @PostMapping(consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    public ApiResponse<Long> create(
            @Parameter(name = "memberId", description = "회원ID", required = true)
            @RequestParam("memberId") String memberId,

            @Parameter(name = "accountBank", description = "은행명", required = true)
            @RequestParam("accountBank") String accountBank,

            @Parameter(name = "accountNumber", description = "계좌번호", required = true)
            @RequestParam("accountNumber") String accountNumber,

            @Parameter(
                name = "accountMain",
                description = "대표 여부(true/false)",
                schema = @Schema(type = "string", allowableValues = {"true","false"}, example = "false")
            )
            @RequestParam(name = "accountMain", defaultValue = "false") boolean accountMain
    ) {
        log.info("[POST]/api/cms/accounts memberId={}, bank={}, number={}, main={}",
                memberId, accountBank, accountNumber, accountMain);

        AccountCreateRequest req = new AccountCreateRequest(); // 폼 → DTO
        req.setMemberId(memberId);
        req.setAccountBank(accountBank);
        req.setAccountNumber(accountNumber);
        req.setAccountMain(accountMain);

        try {
            Long pk = accountService.createAccount(req);     // INSERT 시도
            return ApiResponse.ok(pk);                       // 성공 응답
        } catch (DataIntegrityViolationException | IllegalArgumentException e) {
            // UNIQUE 제약 위반 또는 서비스 사전검사 중복 → 409 고정 메시지
            return ApiResponse.fail(409, "이미 등록된 계좌번호입니다.");
        }
    }


    /** 2) 삭제(DELETE /api/cms/accounts/{accountId}) — 소유자 검증 없이 관리자 권한으로 삭제 */
    @Operation(summary = "CMS 계좌 삭제", description = "관리자 권한으로 삭제(대표계좌는 삭제 불가)")
    @DeleteMapping("/{accountId}")
    public ApiResponse<Void> delete(
            @Parameter(description = "삭제할 계좌 PK") @PathVariable("accountId") Long accountId
    ) {
        log.info("[DELETE]/api/cms/accounts/{}", accountId);
        try {
            accountService.deleteAccountById(accountId); // CMS: 소유자 검증 없음(카드와 동일)
            return ApiResponse.ok();
        } catch (RuntimeException e) {
            String msg = String.valueOf(e.getMessage());
            // 카드와 동일하게 대표 계좌 관련 키워드 매핑
            if (msg.contains("대표계좌") || msg.contains("TRG_ACCOUNT_REQUIRE_MAIN")) {
                return ApiResponse.fail(400, "대표계좌는 삭제할 수 없습니다.");
            }
            throw e;
        }
    }

    /** 3) 대표계좌 설정(PATCH /api/cms/accounts/{accountId}/main?memberId=xxx) */
    @Operation(summary = "CMS 대표계좌 설정", description = "특정 회원의 대표계좌를 지정합니다.")
    @PatchMapping("/{accountId}/main")
    public ApiResponse<Void> setMain(
            @Parameter(description = "대표로 지정할 계좌 PK") @PathVariable("accountId") Long accountId,
            @Parameter(description = "계좌 소유 회원ID") @RequestParam("memberId") String memberId
    ) {
        log.info("[PATCH]/api/cms/accounts/{}/main?memberId={}", accountId, memberId);
        try {
            accountService.setMainAccount(accountId, memberId);
            return ApiResponse.ok();
        } catch (RuntimeException e) {
            String msg = String.valueOf(e.getMessage());
            if (msg.contains("NOT_FOUND") || msg.contains("mismatch")) {
                return ApiResponse.fail(404, "대상 계좌를 찾을 수 없거나 회원이 일치하지 않습니다.");
            }
            return ApiResponse.fail(400, "대표계좌 설정에 실패했습니다.");
        }
    }

    /** 4) 특정 회원 계좌 조회(GET /api/cms/accounts?memberId=xxx&page=0&size=10) — 컨트롤러 슬라이스 */
    @Operation(summary = "CMS 특정 회원 계좌 목록", description = "memberId로 계좌 목록을 조회합니다(간단 페이징).")
    @GetMapping
    public ApiResponse<Map<String, Object>> listByMember(
            @RequestParam("memberId") String memberId,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "10") int size
    ) {
        log.info("[GET]/api/cms/accounts?memberId={}&page={}&size={}", memberId, page, size);

        if (page < 0) page = 0;
        if (size <= 0) size = 10;

        List<AccountResponse> all = accountService.listAccountsByMember(memberId);
        int total = all.size();
        int from = Math.min(page * size, total);
        int to   = Math.min(from + size, total);
        List<AccountResponse> items = all.subList(from, to);

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("items", items);
        payload.put("total", total);
        payload.put("page", page);
        payload.put("size", size);
        payload.put("hasNext", to < total);

        return ApiResponse.ok(payload);
    }
}

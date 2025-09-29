// src/main/java/com/gym/controller/user/UserPaymentController.java
package com.gym.controller.user;

import com.gym.common.ApiResponse;                              // 공통 응답 래퍼
import com.gym.domain.payment.*;                                // DTO/도메인 (변경 없음)
import com.gym.service.PaymentService;                          // 서비스 인터페이스 (변경 없음)

import io.swagger.v3.oas.annotations.Operation;                 // Swagger 요약
import io.swagger.v3.oas.annotations.Parameter;                 // Swagger 파라미터
import io.swagger.v3.oas.annotations.media.Schema;              // Swagger 스키마
import io.swagger.v3.oas.annotations.tags.Tag;                  // Swagger 태그

import lombok.RequiredArgsConstructor;                          // 생성자 주입
import lombok.extern.slf4j.Slf4j;                               // 로깅

import org.springframework.http.MediaType;                      // consumes=폼
import org.springframework.security.core.Authentication;        // 로그인ID 획득
import org.springframework.web.bind.annotation.*;               // REST 애노테이션
import org.springframework.jdbc.core.JdbcTemplate;              // 금액 계산용 단일 SQL (매퍼 무변경 원칙 준수)

import java.util.List;

/**
 * [사용자 결제 API]
 * - 등록(폼): 예약ID + 결제수단만 입력 → 서버가 금액 계산 후 create()
 * - 조회: 본인 결제만 목록/단건 조회 (로그인ID 강제)
 * - 보안: 비로그인 접근 불가(스프링 시큐리티 전제)
 * - 통일성: 폼→DTO는 req.set… 스타일로 일관 유지(시설 CMS 컨트롤러와 동일)
 */
@Tag(name = "12.Payment-User", description = "사용자 결제 신청/조회")
@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
@Slf4j
public class UserPaymentController {

    private final PaymentService paymentService;  // 서비스 빈 (create/findList/updateStatus 그대로 사용) :contentReference[oaicite:4]{index=4}
    private final JdbcTemplate jdbc;              // 매퍼 무변경을 위해 컨트롤러에서 단일 SQL로 금액 계산

    /* ---------------------------------------------------------------------
       1) 결제 등록 — 폼 입력: 예약ID/결제수단만, 금액은 컨트롤러가 계산
       - 타인 예약ID 결제 시도 차단(소유자 검증)
       - TIMESTAMP 차이(일 단위) × 24 → 시간 환산 후 시설 단가와 곱셈
       ---------------------------------------------------------------------*/
    @Operation(summary = "결제 신청(폼)",
            description = "예약ID와 결제수단(계좌/카드)을 입력하면 서버가 결제금액을 계산해 등록합니다.")
    @PostMapping(consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    public ApiResponse<Long> createForm(
            @Parameter(description = "예약ID(PK)", required = true,
                    schema = @Schema(type = "long", example = "1001"))
            @RequestParam("resvId") Long resvId,

            @Parameter(description = "결제수단", required = true,
                    schema = @Schema(allowableValues = {"계좌","카드"}, example = "카드"))
            @RequestParam("paymentMethod") String paymentMethod,

            @Parameter(description = "계좌ID(수단이 '계좌'일 때만)", schema = @Schema(type="long"))
            @RequestParam(name = "accountId", required = false) Long accountId,

            @Parameter(description = "카드ID(수단이 '카드'일 때만)", schema = @Schema(type="long"))
            @RequestParam(name = "cardId", required = false) Long cardId,

            Authentication auth   // 로그인 사용자(본인만 결제 가능)
    ) {
        // 0) 로그인 사용자 ID 확보(본인 소유 보장)
        final String loginId = auth.getName();

        // 0-1) 결제수단 값 검증(허용: '계좌' 또는 '카드' — 공백/대소문자 등 방지)
        if (paymentMethod == null || (!paymentMethod.equals("계좌") && !paymentMethod.equals("카드"))) {
            throw new IllegalArgumentException("결제수단은 '계좌' 또는 '카드'만 허용됩니다.");
        }
        // 0-2) 수단별 ID 상호 배타 검증(계좌면 cardId 금지, 카드면 accountId 금지)
        if ("계좌".equals(paymentMethod) && cardId != null) {
            throw new IllegalArgumentException("결제수단이 '계좌'일 때는 카드ID를 함께 보낼 수 없습니다.");
        }
        if ("카드".equals(paymentMethod) && accountId != null) {
            throw new IllegalArgumentException("결제수단이 '카드'일 때는 계좌ID를 함께 보낼 수 없습니다.");
        }

        // 1-1) 신청자 검증: resvId → member_id 조회 후 로그인ID와 일치 여부 확인
        final String ownerSql = "SELECT member_id FROM reservation_tbl WHERE resv_id = ?";
        final String ownerId;
        try {
            ownerId = jdbc.queryForObject(ownerSql, String.class, resvId);
        } catch (org.springframework.dao.EmptyResultDataAccessException e) {
            throw new IllegalArgumentException("해당 예약ID가 존재하지 않습니다."); // 잘못된 예약ID
        }
        if (!loginId.equals(ownerId)) {
            // 스프링 시큐리티: AccessDeniedException → 403
			/*
			 * throw new org.springframework.security.access.AccessDeniedException(
			 * "본인 예약건에 대해서만 결제 신청이 가능합니다." );
			 */
        	// GlobalExceptionHandler의 409 매핑돼 있는 IllegalStateException으로 실행
        	throw new IllegalStateException("본인 예약건에 대해서만 결제 신청이 가능합니다.");
        }
        // 1-2) 계좌 주인 검증: accountId → member_id 조회 후 로그인ID와 일치 여부 확인
        if ("계좌".equals(paymentMethod) && accountId != null) {
            final String accSql = "SELECT member_id FROM account_tbl WHERE account_id = ?";
            String accOwner = null;
            try {
                accOwner = jdbc.queryForObject(accSql, String.class, accountId);
            } catch (org.springframework.dao.EmptyResultDataAccessException e) {
                throw new IllegalStateException("계좌ID가 존재하지 않습니다.");
            }
            if (!loginId.equals(accOwner)) {
                throw new IllegalStateException("본인 소유 계좌로만 결제할 수 있습니다.");
            }
        }
        // 1-3) 카드 주인 검증: cardId → member_id 조회 후 로그인ID와 일치 여부 확인
        if ("카드".equals(paymentMethod) && cardId != null) {
            final String cardSql = "SELECT member_id FROM card_tbl WHERE card_id = ?";
            String cardOwner = null;
            try {
                cardOwner = jdbc.queryForObject(cardSql, String.class, cardId);
            } catch (org.springframework.dao.EmptyResultDataAccessException e) {
                throw new IllegalStateException("카드ID가 존재하지 않습니다.");
            }
            if (!loginId.equals(cardOwner)) {
                throw new IllegalStateException("본인 소유 카드로만 결제할 수 있습니다.");
            }
        }

        // 2) 결제금액 계산: (시설이용료 × 이용시간(시간))
        //    - TIMESTAMP 차이는 '일(day)' 단위 실수 → ×24로 시간 환산
        //    - ROUND로 정수 금액 확정(요금 정책에 따라 TRUNC로 변경 가능)
        final String amountSql = """
            SELECT ROUND(
                     f.facility_money *
                     ((CAST(r.resv_end_time AS DATE) - CAST(r.resv_start_time AS DATE)) * 24)
                   ) AS payment_amount
              FROM reservation_tbl r
              JOIN facility_tbl    f ON f.facility_id = r.facility_id
             WHERE r.resv_id = ?
        """;
        final Long paymentMoney = jdbc.queryForObject(amountSql, Long.class, resvId);
        if (paymentMoney == null || paymentMoney <= 0L) {
            throw new IllegalStateException("결제금액 계산에 실패했습니다. (0원 이하)");
        }

        // 3) 폼 → DTO 매핑(통일성: req.set… 패턴)
        PaymentCreateRequest req = new PaymentCreateRequest();
        req.setMemberId(loginId);            // 본인 결제 고정
        req.setResvId(resvId);               // 예약 FK
        req.setPaymentMethod(paymentMethod); // 결제수단(계좌/카드)
        req.setAccountId(accountId);         // 계좌ID(선택)
        req.setCardId(cardId);               // 카드ID(선택)
        req.setPaymentMoney(paymentMoney);   // ★ 서버 계산 금액
        // 상태값은 매퍼에서 NVL로 '예약' 보정(파라미터 null이면 '예약' 저장) — 매퍼 구조 준수

        // 4) 서비스 호출(INSERT → 시퀀스 CURRVAL 회수)
        log.info("[USER][POST]/api/payments form req={}", req);
        Long paymentId = paymentService.create(req);
        return ApiResponse.ok(paymentId);
    }
    

    // ---------------------------------------------------------------------
    // 2) 결제 목록(본인 강제) — 미입력 시 본인 전체
    // ---------------------------------------------------------------------
    @Operation(summary = "결제 목록(본인)", description = "로그인한 본인의 결제만 조회합니다.")
    @GetMapping
    public ApiResponse<List<PaymentResponse>> listForUser(
            @Parameter(description = "결제ID") @RequestParam(name="paymentId", required=false) Long paymentId,
            @Parameter(description = "예약ID") @RequestParam(name="resvId", required=false) Long resvId,
            @Parameter(description = "결제수단(계좌/카드)") @RequestParam(name="method", required=false) String method,
            @Parameter(description = "결제상태(예약/완료/취소)") @RequestParam(name="paymentStatus", required=false) String paymentStatus,
            Authentication auth
    ) {
        final String loginId = auth.getName(); // 본인 강제
        PaymentSearchRequest req = PaymentSearchRequest.builder()
                .paymentId(paymentId)
                .memberId(loginId)            // ★ 항상 로그인ID
                .resvId(resvId)
                .method(method)
                .paymentStatus(paymentStatus)
                .build();
        return ApiResponse.ok(paymentService.findList(req));     // XML selectPayments 사용 :contentReference[oaicite:7]{index=7}
    }

    // ---------------------------------------------------------------------
    // 3) 결제 단건(본인 강제) — paymentId로 1건 필터링
    // ---------------------------------------------------------------------
    @Operation(summary = "결제 단건(본인)", description = "결제ID로 본인 결제를 단건 조회합니다.")
    @GetMapping("/{paymentId}")
    public ApiResponse<PaymentResponse> getOneForUser(
            @PathVariable("paymentId") Long paymentId,
            Authentication auth
    ) {
        final String loginId = auth.getName();
        PaymentSearchRequest req = PaymentSearchRequest.builder()
                .paymentId(paymentId)
                .memberId(loginId)   // 본인 강제
                .build();
        List<PaymentResponse> list = paymentService.findList(req); // 리스트에서 1건만
        if (list.isEmpty()) throw new IllegalArgumentException("조회 결과가 없습니다.");
        return ApiResponse.ok(list.get(0));
    }
}

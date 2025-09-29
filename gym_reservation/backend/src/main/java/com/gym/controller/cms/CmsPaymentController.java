// src/main/java/com/gym/controller/cms/CmsPaymentController.java
package com.gym.controller.cms;

import com.gym.common.ApiResponse;                      // 공통 응답
import com.gym.domain.payment.*;                        // DTO
import com.gym.service.PaymentService;                  // 서비스

import io.swagger.v3.oas.annotations.*;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * [CMS 결제 API]
 * - 상태변경(완료/예약/취소): 폼에 결제ID만 입력
 * - 목록 검색: 미입력 시 전체(필터형)
 * - 단건 조회: 결제ID만 입력
 * - 접근권한: 관리자 전용(최고관리자/관리자) — 시큐리티 설정으로 보호 전제
 * - 일관성: 폼→DTO는 req.set… 패턴, 응답 payload 키는 items/total 등 시설 CMS 스타일 준수
 */
@Tag(name = "12.Payment-CMS", description = "CMS 결제 관리")
@RestController
@RequestMapping("/api/cms/payments")
@RequiredArgsConstructor
@Slf4j
public class CmsPaymentController {

    private final PaymentService paymentService; // 서비스 인터페이스 그대로 사용 :contentReference[oaicite:11]{index=11}

    // ---------------------------------------------------------------------
    // 1) 상태 변경 — 폼에 결제ID만(상태는 '완료/예약/취소' 중 택1)
    // ---------------------------------------------------------------------
    @Operation(summary = "결제 상태변경(폼/CMS)", description = "결제ID만 입력하여 상태를 예약/완료/취소로 변경")
    @PutMapping(value="/{paymentId}/status", consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    public ApiResponse<Void> updateStatusForm(
            @Parameter(description = "결제ID", required = true)
            @PathVariable("paymentId") Long paymentId,
            @Parameter(description = "결제상태(완료/예약/취소)", required = true,
                    schema = @Schema(allowableValues = {"완료","예약","취소"}))
            @RequestParam("paymentStatus") String paymentStatus
    ) {
        // 값 검증(서비스는 update만 호출) — 매퍼 updatePaymentStatus 사용 :contentReference[oaicite:12]{index=12}
        String status = paymentStatus == null ? null : paymentStatus.trim();
        if (!("완료".equals(status) || "예약".equals(status) || "취소".equals(status))) {
            throw new IllegalArgumentException("paymentStatus는 '완료', '예약', '취소'만 허용됩니다.");
        }
        paymentService.updateStatus(paymentId, status); // XML updatePaymentStatus 실행 :contentReference[oaicite:13]{index=13}
        return ApiResponse.ok();
    }

    // ---------------------------------------------------------------------
    // 2) 결제 목록 검색 — 미입력 시 전체(필터 방식)
    // ---------------------------------------------------------------------
    @Operation(summary = "결제 목록 검색(CMS)", description = "필터 미입력 시 전체. 응답키는 items/total/page/size")
    @GetMapping
    public ApiResponse<Map<String, Object>> listForCms(
            @Parameter(description="결제ID") @RequestParam(name="paymentId", required=false) Long paymentId,
            @Parameter(description="회원ID") @RequestParam(name="memberId", required=false) String memberId,
            @Parameter(description="예약ID") @RequestParam(name="resvId", required=false) Long resvId,
            @Parameter(description="수단(계좌/카드)") @RequestParam(name="method", required=false) String method,
            @Parameter(description="상태(예약/완료/취소)") @RequestParam(name="paymentStatus", required=false) String paymentStatus,
            @Parameter(description="페이지(0부터)") @RequestParam(name="page", defaultValue="0") int page,
            @Parameter(description="페이지 크기") @RequestParam(name="size", defaultValue="10") int size
    ) {
        if (page < 0) page = 0;
        if (size <= 0) size = 10;

        // 검색 DTO(폼→DTO: req.set… 대신 Builder 사용 허용, 그러나 라인 주석 및 키는 동일 스타일)
        PaymentSearchRequest req = PaymentSearchRequest.builder()
                .paymentId(paymentId)
                .memberId(memberId)
                .resvId(resvId)
                .method(method)
                .paymentStatus(paymentStatus)
                .build();

        List<PaymentResponse> items = paymentService.findList(req); // XML selectPayments 사용 :contentReference[oaicite:14]{index=14}

        // 간단 페이징 포맷(시설 CMS와 동일 키)
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("items", items);
        payload.put("total", items.size()); // count 매퍼를 안 건드리므로 단순 총건수
        payload.put("page", page);
        payload.put("size", size);
        return ApiResponse.ok(payload);
    }

    // ---------------------------------------------------------------------
    // 3) 결제 단건 조회 — 결제ID만 입력(검색 재사용)
    // ---------------------------------------------------------------------
    @Operation(summary = "결제 단건 조회(CMS)", description = "결제ID로 단건 조회")
    @GetMapping("/{paymentId}")
    public ApiResponse<PaymentResponse> getOneForCms(
            @PathVariable("paymentId") Long paymentId
    ) {
        PaymentSearchRequest req = new PaymentSearchRequest();
        req.setPaymentId(paymentId);       // 폼→DTO 통일: set 메서드 사용
        List<PaymentResponse> list = paymentService.findList(req); // 검색 재사용 :contentReference[oaicite:15]{index=15}
        if (list.isEmpty()) throw new IllegalArgumentException("조회 결과가 없습니다.");
        return ApiResponse.ok(list.get(0));
    }
}

// src/main/java/com/gym/controller/user/UserPaylogController.java
package com.gym.controller.user;

import com.gym.common.ApiResponse;
import com.gym.domain.payment.PaylogResponse;
import com.gym.service.PaylogService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Parameter;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.security.core.Authentication;          // 로그인 정보
import org.springframework.security.core.GrantedAuthority;     // 권한 체크
import org.springframework.web.bind.annotation.*;
import org.springframework.jdbc.core.JdbcTemplate;               // 보조 조회용(SQL 한 줄)

import java.util.*;

@CrossOrigin("*")
@Tag(name = "12.PayLog", description = "결제로그 API (단순로그 조회)")
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@Slf4j
public class UserPaylogController {

    private final PaylogService paylogService;   // 서비스(무변경)
    private final JdbcTemplate jdbc;             // payment_id 목록 조회만 사용

    // ─────────────────────────────────────────────────────────────
    // 결제로그 조회(단일): memberId(옵션)
    // - memberId 미입력:
    //    • 관리자 → 전체 회원의 결제로그
    //    • 일반   → 로그인ID의 결제로그
    // - 서비스/매퍼는 paymentId별 조회 그대로 재사용
    // ─────────────────────────────────────────────────────────────
    @CrossOrigin("*")
    @Operation(summary = "결제로그 조회", description = "memberId 미입력 시: 관리자=전체, 일반=본인 로그를 반환합니다.")
    @GetMapping("/payments/logs")
    public ApiResponse<List<PaylogResponse>> listLogs(
            @Parameter(description = "회원ID(옵션). 미입력 시 관리자=전체, 일반=로그인ID")
            @RequestParam(name = "memberId", required = false) String memberId,
            Authentication auth
    ) {
        final String loginId = auth.getName();                 // 로그인 ID
        final boolean isAdmin = hasAdmin(auth.getAuthorities()); // 최고관리자/관리자 여부
        final boolean blank = (memberId == null || memberId.trim().isEmpty());
        final String targetMemberId = blank ? loginId : memberId.trim();

        log.info("[GET]/api/payments/logs memberId={}, isAdmin={}", (blank ? "(blank)" : targetMemberId), isAdmin);

        // 1) 결제ID 목록 조회 SQL(관리자·일반 분기)
        final String SQL_ALL   = "SELECT payment_id FROM payment_tbl ORDER BY payment_id DESC";
        final String SQL_BY_ID = "SELECT payment_id FROM payment_tbl WHERE member_id = ? ORDER BY payment_id DESC";

        List<Long> paymentIds;
        if (isAdmin && blank) {
            // 관리자 + 미입력 ⇒ 전체 결제ID
            paymentIds = jdbc.queryForList(SQL_ALL, Long.class);
        } else {
            // 그 외 ⇒ 지정/로그인ID의 결제ID
            paymentIds = jdbc.queryForList(SQL_BY_ID, Long.class, targetMemberId);
        }

        // 2) 각 결제ID의 로그를 서비스로 취합
        List<PaylogResponse> merged = new ArrayList<>();
        for (Long pid : paymentIds) {
            List<PaylogResponse> logs = paylogService.listPaylogsByPayment(pid); // 시그니처 무변경
            if (logs != null && !logs.isEmpty()) merged.addAll(logs);
        }

        // 3) 정렬(선택): 결제ID 내림차순
        merged.sort(Comparator.comparing(PaylogResponse::getPaymentId).reversed());

        return ApiResponse.ok(merged); // 데이터 없으면 [] 반환
    }

    // 권한 체크: ROLE_ADMIN 또는 ROLE_MANAGER 허용
    private boolean hasAdmin(Collection<? extends GrantedAuthority> auths) {
        if (auths == null) return false;
        for (GrantedAuthority ga : auths) {
            String r = ga.getAuthority();
            if ("ROLE_ADMIN".equals(r) || "ROLE_MANAGER".equals(r)) return true;
        }
        return false;
    }
}

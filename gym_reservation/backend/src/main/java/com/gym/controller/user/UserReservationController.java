// src/main/java/com/gym/controller/user/UserReservationController.java
package com.gym.controller.user;

import com.gym.common.ApiResponse;                            // 공통 응답
import com.gym.domain.reservation.ReservationCreateRequest;  // 등록 DTO
import com.gym.domain.reservation.ReservationUpdateRequest;  // 수정 DTO
import com.gym.domain.reservation.ReservationSearchRequest;  // 검색 DTO
import com.gym.domain.reservation.ReservationResponse;       // 응답 DTO
import com.gym.service.ReservationService;                   // 서비스
import io.swagger.v3.oas.annotations.Operation;              // Swagger 요약/설명
import io.swagger.v3.oas.annotations.Parameter;              // Swagger 파라미터
import io.swagger.v3.oas.annotations.media.Schema;           // Swagger 스키마
import io.swagger.v3.oas.annotations.tags.Tag;               // Swagger 태그
import lombok.RequiredArgsConstructor;                       // 생성자 주입
import org.springframework.web.bind.annotation.*;            // REST 애노테이션
import org.springframework.security.core.Authentication;     // 로그인 정보
import org.springframework.http.MediaType;                   // consumes 지정

import java.util.List;                                       // 목록

/**
 * 사용자용 예약신청 컨트롤러
 * - 기능: 등록/수정/목록조회만 제공(삭제 없음)
 * - 소유자 강제: 로그인한 회원ID만 허용(모든 요청에서 사용자ID는 입력받지 않음)
 * - 입력 형식: application/x-www-form-urlencoded (폼 전송)
 */
@CrossOrigin("*") // [251002] 프론트엔드 Http 허용
@Tag(name = "09.Reservation-User", description = "사용자용 예약신청")
@RestController
@RequestMapping("/api/reservations")
@RequiredArgsConstructor
public class UserReservationController {

    private final ReservationService reservationService; // 서비스 주입

    // ---------------------------------------------------------------------
    // 1) 예약 등록 — 폼 입력, 로그인ID를 memberId로 강제 주입
    // ---------------------------------------------------------------------
    @CrossOrigin("*") // [251002] 프론트엔드 Http 허용
    @Operation(summary = "예약 등록", description = "폼 입력, 신청자ID는 로그인ID로 자동 설정")
    @PostMapping(consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE) // 폼 전송 고정
    public ApiResponse<Long> createReservation(
            @Parameter(description = "시설ID", schema = @Schema(type = "long", example = "1"), required = true)
            @RequestParam("facilityId") Long facilityId,

            @Parameter(description = "신청 내용(요구사항)", schema = @Schema(type = "string", example = "팀 연습"))
            @RequestParam(name = "resvContent", required = false) String resvContent,

            // Swagger 테스트 편의용 예시값(placeholder 성격)
            @Parameter(description = "원하는 날짜(yyyy-MM-dd)", schema = @Schema(type = "string", example = "2025-00-00"), required = true)
            @RequestParam("wantDate") String wantDate,

            @Parameter(description = "신청 인원수", schema = @Schema(type = "int", example = "20"), required = true)
            @RequestParam("resvPersonCount") Integer resvPersonCount,

            // 시간은 셀렉트박스 가이드: 09~21시(1시간 단위)
            @Parameter(description = "시작 시각(시 단위, 09~21)", schema = @Schema(type = "string", example = "09"), required = true)
            @RequestParam("startHour") String startHour,

            @Parameter(description = "종료 시각(시 단위, 10~21)", schema = @Schema(type = "string", example = "11"), required = true)
            @RequestParam("endHour") String endHour,

            Authentication auth
    ) {
        final String loginId = auth.getName(); // 로그인 사용자ID

        // 시간 유효성: 09 ≤ start < end ≤ 21
        int sh = Integer.parseInt(startHour);
        int eh = Integer.parseInt(endHour);
        if (sh < 9 || sh > 21 || eh < 9 || eh > 21 || sh >= eh) {
            throw new IllegalArgumentException("시간 선택 오류: 시작은 09~21, 종료는 시작보다 크고 09~21 범위여야 합니다.");
        }

        // "yyyy-MM-dd HH:mm:ss"로 조합
        String resvStartTime = wantDate + " " + String.format("%02d", sh) + ":00:00";
        String resvEndTime   = wantDate + " " + String.format("%02d", eh) + ":00:00";

        // 폼 → DTO
        ReservationCreateRequest request = new ReservationCreateRequest();
        request.setMemberId(loginId);                // 소유자ID = 로그인ID(스푸핑 방지)
        request.setFacilityId(facilityId);
        request.setResvContent(resvContent);
        request.setWantDate(wantDate);
        request.setResvPersonCount(resvPersonCount);
        request.setResvStartTime(resvStartTime);
        request.setResvEndTime(resvEndTime);

        return ApiResponse.ok(reservationService.createReservation(request)); // PK 반환
    }

    /* ===================== [old] JSON 등록 보존(비활성) =====================
    @Operation(summary = "예약 등록")
    @PostMapping
    public ApiResponse<Long> createReservation(@RequestBody ReservationCreateRequest request) {
        return ApiResponse.ok(reservationService.createReservation(request));
    }
    // JSON 예시(보존)
    // {
    //   "memberId": "hong1",
    //   "facilityId": 1,
    //   "resvContent": "팀 연습",
    //   "wantDate": "2025-09-20",
    //   "resvPersonCount": 20,
    //   "resvStartTime": "2025-09-20 10:00:00",
    //   "resvEndTime": "2025-09-20 12:00:00"
    // }
    ======================================================================= */

    // ---------------------------------------------------------------------
    // 2) 예약 목록 — 본인 자동 식별(입력값 없으면 본인 전체가 조회되도록 강제)
    // ---------------------------------------------------------------------
    @CrossOrigin("*") // [251002] 프론트엔드 Http 허용
    @Operation(summary = "예약 목록", description = "예약ID/시설ID로 검색(항상 로그인한 본인 데이터만 조회)")
    @GetMapping
    public ApiResponse<List<ReservationResponse>> listReservation(
        @Parameter(description = "예약ID",
                   schema = @Schema(type = "long", example = "0"))
        @RequestParam(name = "resvId", required = false) Long resvId,

        @Parameter(description = "시설ID",
                   schema = @Schema(type = "long", example = "0"))
        @RequestParam(name = "facilityId", required = false) Long facilityId,

        Authentication auth
    ) {
        final String loginId = auth.getName(); // 본인 자동 적용

        // ✨ 핵심: 입력이 비어 있어도 memberId=loginId를 강제 세팅 → 본인 전체 조회 보장
        ReservationSearchRequest req = ReservationSearchRequest.builder()
                .resvId(resvId)          // null이면 미적용
                .facilityId(facilityId)  // null이면 미적용
                .memberId(loginId)       // ★ 항상 로그인ID로 강제 (전체라도 "본인 전체")
                .build();

        return ApiResponse.ok(reservationService.listReservations(req));
    }

    /* ===================== [old] memberId 쿼리 받던 목록(비활성 보존) =====================
    @Operation(summary = "예약 목록", description = "예약 ID, 시설ID, 신청자ID로 검색.")
    @GetMapping
    public ApiResponse<List<ReservationResponse>> listReservation(
            @Parameter(description = "예약ID")
            @RequestParam(name = "resvId", required = false) Long resvId,
            @Parameter(description = "시설ID")
            @RequestParam(name = "facilityId", required = false) Long facilityId,
            @Parameter(description = "작성자 ID")
            @RequestParam(name = "memberId", required = false) String memberId
    ) {
        ReservationSearchRequest req = ReservationSearchRequest.builder()
                .resvId(resvId).facilityId(facilityId).memberId(memberId).build();
        return ApiResponse.ok(reservationService.listReservations(req));
    }
    ======================================================================= */

    // ---------------------------------------------------------------------
    // 3) 예약 취소신청 — 상태값 변경 없이 resv_cancel만 요청(Y)
    // ---------------------------------------------------------------------
    @CrossOrigin("*") // [251002] 프론트엔드 Http 허용
    @Operation(summary = "취소신청", description = "사용자 취소 요청을 접수(resv_cancel=Y). 상태값은 변경하지 않음.")
    @PostMapping(value = "/{resvId}/cancel-request", consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    public ApiResponse<String> requestCancel(
        @Parameter(name = "resvId", description = "예약 PK", required = true,
                   schema = @Schema(type = "integer", format = "int64", example = "00"))
        @PathVariable("resvId") Long resvId,

        @Parameter(description = "취소 사유(선택)",
                   schema = @Schema(type = "string", example = "팀 사정으로 이용 불가"))
        @RequestParam(name = "resvCancelReason", required = false) String resvCancelReason,

        Authentication auth
    ) {
        final String loginId = auth.getName();
        
        // [251005][신청취소 처리] 기존 서비스 호출 개선
        // reservationService.requestReservationCancel(resvId, loginId, resvCancelReason); // [old]
        reservationService.requestReservationCancel(resvId, loginId, resvCancelReason); // [251005] 로그인ID와 PK를 기반으로 resv_cancel='Y' 처리
        
        return ApiResponse.ok("취소신청이 접수되었습니다.");
    }

    /* ===================== [old] 예약 수정 엔드포인트 =====================
    // @Operation(summary = "예약 수정(본인 자동 식별)", description = "로그인한 회원의 예약만 수정됩니다.")
    // @PutMapping(value = "/{resvId}", consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    // public String updateReservation(...) { ... }
    // ※ 사용자 화면에서는 상태값 변경 금지 및 수정 제거 요구에 따라 비활성화
    ======================================================================= */
    
    // [251002 신규] 예약 단건 조회
    @Operation(summary = "예약 단건 조회", description = "예약PK로 단건 조회")
    @GetMapping("/{resvId}")
    public ApiResponse<ReservationResponse> getReservation(
            @PathVariable("resvId") Long resvId) {
        return ApiResponse.ok(reservationService.getReservation(resvId));
    }
}

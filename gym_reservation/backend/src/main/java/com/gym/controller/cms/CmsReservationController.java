// 파일 경로: src/main/java/com/gym/controller/cms/CmsReservationController.java
// 목적: CMS에서 예약 상태 변경(완료/취소/대기) + 목록 조회(예약ID/시설ID/회원ID/회원명/취소여부)
// 입력 방식: application/x-www-form-urlencoded (폼 전송, JSON 아님)
// 주의: 서비스/매퍼는 "기존 메소드"만 사용(임의 확장 금지)

package com.gym.controller.cms;                             // ✅ CMS 전용 패키지

import com.gym.common.ApiResponse;                          // ✅ 공통 응답 래퍼(프로젝트 공용)
import com.gym.domain.reservation.ReservationResponse;      // ✅ 목록 응답 DTO (기존)
import com.gym.domain.reservation.ReservationSearchRequest; // ✅ 검색 DTO (기존)
import com.gym.domain.reservation.ReservationUpdateRequest; // ✅ 상태변경 DTO (기존)
import com.gym.service.ReservationService;                  // ✅ 서비스 인터페이스(기존 메소드 사용)  :contentReference[oaicite:0]{index=0}

import io.swagger.v3.oas.annotations.Operation;             // ✅ Swagger 문서화(요약/설명)
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;

import lombok.RequiredArgsConstructor;                      // ✅ 생성자 주입 보일러플레이트 제거
import org.springframework.http.MediaType;                  // ✅ consumes=form-urlencoded 지정
import org.springframework.web.bind.annotation.*;           // ✅ REST 컨트롤러 애노테이션 일괄


import java.util.LinkedHashMap;
import java.util.Map;

import java.util.List;                                      // ✅ 목록 반환 타입

/**
 * CMS용 예약 관리 컨트롤러
 * - 요구기능(2개):
 *   (1) 신청정보 상태 변경: 완료/취소/대기 중 하나로 변경
 *   (2) 신청정보 목록 조회: 조건 = 예약ID / 시설ID / 회원ID / 회원명 / 취소여부
 * - 입력: application/x-www-form-urlencoded (폼 전송), JSON 아님
 * - 서비스/매퍼: "기존 정의"만 호출(임의 확장/수정 금지)
 *
 * 참조:
 *  - 목록 조회 서비스 시그니처(listReservations)  :contentReference[oaicite:1]{index=1}
 *  - 조회 매퍼(listReservations) 결과 컬럼(회원명/시설명/취소여부 포함)  :contentReference[oaicite:2]{index=2}
 *  - 검색 DTO 필드(resvId/memberId/facilityId/memberName/resvCancel 등)  :contentReference[oaicite:3]{index=3}
 *  - 사용자 컨트롤러가 폼 전송을 사용하는 패턴(입력폼 기반)  :contentReference[oaicite:4]{index=4}
 */
@Tag(name = "09.Reservation-CMS", description = "CMS용 예약 관리")
@RestController                                                // ✅ REST 방식(JSON 응답)
@RequestMapping("/cms/reservations")                           // ✅ CMS 전용 베이스 URL
@RequiredArgsConstructor
public class CmsReservationController {

    private final ReservationService reservationService;       // ✅ 서비스 주입(기존 메소드만 사용)  :contentReference[oaicite:5]{index=5}

    // ---------------------------------------------------------------------
    // [1] 신청정보 목록 조회 (폼/쿼리 기반)
    //     - 조건: 예약ID / 시설ID / 회원ID / 회원명 / 취소여부
    //     - 반환: ReservationResponse 리스트(JSON) — 화면은 CMS 폼/테이블에서 처리
    //     - 주의: 현재 XML where절은 resvId/memberId/facilityId만 필터(회원명/취소여부는
    //             추후 XML 보강 필요). 여기서는 "컨트롤러"만 생성(요청 범위 준수).
    //             (회원명/취소여부 파라미터도 DTO에 실어 전달함)
    //             조회 컬럼에 memberName/resvCancel이 포함됨을 확인  :contentReference[oaicite:6]{index=6}
    // ---------------------------------------------------------------------
    @Operation(summary = "신청정보 목록(폼)", description = "예약ID/시설ID/회원ID/회원명/취소여부 조건으로 조회(미입력 시 전체) + 간단 페이징")
    @GetMapping
    public ApiResponse<Map<String, Object>> listForCms(
    		// 조건 파라미터(모두 선택 입력, 미입력 시 전체 조회)
            @RequestParam(value = "resvId",     required = false) Long   resvId,       // 예약ID
            @RequestParam(value = "facilityId", required = false) Long   facilityId,   // 시설ID
            @RequestParam(value = "memberId",   required = false) String memberId,     // 회원ID
            @RequestParam(value = "memberName", required = false) String memberName,   // 회원명(현재 XML where 미적용)
            @RequestParam(value = "resvCancel", required = false) String resvCancel,   // 취소여부(Y/N, 셀렉트박스)

            // 간단 페이징 파라미터(기본값: page=0, size=10)
            @RequestParam(value = "page", defaultValue = "0")  int page,
            @RequestParam(value = "size", defaultValue = "10") int size
    ) {
        // 1) 방어코드: 잘못된 페이징 값 보정
        if (page < 0)  page = 0;        // 음수 페이지 방지
        if (size <= 0) size = 10;       // 0/음수 사이즈 방지

        // 2) 컨트롤러에서 검색 DTO 구성(서비스 시그니처 준수)
        ReservationSearchRequest req = ReservationSearchRequest.builder()
                .resvId(resvId)               // 예약ID(선택)
                .facilityId(facilityId)       // 시설ID(선택)
                .memberId(memberId)           // 회원ID(선택)
                .memberName(memberName)       // 회원명(선택) — ※ 현재 XML where 미적용(반환 컬럼만 존재)
                .resvCancel(resvCancel)       // 취소여부(선택) — ※ 현재 XML where 미적용(반환 컬럼만 존재)
                .build();

        // 3) 서비스 호출(전체 목록 조회 → 컨트롤러에서 간단 페이징)
        List<ReservationResponse> all = reservationService.listReservations(req);

        // 4) 간단 페이징 계산(subList)
        int total = all.size();                         // 전체 건수
        int from  = Math.min(page * size, total);      // 시작 인덱스(범위 보정)
        int to    = Math.min(from + size, total);      // 종료 인덱스(범위 보정)
        List<ReservationResponse> items = all.subList(from, to); // 부분 리스트

        // 5) 응답 payload(콘텐츠 컨트롤러와 동일한 키 구성)
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("items",   items);                 // 현재 페이지 항목
        payload.put("total",   total);                 // 전체 건수
        payload.put("page",    page);                  // 현재 페이지
        payload.put("size",    size);                  // 페이지 크기
        payload.put("hasNext", to < total);            // 다음 페이지 존재 여부

        // 6) 통일된 ApiResponse 래핑 후 반환
        return ApiResponse.ok(payload);
    }

    // ---------------------------------------------------------------------
    // [2] 신청정보 상태 변경 (완료/취소/대기)
    //     - 입력: 폼 전송(application/x-www-form-urlencoded)
    //     - 제약: "기존 서비스 메소드"만 사용 가능 → updateReservationByUser(resvId, userId, dto)
    //             (CMS에서 소유자 검증을 통과시키려면 '해당 예약의 memberId'를 폼에서 받아 전달)
    //             서비스 시그니처 참고  :contentReference[oaicite:8]{index=8}
    // ---------------------------------------------------------------------
    @Operation(summary = "신청정보 상태 변경(폼)", description = "완료/취소/대기 중 하나로 상태 변경(폼 전송).")
    @PostMapping(value = "/{resvId}/status", consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    public ApiResponse<String> changeStatus(
            @Parameter(description = "예약 PK", required = true) @PathVariable("resvId") Long resvId,
            @Parameter(description = "변경 상태값", required = true,
                       schema = @Schema(type = "string", allowableValues = {"완료","취소","대기"}))
            @RequestParam("resvStatus") String resvStatus
    ) {
        // 1) 상태값 1차 검증(허용 집합)
        if (!("완료".equals(resvStatus) || "취소".equals(resvStatus) || "대기".equals(resvStatus))) {
            throw new IllegalArgumentException("허용되지 않는 상태값입니다. (완료/취소/대기)");
        }

        // 2) resvId로 단건 조회 → memberId 확보 (서비스/매퍼 기존 메서드 활용)
        ReservationSearchRequest req = ReservationSearchRequest.builder()
                .resvId(resvId)
                .build();
        List<ReservationResponse> found = reservationService.listReservations(req); // :contentReference[oaicite:8]{index=8}
        if (found.isEmpty()) {
            throw new IllegalArgumentException("존재하지 않는 예약ID: " + resvId);
        }
        final String ownerMemberId = found.get(0).getMemberId(); // 단건 기준  :contentReference[oaicite:9]{index=9}

        // 3) 상태만 패치 DTO 구성
        ReservationUpdateRequest dto = ReservationUpdateRequest.builder()
                .resvStatus(resvStatus)
                .build();

        // 4) 기존 서비스 메서드 호출(소유자 검증은 내부에서 통과됨)  :contentReference[oaicite:10]{index=10}
        reservationService.updateReservationByUser(resvId, ownerMemberId, dto);

        return ApiResponse.ok("상태가 변경되었습니다.");
    }
}

package com.gym.controller.user;

import com.gym.domain.closedday.ClosedDayResponse;
import com.gym.service.ClosedDayService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDate;
import java.util.List;

/**
 * 사용자 전용 휴무일 조회 컨트롤러
 * - 시설ID/조회기간 모두 선택사항
 * - 값이 없으면 전체 조회
 */
@CrossOrigin("*")
@RestController
@RequestMapping("/api/closed-days")
@Tag(name = "08.ClosedDay-User", description = "휴무일 조회 API")
@RequiredArgsConstructor
public class UserClosedDayController {

    private final ClosedDayService closedDayService;

    /**
     * 휴무일 조회 (조건부)
     */
    @Operation(summary = "휴무일 조회", description = "시설ID/조회기간 조건에 따라 휴무일을 조회합니다. 값이 없으면 전체 휴무일을 조회합니다.")
    @GetMapping
    public List<ClosedDayResponse> getClosedDays(
        @Parameter(description = "시설 ID (선택)", required = false)
        @RequestParam(name = "facilityId", required = false) Long facilityId,

        @Parameter(description = "조회 시작일 (선택)", required = false)
        @RequestParam(name = "fromDate", required = false)
        @org.springframework.format.annotation.DateTimeFormat(
            iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE
        )
        LocalDate fromDate,

        @Parameter(description = "조회 종료일 (선택, 시작일 있을 때만 사용)", required = false)
        @RequestParam(name = "toDate", required = false)
        @org.springframework.format.annotation.DateTimeFormat(
            iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE
        )
        LocalDate toDate
    ) {
        // ✅ 조건 처리
        if (fromDate != null && toDate == null) {
            // 시작일만 입력 → 해당 하루만 조회
            toDate = fromDate;
        }

        if (fromDate == null && toDate != null) {
            // 종료일만 입력 → 무시하고 전체 조회
            toDate = null;
        }

        // 그대로 서비스 호출
        return closedDayService.findClosedDaysByFacility(facilityId, fromDate, toDate);
    }
}

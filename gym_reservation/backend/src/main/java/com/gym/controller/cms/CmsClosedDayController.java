package com.gym.controller.cms;

import com.gym.domain.closedday.ClosedDay;
import com.gym.service.ClosedDayService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Parameter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.MediaType;

/**
 * CMS 전용 휴무일 관리 컨트롤러
 * - 등록/수정/삭제: 권한 "담당자", "최고관리자"만 가능
 */
@RestController
@RequestMapping("/api/cms/closed-days")
@Tag(name = "08.ClosedDay-CMS", description = "휴무일 관리 API(CMS)")
@RequiredArgsConstructor
@Slf4j
public class CmsClosedDayController {

    private final ClosedDayService closedDayService;

    /**
     * 휴무일 등록 (form 입력)
     */
    @Operation(summary = "휴무일 등록", description = "담당자/최고관리자만 등록 가능")
    @PostMapping(consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    public Long createClosedDay(
        @Parameter(description = "시설 ID", required = true)
        @RequestParam("facilityId") Long facilityId,
        @Parameter(description = "휴무일(YYYY-MM-DD)", required = true)
        @RequestParam("closedDate") String closedDate,
        @Parameter(description = "사유", required = false)
        @RequestParam(value = "closedContent", required = false) String closedContent
    ) {
        ClosedDay closedDay = new ClosedDay();
        closedDay.setFacilityId(facilityId);
        closedDay.setClosedDate(java.time.LocalDate.parse(closedDate));
        closedDay.setClosedContent(closedContent);

        return closedDayService.createClosedDay(closedDay);
    }

    /**
     * 휴무일 수정 (form 입력)
     */
    @Operation(summary = "휴무일 수정", description = "담당자/최고관리자만 수정 가능")
    @PutMapping(value = "/{closedId}", consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    public String updateClosedDay(
        @Parameter(description = "휴무일 ID", required = true)
        @PathVariable("closedId") Long closedId,
        @Parameter(description = "시설 ID", required = true)
        @RequestParam("facilityId") Long facilityId,
        @Parameter(description = "휴무일(YYYY-MM-DD)", required = true)
        @RequestParam("closedDate") String closedDate,
        @Parameter(description = "사유", required = false)
        @RequestParam(value = "closedContent", required = false) String closedContent
    ) {
        ClosedDay closedDay = new ClosedDay();
        closedDay.setFacilityId(facilityId);
        closedDay.setClosedDate(java.time.LocalDate.parse(closedDate));
        closedDay.setClosedContent(closedContent);

        closedDayService.updateClosedDay(closedId, closedDay);
        return "휴무일이 수정되었습니다.";
    }

    /**
     * 휴무일 삭제
     */
    @Operation(summary = "휴무일 삭제", description = "담당자/최고관리자만 삭제 가능")
    @DeleteMapping("/{closedId}")
    public String deleteClosedDay(
        @Parameter(description = "휴무일 ID", required = true)
        @PathVariable("closedId") Long closedId
    ) {
        closedDayService.deleteClosedDayById(closedId);
        return "휴무일이 삭제되었습니다.";
    }
}

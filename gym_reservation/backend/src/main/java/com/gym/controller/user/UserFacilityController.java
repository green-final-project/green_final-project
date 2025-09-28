package com.gym.controller.user;

// [공통 응답]
import com.gym.common.ApiResponse;
// [도메인]
import com.gym.domain.facility.FacilityResponse;
// [서비스]
import com.gym.service.FacilityService;
// [스프링 MVC]
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
// [유틸/컬렉션]
//import java.util.ArrayList;
import java.util.LinkedHashMap;
//import java.util.List;
import java.util.Map;
// [문서/로그]
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 1) 목록(GET /api/facilities/list) — name(선택) + 간단 페이징 → payload(Map) 반환
 * 2) 단건(GET /api/facilities/{facilityId}) — PK로 FacilityResponse 반환
 * - 사용자 전용: 생성/수정/삭제 없음
 * - 사용 여부 필터는 받지 않음(내부적으로 null 고정)
 */
@Tag(name = "02.Facility-User", description = "사용자 시설 조회 API (목록/단건)")
@RestController
@RequestMapping("/api/facilities")
@RequiredArgsConstructor
@Slf4j
public class UserFacilityController {

    // 서비스 빈 주입(의존성)
    private final FacilityService facilityService;

    /** 1) 목록(GET /api/facilities/list) — name(선택) + 간단 페이징 → payload(Map)
     *  - 요청 파라미터가 비어있으면 전체 조회
     *  - 응답 payload 키는 기존 컨트롤러와 동일: items/total/page/size
     */
    @Operation(summary = "시설 목록(사용자, 폼 패턴)", description = "시설명(name) 선택 입력 + 간단 페이징. 비어있으면 전체 조회")
    @GetMapping("/list")
    public ApiResponse<Map<String, Object>> listForUser(
            // 시설명 검색어(부분 일치; 선택)
            @Parameter(description = "시설명(부분일치)")
            @RequestParam(name = "name", required = false) String name,
            // 페이지 번호(0부터; 기본 0)
            @Parameter(description = "페이지(0부터)")
            @RequestParam(name = "page", defaultValue = "0") int page,
            // 페이지 크기(기본 10)
            @Parameter(description = "페이지 크기")
            @RequestParam(name = "size", defaultValue = "10") int size
    ) {
        // 방어로직: 음수/0 보정
        if (page < 0) page = 0;
        if (size <= 0) size = 10;

        // 서비스 호출(사용여부 필터는 사용자화면에서 받지 않으므로 null 고정, 정렬은 요구 없음 → null)
        var pr = facilityService.searchFacilities(name, null, page, size, null);

        // 응답 payload: 프론트 폼테이블과 통일된 키만 포함 (hasNext 제거)
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("items", pr.getItems()); // 현재 페이지 데이터
        payload.put("total", pr.getTotal()); // 전체 건수
        payload.put("page",  pr.getPage());  // 현재 페이지
        payload.put("size",  pr.getSize());  // 페이지 크기

        log.info("[USER][GET]/api/facilities/list?name={}&page={}&size={}", name, page, size);
        return ApiResponse.ok(payload);
    }

    /** 2) 단건(GET /api/facilities/{facilityId}) — PK로 FacilityResponse 반환 */
    @Operation(summary = "시설 단건 조회(사용자)", description = "PK로 단건 조회")
    @GetMapping("/{facilityId}")
    public ApiResponse<FacilityResponse> getOneForUser(
            // 경로변수: 시설PK
            @Parameter(description = "시설ID(PK)")
            @PathVariable("facilityId") Long facilityId
    ) {
        log.info("[USER][GET]/api/facilities/{}", facilityId);
        // 서비스에서 단건 조회 후 바로 래핑
        return ApiResponse.ok(facilityService.getFacilityById(facilityId));
    }
}

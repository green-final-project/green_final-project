package com.gym.controller.cms;

// [공통 응답/페이지]
import com.gym.common.ApiResponse;
import com.gym.common.PageResponse;
// [도메인 DTO]
import com.gym.domain.facility.FacilityCreateRequest;
import com.gym.domain.facility.FacilityResponse;
import com.gym.domain.facility.FacilityUpdateRequest;
// [서비스]
import com.gym.service.FacilityService;
// [스프링]
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
// [보안] ← ✅ 추가: 로그인 사용자 ID 주입용
import org.springframework.security.core.Authentication;
// [유틸]
import java.util.LinkedHashMap;
import java.util.Map;
// [문서/로그]
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 1) 등록(POST /api/cms/facilities) — 폼 입력(openHour/closeHour 셀렉트) → PK(Long)
 * 2) 수정(PUT /api/cms/facilities/{id}) — 폼 입력(빈값=미변경) → Void
 * 3) 목록(GET /api/cms/facilities) — name/facilityUse + 간단 페이징 → payload(Map)
 * 4) 단건(GET /api/cms/facilities/{id}) — PK로 FacilityResponse
 * - 검증 메시지: “필수 입력사항은 입력해주세요.”, “최소 인원수가 최대 인원수보다 많습니다.”, 시간 선택 오류 등
 * - 금액은 숫자만(프론트 number), 백엔드도 음수 방지
 */
@Tag(name = "02. Facility-CMS", description = "CMS 시설 API (등록/수정 + 공통 조회)")
@RestController
@RequestMapping("/api/cms/facilities")
@RequiredArgsConstructor
@Slf4j
public class CmsFacilityController {

    private final FacilityService facilityService; // 서비스 빈 주입

    /**
     * 1) 등록(POST /api/cms/facilities) — 폼 입력 → PK(Long)
     * - 필수: facilityName
     * - 작성자ID: ✅ 로그인 사용자ID를 서버가 자동 설정(Authentication.getName)
     * - 시간: openHour/closeHour(셀렉트, 정수 문자열) → 서버에서 "HH:mm"
     * - 금액: 숫자만 허용(음수 금지)
     */
    @Operation(
        summary = "시설 등록(폼/CMS)",
        description = "시설명은 필수, 담당자ID는 로그인ID로 자동 설정됩니다. 시간은 openHour/closeHour(셀렉트)로 받고 서버에서 HH:mm으로 조합."
    )
    @PostMapping(consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    public ApiResponse<Long> createFacilityForm(

    		@Parameter(description = "카테고리",
            schema = @Schema(allowableValues = {"수영장","농구장","풋살장","배드민턴장","볼링장"}, example = "수영장"))
    		@RequestParam(name = "facilityType", defaultValue = "수영장") String facilityType,
    
    		@Parameter(description = "시설명", required = true)
            @RequestParam("facilityName") String facilityName,

            // ✅ memberId 입력폼 제거(로그인ID로 자동 주입)
            @Parameter(description = "전화번호")
            @RequestParam(name = "facilityPhone", required = false) String facilityPhone,
            
            @Parameter(description = "내용")
            @RequestParam(name = "facilityContent", required = false) String facilityContent,
            
            @Parameter(description = "이미지")
            @RequestParam(name = "facilityImagePath", required = false) String facilityImagePath,

            @Parameter(description = "최소인원")
    		@RequestParam(name = "facilityPersonMin", required = false) Integer facilityPersonMin,
    		
            @Parameter(description = "최대인원")
            @RequestParam(name = "facilityPersonMax", required = false) Integer facilityPersonMax,
            
            @Parameter(description = "사용여부(true=Y,false=N)",
                    schema = @Schema(allowableValues = {"true","false"}, example = "true"))
            @RequestParam(name = "facilityUse", defaultValue = "true") boolean facilityUse,

            // 셀렉트박스(예약신청 UX와 동일): 정수 문자열 0~23
            @Parameter(description = "운영 시작 시(셀렉트박스, 예: 08)", schema = @Schema(example = "08"))
            @RequestParam(name = "openHour", required = false) String openHour,
            
            @Parameter(description = "운영 종료 시(셀렉트박스, 예: 22)", schema = @Schema(example = "22"))
            @RequestParam(name = "closeHour", required = false) String closeHour,

            @Parameter(description = "1시간 이용료(원, 숫자만)", schema = @Schema(example = "50000"))
            @RequestParam(name = "facilityMoney", required = false) Long facilityMoney,



            // ✅ 로그인 사용자 정보
            Authentication auth
    ) {
        // [필수값] 시설명 필수
        if (isBlank(facilityName)) {
            throw new IllegalArgumentException("필수 입력사항은 입력해주세요.");
        }

        // ✅ 로그인 사용자ID를 담당자ID로 사용
        final String loginId = auth.getName();

        // [인원 검증] 둘 다 있을 때만 비교(부분 입력 허용)
        if (facilityPersonMax != null && facilityPersonMin != null && facilityPersonMin > facilityPersonMax) {
            throw new IllegalArgumentException("최소 인원수가 최대 인원수보다 많습니다.");
        }

        // [시간 조합] 둘 다 있을 때만 HH:mm 생성 + 순서 검증
        String facilityOpenTime = null, facilityCloseTime = null;
        if (!isBlank(openHour) && !isBlank(closeHour)) {
            int oh = parseHour(openHour, "운영시작");
            int ch = parseHour(closeHour, "운영종료");
            if (oh >= ch) throw new IllegalArgumentException("운영시간 선택 오류: 시작 시간은 종료 시간보다 빨라야 합니다.");
            facilityOpenTime = String.format("%02d:00", oh);
            facilityCloseTime = String.format("%02d:00", ch);
        }

        // [금액 검증] 음수 금지
        if (facilityMoney != null && facilityMoney < 0) {
            throw new IllegalArgumentException("이용료는 0 이상 숫자만 입력해주세요.");
        }

        // 폼 → DTO 매핑
        FacilityCreateRequest req = new FacilityCreateRequest();
        req.setFacilityName(facilityName.trim());
        req.setMemberId(loginId); // ✅ 담당자ID = 로그인ID
        req.setFacilityPhone(trimOrNull(facilityPhone));
        req.setFacilityContent(trimOrNull(facilityContent));
        req.setFacilityImagePath(trimOrEmpty(facilityImagePath)); // ✅ 변경: NULL 대신 ""로 전달
        req.setFacilityPersonMax(facilityPersonMax);
        req.setFacilityPersonMin(facilityPersonMin);
        req.setFacilityUse(facilityUse);
        req.setFacilityOpenTime(facilityOpenTime);
        req.setFacilityCloseTime(facilityCloseTime);
        req.setFacilityMoney(facilityMoney);
        req.setFacilityType(trimOrNull(facilityType));

        log.info("[CMS][POST]/api/cms/facilities form={}", req);
        return ApiResponse.ok(facilityService.createFacility(req));
    }

    
    @Operation(summary = "시설 수정(폼/CMS)", description = "빈값은 미변경. 시간은 openHour/closeHour가 둘 다 있을 때만 HH:mm으로 갱신.")
    @PutMapping(value = "/{facilityId}", consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    public ApiResponse<Void> updateFacilityForm(
            @PathVariable("facilityId") Long facilityId,

            @Parameter(description = "카테고리",
                    schema = @Schema(allowableValues = {"수영장","농구장","풋살장","배드민턴장","볼링장"}, example = "수영장"))
            @RequestParam(name = "facilityType", required = false) String facilityType,

            @Parameter(description = "시설명")
            @RequestParam(name = "facilityName", required = false) String facilityName,

            // ✅ memberId 입력폼 제거(수정 시에도 담당자ID는 미변경)
            @Parameter(description = "전화번호")
            @RequestParam(name = "facilityPhone", required = false) String facilityPhone,

            @Parameter(description = "내용")
            @RequestParam(name = "facilityContent", required = false) String facilityContent,

            @Parameter(description = "이미지")
            @RequestParam(name = "facilityImagePath", required = false) String facilityImagePath,

            @Parameter(description = "최소인원")
            @RequestParam(name = "facilityPersonMin", required = false) Integer facilityPersonMin,

            @Parameter(description = "최대인원")
            @RequestParam(name = "facilityPersonMax", required = false) Integer facilityPersonMax,

            @Parameter(description = "사용여부(true=Y,false=N)",
                    schema = @Schema(allowableValues = {"true","false"}, example = "true"))
            @RequestParam(name = "facilityUse", required = false) Boolean facilityUse,

            // 셀렉트박스(예약신청 UX와 동일): 정수 문자열 0~23
            @Parameter(description = "운영 시작 시(셀렉트박스, 예: 08)", schema = @Schema(example = "08"))
            @RequestParam(name = "openHour", required = false) String openHour,

            @Parameter(description = "운영 종료 시(셀렉트박스, 예: 22)", schema = @Schema(example = "22"))
            @RequestParam(name = "closeHour", required = false) String closeHour,

            @Parameter(description = "1시간 이용료(원, 숫자만)", schema = @Schema(example = "50000"))
            @RequestParam(name = "facilityMoney", required = false) Long facilityMoney
    ) {
        // [인원 비교] 둘 다 있을 때만 체크 (부분 입력 허용)
        if (facilityPersonMax != null && facilityPersonMin != null && facilityPersonMin > facilityPersonMax) {
            throw new IllegalArgumentException("최소 인원수가 최대 인원수보다 많습니다.");
        }

        // [시간 조합] 둘 다 있을 때만 HH:mm 생성 + 순서 검증
        String facilityOpenTime = null, facilityCloseTime = null;
        if (!isBlank(openHour) && !isBlank(closeHour)) {
            int oh = parseHour(openHour, "운영시작");
            int ch = parseHour(closeHour, "운영종료");
            if (oh >= ch) throw new IllegalArgumentException("운영시간 선택 오류: 시작 시간은 종료 시간보다 빨라야 합니다.");
            facilityOpenTime = String.format("%02d:00", oh);
            facilityCloseTime = String.format("%02d:00", ch);
        }

        // [금액 검증] 음수 금지
        if (facilityMoney != null && facilityMoney < 0) {
            throw new IllegalArgumentException("이용료는 0 이상 숫자만 입력해주세요.");
        }

        // DTO 구성 — ❗빈문자는 null로 바꿔서 '미변경' 처리(서비스에서 null-머지 가정)
        FacilityUpdateRequest req = new FacilityUpdateRequest();
        req.setFacilityName(trimOrNull(facilityName));
        req.setMemberId(null);                         // ✅ 담당자ID는 수정하지 않음(미변경)
        req.setFacilityPhone(trimOrNull(facilityPhone));
        req.setFacilityContent(trimOrNull(facilityContent));
        req.setFacilityImagePath(trimOrEmpty(facilityImagePath)); // ✅ 빈값→null→미변경
        req.setFacilityPersonMax(facilityPersonMax);
        req.setFacilityPersonMin(facilityPersonMin);
        req.setFacilityUse(facilityUse);
        req.setFacilityOpenTime(facilityOpenTime);     // 둘 다 없으면 null → 미변경
        req.setFacilityCloseTime(facilityCloseTime);
        req.setFacilityMoney(facilityMoney);
        req.setFacilityType(trimOrNull(facilityType));

        log.info("[CMS][PUT]/api/cms/facilities/{} form={}", facilityId, req);
        facilityService.updateFacility(facilityId, req);
        return ApiResponse.ok();
    }
    

    @Operation(summary = "시설 목록(폼/CMS)", description = "시설명/사용여부 조건으로 조회(미입력 시 전체) + 간단 페이징. 응답키는 items/total/page/size")
    @GetMapping
    public ApiResponse<Map<String, Object>> listForCms(
            @Parameter(description = "시설명(부분일치)") @RequestParam(name = "name", required = false) String name,
            @Parameter(description = "사용여부(true=Y,false=N). 미입력 시 전체") @RequestParam(name = "facilityUse", required = false) Boolean facilityUse,
            @Parameter(description = "페이지(0부터)") @RequestParam(name = "page", defaultValue = "0") int page,
            @Parameter(description = "페이지 크기") @RequestParam(name = "size", defaultValue = "10") int size
    ) {
        if (page < 0) page = 0;
        if (size <= 0) size = 10;

        PageResponse<FacilityResponse> pr = facilityService.searchFacilities(name, facilityUse, page, size, null);

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("items", pr.getItems());
        payload.put("total", pr.getTotal());
        payload.put("page", pr.getPage());
        payload.put("size", pr.getSize());

        log.info("[CMS][GET]/api/cms/facilities?name={}&use={}&page={}&size={}", name, facilityUse, page, size);
        return ApiResponse.ok(payload);
    }

    
    
    /** 4) 단건(GET /api/cms/facilities/{id}) — PK로 FacilityResponse */
    @Operation(summary = "시설 단건 조회(CMS)", description = "PK 기준 단건 조회 (공통)")
    @GetMapping("/{facilityId}")
    public ApiResponse<FacilityResponse> getFacilityByIdCms(
            @Parameter(description = "시설ID(PK)") @PathVariable("facilityId") Long facilityId
    ) {
        log.info("[CMS][GET]/api/cms/facilities/{}", facilityId);
        return ApiResponse.ok(facilityService.getFacilityById(facilityId));
    }

    // 내부 유틸: 빈문자 처리/시 검증 
    private static boolean isBlank(String s) {
        return s == null || s.trim().isEmpty();
    }
    private static String trimOrNull(String s) {
        if (s == null) return null;
        String t = s.trim();
        return t.isEmpty() ? null : t;
    }
    private static int parseHour(String hour, String label) {
        try {
            int h = Integer.parseInt(hour); // "08" → 8, "22" → 22
            if (h < 0 || h > 23) throw new NumberFormatException();
            return h;
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(label + " 시간은 0~23 사이 정수만 허용됩니다.");
        }
    }
    private static String trimOrEmpty(String s) {  // ✅ 추가: NULL → "" 변환(Oracle setNull(… ,1111) 회피)
        return (s == null) ? "" : s.trim();
    }
}

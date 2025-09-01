package com.gym.controller.user;                                           // 📦 컨트롤러 패키지

import com.gym.common.ApiResponse;                                         // 📦 표준 응답
import com.gym.domain.facility.Facility;                                   // 🏟 DTO
import com.gym.mapper.xml.FacilityQueryMapper;                             // 🗺 매퍼
import lombok.RequiredArgsConstructor;                                     // 🧩 생성자 주입
import org.springframework.web.bind.annotation.*;                          // 🌐 REST

import java.util.List;                                                     // 📚 목록

// Swagger 문서화(드롭다운 표시용 enum)
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;

@RestController
@RequestMapping("/api/facilities")                                         // 🌐 공통 Prefix
@RequiredArgsConstructor
public class FacilityController {

    private final FacilityQueryMapper facilityQueryMapper;                 // 💉 매퍼 주입

    @Operation(summary = "시설 목록 조회", description = "시설명 부분검색, 카테고리, 사용여부로 필터링합니다.")
    @GetMapping
    public ApiResponse<List<Facility>> searchFacilities(
            @Parameter(description = "시설명 부분검색")
            @RequestParam(name = "name", required = false) String name,    // 🔎 선택

            @Parameter(
                description = "카테고리 선택",
                schema = @Schema(allowableValues = {"수영장","농구장","풋살장","배드민턴장","볼링장"}) // ★ DDL 체크값과 동일(배드민턴장)
            )
            @RequestParam(name = "category", required = false) String category, // 🏷 선택(= facility_type)

            @Parameter(description = "사용여부(Boolean). true=Y, false=N")
            @RequestParam(name = "facilityUse", required = false) Boolean facilityUse // ✅ 선택
    ) {
        // Boolean → 'Y'/'N' 변환(미지정 시 null 유지)
        String facilityUseYn = (facilityUse == null) ? null : (facilityUse ? "Y" : "N");

        // DB 조회 호출 (category → facility_type 로 매핑)
        List<Facility> rows = facilityQueryMapper.searchFacilities(name, category, facilityUseYn);

        return ApiResponse.ok(rows);                                       // 표준 응답
    }
}

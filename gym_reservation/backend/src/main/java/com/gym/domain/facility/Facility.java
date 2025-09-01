package com.gym.domain.facility;                                           // 📦 DTO 패키지

import lombok.*;                                                           // 🧩 롬복

/**
 * facility_tbl ↔ DTO 매핑
 * - 스네이크(snake_case) ↔ 카멜(camelCase)은 전역 설정(map-underscore-to-camel-case=true)로 자동 매핑
 * - 카테고리 컬럼(facility_category) 매핑 필드 추가
 */
@Getter @Setter @ToString
@NoArgsConstructor @AllArgsConstructor @Builder
public class Facility {
    private Long   facilityId;      // 시설 고유 번호(PK)
    private String facilityName;    // 시설명
    private String facilityType;    // ★ 카테고리(수영장/농구장/풋살장/배드민턴장/볼링장) ← DDL의 facility_type
    private String memberId;        // 담당자ID
    private String facilityUse;     // 사용여부(Y/N)
    private Long   facilityMoney;   // 1시간 이용료(원)

    // 필요 시 스키마 맞춰 확장 (주석 해제)
    // private String  facilityPhone;
    // private String  facilityContent;
    // private String  facilityImagePath;
    // private Integer facilityPersonMax;
    // private Integer facilityPersonMin;
    // private String  facilityOpenTime;
    // private String  facilityCloseTime;
    // private String  facilityRegDate;
    // private String  facilityModDate;
}

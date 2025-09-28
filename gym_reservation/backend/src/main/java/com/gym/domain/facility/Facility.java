package com.gym.domain.facility;

import lombok.*;
import java.time.LocalDateTime;

/** facility_tbl 1:1 매핑 엔티티 */
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder @ToString
public class Facility {
    private Long facilityId;             // NUMBER PK
    private String facilityName;         // VARCHAR2 NOT NULL
    private String memberId;             // VARCHAR2 (강사/관리자 ID)
    private String facilityPhone;        // VARCHAR2
    private String facilityContent;      // CLOB
    private String facilityImagePath;    // VARCHAR2
    private Integer facilityPersonMax;   // NUMBER NOT NULL
    private Integer facilityPersonMin;   // NUMBER NOT NULL
    private boolean facilityUse;         // CHAR('Y'/'N') ↔ boolean(전역 핸들러)
    private LocalDateTime facilityRegDate;   // DATE NOT NULL
    private LocalDateTime facilityModDate;   // DATE
    private String facilityOpenTime;     // DATE ↔ "HH:mm" 문자열(조회시 TO_CHAR)
    private String facilityCloseTime;    // DATE ↔ "HH:mm" 문자열(조회시 TO_CHAR)
    private Long facilityMoney;          // NUMBER NOT NULL(기본 0)
    private String facilityType;		 // VARCHAR2(50)   DEFAULT '수영장' NOT NULL (수영장/농구장/풋살장/배드민턴장/볼링장)
}
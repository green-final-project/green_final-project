package com.gym.domain.facility;

import lombok.*;

/** 시설 생성 요청 DTO (NOT NULL만 필수) */
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder @ToString
public class FacilityCreateRequest {
    private String facilityName;          // required
    private String memberId;
    private String facilityPhone;
    private String facilityContent;
    private String facilityImagePath;
    private Integer facilityPersonMax;    // required
    private Integer facilityPersonMin;    // required
    private Boolean facilityUse;          // null → DB default(또는 Service 보정)
    private String facilityOpenTime;      // "HH:mm"
    private String facilityCloseTime;     // "HH:mm"
    private Long facilityMoney;           // required(또는 0)
    private String facilityType;		 // VARCHAR2(50)   DEFAULT '수영장' NOT NULL (수영장/농구장/풋살장/배드민턴장/볼링장)
}

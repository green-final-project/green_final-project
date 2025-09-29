package com.gym.domain.facility;

import lombok.*;

/** null 필드 미변경 */
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder @ToString
public class FacilityUpdateRequest {
    private String facilityName;
    private String memberId;
    private String facilityPhone;
    private String facilityContent;
    private String facilityImagePath;
    private Integer facilityPersonMax;
    private Integer facilityPersonMin;
    private Boolean facilityUse;
    private String facilityOpenTime;   // "HH:mm"
    private String facilityCloseTime;  // "HH:mm"
    private Long facilityMoney;
    private String facilityType;		 // VARCHAR2(50)   DEFAULT '수영장' NOT NULL (수영장/농구장/풋살장/배드민턴장/볼링장)
}

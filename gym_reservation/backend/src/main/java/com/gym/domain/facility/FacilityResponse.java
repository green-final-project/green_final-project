package com.gym.domain.facility;

import lombok.*;
import java.time.LocalDateTime;

/** 비밀번호/민감정보 없음 */
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder @ToString
public class FacilityResponse {
    private Long facilityId;
    private String facilityName;
    private String memberId;
    private String facilityPhone;
    private String facilityContent;
    private String facilityImagePath;
    private Integer facilityPersonMax;
    private Integer facilityPersonMin;
    private boolean facilityUse;
    private LocalDateTime facilityRegDate;
    private LocalDateTime facilityModDate;
    private String facilityOpenTime;    // "HH:mm"
    private String facilityCloseTime;   // "HH:mm"
    private Long facilityMoney;
    private String facilityType;		 // VARCHAR2(50)   DEFAULT '수영장' NOT NULL (수영장/농구장/풋살장/배드민턴장/볼링장)
}

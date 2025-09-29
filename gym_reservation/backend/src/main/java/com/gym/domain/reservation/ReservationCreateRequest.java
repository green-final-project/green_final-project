package com.gym.domain.reservation;

import lombok.*;

/**
 * 예약 등록 요청 DTO
 * - 날짜/시간은 문자열로 받아 서비스 계층에서 파싱
 */
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder @ToString
public class ReservationCreateRequest {
    private String memberId;
    private Long facilityId;
    private String resvContent;
    private String wantDate;       // "yyyy-MM-dd"
    private Integer resvPersonCount;
    private String resvStartTime;  // "yyyy-MM-dd HH:mm:ss"
    private String resvEndTime;    // "yyyy-MM-dd HH:mm:ss"
}

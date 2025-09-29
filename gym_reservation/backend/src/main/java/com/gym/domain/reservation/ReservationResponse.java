package com.gym.domain.reservation;

import lombok.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 예약 응답 DTO
 * - DATE ↔ LocalDate, TIMESTAMP ↔ LocalDateTime
 */
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder @ToString
public class ReservationResponse {
    private Long resvId;
    private String memberId;
    private String memberName; // [250919추가] 회원명
    private Long facilityId;
    private String facilityName;  // [250919 추가] 시설명
    private String resvContent;
    private LocalDate wantDate;
    private LocalDate resvDate;
    private Integer resvPersonCount;
    private String resvStatus;
    private Long facilityMoney;
    private LocalDateTime resvStartTime;
    private LocalDateTime resvEndTime;
    private Integer resvMoney;
    
    // [250919 추가] 예약 취소 신청
    private String resvCancel;        // 'Y'/'N' 등 -> String
    private String resvCancelReason;  // 취소사유 -> String
}

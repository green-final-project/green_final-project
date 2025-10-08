package com.gym.domain.reservation;

import lombok.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * reservation_tbl 매핑 엔티티
 * - DB snake_case ↔ Java camelCase 매핑(mapUnderscoreToCamelCase=true 전제)
 */
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder @ToString
public class Reservation {

    private Long resvId;                 // PK
    private String memberId;             // FK
    private Long facilityId;             // FK
    private String resvContent;          // 요구사항

    // 엑셀/DDL 정합화: want_date/resv_date → DATE ↔ LocalDate
    private LocalDate wantDate;          // 예약 희망일 (DATE)
    private LocalDate resvDate;          // 신청일 (DATE)

    private LocalDateTime resvLogTime;   // 신청 로그 시각 (TIMESTAMP)
    private Integer resvPersonCount;     // 신청 인원
    private String resvStatus;           // 상태
    private Long facilityMoney;          // 시설 이용료(스냅샷)  ← Long으로 정합화
    private LocalDateTime resvStartTime; // 이용 시작 (TIMESTAMP)
    private LocalDateTime resvEndTime;   // 이용 종료 (TIMESTAMP)
    private Integer resvMoney;           // 총 금액
    
    // [250919] 추가 컬럼
    private String resvCancel;        // 취소신청 여부 (Y/N)
    private String resvCancelReason;  // 취소 사유
}

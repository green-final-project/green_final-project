package com.gym.domain.reservation;


import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.ToString;

/**
 * 예약 목록 조회 요청 DTO
 */
@Getter
@Setter                // << 추가: Spring MVC 바인딩 위해 필요
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString
public class ReservationSearchRequest {
	// 예약신청 ID
	private Long resvId;
    
	// 회원(신청자) ID
    private String memberId;
    
    // [250919 추가] 회원명
    private String memberName;
    
    // [250919 추가] 시설명
    private String facilityName;
    
    // 시설 ID 
    private Long facilityId;
    
    // [250919 추가] 검색·표시용 주요 컬럼(필요 최소만 노출)
    private String resvStatus;      // 예약상태(예: 신청/완료/취소 등)
    private String wantDate;        // 예약일(문자열/포맷은 서비스단에서 결정)
    private String resvStartTime;   // 시작일시
    private String resvEndTime;     // 종료일시
    private String resvCancel;    // 취소여부
}
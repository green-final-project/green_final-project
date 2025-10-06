package com.gym.service; // 서비스 인터페이스 패키지

import com.gym.domain.reservation.ReservationCreateRequest; // 등록 DTO
import com.gym.domain.reservation.ReservationUpdateRequest; // 수정 DTO
import com.gym.domain.reservation.ReservationSearchRequest; // 검색 DTO 
import com.gym.domain.reservation.ReservationResponse;		// 응답 DTO
import java.util.List; // 목록


public interface ReservationService {

    // 등록: 생성된 PK(resvId) 반환
    Long createReservation(ReservationCreateRequest request);

    // 목록 조회: 검색 도메인에 있는 것들로만 검색 가능(resvId/userId/facilityId)
    // 미입력 상태에서 검색 시, 전체가 일갈 조회 
    List<ReservationResponse> listReservations(ReservationSearchRequest req);

    // 수정(소유자 강제): resvId + userId 일치 시만 반영
    int updateReservationByUser(Long resvId, String userId,
                                ReservationUpdateRequest request);

    // 삭제(소유자 강제): resvId + userId 일치 시만 삭제
    int deleteReservationByUser(Long resvId, String userId);
    

    // [250919 신규] 취소신청 — resv_cancel='Y' 로만 업데이트 (idempotent)
    int requestReservationCancel(Long resvId, String userId, String resvCancelReason);
    
    // [251002 신규] 예약단건조회
    ReservationResponse getReservation(Long resvId); 
    // 필요한 이유 : (플로우 구조) 시설정보 조회 후, 예약신청을 진행하는 구조를 구성하기 위해선 단건조회 기능 필요함  
}

package com.gym.mapper.xml;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;

// [추가]
import com.gym.domain.reservation.ReservationSearchRequest;   // [추가]
import com.gym.domain.reservation.ReservationResponse;        // [추가]

@Mapper
public interface ReservationQueryMapper {

    // [삭제] 예전 시그니처
//  List<Reservation> listReservations(@Param("userId") String userId,
//                                     @Param("facilityId") Long facilityId,
//                                     @Param("page") Integer page,
//                                     @Param("size") Integer size);               // [삭제]

    // [수정] 검색 DTO 단일 파라미터(@Param("req")), 반환 타입 응답 DTO
    List<ReservationResponse> listReservations(@Param("req") ReservationSearchRequest req); // [수정]
    
    
    // [추가] 예약 중복 여부 확인
    boolean existsOverlapReservation(
    	    @Param("facilityId") Long facilityId,
    	    @Param("resvStartTime") LocalDateTime resvStartTime,
    	    @Param("resvEndTime")   LocalDateTime resvEndTime
    	    );

}

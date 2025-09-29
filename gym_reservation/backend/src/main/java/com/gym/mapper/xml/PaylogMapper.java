package com.gym.mapper.xml;                         // 매퍼 패키지 고정 (프로젝트 구조 준수)

import org.apache.ibatis.annotations.Mapper;         // MyBatis 매퍼 선언
import org.apache.ibatis.annotations.Param;          // 파라미터 바인딩
import java.util.List;                               // 목록 반환

import com.gym.domain.payment.PaylogResponse;        // ← 사용자가 작성한 조회 DTO 재사용

@Mapper                                              // 매퍼 스캔 대상
public interface PaylogMapper {

    /**
     * 결제ID별 결제로그 조회(최신순)
     * - 입력: paymentId (결제 PK)
     * - 반환: PaylogResponse 리스트(조회 전용 DTO)
     * - XML 쿼리ID: selectByPaymentId
     */
    List<PaylogResponse> selectByPaymentId(@Param("paymentId") Long paymentId);
}

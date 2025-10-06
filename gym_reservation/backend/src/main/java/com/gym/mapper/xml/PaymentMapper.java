package com.gym.mapper.xml;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import java.util.List;

import com.gym.domain.payment.Payment;
import com.gym.domain.payment.PaymentSearchRequest;

@Mapper
public interface PaymentMapper {

    /**
     * 결제 등록 (INSERT)
     * - 입력: Payment 엔티티 (paymentId는 트리거/시퀀스로 자동 생성)
     * - 반환: 영향받은 행 수 (성공 시 1, 실패 시 0)
     * - XML 매퍼: insertPayment
     */
    int insertPayment(@Param("p") Payment p);
    

    /**
     * 마지막으로 발급된 결제 PK값 조회
     * - 같은 세션/트랜잭션 내에서만 사용 가능
     * - INSERT 직후 호출하여 방금 생성된 paymentId를 회수하는 용도
     * - XML 매퍼: getPaymentSeqCurrval
     */
    Long getPaymentSeqCurrval(); 

    /**
     * 결제 단건 조회 (PK 기준)
     * - 입력: paymentId (결제ID)
     * - 반환: Payment 엔티티 (없으면 null)
     * - XML 매퍼: selectPaymentById
     */
    //Payment selectPaymentById(@Param("paymentId") Long paymentId);

    /**
     * 결제 목록/검색 조회
     * - 입력: PaymentSearchRequest (회원ID, 예약ID, 결제수단, 상태 등 조건)
     * - 반환: 조건에 맞는 결제 리스트
     * - XML 매퍼: selectPayments
     */
    List<Payment> selectPayments(@Param("req") PaymentSearchRequest req);

    /**
     * 결제 건수 조회
     * - 입력: PaymentSearchRequest (회원ID, 예약ID, 결제수단, 상태 등 조건)
     * - 반환: 조건에 맞는 총 건수
     * - 보통 페이징 처리 시 전체 개수 계산 용도
     * - XML 매퍼: countPayments
     */
    //int countPayments(@Param("req") PaymentSearchRequest req);

    /**
     * 결제 상태 변경 (UPDATE)
     * - 입력: paymentId (결제ID), status (새로운 상태값: 완료/예약/취소)
     * - 반환: 영향받은 행 수 (성공 시 1)
     * - XML 매퍼: updatePaymentStatus
     */
    int updatePaymentStatus(@Param("paymentId") Long paymentId,
                            @Param("status") String status);
    
    
    
}

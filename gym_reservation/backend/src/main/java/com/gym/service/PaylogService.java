package com.gym.service;                              // 서비스 패키지

import java.util.List;
import com.gym.domain.payment.PaylogResponse;         // 조회 전용 DTO

/**
 * 결제로그 서비스(조회 전용)
 * - 쓰기(INSERT) 없음: 로그는 트리거에서 기록됨
 */
public interface PaylogService {

    /**
     * 결제ID별 로그 조회(최신순)
     * @param paymentId 결제 PK
     * @return PaylogResponse 목록
     */
    List<PaylogResponse> listPaylogsByPayment(Long paymentId);
}


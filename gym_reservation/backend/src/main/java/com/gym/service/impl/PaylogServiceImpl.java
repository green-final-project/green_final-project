package com.gym.service.impl;                         // 구현 패키지

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import com.gym.domain.payment.PaylogResponse;         // 조회 DTO
import com.gym.mapper.xml.PaylogMapper;               // 매퍼
import com.gym.service.PaylogService;                 // 서비스 인터페이스

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(rollbackFor = Exception.class)         // 읽기 전용 메서드도 트랜잭션 경계 유지
public class PaylogServiceImpl implements PaylogService {

    private final PaylogMapper paylogMapper;          // 매퍼 주입

    /**
     * 결제ID별 로그 조회(최신순)
     */
    @Override
    @Transactional(readOnly = true)                   // 읽기 전용 트랜잭션
    public List<PaylogResponse> listPaylogsByPayment(Long paymentId) {
        return paylogMapper.selectByPaymentId(paymentId);
    }
}


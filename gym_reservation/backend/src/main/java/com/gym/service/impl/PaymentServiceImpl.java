package com.gym.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

import com.gym.domain.payment.*;
import com.gym.mapper.xml.PaymentMapper;
import com.gym.service.PaymentService;
// import org.springframework.dao.DataIntegrityViolationException;


//[250925추가] 예약 동기화/조회용
import org.springframework.jdbc.core.JdbcTemplate; // 예약/결제 보조 조회/갱신(SQL 한 줄)
//[250925추가] 문자 전송
import com.gym.service.MessageService; // 기존 서비스 인터페이스 사용
import com.gym.domain.message.Message; // 메시지 엔티티(이력/전송용)



@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(rollbackFor = Exception.class)
public class PaymentServiceImpl implements PaymentService {

	private final PaymentMapper paymentMapper;
	private final JdbcTemplate jdbcTemplate; // [250925추가] 보조 SQL
    private final MessageService messageService; // [250925추가] 문자 서비스
	/**
     * 결제 등록
     * - paymentMethod 미입력 시 accountId/cardId로 자동 유추
     * - paymentStatus 미입력 시 '예약' 기본값 적용
     * - INSERT 성공 시 같은 세션에서 CURRVAL 회수
     */
	@Override
    @Transactional(rollbackFor = Exception.class)
    public Long create(PaymentCreateRequest req) {
        // [1] 결제수단 보정: 미입력 시 accountId/cardId로 자동 유추
        String method = (req.getPaymentMethod() == null) ? "" : req.getPaymentMethod().trim();
        if (method.isEmpty()) { // 미입력 → 자동 유추
            if (req.getAccountId() != null && req.getCardId() == null) {
                method = "계좌";
            } else if (req.getCardId() != null && req.getAccountId() == null) {
                method = "카드";
            } else {
                throw new IllegalArgumentException("결제수단을 유추할 수 없습니다. 계좌 또는 카드 중 하나만 지정하세요.");
            }
            req.setPaymentMethod(method);
        } else { // 명시 입력 → 조합 규칙 검사(상호배타)
            if ("계좌".equals(method)) {
                if (req.getAccountId() == null || req.getCardId() != null) {
                    throw new IllegalArgumentException("결제수단이 '계좌'이면 accountId만 지정해야 합니다.");
                }
            } else if ("카드".equals(method)) {
                if (req.getCardId() == null || req.getAccountId() != null) {
                    throw new IllegalArgumentException("결제수단이 '카드'이면 cardId만 지정해야 합니다.");
                }
            } else {
                throw new IllegalArgumentException("paymentMethod는 '계좌' 또는 '카드'만 허용됩니다.");
            }
        }

        // [2] 상태 기본값 보정: 미입력 시 '예약'
        if (req.getPaymentStatus() == null || req.getPaymentStatus().isBlank()) {
            req.setPaymentStatus("예약");
        }

        // [3] 엔티티 빌드
        Payment p = Payment.builder()
                .memberId(req.getMemberId())
                .accountId(req.getAccountId())
                .cardId(req.getCardId())
                .resvId(req.getResvId())
                .paymentMoney(req.getPaymentMoney())
                .paymentMethod(req.getPaymentMethod())   // 위에서 보정
                .paymentStatus(req.getPaymentStatus())   // 위에서 보정
                .build();

        // [4] INSERT 실행 (조건 불일치 시 0 반환)
        int rows = paymentMapper.insertPayment(p);
        if (rows != 1) {
            throw new IllegalArgumentException("결제 등록 실패: 잘못된 값 또는 존재하지 않는 참조");
        }

        // [5] 같은 세션에서 CURRVAL 회수
        Long id = paymentMapper.getPaymentSeqCurrval();
        p.setPaymentId(id);
        return id;
    }

	/**
     * 결제 목록/검색
     * - paymentId, memberId, resvId, method, paymentStatus 등 선택적 필터
     * - 페이징 없음(요청 범위 준수)
     */
    @Override
    public List<PaymentResponse> findList(PaymentSearchRequest req) {
        return paymentMapper.selectPayments(req).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }


	/*
	 * @Override public int count(PaymentSearchRequest req) { return
	 * paymentMapper.countPayments(req); }
	 */

    
    /**
     * 결제 상태 변경
     * - 허용값: '완료' | '예약' | '취소'
     */
	@Override
	@Transactional(rollbackFor = Exception.class)
	public void updateStatus(Long paymentId, String status) {
		int rows = paymentMapper.updatePaymentStatus(paymentId, status);
		if (rows != 1)
			throw new IllegalStateException("상태 변경 실패");
		// [250925추가] 상태 변경 성공 시 후속 처리(예약 동기화 + 문자 발송)
        // 1) 결제건에서 예약ID/회원ID 확보
        Long resvId = jdbcTemplate.queryForObject(
                "SELECT p.resv_id FROM payment_tbl p WHERE p.payment_id = ?",
                Long.class,  // requiredType 먼저
                paymentId
        ); // 예약 PK

        String memberId = jdbcTemplate.queryForObject(
                "SELECT p.member_id FROM payment_tbl p WHERE p.payment_id = ?",
                String.class,
                paymentId
        ); // 신청자 ID

        // 2) 예약 상태 동기화(트리거 보정은 남기되, 서비스 계층에서 먼저 반영)
        if ("완료".equals(status)) {
            jdbcTemplate.update(
                    "UPDATE reservation_tbl SET resv_status = '완료' WHERE resv_id = ? AND resv_status <> '완료' AND resv_status <> '취소'",
                    resvId
            ); // 취소된 예약은 되살리지 않음
        } else if ("취소".equals(status)) {
            jdbcTemplate.update(
                    "UPDATE reservation_tbl SET resv_status = '취소' WHERE resv_id = ? AND resv_status <> '취소'",
                    resvId
            );
        } // '예약'은 동기화 불필요

        // 3) 문자 발송(상태별 고정 문구) — MessageService 시그니처 유지
        if ("완료".equals(status)) {
            Message msg = Message.builder()
                    .memberId(memberId)                 // 수신자 회원ID
                    .resvId(resvId)                     // 관련 예약ID
                    .messageType("예약확인")            // 유형
                    .messageContent("예약신청 완료되었습니다.") // 본문
                    .build();
            messageService.sendMessage(msg);            // 기존 시그니처 사용
        } else if ("취소".equals(status)) {
            Message msg = Message.builder()
                    .memberId(memberId)
                    .resvId(resvId)
                    .messageType("예약취소")
                    .messageContent("예약신청 취소되었습니다.")
                    .build();
            messageService.sendMessage(msg);
        }
	
	}

		
	/** 내부 변환: 엔티티 → 응답 DTO */
    private PaymentResponse toResponse(Payment p) {
        return PaymentResponse.builder()
                .paymentId(p.getPaymentId())
                .memberId(p.getMemberId())
                .accountId(p.getAccountId())
                .cardId(p.getCardId())
                .resvId(p.getResvId())
                .paymentMoney(p.getPaymentMoney())
                .paymentMethod(p.getPaymentMethod())
                .paymentStatus(p.getPaymentStatus())
                .paymentDate(p.getPaymentDate())
                .build();
    }
}

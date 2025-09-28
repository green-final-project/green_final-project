package com.gym.domain.payment;   // 결제 도메인 패키지

import lombok.*;
import java.time.LocalDateTime;

/**
 * 결제 조회 응답 DTO
 * - 단건 조회 및 목록 조회 시 반환
 */
@Getter				// 클래스의 모든 필드에 대해 getter 메서드를 자동 생성
@Setter				// 클래스의 모든 필드에 대해 setter 메서드를 자동 생성
@NoArgsConstructor	// 파라미터가 없는 기본 생성자를 자동 생성
@AllArgsConstructor	// 모든 필드를 파라미터로 받는 생성자를 자동 생성
@Builder 			// 빌더 패턴 메서드를 자동 생성 (체이닝으로 객체 생성 가능)
@ToString			// 모든 필드 값을 문자열로 표현하는 toString() 메서드를 자동 생성
public class PaymentResponse {

    private Long paymentId;				// 결제PK NUMBER PK → payment_tbl.payment_id
    private String memberId;			// 회원ID VARCHAR2 → payment_tbl.member_id
    private Long accountId;				// 계좌ID NUMBER → payment_tbl.account_id
    private Long cardId;				// 카드ID NUMBER → payment_tbl.card_id
    private Long resvId;				// 예약ID NUMBER → payment_tbl.resv_id
    private Long paymentMoney;			// 결제금액 NUMBER → payment_tbl.payment_money
    private String paymentMethod;		// 결제수단 VARCHAR2 → payment_tbl.payment_method
    private String paymentStatus;		// 결제상태 VARCHAR2 → payment_tbl.payment_status
    private LocalDateTime paymentDate;	// 결제일시 DATE → payment_tbl.payment_date
}

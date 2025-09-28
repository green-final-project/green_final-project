package com.gym.domain.payment;

import lombok.*;                  // 롬복 일괄 적용
import java.time.LocalDateTime;   // Oracle DATE ↔ Java LocalDateTime 매핑

/**
 * payment_tbl 엔티티(컬럼 1:1 매핑)
 * - DDL 제약조건과 동일하게 작성
 * - payment_date: INSERT 시 DB 기본값(SYSDATE) 사용
 */
@Getter				// 클래스의 모든 필드에 대해 getter 메서드를 자동 생성
@Setter				// 클래스의 모든 필드에 대해 setter 메서드를 자동 생성
@NoArgsConstructor	// 파라미터가 없는 기본 생성자를 자동 생성
@AllArgsConstructor	// 모든 필드를 파라미터로 받는 생성자를 자동 생성
@Builder 			// 빌더 패턴 메서드를 자동 생성 (체이닝으로 객체 생성 가능)
@ToString			// 모든 필드 값을 문자열로 표현하는 toString() 메서드를 자동 생성
public class Payment {

    private Long paymentId;				// 결제PK NUMBER PK → payment_tbl.payment_id
    private String memberId;			// 회원ID VARCHAR2 NOT NULL → payment_tbl.member_id (FK)
    private Long accountId;				// 계좌ID NUMBER NULL → payment_tbl.account_id (FK)
    private Long cardId;				// 카드ID NUMBER NULL → payment_tbl.card_id (FK)
    private Long resvId;				// 예약ID NUMBER NOT NULL → payment_tbl.resv_id (FK)
    private Long paymentMoney;			// 결제금액 NUMBER NOT NULL → payment_tbl.payment_money
    private String paymentMethod;		// 결제수단 VARCHAR2 NOT NULL('계좌'/'카드') → payment_tbl.payment_method
    private String paymentStatus;		// 결제상태 VARCHAR2 NOT NULL('완료','예약','취소') → payment_tbl.payment_status
    private LocalDateTime paymentDate;	// 결제일시 DATE DEFAULT SYSDATE → payment_tbl.payment_date
}


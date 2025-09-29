package com.gym.domain.payment;

import lombok.*;				// 롬복 import
import java.time.LocalDateTime;	// Oracle DATE ↔ Java LocalDateTime 매핑

// import com.gym.domain.payment.Payment;

/**
 * paylog_tbl 엔티티(컬럼 1:1 매핑)
 * ─────────────────────────────────────────────────────────────────────────
 * • 용도: 결제 상태/금액/방식 등 변경 이력을 보관하는 로그의 "조회 전용" 엔티티
 * • 매핑: DB 컬럼과 동일한 이름/의미를 갖는 필드로 구성(스네이크→카멜 매핑은 MyBatis 별칭 또는 설정 이용)
 * • 제약: 각 필드 라인 주석에 DB 컬럼/타입/제약을 표기(초보자도 이해 가능하게)
 * • INSERT/UPDATE: 애플리케이션에서 직접 엔티티로 INSERT하지 않음
 *   				→ 로그 기록은 Service→Mapper에서 DTO(PaylogWriteRequest)로 수행
 * • 사용처: Mapper(XML) 결과 매핑 → Service → Controller(조회 API 응답 변환)
 * • 날짜: Oracle DATE는 Java LocalDateTime으로 수신(세션 타임존 고려)
 * ─────────────────────────────────────────────────────────────────────────
 */
@Getter				// 클래스의 모든 필드에 대해 getter 메서드를 자동 생성
@Setter				// 클래스의 모든 필드에 대해 setter 메서드를 자동 생성
@NoArgsConstructor	// 파라미터가 없는 기본 생성자를 자동 생성
@AllArgsConstructor	// 모든 필드를 파라미터로 받는 생성자를 자동 생성
@Builder 			// 빌더 패턴 메서드를 자동 생성 (체이닝으로 객체 생성 가능)
@ToString			// 모든 필드 값을 문자열로 표현하는 toString() 메서드를 자동 생성
public class PaylogResponse {

    // PK: 로그ID (NUMBER, PK, NOT NULL) → paylog_tbl.paylog_id
    private Long paylogId;				// 결제 로그 고유 식별자

    // FK: 결제ID (NUMBER, NOT NULL) → paylog_tbl.payment_id, payment_tbl.payment_id
    private Long paymentId;				// 관련 결제 건 식별자

    // 수행 주체 (VARCHAR2, NULL 허용 여부는 스키마 사양에 따름) → paylog_tbl.member_id
    private String memberId;			// 작업 수행자(회원/관리자 식별용)

    // 사용 계좌 (NUMBER, NULL 허용) → paylog_tbl.account_id
    private Long accountId;				// 계좌 결제 시 사용 계좌ID

    // 사용 카드 (NUMBER, NULL 허용) → paylog_tbl.card_id
    private Long cardId;				// 카드 결제 시 사용 카드ID
    
    // 관련 예약 (NUMBER, NULL 허용) → paylog_tbl.resv_id
    private Long resvId;				// 결제와 연결된 예약ID

    // 금액(원) (NUMBER, NOT NULL) → paylog_tbl.paylog_money
    private Long paylogMoney;			// 로그 시점의 결제금액

    // 방식 (VARCHAR2, 값: '계좌'/'카드'/'현금') → paylog_tbl.paylog_method
    private String paylogMethod;		// 결제 방식(사양 준수: 계좌/카드/현금)

    // 처리자 (VARCHAR2, NULL 허용) → paylog_tbl.paylog_manager
    private String paylogManager;		// 처리 담당자ID(최고관리자/관리자/담당자 등)

    // 상태 이력(요약 문구) (VARCHAR2, NULL 허용) → paylog_tbl.paylog_history
    private String paylogHistory;		// 상태 변화 요약/기록 텍스트

    // 비고 (VARCHAR2, NULL 허용) → paylog_tbl.paylog_memo
    private String paylogMemo;			// 추가 메모/사유

    // 변경 전 상태 (VARCHAR2, NULL 허용) → paylog_tbl.before_status
    private String beforeStatus;		// 상태 변경 직전 값(예: '예약')

    // 변경 후 상태 (VARCHAR2, NULL 허용) → paylog_tbl.after_status
    private String afterStatus;			// 상태 변경 직후 값(예: '완료' 또는 '취소')

    // 로그 시각 (DATE, NOT NULL, DEFAULT SYSDATE) → paylog_tbl.paylog_date
    private LocalDateTime paylogDate;	// 로그 발생 시각
}


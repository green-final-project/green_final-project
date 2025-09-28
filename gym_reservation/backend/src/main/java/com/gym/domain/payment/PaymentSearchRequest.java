package com.gym.domain.payment;

import lombok.*;
//import java.time.LocalDateTime;

/**
 * 결제 검색/목록 요청 DTO
 * - GET /api/payments 쿼리 파라미터 매핑
 * - Oracle 페이징 처리(page/size) 계산에 사용
 */
@Getter				// 클래스의 모든 필드에 대해 getter 메서드를 자동 생성
@Setter				// 클래스의 모든 필드에 대해 setter 메서드를 자동 생성
@NoArgsConstructor	// 파라미터가 없는 기본 생성자를 자동 생성
@AllArgsConstructor	// 모든 필드를 파라미터로 받는 생성자를 자동 생성
@Builder 			// 빌더 패턴 메서드를 자동 생성 (체이닝으로 객체 생성 가능)
@ToString			// 모든 필드 값을 문자열로 표현하는 toString() 메서드를 자동 생성
public class PaymentSearchRequest {
	private Long paymentId;			// 결제ID 필터 → payment_id			
    private String memberId;		// 회원ID 필터 → payment_tbl.member_id
    private Long resvId;			// 예약ID 필터 → payment_tbl.resv_id
    private String method;			// 결제수단 필터('계좌','카드') → payment_tbl.payment_method
    private String paymentStatus;	// 결제상태 VARCHAR2 NOT NULL('완료','예약','취소') → payment_tbl.payment_status
    //private LocalDateTime fromAt;	// 시작일시 → payment_tbl.payment_date >= fromAt
    //private LocalDateTime toAt;		// 종료일시 → payment_tbl.payment_date <= toAt
    //private Integer page;			// 페이지 번호(0부터 시작) → 페이징 연산에 사용
    //private Integer size;			// 페이지 크기 → 페이징 연산에 사용
}
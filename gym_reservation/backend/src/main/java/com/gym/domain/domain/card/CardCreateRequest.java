package com.gym.domain.card;

import lombok.*;

/**
 * 카드 등록 요청 바디
 * - NOT NULL 항목(memberId, cardBank, cardNumber, cardMain)은 필수
 * - cardApproval은 선택
 */

/** 롬복 어노테이션 */
@Getter				// 클래스의 모든 필드에 대해 getter 메서드를 자동 생성
@Setter				// 클래스의 모든 필드에 대해 setter 메서드를 자동 생성
@NoArgsConstructor	// 파라미터가 없는 기본 생성자를 자동 생성
@AllArgsConstructor	// 모든 필드를 파라미터로 받는 생성자를 자동 생성
@Builder 			// 빌더 패턴 메서드를 자동 생성 (체이닝으로 객체 생성 가능)
@ToString			// 모든 필드 값을 문자열로 표현하는 toString() 메서드를 자동 생성
public class CardCreateRequest {
    private String memberId;      // 필수: 소유 회원ID(FK)
    private String cardBank;      // 필수: 카드사
    private String cardNumber;    // 필수: 카드번호(UNIQUE)
    private String cardApproval;  // 선택: 승인정보
    private Boolean cardMain;     // 필수: 대표 여부(true/false) → Y/N
}


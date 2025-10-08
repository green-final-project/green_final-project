package com.gym.domain.card;

import lombok.*;
import java.time.LocalDateTime;

/**
 * 카드 응답 DTO(민감정보 최소화)
 */
@Getter				// 클래스의 모든 필드에 대해 getter 메서드를 자동 생성
@Setter				// 클래스의 모든 필드에 대해 setter 메서드를 자동 생성
@NoArgsConstructor	// 파라미터가 없는 기본 생성자를 자동 생성
@AllArgsConstructor	// 모든 필드를 파라미터로 받는 생성자를 자동 생성
@Builder 			// 빌더 패턴 메서드를 자동 생성 (체이닝으로 객체 생성 가능)
@ToString			// 모든 필드 값을 문자열로 표현하는 toString() 메서드를 자동 생성
public class CardResponse {
    private Long cardId;                 // PK
    private String memberId;             // 소유 회원
    private String cardBank;             // 카드사
    private String cardNumber;           // 카드번호
    private String cardApproval;         // 승인정보
    private boolean cardMain;            // 대표 여부
    private LocalDateTime cardRegDate;   // 등록일
}


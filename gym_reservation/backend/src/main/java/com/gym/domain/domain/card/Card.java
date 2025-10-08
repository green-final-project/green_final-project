package com.gym.domain.card; // 카드 도메인 패키지

import lombok.*;
import java.time.LocalDateTime;

/**
 * card_tbl 엔티티(컬럼 1:1 매핑)
 * - card_main: CHAR('Y'/'N') ↔ boolean (BooleanYNTypeHandler 전제)
 * - card_reg_date: INSERT 시 DB 기본값(SYSDATE)
 */
/** 롬복 어노테이션 */
@Getter				// 클래스의 모든 필드에 대해 getter 메서드를 자동 생성
@Setter				// 클래스의 모든 필드에 대해 setter 메서드를 자동 생성
@NoArgsConstructor	// 파라미터가 없는 기본 생성자를 자동 생성
@AllArgsConstructor	// 모든 필드를 파라미터로 받는 생성자를 자동 생성
@Builder 			// 빌더 패턴 메서드를 자동 생성 (체이닝으로 객체 생성 가능)
@ToString			// 모든 필드 값을 문자열로 표현하는 toString() 메서드를 자동 생성
public class Card {
    private Long cardId;				// 카드PK NUMBER PK			→ card_tbl.card_id
    private String memberId;			// 회원ID VARCHAR2 NOT NULL	→ card_tbl.member_id
    private String cardBank;			// 카드은행 VARCHAR2 NOT NULL	→ card_tbl.card_bank
    private String cardNumber;			// 카드번호 VARCHAR2 NOT NULL	→ card_tbl.card_number (UNIQUE)
    private String cardApproval;		// 승인번호 VARCHAR2(승인정보)		→ card_tbl.card_approval
    private boolean cardMain;			// 대표카드여부	CHAR('Y'/'N')	→ card_tbl.card_main (TypeHandler)
    private LocalDateTime cardRegDate;  // 생성일	DATE				→ card_tbl.card_reg_date
}


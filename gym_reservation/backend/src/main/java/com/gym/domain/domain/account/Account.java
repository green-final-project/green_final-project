package com.gym.domain.account;                         // 📦 계좌 도메인 패키지

import lombok.*;                                        // 🧩 롬복(게터/세터/빌더 등)
import java.time.LocalDateTime;                         // 🕒 오라클 DATE ↔ 자바 시간 타입

/**
 * account_tbl 엔티티 (DB 컬럼과 1:1 매핑)
 * - account_main: CHAR('Y'/'N') ↔ boolean (BooleanYNTypeHandler 전제)
 * - account_reg_date: INSERT 시 DB 기본값(SYSDATE), 조회 시 매핑만 수행
 */
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder @ToString
public class Account {
    private Long accountId;					// 계좌ID(단순번호)			NUMBER PK        → account_tbl.account_id
    private String memberId;				// 회원ID 				VARCHAR2 NOT NULL→ account_tbl.member_id
    private String accountBank;				// 계좌은행				VARCHAR2 NOT NULL→ account_tbl.account_bank
    private String accountNumber;			// 계좌번호				VARCHAR2 NOT NULL→ account_tbl.account_number (UNIQUE)
    private boolean accountMain;			// 메인계좌설정(true/false)	CHAR Y/N NOT NULL→ account_tbl.account_main (TypeHandler)
    private LocalDateTime accountRegDate;	// 계좌등록일				DATE DEFAULT     → account_tbl.account_reg_date
}


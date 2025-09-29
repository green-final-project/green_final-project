package com.gym.domain.account;                         // 📦 계좌 도메인 패키지

import lombok.*;

/**
 * 계좌 등록 요청 바디
 * - NOT NULL 항목은 필수값으로 검증(서비스에서 1차 검증)
 * - accountMain이 null이면 DB 기본값 'N'에 위임(또는 서비스에서 보정 가능)
 * - UNIQUE : 고유값이라서 중복 불가능
 */
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder @ToString
public class AccountCreateRequest {
    private String memberId;		// 필수: 계좌 소유 회원ID (UNIQUE아닌 이유 : 한 회원이 복수의 계좌 소유 가능해서)
    private String accountBank;		// 필수: 은행명
    private String accountNumber;	// 필수: 계좌번호(UNIQUE)
    private Boolean accountMain;	// 선택: true/false (null이면 DB 기본값 사용)
}


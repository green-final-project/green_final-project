package com.gym.domain.account;                         // 📦 계좌 도메인 패키지

import lombok.*;
import java.time.LocalDateTime;

/**
 * API(swagger) 응답 전용(민감정보 없음)
 */
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder @ToString
public class AccountResponse {
    private Long accountId;					// PK
    private String memberId;				// 소유 회원
    private String accountBank;				// 은행명
    private String accountNumber;			// 계좌번호
    private boolean accountMain;			// 대표 여부 (true/false → Y/N)
    private LocalDateTime accountRegDate;	// 등록일
}


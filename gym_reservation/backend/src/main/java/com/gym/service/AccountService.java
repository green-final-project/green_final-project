package com.gym.service;

import com.gym.domain.account.*;                 // 계좌 DTO/도메인
import java.util.List;                           // 목록

/**
 * 계좌 서비스 시그니처 (컨트롤러 ↔ 매퍼 사이)
 * - 매퍼 수정 금지 조건 준수
 */
public interface AccountService {

    Long createAccount(AccountCreateRequest req);                 // 등록: PK 반환

    List<AccountResponse> listAccountsByMember(String memberId);  // 회원별 목록

    void setMainAccount(Long accountId, String memberId);         // 대표계좌 지정

    void deleteAccountById(Long accountId);                       // (CMS) 삭제

    // [250917] 추가 — 본인 소유자 검증 포함 삭제(매퍼 변경 없이 구현)
    void deleteAccountByIdForOwner(Long accountId, String loginMemberId);
}

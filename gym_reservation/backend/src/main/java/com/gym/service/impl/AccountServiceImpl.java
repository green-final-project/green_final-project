package com.gym.service.impl;

import com.gym.domain.account.*;                    // Account, AccountCreateRequest, AccountResponse
import com.gym.mapper.annotation.AccountMapper;     // 계좌 매퍼(어노테이션) — 변경 금지
import com.gym.service.AccountService;              // 서비스 인터페이스
import org.springframework.stereotype.Service;      // @Service
import org.springframework.transaction.annotation.Transactional; // 트랜잭션

import java.util.List;                              // 목록
import java.util.stream.Collectors;                 // 변환

/**
 * 계좌 서비스 구현
 * - 유효성 검증(필수값), 트랜잭션 경계, 엔티티↔응답 DTO 변환
 * - DDL 제약/트리거에 의한 예외는 그대로 전파(로그로 원인 확인)
 */
@Service
public class AccountServiceImpl implements AccountService {

    private final AccountMapper accountMapper;      // 매퍼 주입

    public AccountServiceImpl(AccountMapper accountMapper) {
        this.accountMapper = accountMapper;         // 생성자 주입
    }

    /** 등록(INSERT) — REQUIRED: 실패 시 롤백 */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createAccount(AccountCreateRequest req) {
        if (req.getMemberId() == null || req.getMemberId().isBlank())
            throw new IllegalArgumentException("memberId is required");
        if (req.getAccountBank() == null || req.getAccountBank().isBlank())
            throw new IllegalArgumentException("accountBank is required");
        if (req.getAccountNumber() == null || req.getAccountNumber().isBlank())
            throw new IllegalArgumentException("accountNumber is required");

        if (!accountMapper.existsMemberId(req.getMemberId()))
            throw new IllegalArgumentException("memberId not found");
        if (accountMapper.existsByAccountNumber(req.getAccountNumber()))
            throw new IllegalArgumentException("accountNumber already exists");

        long cnt = accountMapper.countAccountsByMember(req.getMemberId()); // 0 = 첫 등록
        boolean requestMain = Boolean.TRUE.equals(req.getAccountMain());
        boolean insertAsMain = (cnt == 0);                    // 첫 등록은 Y
        boolean promoteAfterInsert = (cnt > 0 && requestMain);// 이후 승격 필요

        Account a = Account.builder()
                .memberId(req.getMemberId())
                .accountBank(req.getAccountBank())
                .accountNumber(req.getAccountNumber())
                .accountMain(insertAsMain)                    // 첫 등록: Y, 아니면 N
                .build();

        int affected = accountMapper.insertAccount(a);
        if (affected != 1) throw new RuntimeException("INSERT failed");

        Long newId = a.getAccountId();

        // 승격: 대상 Y → 나머지 N (트리거 충돌 회피)
        if (promoteAfterInsert) {
            int up1 = accountMapper.setAccountToMain(newId, req.getMemberId());
            if (up1 == 0) throw new RuntimeException("NOT_FOUND: account member mismatch");
            accountMapper.unsetOtherMains(newId, req.getMemberId());
        }

        return newId;
    }

    /** 회원별 목록(SELECT) — readOnly */
    @Override
    @Transactional(readOnly = true)
    public List<AccountResponse> listAccountsByMember(String memberId) {
        List<Account> rows = accountMapper.selectAccountsByMember(memberId);
        return rows.stream().map(this::toResp).collect(Collectors.toList());
    }

    /** 대표계좌 지정(PATCH) — 대상 Y → 나머지 N */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void setMainAccount(Long accountId, String memberId) {
        int up1 = accountMapper.setAccountToMain(accountId, memberId); // 대상 Y
        if (up1 == 0) throw new RuntimeException("NOT_FOUND: account member mismatch");
        accountMapper.unsetOtherMains(accountId, memberId);            // 나머지 N
    }

    /** (CMS) 삭제 — 소유자 검증 없음 (대표계좌는 컨트롤러/서비스 레벨에서 이미 차단) */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteAccountById(Long accountId) {
        int affected = accountMapper.deleteAccountById(accountId);
        if (affected == 0) throw new RuntimeException("NOT_FOUND: account " + accountId);
    }

    // ============================== [old] 중복 정의 블록(비활성 보존) ==============================
    /*
    // ⚠️ [old] — 동일 시그니처 중복으로 컴파일 에러 유발하던 메서드(내용은 아래 신규본으로 통합)
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteAccountByIdForOwner(Long accountId, String loginMemberId) {
        List<AccountResponse> my = accountMapper.selectAccountsByMember(loginMemberId)
                .stream().map(this::toResp).collect(java.util.stream.Collectors.toList());

        AccountResponse target = my.stream()
                .filter(a -> java.util.Objects.equals(a.getAccountId(), accountId))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("ACCESS_DENIED: 본인 소유 계좌만 삭제할 수 있습니다."));

        if (Boolean.TRUE.equals(target.getAccountMain())) {
            throw new RuntimeException("대표계좌는 삭제할 수 없습니다.");
        }

        int affected = accountMapper.deleteAccountById(accountId);
        if (affected == 0) throw new RuntimeException("NOT_FOUND: account " + accountId);
    }
    */
    // ============================================================================================

    /** [신규/유지] 본인 소유만 삭제 — 매퍼 변경 없이 소유 + 대표 여부 검증 */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteAccountByIdForOwner(Long accountId, String loginMemberId) {
        // 1) 내 계좌 목록 조회(기존 매퍼 재활용)
        List<AccountResponse> my = accountMapper.selectAccountsByMember(loginMemberId)
                .stream().map(this::toResp).collect(java.util.stream.Collectors.toList());

        // 2) 소유 검증
        AccountResponse target = my.stream()
                .filter(a -> java.util.Objects.equals(a.getAccountId(), accountId))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("ACCESS_DENIED: 본인 소유 계좌만 삭제할 수 있습니다."));

        // 3) 대표 여부 차단
        if (target.isAccountMain()) {
            throw new RuntimeException("대표계좌는 삭제할 수 없습니다.");
        }

        // 4) 삭제 실행
        int affected = accountMapper.deleteAccountById(accountId);
        if (affected == 0) throw new RuntimeException("NOT_FOUND: account " + accountId);
    }

    /** 엔티티→응답 DTO 변환 */
    private AccountResponse toResp(Account a) {
        return AccountResponse.builder()
                .accountId(a.getAccountId())
                .memberId(a.getMemberId())
                .accountBank(a.getAccountBank())
                .accountNumber(a.getAccountNumber())
                .accountMain(a.isAccountMain())
                .accountRegDate(a.getAccountRegDate())
                .build();
    }
}

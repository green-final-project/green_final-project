package com.gym.service.impl;

import com.gym.domain.card.*;                 // Card, CardCreateRequest, CardResponse
import com.gym.mapper.annotation.CardMapper;  // 카드 매퍼(어노테이션)
import com.gym.service.CardService;           // 서비스 인터페이스
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 카드 서비스 구현
 * - 계좌(AccountServiceImpl)와 동일한 패턴으로 작성
 * - 등록 시 규칙
 *   1) 회원의 첫 카드면 무조건 대표('Y')
 *   2) 첫 카드가 아니고, 요청이 cardMain=true 이면
 *      → INSERT 성공 후 해당 카드만 'Y'로 승격, 같은 회원의 나머지는 'N'
 * - DB에는 "대표 최소 1개 유지" 트리거/인덱스가 있을 수 있으므로
 *   대표 변경은 반드시 "대상 Y → 나머지 N" 순서로 처리하여 충돌을 피함
 *
 * [250917] 권한 분리 메모
 * - UserCardController : 로그인 본인 계정 카드만 등록/조회/수정/삭제
 * - CmsCardController  : 관리자/최고관리자 권한으로 모든 회원 카드 접근 가능
 * - 서비스 계층은 비즈니스 로직만 유지, 권한 검증은 Controller 단에서 수행
 */
@Service
public class CardServiceImpl implements CardService {

    private final CardMapper cardMapper;

    public CardServiceImpl(CardMapper cardMapper) {
        this.cardMapper = cardMapper; // 생성자 주입
    }

    /** 1) 카드 등록 */
    // [old]
    /*
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createCard(CardCreateRequest req) {
        // ... (기존 유효성 검증 및 등록 로직)
    }
    */
    // [250917] 권한 체크는 Controller에서 수행, 서비스는 비즈니스 로직만 유지
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createCard(CardCreateRequest req) {
        if (req.getMemberId() == null || req.getMemberId().isBlank())
            throw new IllegalArgumentException("memberId is required");
        if (req.getCardBank() == null || req.getCardBank().isBlank())
            throw new IllegalArgumentException("cardBank is required");
        if (req.getCardNumber() == null || req.getCardNumber().isBlank())
            throw new IllegalArgumentException("cardNumber is required");

        if (!cardMapper.existsMemberId(req.getMemberId()))
            throw new IllegalArgumentException("memberId not found");
        if (cardMapper.existsByCardNumber(req.getCardNumber()))
            throw new IllegalArgumentException("cardNumber already exists");

        Card c = Card.builder()
                .memberId(req.getMemberId())
                .cardBank(req.getCardBank())
                .cardNumber(req.getCardNumber())
                .cardApproval(req.getCardApproval())
                .cardMain(false) // 항상 N으로 INSERT
                .build();

        int affected = cardMapper.insertCard(c);
        if (affected != 1) throw new RuntimeException("INSERT failed");
        Long newId = c.getCardId();

        long postCnt   = cardMapper.countCardsByMember(req.getMemberId());
        boolean wantMain = Boolean.TRUE.equals(req.getCardMain());

        if (postCnt == 1 || wantMain) {
            int upd = cardMapper.updateMainCard(newId, req.getMemberId());
            if (upd == 0) throw new RuntimeException("NOT_FOUND: card member mismatch");
        }

        return newId;
    }

    /** 2) 회원별 카드 목록 */
    // [old]
    /*
    @Override
    @Transactional(readOnly = true)
    public List<CardResponse> listCardsByMember(String memberId) {
        return cardMapper.selectCardsByMember(memberId)
                .stream().map(this::toResp)
                .collect(Collectors.toList());
    }
    */
    // [250917] 서비스 로직 동일, 본인 여부 검증은 UserCardController에서 처리
    @Override
    @Transactional(readOnly = true)
    public List<CardResponse> listCardsByMember(String memberId) {
        return cardMapper.selectCardsByMember(memberId)
                .stream().map(this::toResp)
                .collect(Collectors.toList());
    }

    /** 3) 대표카드 설정 */
    // [old]
    /*
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void setMainCard(Long cardId, String memberId) {
        int up1 = cardMapper.setCardToMain(cardId, memberId);
        if (up1 == 0) throw new RuntimeException("NOT_FOUND: card member mismatch");
        cardMapper.unsetOtherMains(cardId, memberId);
    }
    */
    // [250917] 본인 카드 검증은 Controller에서 수행
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void setMainCard(Long cardId, String memberId) {
        int upd = cardMapper.updateMainCard(cardId, memberId);
        if (upd == 0) {
            throw new RuntimeException("NOT_FOUND: card member mismatch");
        }
    }

    /** 4) 카드 삭제 */
    // [old]
    /*
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteCardById(Long cardId) {
        int affected = cardMapper.deleteCardById(cardId);
        if (affected == 0) throw new RuntimeException("NOT_FOUND: card " + cardId);
    }
    */
    // [250917] CmsCardController에서는 관리자 권한으로 호출 가능
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteCardById(Long cardId) {
        int affected = cardMapper.deleteCardById(cardId);
        if (affected == 0) throw new RuntimeException("NOT_FOUND: card " + cardId);
    }

    // [250917] 추가 — 본인 소유 카드만 삭제(매퍼 추가 없이 소유 검증)
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteCardByIdForOwner(Long cardId, String loginMemberId) {
        // 1) 본인 카드 목록 조회(기존 매퍼 재활용)
        List<CardResponse> myCards = cardMapper.selectCardsByMember(loginMemberId)
                .stream().map(this::toResp).collect(Collectors.toList());

        // 2) 소유 검증: 내 카드 목록에 해당 cardId가 있는지 확인
        boolean owns = myCards.stream().anyMatch(c -> java.util.Objects.equals(c.getCardId(), cardId));
        if (!owns) throw new RuntimeException("ACCESS_DENIED: 본인 소유 카드만 삭제할 수 있습니다.");

        // 3) 삭제 실행(기존 삭제 메서드 재사용)
        int affected = cardMapper.deleteCardById(cardId);
        if (affected == 0) throw new RuntimeException("NOT_FOUND: card " + cardId);
    }

    /** 엔티티 → DTO 변환 */
    private CardResponse toResp(Card c) {
        return CardResponse.builder()
                .cardId(c.getCardId())
                .memberId(c.getMemberId())
                .cardBank(c.getCardBank())
                .cardNumber(c.getCardNumber())
                .cardApproval(c.getCardApproval())
                .cardMain(c.isCardMain())
                .cardRegDate(c.getCardRegDate())
                .build();
    }
}

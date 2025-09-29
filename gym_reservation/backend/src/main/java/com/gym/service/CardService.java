package com.gym.service;

import com.gym.domain.card.*;
import java.util.List;

/**
 * 카드 서비스 시그니처
 */
public interface CardService {
    Long createCard(CardCreateRequest req);                    // 등록: PK 반환
    List<CardResponse> listCardsByMember(String memberId);     // 회원별 목록
    void setMainCard(Long cardId, String memberId);            // 대표카드 설정
    void deleteCardById(Long cardId);                          // 삭제
    void deleteCardByIdForOwner(Long cardId, String loginMemberId);  // [250917] 본인 소유자 검증 후 삭제(컨트롤러에서 memberId 없이 호출 가능)
}


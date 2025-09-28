package com.gym.mapper.annotation; // 매퍼 패키지

import org.apache.ibatis.annotations.*;
import com.gym.domain.card.Card;
import com.gym.config.type.BooleanYNTypeHandler;
import java.util.List;

/**
 * CardMapper: 등록/회원별목록/대표설정/삭제
 * - UNIQUE(card_number), CHECK(card_main IN('Y','N')) 준수
 * - 삭제/대표변경 시 트리거/인덱스 규칙 충돌 가능성은 DB에서 제어(예외 그대로 전파)
 */
@Mapper
public interface CardMapper {

    // 회원 존재(FK) 사전검증: INSERT 전 시퀀스 소비 방지
    @Select("""
        SELECT CASE WHEN COUNT(1) > 0 THEN 1 ELSE 0 END
          FROM member_tbl
         WHERE member_id = #{memberId}
    """)
    boolean existsMemberId(@Param("memberId") String memberId);

    // 카드번호 중복 여부(UNIQUE 선제 체크)
    @Select("""
        SELECT CASE WHEN COUNT(1) > 0 THEN 1 ELSE 0 END
          FROM card_tbl
         WHERE card_number = #{cardNumber}
    """)
    boolean existsByCardNumber(@Param("cardNumber") String cardNumber);

    // 회원 보유 카드 건수(첫 등록 판단용)
    @Select("""
        SELECT COUNT(1)
          FROM card_tbl
         WHERE member_id = #{memberId}
    """)
    long countCardsByMember(@Param("memberId") String memberId);

    // 등록(INSERT)
    // [임시추가] DB 시퀀스명: seq_card_id (계좌와 동일 패턴). 시퀀스명이 다른 경우 아래 2곳만 맞추면 됨.
    @Insert("""
        INSERT INTO card_tbl (
            card_id,
            member_id,
            card_bank,
            card_number,
            card_approval,
            card_main
        ) VALUES (
            seq_card_id.NEXTVAL,
            #{memberId},
            #{cardBank},
            #{cardNumber},
            #{cardApproval},
            #{cardMain, typeHandler=com.gym.config.type.BooleanYNTypeHandler}
        )
    """)
    @SelectKey(statement = "SELECT seq_card_id.CURRVAL FROM dual",
               keyProperty = "cardId", before = false, resultType = Long.class)
    int insertCard(Card c); // 실행 후 c.getCardId() ← 방금 증가한 PK

    // 회원별 목록(SELECT)
    @Select("""
        SELECT
            c.card_id        AS cardId,
            c.member_id      AS memberId,
            c.card_bank      AS cardBank,
            c.card_number    AS cardNumber,
            c.card_approval  AS cardApproval,
            c.card_main      AS cardMain,
            c.card_reg_date  AS cardRegDate
        FROM card_tbl c
        WHERE c.member_id = #{memberId}
        ORDER BY c.card_id
    """)
    @Results(id="CardMap", value = {
        @Result(column="card_main", property="cardMain",
                typeHandler=BooleanYNTypeHandler.class)
    })
    List<Card> selectCardsByMember(@Param("memberId") String memberId);

    // 대표카드 대상만 'Y'
    @Update("""
        UPDATE card_tbl
           SET card_main = 'Y'
         WHERE card_id = #{cardId}
           AND member_id = #{memberId}
    """)
    int setCardToMain(@Param("cardId") Long cardId,
                      @Param("memberId") String memberId);

    // 같은 회원의 나머지 카드는 모두 'N'
    @Update("""
        UPDATE card_tbl
           SET card_main = 'N'
         WHERE member_id = #{memberId}
           AND card_id <> #{cardId}
    """)
    int unsetOtherMains(@Param("cardId") Long cardId,
                        @Param("memberId") String memberId);

    // 대표카드 단일 업데이트(문장 내 CASE) — 환경에 따라 트리거 충돌 가능
    @Update("""
        UPDATE card_tbl
           SET card_main = CASE WHEN card_id = #{cardId} THEN 'Y' ELSE 'N' END
         WHERE member_id = #{memberId}
    """)
    int updateMainCard(@Param("cardId") Long cardId,
                       @Param("memberId") String memberId);

    // 삭제(DELETE) — PK 기준
    @Delete("DELETE FROM card_tbl WHERE card_id = #{cardId}")
    int deleteCardById(@Param("cardId") Long cardId);
    
    // [250927] 회원ID 삭제처리하면 카드정보 함깨 삭제
    // 1) 회원정보 삭제 시, 카드정보 삭제처리
    /*
    @Delete("DELETE FROM card_tbl WHERE member_id = #{memberId}")
    int deleteByMemberId(String memberId);
    // 2) 회원 카드 대표 해제 (card_main을 'N'으로 변경)
    @Update("UPDATE card_tbl SET card_main = 'N' WHERE member_id = #{memberId}")
    int unsetMainCardsByMember(@Param("memberId") String memberId);
    */
}


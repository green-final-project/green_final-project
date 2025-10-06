package com.gym.mapper.annotation;

import org.apache.ibatis.annotations.*; // MyBatis 애노테이션
import com.gym.domain.account.Account;	// 도메인 엔티티
import com.gym.config.type.BooleanYNTypeHandler; // Y/N ↔ boolean 변환 핸들러
import java.util.List; // 목록 반환

/**
 * AccountMapper: 단건 등록/목록/대표계좌 지정/삭제
 * - DDL 제약 준수: UNIQUE(account_number), CHECK(account_main IN('Y','N'))
 * - 트리거 주의: 대표계좌 최소 1개 유지 트리거가 있어 DML 시 차단 가능(아래 메서드 주석 참고)
 */
@Mapper
public interface AccountMapper {

	// ⚠ 시나리오 파일에서 DB컬럼이랑 변수 대조시키면서 진행
	
	/* INSERT INTO 테이블명 (컬럼값 목록)
	 * VALUES (#{변수} 혹은 디코드 같은 조건값 변경)
	 * */  
	
    /* 1) 계좌 등록(INSERT)   
     * - seq_account_id.NEXTVAL, : 등록하면 자동으로 PK시퀀스(회원ID) 상승
     * - account_main은 TypeHandler로 'Y'/'N' 변환 
     * */
    @Insert("""
        INSERT INTO account_tbl (
            account_id,
            member_id,
            account_bank,
            account_number,
            account_main
        ) VALUES (
            seq_account_id.NEXTVAL,
            #{memberId},
            #{accountBank},
            #{accountNumber},
            #{accountMain, typeHandler=com.gym.config.type.BooleanYNTypeHandler}
        )
    """)
    // 등록(INSERT) 실행 후 → 자동으로 PK값 증가
    @SelectKey(statement = "SELECT seq_account_id.CURRVAL FROM dual",
               keyProperty = "accountId", before = false, resultType = Long.class)
    int insertAccount(Account a);                        // 반환: 영향행수(1 기대)

    // 중복값 검증 
    @Select("""
            SELECT CASE WHEN COUNT(1) > 0 THEN 1 ELSE 0 END   -- ▶ 1(중복) / 0(미중복)
              FROM account_tbl                                 -- ▶ 계좌 테이블
             WHERE account_number = #{accountNumber}           -- ▶ 조회 대상 계좌번호
        """)
        boolean existsByAccountNumber(@Param("accountNumber") String accountNumber);   
    
    
    // 회원 계좌 건수(첫 등록 여부 판단용) → 첫 계좌일 경우 자동으로 메인계좌 여부 'Y'로 처리하기 위해서
    // [추가] 회원 계좌 개수(첫 등록 판단)
    @Select("""
        SELECT COUNT(1)
          FROM account_tbl
         WHERE member_id = #{memberId}
    """)
    long countAccountsByMember(@Param("memberId") String memberId);

    // [추가] 대상만 대표 'Y' (트리거 충돌 없음)
    @Update("""
        UPDATE account_tbl
           SET account_main = 'Y'
         WHERE account_id = #{accountId}
           AND member_id   = #{memberId}
    """)
    int setAccountToMain(@Param("accountId") Long accountId,
                         @Param("memberId") String memberId);

    // [추가] 같은 회원의 "다른 모든 계좌"를 'N'
    @Update("""
        UPDATE account_tbl
           SET account_main = 'N'
         WHERE member_id = #{memberId}
           AND account_id <> #{accountId}
    """)
    int unsetOtherMains(@Param("accountId") Long accountId,
                        @Param("memberId") String memberId);

    
    // [추가] 회원 존재(FK) 검증 — INSERT 전에 FK 에러를 선제 차단(시퀀스 미소비)
    @Select("""
        SELECT CASE WHEN COUNT(1) > 0 THEN 1 ELSE 0 END   -- ▶ 1(존재) / 0(미존재)
          FROM member_tbl                                 -- ▶ 회원 테이블
         WHERE member_id = #{memberId}                    -- ▶ 조회 대상 회원ID
    """)
    boolean existsMemberId(@Param("memberId") String memberId);  // //[추가]
    
    
    
    /* 2) 회원별 목록(SELECT) 
     * — 최신 등록 순서 또는 PK 오름차 기준 선택 가능(아래는 PK) */
    @Select("""
        SELECT
            a.account_id       AS accountId,
            a.member_id        AS memberId,
            a.account_bank     AS accountBank,
            a.account_number   AS accountNumber,
            a.account_main     AS accountMain,
            a.account_reg_date AS accountRegDate
        FROM account_tbl a
        WHERE a.member_id = #{memberId}
        ORDER BY CASE WHEN a.account_main = 'Y' THEN 0 ELSE 1 END, a.account_id
    """)
    @Results(id="AccountMap", value = {
        @Result(column="account_main", property="accountMain",
                typeHandler=BooleanYNTypeHandler.class) // 'Y'/'N' → boolean
    })
    List<Account> selectAccountsByMember(@Param("memberId") String memberId);

    /* 3) 대표계좌 설정(PATCH) — 단일 UPDATE로 대상 'Y', 나머지 'N' 처리
     *   ⚠ 중요: 현재 DDL에는 "대표 최소 1개 유지" 트리거가 존재
     *           아래 단일 UPDATE(SET CASE WHEN ...)는 문장 단위로 '하나만 Y'가 되도록 보장
     *           하지만 트리거 구현 방식에 따라 차단(-20032 등)이 발생할 수 있음(테스트 환경에서 정상 재현됨).
     */
    @Update("""
        UPDATE account_tbl
           SET account_main =
               CASE WHEN account_id = #{accountId} THEN 'Y' ELSE 'N' END
         WHERE member_id = #{memberId}
    """)
    int updateMainAccount(@Param("accountId") Long accountId,
                          @Param("memberId") String memberId);

    /* 4) 삭제(DELETE) — PK 기준
     *   ⚠ 주의: 대표계좌 최소 1개 유지 트리거에 의해 '대표 Y' 단독행 삭제가 차단될 수 있음(-20031).
     */
    @Delete("DELETE FROM account_tbl WHERE account_id = #{accountId}")
    int deleteAccountById(@Param("accountId") Long accountId);
    
    /*
    // [250927] 회원ID 삭제처리하면 계좌정보 함깨 삭제
    // 1) 회원정보 삭제 시, 계좌정보 삭제처리
    @Delete("DELETE FROM account_tbl WHERE member_id = #{memberId}")
    int deleteByMemberId(String memberId);
    // 2) 회원 계좌 대표 해제 (account_main을 'N'으로 변경)
    @Update("UPDATE account_tbl SET account_main = 'N' WHERE member_id = #{memberId}")
    int unsetMainAccountsByMember(@Param("memberId") String memberId);\
    */
    
}


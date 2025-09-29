package com.gym.mapper.annotation; // 📦 어노테이션 매퍼

import org.apache.ibatis.annotations.*;
import com.gym.domain.member.Member;

/**
 * MemberMapper (정적 CRUD)
 * - SELECT/INSERT/UPDATE/DELETE
 * - 복잡한 검색/페이징은 XML로 분리 가능
 */
@Mapper
public interface MemberMapper {
    // DB컬럼 및 변수와 대조할 것

    /** 단건 조회 */
	@Select("""
		    SELECT
		      member_id        AS memberId,
		      member_pw        AS memberPw,
		      member_name      AS memberName,
		      member_gender    AS memberGender,
		      member_email     AS memberEmail,
		      member_mobile    AS memberMobile,
		      member_phone     AS memberPhone,
		      zip,
		      road_address     AS roadAddress,
		      jibun_address    AS jibunAddress,
		      detail_address   AS detailAddress,
		      member_birthday  AS memberBirthday,
		      member_manipay   AS memberManipay,
		      member_joindate  AS memberJoindate,
		      member_role      AS memberRole,
		      admin_type       AS adminType
		    FROM member_tbl
		    WHERE member_id = #{memberId}
		""")
		Member selectMemberById(@Param("memberId") String memberId);


    /** 등록(INSERT) — DEFAULT 컬럼은 Service에서 값 보정
     *  - DDL: member_joindate DATE DEFAULT SYSDATE NOT NULL
     *  - 따라서 INSERT 목록에 member_joindate 를 넣지 않는다(기본값 사용)
     */
	
	/* SWAGGER 등록 예시
	{
	  "memberId": "hong99",
	  "memberPw": "asd123!@#",
	  "memberName": "홍길동",
	  "memberGender": "f",
	  "memberEmail": "hong99@example.com",
	  "memberMobile": "010-1111-2222",
	  "memberPhone": "031-4131-9876",
	  "zip": "08395",
	  "roadAddress": "서울특별시 구로구 새말로9길 45",
	  "jibunAddress": "서울특별시 구로구 구로동 123-45",
	  "detailAddress": "101동 100호",
	  "memberBirthday": "1995-01-01",
	  "memberManipay": "account",
	  "memberRole": "user",
	  "adminType": "관리자"
	}

	 * */
	// [250926] #{zip} → #{zip,jdbcType=CHAR}으로 변경함
    @Insert("""
        INSERT INTO member_tbl (
          member_id,
          member_pw,
          member_name,
          member_gender,
          member_email,
          member_mobile,
          member_phone,
          zip,
          road_address,
          jibun_address,
          detail_address,
          member_birthday,
          member_manipay,
          member_role,
          admin_type
        ) VALUES (
          #{memberId},
          #{memberPw},
          #{memberName},
          #{memberGender},
          #{memberEmail},
          #{memberMobile},
          #{memberPhone},
          #{zip,jdbcType=CHAR}, 
          #{roadAddress},
          #{jibunAddress},
          #{detailAddress},
          #{memberBirthday},
          #{memberManipay},
          #{memberRole},
          #{adminType,jdbcType=VARCHAR}
        )
    """)
    int insert(Member m); // ➕ 1 기대

    /** 수정 — 아이디와 이름 수정 시, 에러 발생*/
    @Update("""
        UPDATE member_tbl
           SET member_pw       = #{memberPw},
               member_gender   = #{memberGender},
               member_email    = #{memberEmail},
               member_mobile   = #{memberMobile},
               member_phone    = #{memberPhone},
               zip             = #{zip,jdbcType=CHAR},
               road_address    = #{roadAddress},
               jibun_address   = #{jibunAddress},
               detail_address  = #{detailAddress},
               member_birthday = #{memberBirthday},
               member_manipay  = #{memberManipay},
               member_role     = #{memberRole},
               admin_type      = #{adminType,jdbcType=VARCHAR}
         WHERE member_id       = #{memberId}
    """)
    int update(Member m); // ✏️

    /** 삭제 */
    @Delete("DELETE FROM member_tbl WHERE member_id = #{memberId}")
    int delete(@Param("memberId") String memberId);

    /** ID 존재 여부(INSERT 전 중복 체크) */
    @Select("""
        SELECT CASE WHEN COUNT(1) > 0 THEN 1 ELSE 0 END
          FROM member_tbl
         WHERE member_id = #{memberId}
    """)
    boolean existsMemberById(@Param("memberId") String memberId);
}


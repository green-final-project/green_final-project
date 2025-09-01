package com.gym.mapper.annotation;                  // 📦 어노테이션 매퍼 패키지

import org.apache.ibatis.annotations.Delete;        // 🧩 @Delete 어노테이션
import org.apache.ibatis.annotations.Insert;        // 🧩 @Insert  어노테이션
import org.apache.ibatis.annotations.Mapper;        // 🧩 @Mapper  스캔 대상
import org.apache.ibatis.annotations.Param;         // 🧩 @Param   바인딩
import org.apache.ibatis.annotations.Select;        // 🧩 @Select  어노테이션
import org.apache.ibatis.annotations.Update;        // 🧩 @Update  어노테이션

import com.gym.domain.member.Member;                // 👥 도메인 DTO

/**
 * 회원 단건 중심 CRUD 매퍼(어노테이션 기반)
 * - SQL은 정적이고 단순한 항목만 여기에 둠(로그인/회원가입/단건 수정/삭제 등)
 * - 복잡한 검색/페이징/통계는 XML 매퍼로 분리(요청 시 별도 작성)
 */
@Mapper
public interface MemberMapper {

    /** 회원 단건 조회: PK로 조회 (DDL 컬럼명 정확히 반영) */
    @Select("""
        SELECT
          member_id,
          member_pw,
          member_name,
          member_gender,
          member_email,
          member_mobile,
          member_phone,
          zip, road_address, jibun_address, detail_address,
          member_birthday,
          member_manipay,
          member_joindate,
          member_role,
          admin_type
        FROM member_tbl
        WHERE member_id = #{memberId}
    """)
    //Member findById(@Param("memberId") String memberId);   // 🔁 단건 상세 반환
    Member selectMemberById(@Param("memberId") String memberId); // 🔁 단건 상세 반환
    
    /** 회원 등록: DB DEFAULT(가입일 등)는 DB가 채우도록 함 */
    @Insert("""
        INSERT INTO member_tbl (
          member_id,
          member_pw,
          member_name,
          member_gender,
          member_email,
          member_mobile,
          member_phone,
          zip, road_address, jibun_address, detail_address,
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
          #{zip}, #{roadAddress}, #{jibunAddress}, #{detailAddress},
          #{memberBirthday},
          #{memberManipay},
          #{memberRole},
          #{adminType}
        )
    """)
    int insert(Member m);                                 // 🔁 반영 행 수(성공 시 1)

    /** 회원 수정: PK 고정, 나머지 필드 갱신 */
    @Update("""
        UPDATE member_tbl
           SET member_pw       = #{memberPw},
               member_name     = #{memberName},
               member_gender   = #{memberGender},
               member_email    = #{memberEmail},
               member_mobile   = #{memberMobile},
               member_phone    = #{memberPhone},
               zip             = #{zip},
               road_address    = #{roadAddress},
               jibun_address   = #{jibunAddress},
               detail_address  = #{detailAddress},
               member_birthday = #{memberBirthday},
               member_manipay  = #{memberManipay},
               member_role     = #{memberRole},
               admin_type      = #{adminType}
         WHERE member_id       = #{memberId}
    """)
    int update(Member m);                                 // 🔁 반영 행 수

    /** 회원 삭제: PK 기준 삭제 */
    @Delete("DELETE FROM member_tbl WHERE member_id = #{memberId}")
    int delete(@Param("memberId") String memberId);       // 🔁 반영 행 수
}

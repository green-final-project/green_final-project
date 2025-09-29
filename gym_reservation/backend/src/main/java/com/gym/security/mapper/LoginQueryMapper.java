package com.gym.security.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import java.util.List;

/** 로그인 전용 조회(회원 기본 + 역할) */
@Mapper
public interface LoginQueryMapper {
    LoginRow findUser(@Param("memberId") String memberId); // 아이디/해시 조회
    List<String> findRoles(@Param("memberId") String memberId); // 원본 역할값(user/admin/한글)

    class LoginRow {
        private String memberId;     // member_tbl.member_id
        private String passwordHash; // member_tbl.member_pw
        public String getMemberId(){ return memberId; }
        public void setMemberId(String v){ this.memberId=v; }
        public String getPasswordHash(){ return passwordHash; }
        public void setPasswordHash(String v){ this.passwordHash=v; }
    }
}


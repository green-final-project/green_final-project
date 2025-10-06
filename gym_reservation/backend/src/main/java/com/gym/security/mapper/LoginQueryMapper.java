package com.gym.security.mapper;

import com.gym.domain.member.Member;
import org.apache.ibatis.annotations.Mapper;
import java.util.List;

@Mapper
public interface LoginQueryMapper {

    // [1] 회원 로그인 시 아이디/비밀번호 조회
    LoginRow findUser(String memberId);

    // [2] 권한 조회
    List<String> findRoles(String memberId);

    // ⚠️ [3] 전체 회원 정보 조회 (MyBatis XML과 매핑) [250930]추가
    Member selectMemberById(String memberId);

    // [내부 static 클래스: findUser 리턴타입]
    class LoginRow {
        private String memberId;
        private String passwordHash;

        public String getMemberId() { return memberId; }
        public void setMemberId(String memberId) { this.memberId = memberId; }

        public String getPasswordHash() { return passwordHash; }
        public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }
    }
}

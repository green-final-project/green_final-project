package com.gym.service;                                 // 📦 서비스 인터페이스

import com.gym.domain.member.Member;

import java.util.List;

/**
 * 회원 서비스 인터페이스
 * - Controller ↔ Mapper 사이 비즈니스 계층
 */
public interface MemberService {

    Member getMemberById(String memberId);         // 🔎 단건 조회(readOnly)

    int createMember(Member req);                  // ➕ 등록(REQUIRED)

    int updateMember(String memberId, Member req); // ✏️ 수정(REQUIRED)

    int deleteMember(String memberId);             // 🗑 삭제(REQUIRED)

    List<Member> listMembers(Integer page, Integer size, String keyword, String role); // 📃 목록

    long countMembers(String keyword, String role); // 🔢 총 개수
    
    // [251007] 회원ID 존재 여부 확인용 (중복검사용)
    boolean existsById(String memberId);
}


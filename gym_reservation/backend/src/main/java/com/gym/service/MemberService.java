package com.gym.service;                         // 📦 서비스 인터페이스 패키지(팀 공통 위치 유지)

import com.gym.domain.member.Member;            // 👥 회원 DTO 임포트

/**
 * 회원 서비스 인터페이스
 * - 목적: 컨트롤러와 매퍼 사이의 비즈니스 계층(트랜잭션/검증/예외 변환)
 * - 규칙: 메서드명/파라미터/반환값은 팀 표준대로 간결하게 유지
 */
public interface MemberService {

    /**
     * 회원 단건 조회
     * @param memberId 조회할 회원ID(PK)
     * @return Member(없으면 예외)
     */
    Member getMemberById(String memberId);      // 🔎 단건 조회

    /**
     * 회원 등록
     * @param member 신규 회원 정보(암호는 서비스 상층에서 해시 후 전달 가정)
     * @return 반영 행 수(성공 시 1)
     */
    int createMember(Member member);            // ➕ 등록

    /**
     * 회원 수정
     * @param member 수정할 회원 정보(PK + 변경필드)
     * @return 반영 행 수(성공 시 1)
     */
    int updateMember(Member member);            // ✏️ 수정

    /**
     * 회원 삭제
     * @param memberId 삭제할 회원ID(PK)
     * @return 반영 행 수(성공 시 1)
     */
    int deleteMember(String memberId);          // 🗑 삭제
}

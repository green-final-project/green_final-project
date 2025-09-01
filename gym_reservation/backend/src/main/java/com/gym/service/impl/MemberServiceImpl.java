package com.gym.service.impl;                   // 📦 서비스 구현 패키지(팀 공통 위치 유지)

import com.gym.domain.member.Member;            // 👥 회원 DTO
import com.gym.mapper.annotation.MemberMapper;  // 🗺 매퍼(어노테이션 기반 CRUD)
import com.gym.service.MemberService;           // 🧠 서비스 인터페이스
import org.springframework.stereotype.Service;  // 🏷 @Service 컴포넌트 스캔 대상
import org.springframework.transaction.annotation.Transactional; // 🔐 트랜잭션

/**
 * 회원 서비스 구현
 * - 매퍼 호출 + 트랜잭션 + 예외 변환(전역 예외핸들러가 표준 응답으로 변환)
 * - 민감정보(비밀번호)는 이 계층 들어오기 전 해시 처리 가정(규칙: 암호 평문 로그 금지)
 */
@Service
public class MemberServiceImpl implements MemberService {

    private final MemberMapper memberMapper;    // 💉 매퍼 주입(생성자 방식 권장)

    public MemberServiceImpl(MemberMapper memberMapper) { // 🔧 스프링이 자동 주입
        this.memberMapper = memberMapper;       // 💉 필드 할당
    }

    @Override
    @Transactional(readOnly = true)             // 🔒 조회 전용 트랜잭션(성능/일관성)
    public Member getMemberById(String memberId) {
        // 1) 매퍼로 DB 단건 조회
        Member found = memberMapper.selectMemberById(memberId); // 🔎 DB 조회
        // 2) 없으면 예외(전역 핸들러에서 404 변환 가정: "NOT_FOUND" 포함)
        if (found == null) {
            throw new RuntimeException("NOT_FOUND: member " + memberId); // ❗미존재
        }
        // 3) 조회 성공 시 반환
        return found;                             // ✅ 정상 반환
    }

    @Override
    @Transactional(rollbackFor = Exception.class) // 🧾 쓰기 트랜잭션(예외 시 롤백)
    public int createMember(Member member) {
        // 1) 입력값으로 INSERT 실행(DB DEFAULT는 DB가 채움: joinDate 등)
        int affected = memberMapper.insert(member); // ➕ INSERT 실행
        // 2) 반영 행 수 검증(0이면 비정상 → 서버 오류로 보고 예외)
        if (affected != 1) {
            throw new RuntimeException("INTERNAL_ERROR: insert failed (affected=" + affected + ")"); // ❗실패
        }
        return affected;                           // ✅ 1 반환
    }

    @Override
    @Transactional(rollbackFor = Exception.class) // 🧾 쓰기 트랜잭션
    public int updateMember(Member member) {
        // 1) UPDATE 실행
        int affected = memberMapper.update(member); // ✏️ UPDATE
        // 2) 대상 없음(=0) → 404로 변환되도록 예외
        if (affected == 0) {
            throw new RuntimeException("NOT_FOUND: member " + member.getMemberId()); // ❗대상 없음
        }
        return affected;                           // ✅ 1 반환
    }

    @Override
    @Transactional(rollbackFor = Exception.class) // 🧾 쓰기 트랜잭션
    public int deleteMember(String memberId) {
        // 1) DELETE 실행
        int affected = memberMapper.delete(memberId); // 🗑 DELETE
        // 2) 대상 없음(=0) → 404로 변환되도록 예외
        if (affected == 0) {
            throw new RuntimeException("NOT_FOUND: member " + memberId); // ❗대상 없음
        }
        return affected;                           // ✅ 1 반환
    }
}

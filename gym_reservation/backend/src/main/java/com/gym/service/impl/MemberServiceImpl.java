package com.gym.service.impl;                                      // 📦 서비스 구현

import com.gym.domain.member.Member;
import com.gym.mapper.annotation.MemberMapper;
import com.gym.mapper.xml.MemberQueryMapper;
import com.gym.service.MemberService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

/**
 * 회원 서비스 구현
 * - 규칙: 트랜잭션/검증/예외 변환(DDL 준수)
 * - DDL: member_manipay DEFAULT 'account', member_role DEFAULT 'user', member_joindate DEFAULT SYSDATE
 */
@Service
public class MemberServiceImpl implements MemberService {

    private final MemberMapper memberMapper;
    private final MemberQueryMapper memberQueryMapper;

    public MemberServiceImpl(MemberMapper memberMapper, MemberQueryMapper memberQueryMapper) {
        this.memberMapper = memberMapper;
        this.memberQueryMapper = memberQueryMapper;
    }

    // 🔎 단건 조회
    @Override
    @Transactional(readOnly = true)
    public Member getMemberById(String memberId) {
        Member found = memberMapper.selectMemberById(memberId);
        if (found == null) throw new RuntimeException("NOT_FOUND: member " + memberId);
        return found;
    }

    // ➕ 등록
    @Override
    @Transactional(rollbackFor = Exception.class)
    public int createMember(Member req) {
        // 기본 검증(성별)
        if (req.getMemberGender() != null) {
            String g = req.getMemberGender().trim().toLowerCase();
            if (!g.equals("m") && !g.equals("f")) {
                throw new RuntimeException("BAD_REQUEST: memberGender는 'm' 또는 'f'만 허용");
            }
            req.setMemberGender(g);
        }
        // DEFAULT 컬럼 보정(INSERT 컬럼 명시하므로 우리가 안전하게 세팅 — DDL과 동일)
        if (req.getMemberManipay() == null) req.setMemberManipay("account");
        if (req.getMemberRole() == null)    req.setMemberRole("user");
        // member_joindate 는 DDL DEFAULT SYSDATE 사용 → INSERT 목록에 없음

        // INSERT
        try {
            return memberMapper.insert(req);
        } catch (org.springframework.dao.DuplicateKeyException e) {
            throw new RuntimeException("CONFLICT: PK/이메일/휴대폰 중복");
        } catch (org.springframework.dao.DataIntegrityViolationException e) {
            String msg = (e.getMostSpecificCause() != null) ? e.getMostSpecificCause().getMessage() : e.getMessage();
            throw new RuntimeException("BAD_REQUEST: 무결성 위반(" + msg + ")");
        }
    }

    // ✏️ 수정 (memberId, memberName 정책상 수정 금지)
    @Override
    @Transactional(rollbackFor = Exception.class)
    public int updateMember(String memberId, Member req) {
        Member target = memberMapper.selectMemberById(memberId);
        if (target == null) throw new RuntimeException("NOT_FOUND: member " + memberId);

        // null → 미변경
        if (req.getMemberPw() != null)        target.setMemberPw(req.getMemberPw());
        if (req.getMemberGender() != null)    target.setMemberGender(req.getMemberGender().toLowerCase());
        if (req.getMemberEmail() != null)     target.setMemberEmail(req.getMemberEmail());
        if (req.getMemberMobile() != null)    target.setMemberMobile(req.getMemberMobile());
        if (req.getMemberPhone() != null)     target.setMemberPhone(req.getMemberPhone());
        if (req.getZip() != null)             target.setZip(req.getZip());
        if (req.getRoadAddress() != null)     target.setRoadAddress(req.getRoadAddress());
        if (req.getJibunAddress() != null)    target.setJibunAddress(req.getJibunAddress());
        if (req.getDetailAddress() != null)   target.setDetailAddress(req.getDetailAddress());
        if (req.getMemberBirthday() != null)  target.setMemberBirthday(req.getMemberBirthday());
        if (req.getMemberManipay() != null)   target.setMemberManipay(req.getMemberManipay());
        if (req.getMemberRole() != null)      target.setMemberRole(req.getMemberRole());
        if (req.getAdminType() != null)       target.setAdminType(req.getAdminType());

        target.setMemberId(memberId);

        int affected = memberMapper.update(target);
        if (affected == 0) throw new RuntimeException("NOT_FOUND: member " + memberId);
        return affected;
    }

    // 🗑 삭제
    @Override
    @Transactional(rollbackFor = Exception.class)
    public int deleteMember(String memberId) {
        int affected = memberMapper.delete(memberId);
        if (affected == 0) throw new RuntimeException("NOT_FOUND: member " + memberId);
        return affected;
    }

    // 📃 목록
    @Override
    @Transactional(readOnly = true)
    public List<Member> listMembers(Integer page, Integer size, String keyword, String role) {
        return memberQueryMapper.selectMembers(page, size, keyword, role);
    }

    // 🔢 총 개수
    @Override
    @Transactional(readOnly = true)
    public long countMembers(String keyword, String role) {
        return memberQueryMapper.countMembers(keyword, role); // MemberQueryMapper랑 연동되어 있음
    }
}


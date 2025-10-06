package com.gym.service.impl;                                      // 📦 서비스 구현

import com.gym.domain.member.Member;
import com.gym.mapper.annotation.MemberMapper;
import com.gym.mapper.xml.MemberQueryMapper;
import com.gym.service.MemberService;

import lombok.extern.log4j.Log4j2;

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
@Log4j2 //250930 추가
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
    public int createMember(Member member) {
        // 기본 검증(성별)
        if (member.getMemberGender() != null) {
            String g = member.getMemberGender().trim().toLowerCase();
            if (!g.equals("m") && !g.equals("f")) {
                throw new RuntimeException("BAD_REQUEST: memberGender는 'm' 또는 'f'만 허용");
            }
            member.setMemberGender(g);
        }
        // DEFAULT 컬럼 보정(INSERT 컬럼 명시하므로 우리가 안전하게 세팅 — DDL과 동일)
        if (member.getMemberManipay() == null) member.setMemberManipay("account");
        if (member.getMemberRole() == null)    member.setMemberRole("user");
        // member_joindate 는 DDL DEFAULT SYSDATE 사용 → INSERT 목록에 없음
        
        // ---------------------- 250930 ---------------------- 
        // 전화번호 null값 허용
        String tempPhone = member.getMemberPhone() == null ? "": member.getMemberPhone();
        member.setMemberPhone(tempPhone);
        
        // 상세주소 null값 허용
        String tempZip = member.getZip() == null ? "": member.getZip();
        member.setZip(tempZip);
        
        String tempRoadAddress = member.getRoadAddress() == null ? "": member.getRoadAddress();
        member.setRoadAddress(tempRoadAddress);
        
        String tempJibunAddress = member.getJibunAddress() == null ? "": member.getJibunAddress();
        member.setJibunAddress(tempJibunAddress);
        
        String tempDetailAddress = member.getDetailAddress() == null ? "": member.getDetailAddress();
        member.setDetailAddress(tempDetailAddress);
        
        
        log.info("회원등록 서비스:{}", member);
        // ---------------------- 250930 ---------------------- 
        
        
        // INSERT
        try {
            return memberMapper.insert(member);
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
    public int updateMember(String memberId, Member member) {
        Member target = memberMapper.selectMemberById(memberId);
        if (target == null) throw new RuntimeException("NOT_FOUND: member " + memberId);

        // null → 미변경
        if (member.getMemberPw() != null)        target.setMemberPw(member.getMemberPw());
        if (member.getMemberGender() != null)    target.setMemberGender(member.getMemberGender().toLowerCase());
        if (member.getMemberEmail() != null)     target.setMemberEmail(member.getMemberEmail());
        if (member.getMemberMobile() != null)    target.setMemberMobile(member.getMemberMobile());
        if (member.getMemberPhone() != null)     target.setMemberPhone(member.getMemberPhone());
        if (member.getZip() != null)             target.setZip(member.getZip());
        if (member.getRoadAddress() != null)     target.setRoadAddress(member.getRoadAddress());
        if (member.getJibunAddress() != null)    target.setJibunAddress(member.getJibunAddress());
        if (member.getDetailAddress() != null)   target.setDetailAddress(member.getDetailAddress());
        if (member.getMemberBirthday() != null)  target.setMemberBirthday(member.getMemberBirthday());
        if (member.getMemberManipay() != null)   target.setMemberManipay(member.getMemberManipay());
        if (member.getMemberRole() != null)      target.setMemberRole(member.getMemberRole());
        if (member.getAdminType() != null)       target.setAdminType(member.getAdminType());

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


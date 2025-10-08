package com.gym.domain.member;

import lombok.*;

/**
 * 회원 등록 요청 DTO
 * - NOT NULL: memberId, memberPw, memberName, memberGender, memberEmail, memberMobile
 * - DEFAULT 컬럼은 Service에서 안전값 세팅
 */
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder @ToString
public class MemberCreateRequest {
    private String memberId;
    private String memberPw;
    private String memberName;
    private String memberGender;   // 'm' or 'f'
    private String memberEmail;
    private String memberMobile;
    private String memberPhone;
    private String zip;
    private String roadAddress;
    private String jibunAddress;
    private String detailAddress;
    private String memberBirthday; // "YYYY-MM-DD" 문자열 → Service에서 LocalDate 변환
    private String memberManipay;  // 지정 없으면 'account'
    private String memberRole;     // 지정 없으면 'user'
    private String adminType;      // 일반 회원이면 null 허용
}

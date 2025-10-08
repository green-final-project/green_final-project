package com.gym.domain.member;

import lombok.*;

/**
 * 회원 수정 요청 DTO
 * - null 필드는 미변경
 * - memberId, memberName 수정 금지(컨트롤러 경로변수/정책)
 */
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder @ToString
public class MemberUpdateRequest {
    private String memberPw;
    private String memberGender;
    private String memberEmail;
    private String memberMobile;
    private String memberPhone;
    private String zip;
    private String roadAddress;
    private String jibunAddress;
    private String detailAddress;
    private String memberBirthday; // "YYYY-MM-DD"
    private String memberManipay;
    private String memberRole;
    private String adminType;
}

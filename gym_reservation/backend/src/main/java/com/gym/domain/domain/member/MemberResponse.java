package com.gym.domain.member;

import lombok.*;
import java.time.LocalDate;

/** 회원 응답 DTO (비밀번호 제외) */
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder @ToString
public class MemberResponse {
    private String memberId;
    private String memberName;
    private String memberGender;
    private String memberEmail;
    private String memberMobile;
    private String memberPhone;
    private String zip;
    private String roadAddress;
    private String jibunAddress;
    private String detailAddress;
    private LocalDate memberBirthday;
    private String memberManipay;
    private LocalDate memberJoindate;
    private String memberRole;
    private String adminType;

	//[250926] Member → MemberResponse 변환
	 
	public static MemberResponse from(Member m) {
	    return MemberResponse.builder()
	        .memberId(m.getMemberId())
	        .memberName(m.getMemberName())
	        .memberGender(m.getMemberGender())
	        .memberEmail(m.getMemberEmail())
	        .memberMobile(m.getMemberMobile())
	        .memberPhone(m.getMemberPhone())
	        .zip(m.getZip())
	        .roadAddress(m.getRoadAddress())
	        .jibunAddress(m.getJibunAddress())
	        .detailAddress(m.getDetailAddress())
	        .memberBirthday(m.getMemberBirthday())
	        .memberManipay(m.getMemberManipay())
	        .memberJoindate(m.getMemberJoindate())
	        .memberRole(m.getMemberRole())
	        .adminType(m.getAdminType())
	        .build();
	}
}

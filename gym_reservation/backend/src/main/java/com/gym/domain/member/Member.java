package com.gym.domain.member;                 // 📦 도메인 DTO 패키지(회원)

import lombok.*;
import java.time.LocalDate;                    // 🗓 날짜 타입(Oracle DATE ↔ LocalDate 매핑)

/**
 * member_tbl 1:1 매핑 엔티티
 * - DDL 기준 컬럼을 카멜케이스로 보유
 * - member_joindate 는 DDL DEFAULT SYSDATE → INSERT 목록에서 제외(기본값 사용)
 */
@Getter @Setter @ToString
@NoArgsConstructor @AllArgsConstructor @Builder
public class Member {

    private String memberId;          // 🔑 PK
    private String memberPw;          // 🔐 비밀번호(로그 금지)
    private String memberName;        // 🧑 이름(정책상 수정 금지)
    private String memberGender;      // ⚥ 성별('m'/'f')
    private String memberEmail;       // ✉️ UNIQUE
    private String memberMobile;      // 📱 UNIQUE
    private String memberPhone;       // ☎
    private String zip;               // 📮
    private String roadAddress;       // 🏠
    private String jibunAddress;      // 🏘
    private String detailAddress;     // 🏷
    private LocalDate memberBirthday; // 🎂
    private String memberManipay;     // 💳 'account'/'card' (DEFAULT 'account')
    private LocalDate memberJoindate; // 🗓 가입일(DDL DEFAULT SYSDATE)
    private String memberRole;        // 🔐 'user'/'admin' (DEFAULT 'user')
    private String adminType;         // 🔐 관리자 세분화(관리자/강사/책임자)
}


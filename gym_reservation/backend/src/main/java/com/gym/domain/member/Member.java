package com.gym.domain.member;                 // 📦 도메인(회원) 전용 패키지

import com.fasterxml.jackson.annotation.JsonIgnore; // ✉️ 응답 시 비밀번호 숨김 처리
import lombok.AllArgsConstructor;              // 🧩 모든 필드 생성자 자동 생성
import lombok.Builder;                         // 🧩 빌더 패턴 자동 생성
import lombok.Getter;                          // 🧩 getter 자동 생성
import lombok.NoArgsConstructor;               // 🧩 기본 생성자 자동 생성
import lombok.Setter;                          // 🧩 setter 자동 생성
import lombok.ToString;                        // 🧩 toString 자동 생성

import java.time.LocalDate;                    // 🗓 Oracle DATE ↔ Java LocalDate 매핑용

/**
 * member_tbl과 1:1 필드 매핑 DTO
 * - 컬럼명(snake_case) ↔ 필드명(camelCase)은 MyBatis 전역 설정(map-underscore-to-camel-case: true)로 자동 매핑
 * - 각 필드는 엑셀/DDL 정의의 의미 그대로 주석 처리 (임의 해석/변경 금지)
 */
@Getter @Setter @ToString
@NoArgsConstructor @AllArgsConstructor @Builder
public class Member {

    private String memberId;          // 회원 ID( PK )              : 로그인/식별 키

    @JsonIgnore
    private String memberPw;          // 비밀번호                   : 응답 숨김(서비스/보안단에서 해시 저장 전제)

    private String memberName;        // 이름                       : 한글/영문 허용
    private String memberGender;      // 성별('m','f')              : CHECK 제약 준수
    private String memberEmail;       // 이메일                     : UNIQUE 제약 가능(엑셀 사양 따름)
    private String memberMobile;      // 휴대폰 번호                : 010- 형태 등
    private String memberPhone;       // 일반 전화번호(선택)        : 없을 수 있음
    private String zip;               // 우편번호(선택)
    private String roadAddress;       // 도로명 주소(선택)
    private String jibunAddress;      // 지번 주소(선택)
    private String detailAddress;     // 상세 주소(선택)

    private LocalDate memberBirthday; // 생년월일(선택)             : DATE → LocalDate

    private String memberManipay;     // 주요 결제수단              : 계좌/카드 등 문자열 코드
    private LocalDate memberJoindate; // 가입일                     : DB DEFAULT(SYSDATE) 가정

    private String memberRole;        // 계정권한('user','admin')   : 권한 분기용
    private String adminType;         // 관리자 역할 세분화         : '최고관리자'/'관리자'/'담당자'
}

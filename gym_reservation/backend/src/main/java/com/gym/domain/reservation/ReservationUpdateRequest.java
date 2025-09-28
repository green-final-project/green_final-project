// [김종범]
package com.gym.domain.reservation; // 📦 예약 도메인 패키지

import lombok.*; // 🧩 롬복 라이브러리

/**
 * 예약 수정 요청 DTO (PUT /api/reservations/{resvId})
 * - 변경할 수 있는 정보만 담고 있어.
 */
@Getter // 각 필드의 Getter 메소드를 자동 생성
@Setter // 각 필드의 Setter 메소드를 자동 생성
@NoArgsConstructor // 파라미터 없는 기본 생성자를 자동 생성
@AllArgsConstructor // 모든 필드를 받는 생성자를 자동 생성
@Builder // 빌더 패턴을 사용할 수 있게 지원
@ToString // toString() 메소드를 자동 생성
public class ReservationUpdateRequest { // 예약 수정 요청 DTO 시작

    private String resvContent;         // 요구사항
    private Integer resvPersonCount;    // 신청 인원 수
    private String resvStatus;          // 예약 상태 ('완료', '취소', '대기')
}
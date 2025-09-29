package com.gym.domain.message;

import java.time.LocalDateTime;
import lombok.*;

/**
 * 메시지 조회 시 반환되는 DTO 클래스
 * - member_name 칼럼 조인 결과 포함
 */
@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MessageResponse {

    /** 메시지 고유 ID */
    private Long messageId;

    /** 문자 수신자 ID */
    private String memberId;

    /** 수신자 이름 (JOIN으로 가져옴) */
    private String memberName;

    /** 관련 예약 ID */
    private Long resvId;

    /** 관련 휴관일 ID */
    private Long closedId;

    /** 문자 유형 */
    private String messageType;

    /** 발송된 문자 내용 */
    private String messageContent;

    /** 문자 발송 일시 */
    private LocalDateTime messageDate;
}

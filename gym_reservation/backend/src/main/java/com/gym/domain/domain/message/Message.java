package com.gym.domain.message;

import java.time.LocalDateTime;
import lombok.*;

/**
 * 메시지 전송 이력 데이터 모델 클래스
 * DB의 message_tbl 테이블과 매핑됨
 * messagePhone 필드 제거
 */
@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Message {

    /** 메시지 고유 ID (PK) */
    private Long messageId;

    /** 문자 수신자 ID (FK member_tbl) */
    private String memberId;

    /** 관련 예약 ID (nullable) */
    private Long resvId;

    /** 관련 휴관일 ID (nullable) */
    private Long closedId;

    /** 문자 유형 (예약확인, 예약취소, 휴관공지) */
    private String messageType;

    /** 문자 내용 */
    private String messageContent;

    /** 문자 발송 일시 */
    private LocalDateTime messageDate;
}

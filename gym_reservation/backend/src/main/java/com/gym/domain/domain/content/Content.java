package com.gym.domain.content;

import lombok.Data;
import java.time.LocalDateTime;

/**
 * Content 엔티티
 * - contents_tbl 과 매핑되는 도메인 클래스
 */
@Data
public class Content {
    private Long contentId;        // PK
    private String contentTitle;   // 제목
    private String contentContent; // 내용
    private String memberId;       // 작성자
    private String contentUse;     // 사용 여부
    private Integer contentNum;    // 순번
    private String contentType;    // 타입
    private LocalDateTime contentRegDate; // 등록일
    private LocalDateTime contentModDate; // 수정일
}

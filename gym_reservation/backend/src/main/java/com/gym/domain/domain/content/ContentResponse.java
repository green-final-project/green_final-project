package com.gym.domain.content;

import lombok.Data;

/**
 * 콘텐츠 응답 DTO
 * - DB에서 조회한 콘텐츠를 반환할 때 사용
 */
@Data
public class ContentResponse {
    private Long contentId;          // PK
    private String contentTitle;     // 제목
    private String contentContent;   // HTML 본문
    private String memberId;         // 작성자
    private String contentUse;       // 사용 여부
    private Integer contentNum;      // 메뉴 순서
    private String contentType;      // 카테고리
    private String contentRegDate;   // 작성일
    private String contentModDate;   // 수정일
}

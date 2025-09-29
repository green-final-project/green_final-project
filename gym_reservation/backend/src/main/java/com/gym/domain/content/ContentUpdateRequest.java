package com.gym.domain.content;

import lombok.Data;

/**
 * 콘텐츠 수정 요청 DTO
 * - 기존 콘텐츠 수정 시 입력값
 */
@Data
public class ContentUpdateRequest {
    
	private Long contentId;          // 수정할 콘텐츠 ID → contents_tbl.content_id
    private String contentTitle;     // 제목
    private String contentContent;   // HTML 본문
    private String memberId;         // 수정자 ID
    private String contentUse;       // 사용 여부(Y/N)
    private Integer contentNum;      // 메뉴 순서
    private String contentType;      // 카테고리
    
    
}

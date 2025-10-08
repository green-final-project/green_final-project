package com.gym.domain.content;

import lombok.Data;

/**
 * 콘텐츠 목록/검색 요청 DTO
 * - 시나리오 표 기준(title, useYn, page, size)
 * - 목록 조회 시 Controller → Service → Mapper 로 전달됨
 */
@Data
public class ContentSearchRequest {
    private Long contentId;        // 콘텐츠 ID
    private String contentTitle;   // 제목 검색 키워드
    private String memberId;       // 작성자 ID
    private String contentType;    // 카테고리

    private String useYn;          // 사용 여부 (Y/N)
    private int page;              // 페이지 번호
    private int size;              // 페이지 크기
}


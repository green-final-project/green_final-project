package com.gym.domain.content;

import lombok.Data;

/**
 * 콘텐츠 생성 요청 DTO
 * - 콘텐츠 등록 시 클라이언트가 보낸 값(JSON → 객체 매핑)
 * - DB 테이블 contents_tbl 기준
 */
@Data
public class ContentCreateRequest {
    private String contentTitle;     // 제목 → contents_tbl.content_title
    private String contentContent;   // HTML 본문 → contents_tbl.content_content
    private String memberId;         // 작성자 ID → contents_tbl.member_id
    private String contentUse;       // 사용 여부(Y/N) → contents_tbl.content_use
    private Integer contentNum;      // 메뉴 순서 번호(2자리) → contents_tbl.content_num
    private String contentType;      // 상위 카테고리(이용안내/상품·시설안내) → contents_tbl.content_type
}

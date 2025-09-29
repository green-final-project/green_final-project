package com.gym.domain.post;

import lombok.*;

/**
 * 게시글 등록용 DTO
 * - 게시글 생성 시 클라이언트에서 전달하는 데이터 모델
 */
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class PostCreateRequest {

    private Long boardId;          /* 게시판 ID */
    private Long boardPostNo; // [250924추가] 게시판별 번호
    private String postTitle;      /* 게시글 제목 */
    private String postContent;    /* 게시글 내용 */
    private String memberId;       /* 작성자 회원 ID */
    private Boolean postNotice;    /* 공지 여부 */
    private Boolean postSecret;    /* 비밀글 여부 */
    private String postType;       /* 게시글 유형 */
}

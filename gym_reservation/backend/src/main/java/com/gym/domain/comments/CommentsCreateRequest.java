package com.gym.domain.comments;

import lombok.*;

/**
 * 댓글 등록 및 수정 요청 DTO
 * 수정 시에도 content, boardId, memberId를 포함해 재사용 가능
 */
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class CommentsCreateRequest {
    private Long postId;     // 게시글 ID (외래키)
    private String memberId;  // 작성자 회원 ID
    private String content;   // 댓글 내용
}

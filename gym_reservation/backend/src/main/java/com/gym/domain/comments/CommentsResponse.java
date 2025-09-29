package com.gym.domain.comments;

import lombok.*;
import java.time.LocalDateTime;

/**
 * 댓글 조회 시 반환 DTO
 */
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class CommentsResponse {
    private Long commentsId;
    private Long postId;
    private String memberId;
    private String memberName;
    private String content;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}

package com.gym.domain.comments;

import lombok.*;
import java.time.LocalDateTime;

/**
 * 댓글 엔티티 DTO (DB 매핑용)
 */
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class Comments {
    private Long commentsId;
    private Long postId;
    private String memberId;
    private String content;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}

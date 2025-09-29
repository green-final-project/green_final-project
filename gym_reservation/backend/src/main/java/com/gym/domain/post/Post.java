package com.gym.domain.post;

import java.time.LocalDateTime;
import lombok.*;

@Getter @Setter @ToString
@NoArgsConstructor @AllArgsConstructor @Builder
public class Post {
    private Long postId;
    private Long boardId;
    private Long boardPostNo; // [250924추가] 게시판별 번호
    private String postTitle;
    private String postContent;
    private String memberId;
    private LocalDateTime postRegDate;
    private LocalDateTime postModDate;
    private Integer postViewCount;
    private Boolean postNotice;
    private Boolean postSecret;
    private String postType;
    
}

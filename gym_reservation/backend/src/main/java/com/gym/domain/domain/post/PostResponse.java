package com.gym.domain.post;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 게시글 조회 및 수정용 DTO
 * postId를 포함하여 클라이언트에 반환하거나, 수정 요청시 사용
 */

@Builder(toBuilder = true)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PostResponse {

    private Long postId;  // 고유키, 수정시 필수

    private Long boardId;

    private Long boardPostNo; // [250924추가] 게시판별 번호
    
    private String postTitle;

    private String postContent;

    private String memberId;

    private String memberName;  // 조인 결과, 조회용

    private LocalDateTime postRegDate;

    private Integer postViewCount;

    private Boolean postNotice;

    private Boolean postSecret;

    private String postType;
    

}

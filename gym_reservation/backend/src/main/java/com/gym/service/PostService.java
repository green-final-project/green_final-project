package com.gym.service;

import com.gym.domain.post.PostResponse;

import java.util.List;

// import com.gym.domain.post.PostCreateRequest;

public interface PostService {

    // ====================    [OLD]     ====================
    /**
     * 게시글 등록 처리
     *
     * @param postResponse 게시글 DTO (등록일, 수정일은 DB SYSDATE 자동 처리)
     * @return 생성된 게시글 ID
     */
    // Long createPost(PostResponse postResponse);   // ★ 구버전: 주석으로 보존

    // ==================== [NEW -250910-] ====================
    /**
     * 게시글 등록 처리 (생성 전용 DTO 사용)
     *
     * @param req 게시글 생성 요청 DTO (등록일/수정일은 DB SYSDATE)
     * @return 생성된 게시글 ID
     */
     Long createPost(PostResponse postResponse);	// [NEW] 현재 컨트롤러/서비스 흐름과 맞춤
    // ==================== [NEW -250910-] ====================
    
    List<PostResponse> getPostsByBoard(Long boardId, int page, int size, String keyword, Boolean notice);
    PostResponse getPostById(Long postId);
    void updatePost(PostResponse postResponse);
    void deletePostById(Long postId);
    int countPostsByBoard(Long boardId, String keyword, Boolean notice);
    
    // ==================== [NEW -250925-] ====================
    void increaseViewCount(Long postId); // [250925추가] 게시글 조회수 1 증가
    
}

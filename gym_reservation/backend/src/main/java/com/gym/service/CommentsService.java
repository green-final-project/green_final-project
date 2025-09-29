package com.gym.service;

import com.gym.domain.comments.CommentsCreateRequest;
import com.gym.domain.comments.CommentsResponse;
import java.util.List;

/**
 * 댓글 관련 서비스 인터페이스
 */
public interface CommentsService {

    /**
     * 댓글 등록
     * @param request 댓글 생성 요청 DTO
     * @return 생성된 댓글 ID
     */
    Long createComments(CommentsCreateRequest request);

    /**
     * 게시글 별 댓글 목록 조회
     * @param postId 게시글 ID
     * @return 댓글 리스트
     */
    List<CommentsResponse> getCommentsByPost(Long postId);

    /**
     * 댓글 단건 조회
     * @param commentsId 댓글 ID
     * @return 댓글 상세 DTO
     */
    CommentsResponse getCommentsById(Long commentsId);

    /**
     * 회원 ID + 댓글 ID 기준 댓글 수정 (회원 소유 체크)
     * @param memberId 회원 ID
     * @param commentsId 댓글 ID
     * @param request 수정 요청 DTO
     */
    void updateCommentsByMember(String memberId, Long commentsId, CommentsCreateRequest request);

    /**
     * 댓글 삭제
     * @param commentsId 댓글 ID
     */
    void deleteComments(Long commentsId);
}

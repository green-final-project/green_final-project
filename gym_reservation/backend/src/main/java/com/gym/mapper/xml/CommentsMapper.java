package com.gym.mapper.xml;

import com.gym.domain.comments.Comments;
import com.gym.domain.comments.CommentsResponse;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 댓글 관련 MyBatis 매퍼 인터페이스
 */
@Mapper
public interface CommentsMapper {

    /**
     * 댓글 등록
     * @param comments 댓글 엔티티
     * @return 영향받은 행 수
     */
    int insertComments(Comments comments);

    /**
     * 특정 게시글 댓글 전체 조회
     * @param postId 게시글 ID
     * @return 댓글 리스트
     */
    List<CommentsResponse> selectCommentsByPost(@Param("postId") Long postId);

    /**
     * 댓글 단건 조회
     * @param commentsId 댓글 ID
     * @return 댓글 상세 DTO
     */
    CommentsResponse selectCommentsById(@Param("commentsId") Long commentsId);

    /**
     * 회원 ID + 댓글 ID 기준 댓글 수정 (회원 소유 검증)
     * @param memberId 회원 ID
     * @param commentsId 댓글 ID
     * @param content 수정 내용
     * @return 영향받은 행 수
     */
    int updateCommentsByMember(@Param("memberId") String memberId,
                               @Param("commentsId") Long commentsId,
                               @Param("content") String content);

    /**
     * 댓글 삭제
     * @param commentsId 댓글 ID
     * @return 영향받은 행 수
     */
    int deleteCommentsById(@Param("commentsId") Long commentsId);
}
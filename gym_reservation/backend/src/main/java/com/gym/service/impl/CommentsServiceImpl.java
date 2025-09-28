package com.gym.service.impl;

import com.gym.domain.comments.Comments;
import com.gym.domain.comments.CommentsCreateRequest;
import com.gym.domain.comments.CommentsResponse;
import com.gym.mapper.xml.CommentsMapper;
import com.gym.service.CommentsService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 댓글 서비스 구현체
 */
@Service
public class CommentsServiceImpl implements CommentsService {

    private final CommentsMapper commentsMapper;

    public CommentsServiceImpl(CommentsMapper commentsMapper) {
        this.commentsMapper = commentsMapper;
    }

    /**
     * 댓글 등록
     */
    @Override
    @Transactional
    public Long createComments(CommentsCreateRequest request) {
        Comments comments = Comments.builder()
            .postId(request.getPostId())
            .memberId(request.getMemberId())
            .content(request.getContent())
            .build();
        commentsMapper.insertComments(comments);
        return comments.getCommentsId();
    }

    /**
     * 특정 게시글 댓글 전체 조회
     */
    @Override
    @Transactional(readOnly = true)
    public List<CommentsResponse> getCommentsByPost(Long postId) {
        return commentsMapper.selectCommentsByPost(postId);
    }

    /**
     * 댓글 단건 조회
     */
    @Override
    @Transactional(readOnly = true)
    public CommentsResponse getCommentsById(Long commentsId) {
        return commentsMapper.selectCommentsById(commentsId);
    }

    /**
     * 댓글 수정 (회원 ID와 댓글 ID가 일치해야 수정 가능)
     */
    @Override
    @Transactional
    public void updateCommentsByMember(String memberId, Long commentsId, CommentsCreateRequest request) {
        int updated = commentsMapper.updateCommentsByMember(memberId, commentsId, request.getContent());
        if (updated == 0) {
            throw new RuntimeException("수정할 댓글이 없습니다. 회원ID=" + memberId + ", 댓글ID=" + commentsId);
        }
    }

    /**
     * 댓글 삭제
     */
    @Override
    @Transactional
    public void deleteComments(Long commentsId) {
        int deleted = commentsMapper.deleteCommentsById(commentsId);
        if (deleted == 0) {
            throw new RuntimeException("삭제할 댓글이 없습니다. ID=" + commentsId);
        }
    }
}

package com.gym.mapper.xml;

import com.gym.domain.post.PostResponse;
import com.gym.domain.post.Post;                   // ★ 추가: INSERT용 엔티티
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import java.util.List;

/**
 * 게시글 관련 MyBatis 매퍼 인터페이스
 * - XML 매퍼 파일과 연동
 * - 게시글 등록, 조회, 수정, 삭제, 개수 카운트 기능
 */
@Mapper
public interface PostMapper {

    /**
     * 게시글 등록
     * @param postResponse 등록할 게시글 DTO
     * @return 영향받은 행 수 (보통 생성된 PK)
     */
    // ===== [원본 보존] =====
    // Long insertPost(PostResponse postResponse);

    // ===== [수정본] INSERT는 엔티티(Post)로 받고 영향행수(int) 반환 =====
    //  - XML: VALUES에서 post_seq.NEXTVAL
    //  - AFTER selectKey로 CURRVAL → Post.postId 주입
    int insertPost(Post post);   // ✏️수정

    /**
     * 게시판별 게시글 목록 조회 (페이징, 키워드 검색, 공지 필터 포함)
     * @param boardId 게시판 ID
     * @param offset 조회 시작 위치
     * @param limit 조회 개수
     * @param keyword 검색 키워드
     * @param notice 공지글 필터 여부
     * @return 게시글 목록 리스트
     */
    List<PostResponse> selectPostsByBoard(@Param("boardId") Long boardId,
                                          @Param("offset") int offset,
                                          @Param("limit") int limit,
                                          @Param("keyword") String keyword,
                                          @Param("notice") String notice);

    /**
     * 게시글 단건 조회
     * @param postId 게시글 ID
     * @return 게시글 상세 정보 DTO
     */
    PostResponse selectPostById(@Param("postId") Long postId);

    /**
     * 게시글 수정 처리
     * @param postResponse 수정할 게시글 DTO
     * @return 영향받은 행 수
     */
    int updatePost(PostResponse postResponse);   // ★ 그대로 유지 (XML도 PostResponse로 맞춤)

    /**
     * 게시글 삭제
     * @param postId 삭제할 게시글 ID
     * @return 영향받은 행 수
     */
    int deletePostById(@Param("postId") Long postId);

    /**
     * 게시판별 게시글 개수 조회 (검색 및 공지 필터 포함)
     * @param boardId 게시판 ID
     * @param keyword 검색 키워드
     * @param notice 공지 필터
     * @return 게시글 총 수
     */
    int countPostsByBoard(@Param("boardId") Long boardId,
                          @Param("keyword") String keyword,
                          @Param("notice") Boolean notice);

    /** 
     * 추가: INSERT 이전 사전검증 (실패 시 INSERT 진입 금지 → NEXTVAL 미호출)
     */
    boolean existsBoardId(@Param("boardId") Long boardId);     // ✏️ 추가
    boolean existsMemberId(@Param("memberId") String memberId); // ✏️ 추가
    
    
    /** 
     * 추가: 게시글 조회 1 카운트
     */
    void increaseViewCount(Long postId); // [250925추가]

}

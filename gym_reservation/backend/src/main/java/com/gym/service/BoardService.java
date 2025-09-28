package com.gym.service;

import com.gym.domain.board.BoardCreateRequest;
import com.gym.domain.board.BoardResponse;
import com.gym.domain.board.BoardUpdateRequest;
import java.util.List;

/**
 * [김종범]
 * 게시판 관리 비즈니스 로직을 정의하는 서비스 인터페이스
 * - 컨트롤러는 이 인터페이스에 의존하여 실제 구현체의 변경에 영향을 받지 않습니다.
 * - 트랜잭션의 범위와 속성을 메서드 수준에서 정의합니다.
 */
public interface BoardService {

    // 새로운 게시판을 생성합니다. (트랜잭션: REQUIRED)
    Integer createBoard(BoardCreateRequest request);

    // 게시판 단건 조회합니다. (트랜잭션: readOnly)
    // List<BoardResponse> getAllBoards();

    // 특정 ID의 게시판을 조회합니다. (트랜잭션: readOnly)
    // BoardResponse getBoardById(Integer boardId); // 일괄조회 였었음
    // 상세검색을 위해 게시판ID, 게시판명, 게시자ID 필터링 조회
    List<BoardResponse> getBoards(String boardId,		// 게시판ID
    							  String boardTitle,	// 게시판명
    							  String memberId); 	// 게시자ID(회원-관리자) 추가한거

    // 게시판 정보를 수정합니다. (트랜잭션: REQUIRED)
    Integer updateBoard(Integer boardId,
    					String memberId,
    					BoardUpdateRequest request);

    // 게시판을 삭제합니다. (트랜잭션: REQUIRED)
    void deleteBoard(Integer boardId,
    				 String memberId); // 게시자ID
}

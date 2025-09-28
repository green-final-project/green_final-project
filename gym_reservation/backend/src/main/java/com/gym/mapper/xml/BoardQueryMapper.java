package com.gym.mapper.xml;

import com.gym.domain.board.Board;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import java.util.List;
import java.util.Optional;

/**
 * [김종범]
 * 게시판 목록/검색 등 동적 SQL을 위한 매퍼 인터페이스
 * - 실제 SQL 쿼리는 src/main/resources/mappers/board-mapper.xml 파일에 작성됩니다.
 * - 이 인터페이스의 FQCN(Full Qualified Class Name)은 XML의 namespace와 일치해야 합니다.
 */
@Mapper // MyBatis가 이 인터페이스를 매퍼로 인식하도록 합니다.
public interface BoardQueryMapper { // 🗺️ 게시판 XML 매퍼 인터페이스

    // 게시판 정보를 DB에 삽입합니다.
    int insertBoard(Board board);

    // boardId로 특정 게시판 정보를 조회합니다. (결과가 없을 수 있으므로 Optional 사용)
    Optional<Board> findBoardById(@Param("boardId") Integer boardId);

    // 모든 게시판 목록을 조회합니다.
    List<Board> findAllBoards();

    // 게시판 정보를 수정합니다.
    int updateBoard(Board board);

    // boardId로 특정 게시판을 삭제합니다.
    int deleteBoardById(@Param("boardId") Integer boardId);
    
	// [추가] 목록 검색용
    List<Board> searchBoards(
    		// @Param("JSON변수") 입력해야지 정상적으로 조회됨 (이거 설정안하면 필수입력사항 되버림)
    	    @Param("boardId") String boardId,
    	    @Param("boardTitle") String boardTitle,
    	    @Param("memberId") String memberId
    	);
}

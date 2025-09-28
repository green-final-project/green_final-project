package com.gym.service.impl;

import com.gym.domain.board.Board;
import com.gym.domain.board.BoardCreateRequest;
import com.gym.domain.board.BoardResponse;
import com.gym.domain.board.BoardUpdateRequest;
import com.gym.domain.member.Member;
import com.gym.mapper.annotation.MemberMapper;
import com.gym.mapper.xml.BoardQueryMapper;
import com.gym.service.BoardService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * [김종범]
 * 게시판 관리 서비스 구현체
 * - DB 트리거 대신 서비스 계층에서 모든 유효성 검사를 수행합니다.
 */
@Service // 이 클래스를 스프링의 서비스 빈으로 등록합니다.
@RequiredArgsConstructor // final로 선언된 필드에 대한 생성자를 자동으로 만들어 의존성을 주입합니다.
public class BoardServiceImpl implements BoardService {

    private final BoardQueryMapper boardQueryMapper; // 게시판 관련 DB 작업을 위한 XML 매퍼입니다.
    private final MemberMapper memberMapper; // 회원 정보 조회를 위한 어노테이션 매퍼입니다.

    @Override
    @Transactional // 이 메서드 전체를 하나의 트랜잭션으로 묶습니다. 실패 시 모든 작업이 롤백됩니다.
    public Integer createBoard(BoardCreateRequest request) {
        // ✅ =================== [핵심 수정] 자바 검문소 로직 추가 ===================
        // 1. 요청으로 들어온 memberId가 유효한지 확인합니다.
        Member member = memberMapper.selectMemberById(request.getMemberId());
        if (member == null) {
            // 회원이 존재하지 않으면, 시퀀스를 사용하기 전에 즉시 에러를 발생시킵니다.
            throw new RuntimeException("존재하지 않는 회원 ID입니다: " + request.getMemberId());
        }

        // 2. 해당 회원의 권한이 'admin'이 맞는지 확인합니다.
        if (!"admin".equalsIgnoreCase(member.getMemberRole())) {
            // 관리자가 아니면, 시퀀스를 사용하기 전에 즉시 에러를 발생시킵니다.
            throw new RuntimeException("게시판을 생성할 권한이 없습니다. (관리자만 가능)");
        }
        // =========================================================================

        // 3. 모든 검사를 통과했으므로, 이제 안심하고 엔티티를 만듭니다.
        Board board = Board.builder()
                .boardTitle(request.getBoardTitle())
                .boardContent(request.getBoardContent())
                .memberId(request.getMemberId())
                .boardNum(request.getBoardNum())
                .boardUse(request.getBoardUse() == null ? "Y" : request.getBoardUse())
                .build();

        // 4. 검증된 데이터로 DB에 저장을 요청합니다. 이제 실패할 확률이 거의 없습니다.
        boardQueryMapper.insertBoard(board);

        // 5. mapper.xml의 <selectKey> 덕분에 방금 생성된 boardId가 board 객체에 담겨있습니다.
        return board.getBoardId();
    }

    // [삭제] 컨트롤러가 더 이상 전체 조회 메서드를 사용하지 않으므로 인터페이스와 시그니처 충돌 방지를 위해 제거
    // @Override
    // @Transactional(readOnly = true)
    // public List<BoardResponse> getAllBoards() { ... }

    // [추가] 검색 필터(부분일치 제목, 작성자, 선택적 boardId) 목록 조회
    @Override // [추가] 인터페이스(BoardService)에 동일 시그니처 존재해야 함
    @Transactional(readOnly = true)
    public List<BoardResponse> getBoards(String boardId, String boardTitle, String memberId) {
    	return boardQueryMapper.searchBoards(boardId, boardTitle, memberId).stream() // searchBoards를 위해 BoardQueryMapper 수정해야 함 
    			.map(BoardResponse::from)
                .collect(Collectors.toList());
    }

    /* [삭제] 단건 조회 이제 안씀
    @Override
    @Transactional(readOnly = true)
    public BoardResponse getBoardById(Integer boardId) {
        Board board = boardQueryMapper.findBoardById(boardId)
                .orElseThrow(() -> new RuntimeException("게시판을 찾을 수 없습니다. ID: " + boardId));
        return BoardResponse.from(board);
    }
    */

    // 수정
    @Override
    @Transactional
    public Integer updateBoard(Integer boardId, String memberId, BoardUpdateRequest request) { // [수정] path의 memberId 추가
        // 수정할 게시판이 존재하는지 먼저 확인합니다.
    	var curr = boardQueryMapper.findBoardById(boardId) // [205922] var curr 추가
                .orElseThrow(() -> new RuntimeException("수정할 게시판을 찾을 수 없습니다. ID: " + boardId));

        // [추가] 경로 memberId 유효성 및 관리자 권한 확인 (수정도 관리자만)
        Member actor = memberMapper.selectMemberById(memberId);
        if (actor == null || !"admin".equalsIgnoreCase(actor.getMemberRole())) {
            throw new RuntimeException("수정 권한이 없습니다. (관리자만 가능)");
        }

        // 담당자(memberId)를 변경하는 경우, 변경될 담당자가 유효한 관리자인지 확인합니다.
        if (request.getMemberId() != null) {
            Member newOwner = memberMapper.selectMemberById(request.getMemberId());
            if (newOwner == null || !"admin".equalsIgnoreCase(newOwner.getMemberRole())) {
                throw new RuntimeException("담당자를 변경할 수 없습니다. (존재하지 않거나 권한 없는 회원)");
            }
        }

        // 수정할 내용을 담은 엔티티를 생성합니다.
        Board boardToUpdate = Board.builder()
                .boardId(boardId) // 어떤 게시판을 수정할지 ID를 지정합니다.
                .memberId(curr.getMemberId()) // [205922] 작성자ID 유지
                .boardTitle(request.getBoardTitle())
                .boardContent(request.getBoardContent())
                //.memberId(request.getMemberId()) // 작성자ID (유지 기능이 없음으로 비활성화)
                .boardNum(request.getBoardNum())
                .boardUse(request.getBoardUse())
                .build();

        // DB에 수정을 요청하고, 영향받은 행의 수를 반환합니다.
        return boardQueryMapper.updateBoard(boardToUpdate);
    }

    @Override
    @Transactional
    public void deleteBoard(Integer boardId, String memberId) { // [수정] path의 memberId 추가
        // 삭제할 게시판이 존재하는지 먼저 확인합니다.
        boardQueryMapper.findBoardById(boardId)
                .orElseThrow(() -> new RuntimeException("삭제할 게시판을 찾을 수 없습니다. ID: " + boardId));

        // [추가] 경로 memberId 유효성 및 관리자 권한 확인
        Member actor = memberMapper.selectMemberById(memberId);
        if (actor == null || !"admin".equalsIgnoreCase(actor.getMemberRole())) {
            throw new RuntimeException("삭제 권한이 없습니다. (관리자만 가능)");
        }

        // DB에 삭제를 요청합니다.
        boardQueryMapper.deleteBoardById(boardId);
    }
}

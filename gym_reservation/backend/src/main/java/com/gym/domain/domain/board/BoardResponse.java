package com.gym.domain.board;

import lombok.*;
import java.time.LocalDate;

/**
 * [김종범]
 * 게시판 조회 응답 DTO
 * - 서버가 클라이언트에게 게시판 정보를 반환할 때 사용하는 데이터 구조입니다.
 */
@Getter                 // 각 필드의 Getter 메서드를 자동 생성합니다.
@Builder                // 빌더 패턴을 사용할 수 있게 합니다.
@AllArgsConstructor     // 모든 필드를 파라미터로 받는 생성자를 자동 생성합니다.
public class BoardResponse { // 📤 게시판 조회 응답 DTO

    private Integer boardId;        // 🔑 게시판 고유번호
    private String boardTitle;      // 🏷️ 게시판 이름
    private String boardContent;    // 📄 게시판 상단 내용
    private String boardUse;        // ✅ 사용 여부
    private LocalDate boardRegDate; // 🗓️ 생성 일자
    private LocalDate boardModDate; // 🗓️ 수정 일자
    private String memberId;        // ✍️ 담당자 회원 ID
    private String boardNum;        // 🔢 게시판 순서 번호

    /**
     * [김종범]
     * Board 엔티티를 BoardResponse DTO로 변환하는 정적 팩토리 메서드
     */
    public static BoardResponse from(Board board) {
        return BoardResponse.builder()
                .boardId(board.getBoardId())
                .boardTitle(board.getBoardTitle())
                .boardContent(board.getBoardContent())
                .boardUse(board.getBoardUse())
                .boardRegDate(board.getBoardRegDate())
                .boardModDate(board.getBoardModDate())
                .memberId(board.getMemberId())
                .boardNum(board.getBoardNum())
                .build();
    }
}

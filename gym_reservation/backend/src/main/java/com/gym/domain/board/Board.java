package com.gym.domain.board;

import lombok.*;
import java.time.LocalDate;

/**
 * [김종범]
 * Board 엔티티 클래스 (board_tbl 테이블과 1:1 매핑)
 * - MyBatis의 mapUnderscoreToCamelCase 설정에 의해 DB의 snake_case 컬럼이 Java의 camelCase 필드에 자동 매핑됩니다.
 */
@Getter                 // 각 필드의 Getter 메서드를 자동 생성합니다.
@Setter                 // 각 필드의 Setter 메서드를 자동 생성합니다.
@NoArgsConstructor      // 파라미터가 없는 기본 생성자를 자동 생성합니다.
@AllArgsConstructor     // 모든 필드를 파라미터로 받는 생성자를 자동 생성합니다.
@Builder                // 빌더 패턴을 사용할 수 있게 합니다.
public class Board {    // 👥 게시판 엔티티 클래스

    private Integer boardId;        // 🔑 게시판 고유번호 (PK, NUMBER) → board_tbl.board_id
    private String boardTitle;      // 🏷️ 게시판 이름 (VARCHAR2, NOT NULL) → board_tbl.board_title
    private String boardContent;    // 📄 게시판 상단 내용 (VARCHAR2, NOT NULL) → board_tbl.board_content
    private String boardUse;        // ✅ 사용 여부 ('Y'/'N', DEFAULT 'Y') → board_tbl.board_use
    private LocalDate boardRegDate; // 🗓️ 생성 일자 (DATE, DEFAULT SYSDATE) → board_tbl.board_reg_date
    private LocalDate boardModDate; // 🗓️ 수정 일자 (DATE) → board_tbl.board_mod_date
    private String memberId;        // ✍️ 담당자 회원 ID (FK, VARCHAR2) → board_tbl.member_id
    private String boardNum;        // 🔢 게시판 순서 번호 (CHAR(2)) → board_tbl.board_num
}

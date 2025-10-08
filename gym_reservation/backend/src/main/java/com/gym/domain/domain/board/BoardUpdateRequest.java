package com.gym.domain.board;

import lombok.*;

/**
 * [김종범]
 * 게시판 수정 요청 DTO
 * - 클라이언트가 게시판 정보 수정을 요청할 때 사용하는 데이터 구조입니다.
 * - null이 아닌 필드만 수정됩니다.
 */
@Getter                 // 각 필드의 Getter 메서드를 자동 생성합니다.
@Setter                 // 각 필드의 Setter 메서드를 자동 생성합니다.
@NoArgsConstructor      // 기본 생성자를 자동 생성합니다.
public class BoardUpdateRequest { // 📝 게시판 수정 요청 DTO

    private String boardTitle;   // 🏷️ 수정할 게시판 이름
    private String boardContent; // 📄 수정할 게시판 상단 내용
    private String memberId;     // ✍️ 수정할 담당자 ID ('admin' 권한만 가능)
    private String boardNum;     // 🔢 수정할 게시판 순서 번호
    private String boardUse;     // ✅ 수정할 사용 여부 ('Y'/'N')
}

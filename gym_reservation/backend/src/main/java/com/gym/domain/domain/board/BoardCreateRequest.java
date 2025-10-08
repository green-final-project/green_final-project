package com.gym.domain.board;

import lombok.*;

/**
 * [김종범]
 * 게시판 등록 요청 DTO (Data Transfer Object)
 * - 클라이언트가 게시판 생성을 요청할 때 사용하는 데이터 구조입니다.
 */
@Getter                 // 각 필드의 Getter 메서드를 자동 생성합니다.
@Setter                 // 각 필드의 Setter 메서드를 자동 생성합니다.
@NoArgsConstructor      // 기본 생성자를 자동 생성합니다.
public class BoardCreateRequest { // 📥 게시판 생성 요청 DTO

    private String boardTitle;   // 🏷️ 게시판 이름 (필수)
    private String boardContent; // 📄 게시판 상단 내용 (필수)
    private String memberId;     // ✍️ 담당자 ID (필수, 'admin' 권한만 가능)
    private String boardNum;     // 🔢 게시판 순서 번호 (선택, 2자리 숫자)
    private String boardUse;     // ✅ 사용 여부 (선택, 미입력 시 'Y')
}

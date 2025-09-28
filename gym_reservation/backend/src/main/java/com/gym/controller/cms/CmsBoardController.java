package com.gym.controller.cms;

import com.gym.common.ApiResponse;                    // 공통 응답 래퍼
import com.gym.domain.board.BoardCreateRequest;       // 등록 DTO (기존)
import com.gym.domain.board.BoardResponse;            // 조회 DTO (기존)
import com.gym.domain.board.BoardUpdateRequest;       // 수정 DTO (기존)
import com.gym.service.BoardService;                  // 서비스 인터페이스 (기존)

import io.swagger.v3.oas.annotations.Operation;       // Swagger 요약
import io.swagger.v3.oas.annotations.Parameter;       // Swagger 파라미터
import io.swagger.v3.oas.annotations.tags.Tag;        // Swagger 태그

import lombok.RequiredArgsConstructor;                // 생성자 주입
import lombok.extern.slf4j.Slf4j;                     // 로깅

import org.springframework.http.MediaType;            // consumes=폼
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;    // 컨트롤러 검증용 단일 SQL(서비스/매퍼 무변경 원칙)
import org.springframework.web.bind.annotation.*;     // REST 애노테이션

// [250922]추가사항
import org.springframework.security.core.Authentication; // 로그인ID 확보
import org.springframework.dao.DataIntegrityViolationException; // 제약 위반
import org.springframework.dao.DuplicateKeyException;	// Unique 충돌
import io.swagger.v3.oas.annotations.media.Schema;
import org.springframework.security.access.AccessDeniedException;


import java.util.List;

@Tag(name = "03.Board-CMS", description = "CMS 게시판 관리 API(폼 입력, req.set 매핑 통일)")
@RestController
@RequestMapping("/api/cms/boards")
@RequiredArgsConstructor
@Slf4j
public class CmsBoardController {

    private final BoardService boardService;  // ✅ 서비스 주입(기존 유지)
    private final JdbcTemplate jdbc;          // ✅ 단일 검증 SQL 용도(서비스/매퍼 변경 회피)

    // --------------------------------------------------------------------
    // 1) 게시판 등록(폼 입력) — req.set… 스타일 매핑
    // --------------------------------------------------------------------
    @Operation(summary = "게시판 등록", description = "폼으로 등록(작성자는 로그인ID 자동, 번호는 2자리 필수, 중복 불가)")
    @PostMapping(consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    public ResponseEntity<ApiResponse<Integer>> createBoard(
            @Parameter(name="boardTitle", description="게시판명", required=true)
            @RequestParam("boardTitle") String boardTitle,

            @Parameter(name="boardContent", description="상단내용(HTML/텍스트)", required=true)
            @RequestParam("boardContent") String boardContent,

            @Parameter(name="boardNum", description="게시판번호(2자리, 중복 불가)", required=true,
                    schema=@Schema(example="01"))
            @RequestParam("boardNum") String boardNum,

            @Parameter(name="boardUse", description="사용여부(Y/N)", required=true,
                    schema=@Schema(allowableValues={"Y","N"}, example="Y"))
            @RequestParam("boardUse") String boardUse,

            Authentication auth
    ) {
        // 0) 로그인/권한 체크(관리자만)
        if (auth == null || auth.getName() == null) throw new AccessDeniedException("로그인이 필요합니다.");
        final String loginId = auth.getName();
        String role = jdbc.queryForObject("SELECT member_role FROM member_tbl WHERE member_id = ?",
                                          String.class, loginId);
        if (role == null ||
        	    !( role.equalsIgnoreCase("admin") || role.equals("관리자") || role.equals("책임자") )) {
        	    throw new AccessDeniedException("권한이 없습니다.(관리자/책임자만)");
        	}

        // 1) 번호 형식 검증(콘텐츠 컨트롤러와 동일한 수준의 선검증)
        if (boardNum == null || !boardNum.matches("^\\d{2}$")) {
            return ResponseEntity.ok(ApiResponse.fail(-400, "게시판번호는 숫자 2자리여야 합니다."));
        }
        if (!"Y".equalsIgnoreCase(boardUse) && !"N".equalsIgnoreCase(boardUse)) {
            return ResponseEntity.ok(ApiResponse.fail(-400, "사용여부는 Y 또는 N만 허용됩니다."));
        }

        // 2) 폼 → DTO(req.set… 패턴 통일)
        BoardCreateRequest req = new BoardCreateRequest();
        req.setBoardTitle(boardTitle);         // 제목
        req.setBoardContent(boardContent);     // 상단내용
        req.setBoardNum(boardNum);             // 번호(2자리)
        req.setBoardUse(boardUse.toUpperCase());// Y/N 대문자 정규화
        req.setMemberId(loginId);              // ★ 작성자 = 로그인ID(입력폼 없음)

        try {
            Integer newId = boardService.createBoard(req);
            return ResponseEntity.ok(ApiResponse.ok(newId));
        } catch (RuntimeException ex) {
            // 콘텐츠 컨트롤러와 동일한 방식으로 제약 위반을 친화적으로 변환
            Throwable cause = ex.getCause();
            if (cause instanceof DuplicateKeyException || cause instanceof DataIntegrityViolationException) {
                return ResponseEntity.ok(ApiResponse.fail(-1, "게시판번호가 중복됩니다."));
            }
            String msg = String.valueOf(ex.getMessage());
            if (msg.contains("ORA-00001") || msg.toUpperCase().contains("BOARD_TBL_NUM_UN")
                                          || msg.toUpperCase().contains("BOARD_NUM_UK")) {
                return ResponseEntity.ok(ApiResponse.fail(-1, "게시판번호가 중복됩니다."));
            }
            return ResponseEntity.ok(ApiResponse.fail(-500, "서버 오류가 발생했습니다."));
        }
    }


    // --------------------------------------------------------------------
    // 2) 게시판 조회(기존 필터 그대로) — 변경 없음
    // --------------------------------------------------------------------
    @Operation(summary = "게시판 조회", description = "게시판 목록을 상세 조회합니다.(boardId/boardTitle/memberId 필터)")
    @GetMapping
    public ApiResponse<List<BoardResponse>> getBoards(
            @Parameter(description = "게시판ID") @RequestParam(value = "boardId", required = false) String boardId,
            @Parameter(description = "게시판명(부분일치)") @RequestParam(value = "boardTitle", required = false) String boardTitle,
            @Parameter(description = "작성자 회원ID(담당자)") @RequestParam(value = "memberId", required = false) String memberId
    ) {
        return ApiResponse.ok(boardService.getBoards(boardId, boardTitle, memberId));
    }

    // --------------------------------------------------------------------
    // 3) 게시판 수정(폼 입력) — Path의 memberId=수행자(관리자), 폼의 memberId=담당자(변경 대상)
    //    ※ 변수명 충돌 없이 각각 바인딩되므로 그대로 사용 가능
    // --------------------------------------------------------------------
    @Operation(summary="게시판 수정", description="폼으로 수정(작성자 불변, 번호 중복 감지)")
    @PutMapping(value = "/{boardId}", consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    public ResponseEntity<ApiResponse<Integer>> updateBoard(
            @PathVariable("boardId") Integer boardId,

            @Parameter(name="boardTitle",   description="게시판명(선택)", required=false,
                       schema=@Schema(type="string", example="공지사항"))
            @RequestParam(name="boardTitle", required=false) String boardTitle,

            @Parameter(name="boardContent", description="상단내용(선택)", required=false,
                       schema=@Schema(type="string", example="<p>상단 공지</p>"))
            @RequestParam(name="boardContent", required=false) String boardContent,

            @Parameter(name="boardNum",     description="게시판번호(2자리, 선택)", required=false,
                       schema=@Schema(type="string", example="02"))
            @RequestParam(name="boardNum", required=false) String boardNum,

            @Parameter(name="boardUse",     description="사용여부(Y/N, 선택)", required=false,
                       schema=@Schema(allowableValues={"Y","N"}, example="Y"))
            @RequestParam(name="boardUse", required=false) String boardUse,

            Authentication auth
    ) {
        if (auth == null || auth.getName() == null)
            throw new AccessDeniedException("로그인이 필요합니다.");
        final String actorId = auth.getName();

        // 수행자 권한 체크(관리자만)
        String role = jdbc.queryForObject(
                "SELECT member_role FROM member_tbl WHERE member_id = ?",
                String.class, actorId);
        if (role == null ||
        	    !( role.equalsIgnoreCase("admin") || role.equals("관리자") || role.equals("책임자") )) {
        	    throw new AccessDeniedException("권한이 없습니다.(관리자/책임자만)");
        	}

        // 형식 검증
        if (boardNum != null && !boardNum.isBlank() && !boardNum.matches("^\\d{2}$"))
            return ResponseEntity.ok(ApiResponse.fail(-400, "게시판번호는 숫자 2자리여야 합니다."));
        if (boardUse != null && !(boardUse.equalsIgnoreCase("Y") || boardUse.equalsIgnoreCase("N")))
            return ResponseEntity.ok(ApiResponse.fail(-400, "사용여부는 Y 또는 N만 허용됩니다."));

        // 폼 → DTO (작성자 불변: memberId 세팅 없음)
        BoardUpdateRequest req = new BoardUpdateRequest();
        req.setBoardTitle(boardTitle);
        req.setBoardContent(boardContent);
        req.setBoardNum(boardNum);
        req.setBoardUse(boardUse != null ? boardUse.toUpperCase() : null);

        try {
            Integer rows = boardService.updateBoard(boardId, actorId, req);
            return ResponseEntity.ok(ApiResponse.ok(rows));
        } catch (RuntimeException ex) {
            Throwable cause = ex.getCause();
            if (cause instanceof DuplicateKeyException || cause instanceof DataIntegrityViolationException)
                return ResponseEntity.ok(ApiResponse.fail(-1, "게시판번호가 중복됩니다."));
            String msg = String.valueOf(ex.getMessage());
            if (msg.contains("ORA-00001") || msg.toUpperCase().contains("BOARD_TBL_NUM_UN")
                                          || msg.toUpperCase().contains("BOARD_NUM_UK"))
                return ResponseEntity.ok(ApiResponse.fail(-1, "게시판번호가 중복됩니다."));
            return ResponseEntity.ok(ApiResponse.fail(-500, "서버 오류가 발생했습니다."));
        }
    }

    // --------------------------------------------------------------------
    // 4) 게시판 삭제
    // --------------------------------------------------------------------
    @Operation(
        summary = "게시판 삭제",
        description = "특정 게시판 삭제"
    )
    @DeleteMapping("/{boardId}")
    public ApiResponse<Void> deleteBoard(@PathVariable("boardId") Integer boardId,
                                         Authentication auth) {
        if (auth == null || auth.getName() == null)
            throw new AccessDeniedException("로그인이 필요합니다.");
        final String actorId = auth.getName();

        String role = jdbc.queryForObject(
                "SELECT member_role FROM member_tbl WHERE member_id = ?",
                String.class, actorId);
        if (role == null ||
        	    !( role.equalsIgnoreCase("admin") || role.equals("관리자") || role.equals("책임자") )) {
        	    throw new AccessDeniedException("권한이 없습니다.(관리자/책임자만)");
        	}

        log.info("[CMS][DELETE]/api/cms/boards/{} by {}", boardId, actorId);
        boardService.deleteBoard(boardId, actorId);
        return ApiResponse.ok();
    }
}

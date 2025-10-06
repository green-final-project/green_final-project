package com.gym.controller.user;

import com.gym.common.ApiResponse;
import com.gym.domain.board.BoardResponse;
import com.gym.mapper.xml.BoardQueryMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

/**
 * [사용자] 게시판 단건 조회 전용 컨트롤러
 * - 로그인 없이 접근 가능 (permitAll)
 * - 입력값: boardId (PK)
 * - 출력값: BoardResponse
 */
@Tag(name = "03.Board-User", description = "사용자 게시판 단건 조회 API")
@RestController
@RequestMapping("/api/boards")
@RequiredArgsConstructor
@Slf4j
public class UserBoardController {

    private final BoardQueryMapper boardQueryMapper; // XML 매퍼 직접 사용

    // --------------------------------------------------------------------
    // 단건 조회 (비로그인 허용)
    // --------------------------------------------------------------------
    @CrossOrigin("*")
    @Operation(summary = "게시판 단건 조회",
               description = "게시판 ID를 입력받아 상세 정보를 반환합니다. (비로그인 허용)")
    @GetMapping("/{boardId}")
    public ApiResponse<BoardResponse> getBoardById(
            @Parameter(description = "게시판 PK", example = "1")
            @PathVariable("boardId") Integer boardId) {

        return boardQueryMapper.findBoardById(boardId)   // Optional<Board> 반환
                .map(BoardResponse::from)                // Board → BoardResponse 변환
                .map(ApiResponse::ok)                    // 정상 응답
                .orElseGet(() ->
                        ApiResponse.fail(-404,
                            "해당 게시판을 찾을 수 없습니다. (ID=" + boardId + ")"));
    }
}

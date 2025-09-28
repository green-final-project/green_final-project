/* ============================================================
[전역 예외 처리] GlobalExceptionHandler
- 목적: 컨트롤러 전역 예외를 ApiResponse로 변환
- 금지: 스택트레이스/SQL 등 민감 정보의 응답 노출
- 실전 전 TODO:
  1) 로깅 강화(Log4j2로 에러ID 찍고 응답엔 ID만 제공)
  2) 도메인별 커스텀 예외 매핑 추가
============================================================ */
package com.gym.common.exception; // 예외 패키지

/*------------------------------ 기본 검증(서버 오류) ------------------------------*/
import com.gym.common.ApiResponse;	// 표준 응답
import org.springframework.http.HttpStatus;	// 상태 코드
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*; // 예외 매핑
/*------------------------------ 기본 검증(서버 오류) ------------------------------*/

/*------------------------------ 예약신청 오류 메시지  ------------------------------*/
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.BadSqlGrammarException;
/*------------------------------ 예약신청 오류 메시지  ------------------------------*/

/*----------------------------- 게시글 조회 오류 메시지 ------------------------------*/
import org.springframework.http.ResponseEntity; // 동적 상태코드 응답 생성을 위해 사용
import org.springframework.web.server.ResponseStatusException; // 컨트롤러에서 throw한 상태 예외 타입
/*----------------------------- 게시글 조회 오류 메시지 ------------------------------*/

import org.springframework.dao.DuplicateKeyException;

@RestControllerAdvice(basePackages = "com.gym.controller.user")
public class GlobalExceptionHandler {                           // 전역 예외 어드바이스

	/*------------------------------ 기본 검증(서버 오류) ------------------------------*/
    @ResponseStatus(HttpStatus.BAD_REQUEST)                     // 검증 실패 → 400오류 메시지 출력
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ApiResponse<Void> handleValidation(MethodArgumentNotValidException e){
        String msg = (e.getBindingResult().getFieldError()!=null)
                ? e.getBindingResult().getFieldError().getDefaultMessage()
                : "유효성 검사 실패";
        return ApiResponse.fail(-400, msg);                     // 표준 실패 응답
    }

    /* [250925추가] ResponseStatusException 핸들러
    - 컨트롤러에서 throw new ResponseStatusException(403/404, reason)
      → 그대로 상태코드로 내려 Swagger에서 500으로 보이지 않게 함
    - ApiResponse 포맷 유지. code는 음수 상태코드(-403/-404 등), message는 reason 사용
    */
    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<ApiResponse<Void>> handleResponseStatus(ResponseStatusException e) {
        HttpStatus status = HttpStatus.valueOf(e.getStatusCode().value()); // 원래 상태코드(예: 403)
        String reason = (e.getReason() != null) ? e.getReason() : "요청을 처리할 수 없습니다."; // Alert 메시지
        ApiResponse<Void> body = ApiResponse.fail(-status.value(), reason); // 우리 표준 응답 바디 구성
        return ResponseEntity.status(status).body(body); // 동적 상태코드로 그대로 반환
    }
    
    
    // 핸들러 최우선
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)           // 기타 예외 → 500오류 메시지 출력
    @ExceptionHandler(Exception.class)
    public ApiResponse<Void> handleException(Exception e){
        // TODO(prod): Log4j2로 에러ID와 함께 상세 스택 로깅(응답엔 미노출)
        return ApiResponse.fail(-500, "서버 오류가 발생했습니다."); // 표준 실패 응답
    }
    
    /*------------------------------ 기본 검증(서버 오류) ------------------------------*/
    
    
    
    /*------------------------------ 예약신청 오류 메시지  ------------------------------*/
    // 서비스 레벨에서 우리가 던진 예외(시간 겹침 등) → 409로 그대로 메시지 노출
    @ResponseStatus(HttpStatus.CONFLICT)
    @ExceptionHandler(IllegalStateException.class)
    public ApiResponse<Void> handleIllegalState(IllegalStateException e) {
        return ApiResponse.fail(-409, e.getMessage());
    }

    // DB 제약/트리거로 막힌 경우(오라클 트리거 메시지를 사용자 친화적으로 변경)
    @ResponseStatus(HttpStatus.CONFLICT)
    @ExceptionHandler({ DataIntegrityViolationException.class, BadSqlGrammarException.class })
    public ApiResponse<Void> handleDbConflict_reserv(Exception e) {
        String msg = e.getMessage();

        if (msg != null) {
            if (msg.contains("TRG_RESERVATION_BLOCK_OVERLAP")) {
                msg = "해당 시설의 완료된 예약 시간과 겹칩니다. 다른 시간대를 선택하세요.";
            } else if (msg.contains("ORA-20001")) {
                msg = "요청을 처리할 수 없습니다. 다른 시간대를 선택하세요.";
            } else {
                msg = "요청을 처리할 수 없습니다. 입력 값을 확인하세요.";
            }
        } else {
            msg = "요청을 처리할 수 없습니다.";
        }
        return ApiResponse.fail(-409, msg);
    }
    /*------------------------------ 예약신청 오류 메시지  ------------------------------*/
    
    
    /*--------------------------- 콘텐츠 번호중복 오류 메시지 -----------------------------*/
    // DB 제약/트리거로 막힌 경우(오라클 트리거 메시지를 사용자 친화적으로 변경)
    @ResponseStatus(HttpStatus.CONFLICT)
    @ExceptionHandler(DuplicateKeyException.class) // ⬅️ 여기만 좁힌다
    public ApiResponse<Void> handleDbConflict_contents(DuplicateKeyException e) {
        String msg = e.getMessage();
        if (msg == null && e.getCause() != null) {
            msg = e.getCause().getMessage();
        }

        // UNIQUE 제약명 매칭
        if (msg != null && msg.contains("CONTENTS_TBL_NUM_UN")) {
            msg = "콘텐츠번호가 중복됩니다.";
        } else {
            msg = "요청을 처리할 수 없습니다. 입력 값을 확인하세요.";
        }
        return ApiResponse.fail(-409, msg);
    }
    /*--------------------------- 콘텐츠 번호중복 오류 메시지 -----------------------------*/
    
    /*---------------------------  결제실패 오류 메시지 -----------------------------*/
	// - 예약ID상의 회원ID와 결제신청하려는 회원ID 불일치할 경우 발생 
    // - HTTP 응답코드: 409
	// - 바디 code: -409  (기존 규칙 따름)
	/*
	 * @ResponseStatus(HttpStatus.CONFLICT)
	 * 
	 * @ExceptionHandler(org.springframework.security.access.AccessDeniedException.
	 * class) public ApiResponse<Void>
	 * handleAccessDenied(org.springframework.security.access.AccessDeniedException
	 * e) { return ApiResponse.fail(-409, e.getMessage()); }
	 */
    /*--------------------------- 결제실패 오류 메시지 -----------------------------*/

    
    
}
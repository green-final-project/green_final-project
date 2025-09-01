/* ============================================================
[전역 예외 처리] GlobalExceptionHandler
- 목적: 컨트롤러 전역 예외를 ApiResponse로 변환
- 금지: 스택트레이스/SQL 등 민감 정보의 응답 노출
- 실전 전 TODO:
  1) 로깅 강화(Log4j2로 에러ID 찍고 응답엔 ID만 제공)
  2) 도메인별 커스텀 예외 매핑 추가
============================================================ */
package com.gym.common.exception;                               // 📦 예외 패키지

import com.gym.common.ApiResponse;                              // 📦 표준 응답
import org.springframework.http.HttpStatus;                     // 🌐 상태 코드
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;                // 🌐 예외 매핑

@RestControllerAdvice
public class GlobalExceptionHandler {                           // 🚨 전역 예외 어드바이스

    @ResponseStatus(HttpStatus.BAD_REQUEST)                     // 🔁 검증 실패 → 400
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ApiResponse<Void> handleValidation(MethodArgumentNotValidException e){
        String msg = (e.getBindingResult().getFieldError()!=null)
                ? e.getBindingResult().getFieldError().getDefaultMessage()
                : "유효성 검사 실패";
        return ApiResponse.fail(-400, msg);                     // 🔁 표준 실패 응답
    }

    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)           // 🔁 기타 예외 → 500
    @ExceptionHandler(Exception.class)
    public ApiResponse<Void> handleException(Exception e){
        // TODO(prod): Log4j2로 에러ID와 함께 상세 스택 로깅(응답엔 미노출)
        return ApiResponse.fail(-500, "서버 오류가 발생했습니다."); // 🔁 표준 실패 응답
    }
}
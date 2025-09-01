/* ============================================================
[공통 응답 포맷] ApiResponse
- 목적: 모든 컨트롤러 응답을 동일 포맷(code/message/data)으로 통일
- 금지: 서비스/도메인 로직을 응답 래퍼에 넣지 말 것(표준 포맷만 담당)
- 실전 전 TODO:
  1) 에러코드 표준표 확정(도메인별 -400xx, -500xx 등)
  2) 성공/페이징 응답 전용 팩토리 메서드 확대(요청 시)
============================================================ */
package com.gym.common;                                         // 📦 공통 패키지

import lombok.*;                                                // 🧩 롬복

@Getter @Setter @ToString @NoArgsConstructor @AllArgsConstructor @Builder
public class ApiResponse<T> {                                   // 📦 표준 응답 래퍼(제네릭)
    private int code;       // 0=성공, 음수=오류코드
    private String message; // 설명 메시지(사람이 읽을 문구)
    private T data;         // 실제 데이터(payload)

    public static <T> ApiResponse<T> ok(T data){                // ✅ 성공 응답 생성기
        return ApiResponse.<T>builder().code(0).message("OK").data(data).build();
    }
    public static <T> ApiResponse<T> fail(int code, String msg){ // ✅ 실패 응답 생성기
        return ApiResponse.<T>builder().code(code).message(msg).data(null).build();
    }
}

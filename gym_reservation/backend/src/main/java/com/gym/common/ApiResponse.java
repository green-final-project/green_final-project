package com.gym.common;                              // 📦 공통 응답 패키지

import lombok.*;                                     // 🧩 롬복

@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
public class ApiResponse<T> {

    private int code;        // 0=성공, 음수=실패
    private String message;  // 메시지
    private T data;          // 응답 데이터(없으면 null)

    // ✅ 데이터 있는 성공
    public static <T> ApiResponse<T> ok(T data) {
        return new ApiResponse<>(0, "success", data);
    }

    // ✅ 데이터 없는 성공
    public static ApiResponse<Void> ok() {
        return new ApiResponse<>(0, "success", null);
    }

    // ✅ 실패 응답 (직접 코드+메시지 지정)
    public static <T> ApiResponse<T> fail(int code, String message) {
        return new ApiResponse<>(code, message, null);
    }

    // ✅ 실패 응답 (예외 메시지 그대로 내려줌)
    public static <T> ApiResponse<T> error(Exception e) {
        return new ApiResponse<>(-500, e.getMessage(), null);
    }
}

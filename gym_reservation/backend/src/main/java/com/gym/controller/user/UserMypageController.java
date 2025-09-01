package com.gym.controller.user;                             // 📦 컨트롤러 패키지(경로 유지)

import com.gym.common.ApiResponse;                           // 📦 공통 응답 래퍼
import com.gym.domain.member.Member;                         // 👥 회원 DTO
import com.gym.service.MemberService;                        // 🧠 회원 서비스 계층

// ⬇️ Swagger 문서화(선택이지만 권장: UI에 설명 뜸)
import io.swagger.v3.oas.annotations.Operation;              // 📖 API 요약/설명
import io.swagger.v3.oas.annotations.Parameter;              // 📖 파라미터 설명
import org.springframework.web.bind.annotation.*;            // 🌐 REST 어노테이션
import lombok.RequiredArgsConstructor;                       // 🧩 생성자 주입

/**
 * ============================================================
 * 🚨 [주의] 이 컨트롤러는 Swagger 테스트/배선 점검 전용 코드입니다.
 *
 * 목적:
 * - Swagger UI에서 DB 배선/조회 연결이 정상 동작하는지 확인
 * - 팀원 학습 및 실시간 데모(바이브코딩) 용도
 *
 * 절대 금지:
 * - 정식 기능 개발 시 이 코드를 그대로 복붙/사용 ❌
 *
 * 실제 구현 시:
 * - 반드시 "팀 코드룰" 준수 (Controller → Service → Mapper 계층 분리)
 * - DTO: MemberRequest / MemberResponse 활용
 * - 메서드명/변수명은 시나리오 문서의 코드룰 표를 따라 작성
 * - 예외, 트랜잭션, Validation 적용 필수
 *
 * 요약:
 * 이 코드는 "Swagger 임시 배선 확인"만을 위해 만든 샘플입니다.
 * 운영/실제 구현 시에는 삭제하거나 코드룰에 맞는 정식 MemberController로 교체해야 합니다.
 * ============================================================
 */

@RestController                                   // 🌐 REST 컨트롤러
@RequestMapping("/api/member")                    // 🌐 공통 경로(/api/member/**)
@RequiredArgsConstructor                          // 🧩 final 필드 생성자 자동 생성
public class UserMypageController {

    // 💉 서비스 주입 (매퍼 직접 호출 → 서비스 계층 호출로 변경)
    private final MemberService memberService;    // 🔄 회원 서비스

    /* ============================================================
    [마이페이지] 회원 단건 조회(실제 DB 연동)
    - 목적: member_tbl 단건 조회 → ApiResponse 포맷으로 반환
    - 주의: 실전 전 인증/권한(@PreAuthorize) 및 DTO 변환 예정
    ============================================================ */
    @Operation(summary = "회원 단건 조회", description = "memberId로 member_tbl에서 회원 정보를 DB에서 조회합니다.")
    @GetMapping("/{memberId}")                                // 🌐 GET /api/member/{memberId}
    public ApiResponse<Member> getMemberById(
            @Parameter(description = "회원 ID (예: hong1 ~ hong10)")  // ← Swagger 파라미터 설명
            @PathVariable("memberId") String memberId                // ★ 핵심: PathVariable 이름 명시
    ) {
        Member member = memberService.getMemberById(memberId);       // ✅ 서비스 → 매퍼 → DB 조회
        return ApiResponse.ok(member);                               // ✅ 표준 응답
    }
} // ✅ 클래스 닫는 중괄호

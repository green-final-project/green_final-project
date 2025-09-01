package com.gym.controller;                                           // 📦 컨트롤러 패키지

import org.springframework.web.bind.annotation.GetMapping;            // 🌐 GET 매핑
import org.springframework.web.bind.annotation.PathVariable;          // 🔤 경로 변수 바인딩
import org.springframework.web.bind.annotation.RestController;        // 🌐 REST 컨트롤러
import com.gym.mapper.annotation.MemberMapper;                        // 🗺 매퍼 주입
import com.gym.domain.member.Member;                                  // 🧩 도메인
import lombok.RequiredArgsConstructor;                                // 🧩 생성자 주입

/**
 * ============================================================
 * 🔍 임시 디버그 컨트롤러(매퍼 호출 로그 확인 전용)
 *
 * 목적:
 * - GET /debug/member/{memberId} 호출 시 MyBatis 매퍼가 정상 동작하는지 확인
 * - 콘솔에 SQL 로그 출력 확인용
 *
 * 주의:
 * - 이 코드는 Swagger/DB 배선 점검용 샘플입니다.
 * - 🚫 정식 구현 시 Controller → Service → Mapper 구조와 DTO/예외처리 규칙을 반드시 따를 것.
 * - 운영 배포 전에는 삭제 권장 (외부 노출 불필요).
 * ============================================================
 */
@RestController
@RequiredArgsConstructor
public class DebugController {                                         // 🌐 임시 확인용 컨트롤러

    private final MemberMapper memberMapper;                           // 💉 매퍼 주입

    @GetMapping("/debug/member/{memberId}")                            // 🌐 GET /debug/member/{memberId}
    public String debugFindMember(@PathVariable String memberId) {
        Member member = memberMapper.selectMemberById(memberId);       // ✅ 규칙 반영된 메서드명 사용
        return (member == null)
                ? "NOT FOUND: " + memberId                             // ❌ 결과 없으면
                : "FOUND: " + member.getMemberId();                    // ✅ 결과 있으면
    }
}

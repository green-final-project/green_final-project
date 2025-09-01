package com.gym.smoke;	// 📦 테스트 루트 패키지(운영과 동일 루트 권장)

import com.gym.mapper.annotation.MemberMapper;	// 🗺 매퍼(빈 등록 여부 확인)
import com.gym.domain.member.Member;			// 🧩 도메인(매핑 확인)
import org.junit.jupiter.api.Test;				// ✅ JUnit5 테스트 어노테이션
import org.springframework.beans.factory.annotation.Autowired;	// 💉 스프링 빈 주입
import org.springframework.boot.test.context.SpringBootTest;	// 🚀 스프링 컨텍스트 로딩 테스트
import org.springframework.test.context.TestPropertySource;	// ⚙️ 테스트용 프로퍼티 오버라이드(선택)
import lombok.extern.log4j.Log4j2;				// 📝 Log4j2 로깅

/**
 * 스모크 테스트(테스트 전용)
 * - 목적: 애플리케이션 컨텍스트 로딩 + MyBatis 매퍼 빈 등록 + DB 커넥션/쿼리 동작 여부 확인
 * - 주의:
 *   1) 일부 DB값이 없어도 예외 없이 null이면 정상
 *   2) 운영 코드 오염 방지를 위해 test 소스에만 존재
 */
//@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE) // 🌱 톰캣 미기동(컨텍스트만 로딩)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK) // 🌱 톰캣 미기동(컨텍스트만 로딩)
@TestPropertySource(properties = { // (선택) 테스트 동안에만 MyBatis SQL 로그를 콘솔로 노출
        "mybatis.configuration.log-impl=org.apache.ibatis.logging.stdout.StdOutImpl"
})
@Log4j2
class SmokeTest {

    @Autowired
    MemberMapper memberMapper;                             // 💉 매퍼 빈 주입(스캔/등록 검증)

    @Test
    void contextLoads_and_memberMapper_select_one() {      // ✅ 단일 테스트 메서드(스모크)
        // 🔍 DB에 존재하면 Member 객체, 없으면 null → 예외 없이 통과하면 OK
        Member admin = memberMapper.selectMemberById("hong10"); // ID : hong10으로 설정
        log.info("[SMOKE][TEST] memberMapper.selectMemberById('hong10') => {}", admin);

        // ❗ 굳이 단정문 강제하지 않음(존재/미존재 모두 정상 시나리오)
        //    예외만 없으면 스모크 통과로 간주
    }
}

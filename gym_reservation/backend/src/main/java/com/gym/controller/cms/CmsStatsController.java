//! [설명] CMS 통계 조회 컨트롤러
//! - 프론트엔드 CmsHome.tsx에서 호출하는 /api/cms/stats, /stats/facilities 엔드포인트 담당
//! - 회원, 시설, 게시글, 콘텐츠, 예약(상태별)의 통계 조회 처리

package com.gym.controller.cms; 

import org.springframework.web.bind.annotation.*; // @RestController, @GetMapping, @RequestMapping 등 사용
import org.springframework.beans.factory.annotation.Autowired; // 의존성 주입(Dependency Injection) 어노테이션
import com.gym.mapper.annotation.CmsStatsMapper; // MyBatis 매퍼 인터페이스 import — DB 통계 조회 수행

import java.util.List; // 여러 행(시설별 현황)을 담기 위해 사용
import java.util.Map;  // 단일 Map으로 통계값 반환
import lombok.extern.slf4j.Slf4j; // log.info() 등 로그 출력용 Lombok 어노테이션
import io.swagger.v3.oas.annotations.Operation; // Swagger UI 문서 자동화용 — API 요약 표시
import io.swagger.v3.oas.annotations.tags.Tag;  // Swagger에서 그룹명(카테고리) 정의

@Tag(name = "00.CMS Stats", description = "CMS에서 사이트의 통계정보 제공") 
@Slf4j 
@RestController 
@RequestMapping("/api/cms") 
@CrossOrigin("*") 
public class CmsStatsController { 

    private final CmsStatsMapper cmsStatsMapper; // 매퍼 객체 선언 — 실제 DB 접근 수행자

    // [1] CMS 통계 조회 (전체 카운트 + 예약상태별)
    @Autowired // 스프링이 자동으로 매퍼 구현체 주입 (MyBatis가 자동 생성한 Proxy를 주입함)
    public CmsStatsController(CmsStatsMapper cmsStatsMapper) { // 생성자 주입 방식
        this.cmsStatsMapper = cmsStatsMapper; // 주입된 매퍼를 필드에 저장
    }

    // [2] 시설 전체 예약신청 샅개 비율 통계
    @Operation(summary = "CMS 전체 통계", description = "회원·시설·게시글·콘텐츠·예약(상태별) 통계값 반환")
    @GetMapping("/stats") // GET 요청 시 실행 — /api/cms/stats URL과 매핑
    public Map<String, Object> getStats() { // 단일 Map을 JSON으로 반환 (key-value 형태)
        log.info("[CmsStatsController] CMS 통계 조회 요청 수신"); // 로그: 요청 감지 확인
        Map<String, Object> stats = cmsStatsMapper.selectStats(); // MyBatis 매퍼 호출 — DB에서 통계 쿼리 실행
        log.info("[CmsStatsController] 조회 결과: {}", stats); // DB 결과를 로그로 출력 (백엔드 확인용)
        return stats; // 결과 Map을 그대로 JSON으로 프론트에 반환
    }

    // [3] 시설별 예약신청 샅개 비율 통계
    @Operation(summary = "시설별 예약 상태 통계", description = "5가지 시설별 완료·대기·취소 비율 조회")
    @GetMapping("/dashboard/facility-status")
    public List<Map<String, Object>> getFacilityStatusStats() {
        log.info("[CmsStatsController] 시설별 예약 상태 통계 요청 수신");
        List<Map<String, Object>> list = cmsStatsMapper.selectFacilityStatusStats();
        log.info("[CmsStatsController] 조회 결과: {}", list);
        return list;
    }

}
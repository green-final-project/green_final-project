//! [설명] CMS 통계 조회용 매퍼 인터페이스
//! - 각 테이블의 COUNT 결과를 Map 형태로 반환
//! - member_tbl, facility_tbl, post_tbl, cms_content_tbl, reservation_tbl 기준

package com.gym.mapper.annotation;

import org.apache.ibatis.annotations.Mapper;
import java.util.List;
import java.util.Map;

@Mapper
public interface CmsStatsMapper {
	// [1] CMS 통계 조회용 메서드 (전체 카운트 + 예약상태별)
    Map<String, Object> selectStats(); 
    // [2] 시설 전체 예약신청 샅개 비율 통계 메서드
    List<Map<String, Object>> selectFacilityStats();
    // [3] 시설별 예약신청 샅개 비율 통계 메서드
    List<Map<String, Object>> selectFacilityStatusStats(); //
}
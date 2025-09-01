package com.gym.mapper.xml;                                                // 📦 매퍼 인터페이스

import com.gym.domain.facility.Facility;                                   // 🏟 DTO
import org.apache.ibatis.annotations.Mapper;                               // 🧩 MyBatis
import org.apache.ibatis.annotations.Param;                                // 🧩 바인딩
import java.util.List;                                                     // 📚 목록

/**
 * 시설 목록/검색 매퍼(카테고리 + 사용여부 + 이름 부분검색)
 * - name          : 시설명 부분검색(선택)
 * - category      : 카테고리 필터(선택) [수영장/농구장/풋살장/베드민턴장/볼링장]
 * - facilityUseYn : 'Y'/'N' (선택)
 */


@Mapper
public interface FacilityQueryMapper {

    List<Facility> searchFacilities(
            @Param("name") String name,                    // 시설명 부분검색(선택)
            @Param("facilityType") String facilityType,    // 카테고리(= facility_type, 선택)
            @Param("facilityUseYn") String facilityUseYn   // 사용여부 'Y'/'N'(선택)
    );
}

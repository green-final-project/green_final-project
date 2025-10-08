package com.gym.mapper.xml;

import com.gym.domain.facility.Facility;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import com.gym.domain.member.Member; // [251008] CMS시설 담당강사 조회(admin_type 필터 포함)
import java.util.List;

/** 목록/검색 전용(XML 1:1) */
@Mapper
public interface FacilityQueryMapper {

    /* 검색/페이징/정렬 */
    List<Facility> selectFacilities(
        @Param("name") String name,
        @Param("facilityUse") Boolean facilityUse,
        @Param("page") Integer page,
        @Param("size") Integer size,
        @Param("sort") String sort,   // 예: "name,asc" / "regDate,desc"
        @Param("type") String type // ⚠️ [251001] 카테고리 필터(type) 추가
    );

    long countFacilities(
        @Param("name") String name,
        @Param("facilityUse") Boolean facilityUse,
        @Param("type") String type // ⚠️ [251001] 카테고리 필터(type) 추가
    );
    
    // [251008] CMS시설 담당강사 조회(admin_type 필터 포함)
    List<Member> selectCmsMembers(
    	@Param("adminType") String adminType,
    	@Param("name") String name
    );
    
}

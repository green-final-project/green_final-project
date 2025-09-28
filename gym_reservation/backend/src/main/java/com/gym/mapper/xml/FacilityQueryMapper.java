package com.gym.mapper.xml;

import com.gym.domain.facility.Facility;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
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
        @Param("sort") String sort   // 예: "name,asc" / "regDate,desc"
    );

    long countFacilities(
        @Param("name") String name,
        @Param("facilityUse") Boolean facilityUse
    );
}
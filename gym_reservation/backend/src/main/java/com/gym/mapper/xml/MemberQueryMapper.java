package com.gym.mapper.xml;                          // 📦 XML 매퍼 인터페이스

import com.gym.domain.member.Member;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import java.util.List;

/**
 * 목록/검색 전용(XML 매퍼와 1:1)
 * - namespace: com.gym.mapper.xml.MemberQueryMapper
 */
@Mapper
public interface MemberQueryMapper {
	/** XML 매퍼와 1:1 인터페이스 — namespace 일치 필수 */
    // 키워드/권한 필터 + 페이징
    List<Member> selectMembers(
        @Param("page") Integer page,
        @Param("size") Integer size,
        @Param("keyword") String keyword,
        @Param("role") String role
    );

    long countMembers(
        @Param("keyword") String keyword,
        @Param("role") String role
    );
    
    // [251008 추가] CMS 강사 전용 목록(admin_type 필터)
    List<Member> selectCmsMembers(
        @Param("adminType") String adminType,
        @Param("name") String name
    );
}


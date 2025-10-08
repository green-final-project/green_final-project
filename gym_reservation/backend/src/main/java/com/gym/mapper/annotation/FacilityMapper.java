package com.gym.mapper.annotation;

import org.apache.ibatis.annotations.*;
import com.gym.domain.facility.Facility;

/** 단건 CRUD + exists + use 변경 */
@Mapper
public interface FacilityMapper {

    /* 단건 조회 */
    @Select("""
        SELECT
          f.facility_id         AS facilityId,
          f.facility_name       AS facilityName,
          f.member_id           AS memberId,
          f.instructor_id       AS instructorId,
          f.facility_phone      AS facilityPhone,
          f.facility_content    AS facilityContent,
          f.facility_image_path AS facilityImagePath,
          f.facility_person_max AS facilityPersonMax,
          f.facility_person_min AS facilityPersonMin,
          f.facility_use        AS facilityUse,
          f.facility_reg_date   AS facilityRegDate,
          f.facility_mod_date   AS facilityModDate,
          TO_CHAR(f.facility_open_time,'HH24:MI')  AS facilityOpenTime,
          TO_CHAR(f.facility_close_time,'HH24:MI') AS facilityCloseTime,
          f.facility_money      AS facilityMoney,
          f.facility_type       AS facilityType
        FROM facility_tbl f
        WHERE f.facility_id = #{facilityId}
    """)
    Facility selectFacilityById(@Param("facilityId") Long facilityId);

    /* INSERT — PK는 시퀀스로 직접 세팅, facility_use는 'Y'/'N'으로 변환 */
    @Insert("""
    		  INSERT INTO facility_tbl (
    		    facility_id,
    		    facility_name,
    		    member_id,
    		    instructor_id,
    		    facility_phone,
    		    facility_content,
    		    facility_image_path,
    		    facility_person_max,
    		    facility_person_min,
    		    facility_use,
    		    facility_open_time,
    		    facility_close_time,
    		    facility_money,
    		    facility_type
    		  ) VALUES (
    		    facility_seq.NEXTVAL,
    		    #{facilityName, jdbcType=VARCHAR},
    		    #{memberId, jdbcType=VARCHAR},
    		    #{instructorId, jdbcType=VARCHAR},
    		    #{facilityPhone, jdbcType=VARCHAR},
    		    #{facilityContent, jdbcType=VARCHAR},
    		    #{facilityImagePath, jdbcType=VARCHAR},
    		    #{facilityPersonMax},
    		    #{facilityPersonMin},
    		    /* boolean ↔ 'Y'/'N' */
    		    #{facilityUse, typeHandler=com.gym.config.type.BooleanYNTypeHandler},
    		    TO_DATE(#{facilityOpenTime, jdbcType=VARCHAR}, 'HH24:MI'),
    		    TO_DATE(#{facilityCloseTime, jdbcType=VARCHAR}, 'HH24:MI'),
    		    #{facilityMoney},
    		    #{facilityType, jdbcType=VARCHAR} 
    		  )
    		""")
    		@SelectKey(statement = "SELECT facility_seq.CURRVAL FROM dual",
    		  keyProperty = "facilityId", before = false, resultType = Long.class)
    		int insertFacility(Facility f);

    /* UPDATE — null/빈문자열은 미변경 처리 (Mapper 레벨에서 동적 처리) */
    @Update("""
        <script>
          UPDATE facility_tbl
             <set>
               <if test="facilityName != null and facilityName != ''">
                 facility_name = #{facilityName, jdbcType=VARCHAR},
               </if>

               <if test="memberId != null and memberId != ''">
                 member_id = #{memberId, jdbcType=VARCHAR},
               </if>
               
               <if test="instructorId != null and instructorId != ''">
                 instructor_id = #{instructorId, jdbcType=VARCHAR},
               </if>
               
               <if test="facilityPhone != null and facilityPhone != ''">
                 facility_phone = #{facilityPhone, jdbcType=VARCHAR},
               </if>

               <if test="facilityContent != null and facilityContent != ''">
                 facility_content = #{facilityContent, jdbcType=VARCHAR},
               </if>

               <!-- 이미지: null 또는 빈 문자열이면 SET 자체를 생략 → 기존값 유지 -->
               <if test="facilityImagePath != null and facilityImagePath != ''">
                 facility_image_path = #{facilityImagePath, jdbcType=VARCHAR},
               </if>

               <if test="facilityPersonMax != null">
                 facility_person_max = #{facilityPersonMax},
               </if>

               <if test="facilityPersonMin != null">
                 facility_person_min = #{facilityPersonMin},
               </if>

               <!-- facilityUse이 Boolean 타입(객체)으로 넘어오면 null 검사 -->
               <if test="facilityUse != null">
                 facility_use = #{facilityUse, typeHandler=com.gym.config.type.BooleanYNTypeHandler},
               </if>

               <if test="facilityOpenTime != null and facilityOpenTime != ''">
                 facility_open_time = TO_DATE(#{facilityOpenTime, jdbcType=VARCHAR}, 'HH24:MI'),
               </if>

               <if test="facilityCloseTime != null and facilityCloseTime != ''">
                 facility_close_time = TO_DATE(#{facilityCloseTime, jdbcType=VARCHAR}, 'HH24:MI'),
               </if>

               <if test="facilityMoney != null">
                 facility_money = #{facilityMoney},
               </if>

               <if test="facilityType != null and facilityType != ''">
                 facility_type = #{facilityType, jdbcType=VARCHAR},
               </if>

               facility_mod_date = SYSDATE
             </set>
           WHERE facility_id = #{facilityId}
        </script>
    """)
    int updateFacility(Facility f);


    /* DELETE */
    @Delete("DELETE FROM facility_tbl WHERE facility_id = #{facilityId}")
    int deleteFacilityById(@Param("facilityId") Long facilityId);

    /* 존재 여부 */
    @Select("""
        SELECT CASE WHEN COUNT(1) > 0 THEN 1 ELSE 0 END
        FROM facility_tbl
        WHERE facility_id = #{facilityId}
    """)
    boolean existsFacilityById(@Param("facilityId") Long facilityId);

    /* 사용여부 변경(PATCH) — boolean → 'Y'/'N' */
    @Update("""
        UPDATE facility_tbl
           SET facility_use    = CASE WHEN #{facilityUse} THEN 'Y' ELSE 'N' END,
               facility_mod_date = SYSDATE
         WHERE facility_id     = #{facilityId}
    """)
    int updateFacilityUse(@Param("facilityId") Long facilityId,
                          @Param("facilityUse") boolean facilityUse);
}

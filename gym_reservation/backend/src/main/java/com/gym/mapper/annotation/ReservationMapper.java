package com.gym.mapper.annotation;

import com.gym.domain.reservation.Reservation; // 엔티티(도메인 기준)
import org.apache.ibatis.annotations.*; // MyBatis 어노테이션

@Mapper // MyBatis 매퍼 등록
public interface ReservationMapper {

    // 등록(INSERT) : ORACLE 시퀀스 사용 → INSERT 후 CURRVAL로 resvId 세팅
    // - 널 입력 시 "1111" 오류 방지를 위해 jdbcType 명시
    // - wantDate(LocalDate) → DB는 TIMESTAMP여도 ORACLE이 DATE→TIMESTAMP 암묵 변환 수행
    @Insert("""
        INSERT INTO reservation_tbl (
            resv_id, member_id, facility_id, resv_content, want_date,
            resv_person_count, resv_start_time, resv_end_time
        ) VALUES (
            seq_reservation_id.NEXTVAL,
            #{memberId},                                         -- 회원 ID(필수)
            #{facilityId},                                       -- 시설 ID(필수)
            #{resvContent,    jdbcType=VARCHAR},                 -- 요구사항(선택)
            #{wantDate,       jdbcType=DATE},                    -- LocalDate → DATE 전달
            #{resvPersonCount,jdbcType=NUMERIC},                 -- 신청 인원(필수)
            #{resvStartTime,  jdbcType=TIMESTAMP},               -- LocalDateTime
            #{resvEndTime,    jdbcType=TIMESTAMP}                -- LocalDateTime
        )
        """)
    @SelectKey(statement = "SELECT seq_reservation_id.CURRVAL FROM dual",
               keyProperty = "resvId", before = false, resultType = Long.class)
    int insertReservation(Reservation reservation); // 성공 시 1, reservation.resvId 세팅

    // 소유권 확인(수정/삭제 전 검증): resvId+memberId 일치 여부
    @Select("""
        SELECT CASE WHEN COUNT(1) > 0 THEN 1 ELSE 0 END
          FROM reservation_tbl
         WHERE resv_id = #{resvId}
           AND member_id = #{memberId}
        """)
    boolean existsByIdAndMemberId(@Param("resvId") Long resvId,
                                  @Param("memberId") String memberId);

    // 수정(부분수정): resvId+memberId 일치 시만 반영, null 필드는 미반영
    @Update("""
        <script>
        UPDATE reservation_tbl
        <set>
            <if test="resvContent != null">
                resv_content = #{resvContent, jdbcType=VARCHAR},
            </if>
            <if test="resvPersonCount != null">
                resv_person_count = #{resvPersonCount, jdbcType=NUMERIC},
            </if>
            <if test="resvStatus != null">
                resv_status = #{resvStatus, jdbcType=VARCHAR},
            </if>
        </set>
        WHERE resv_id = #{resvId}
          AND member_id = #{memberId}
        </script>
        """)
    int updateByIdAndMemberId(Reservation reservation); // 정상 1

    // 삭제: resvId+memberId 일치 시에만 삭제
    @Delete("""
        DELETE FROM reservation_tbl
         WHERE resv_id = #{resvId}
           AND member_id = #{memberId}
        """)
    int deleteByIdAndMemberId(@Param("resvId") Long resvId,
                              @Param("memberId") String memberId);
    
    // [250919 신규] 취소신청: resv_cancel='Y' + resv_cancel_reason 업데이트
    // - 조건: resv_id + member_id 일치 시에만 반영
    // - 이미 Y면 영향행 0(에러 아님)
    // - reason은 NULL 가능(jdbcType 명시)
    @Update("""
    	    UPDATE reservation_tbl
    	       SET resv_cancel = 'Y',
    	           resv_cancel_reason = #{resvCancelReason, jdbcType=VARCHAR}
    	     WHERE resv_id   = #{resvId}
    	       AND member_id = #{memberId}
    	       AND NVL(resv_cancel, 'N') = 'N'
    	    """)
    int updateCancelRequest(@Param("resvId") Long resvId,
    						@Param("memberId") String memberId,
    						@Param("resvCancelReason") String resvCancelReason);
}

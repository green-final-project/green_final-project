package com.gym.mapper.annotation;

import com.gym.domain.content.ContentCreateRequest;
import com.gym.domain.content.ContentUpdateRequest;
import com.gym.domain.content.ContentResponse;
import com.gym.domain.content.ContentSearchRequest;

import org.apache.ibatis.annotations.*;

import java.util.List;

/**
 * ContentMapper
 * - contents_tbl 단건 CRUD
 * - INSERT 시 시퀀스 NEXTVAL 사용 후 CURRVAL 조회 가능
 */
@Mapper
public interface ContentMapper {

    /** 콘텐츠 등록 */
    @Insert("""
        INSERT INTO contents_tbl (
            content_id,
            content_title,
            content_content,
            member_id,
            content_use,
            content_num,
            content_type,
            content_reg_date
        ) VALUES (
            seq_content_id.NEXTVAL,
            #{contentTitle},
            #{contentContent},
            #{memberId},
            #{contentUse},
            #{contentNum},
            #{contentType},
            SYSDATE
        )
    """)
    int createContent(ContentCreateRequest request);

    /** ✅ 방금 증가한 시퀀스값(동일 세션) 조회 */
    @Select("SELECT seq_content_id.CURRVAL FROM dual")
    Long getLastContentId();

    /** 콘텐츠 단건 조회 */
    @Select("""
        SELECT
          content_id       AS contentId,
          content_title    AS contentTitle,
          content_content  AS contentContent,
          member_id        AS memberId,
          content_use      AS contentUse,
          content_num      AS contentNum,
          content_type     AS contentType,
          content_reg_date AS contentRegDate,
          content_mod_date AS contentModDate
        FROM contents_tbl
        WHERE content_id = #{contentId}
    """)
    ContentResponse getContentById(@Param("contentId") Long contentId);

    /** 콘텐츠 조건 검색 목록 조회 */
    @Select("""
        <script>
            SELECT
              content_id       AS contentId,
              content_title    AS contentTitle,
              content_content  AS contentContent,
              member_id        AS memberId,
              content_use      AS contentUse,
              content_num      AS contentNum,
              content_type     AS contentType,
              content_reg_date AS contentRegDate,
              content_mod_date AS contentModDate
            FROM contents_tbl
            <where>
                <if test="contentId != null">
                    AND content_id = #{contentId}
                </if>
                <if test="contentTitle != null and contentTitle != ''">
                    AND content_title LIKE '%' || #{contentTitle} || '%'
                </if>
                <if test="memberId != null and memberId != ''">
                    AND member_id = #{memberId}
                </if>
                <if test="contentType != null and contentType != ''">
                    AND content_type = #{contentType}
                </if>
            </where>
            ORDER BY content_id DESC
        </script>
    """)
    List<ContentResponse> listContents(ContentSearchRequest req);


    /** 콘텐츠 수정 */
    @Update("""
        UPDATE contents_tbl
        SET content_title    = #{contentTitle},
            content_content  = #{contentContent},
            content_use      = #{contentUse},
            content_num      = #{contentNum},
            content_type     = #{contentType},
            content_mod_date = SYSDATE
        WHERE content_id = #{contentId}
    """)
    int updateContent(ContentUpdateRequest req);

    /** 콘텐츠 삭제 */
    @Delete("DELETE FROM contents_tbl WHERE content_id = #{contentId}")
    int deleteContentById(@Param("contentId") Long contentId);
}

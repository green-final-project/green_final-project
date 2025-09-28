package com.gym.mapper.annotation;

import com.gym.domain.file.FileUploadRequest;
import com.gym.domain.file.FileResponse;
import org.apache.ibatis.annotations.*;

import java.util.List;

/**
 * FileMapper
 * - file_tbl CRUD (단건 중심)
 * - INSERT 시 seq_file_id.NEXTVAL → CURRVAL 반환
 */
@Mapper
public interface FileMapper {

    /** 파일 업로드 (INSERT + PK 회수) */
	// [250923추가] member_id 
	// [250923변경사항]#{fileExt} → #{fileExt,jdbcType=VARCHAR}
	// [250923변경사항]#{fileSize} → #{fileSize,jdbcType=NUMERIC}
	// [250923변경사항]#{filePath} → #{filePath,jdbcType=VARCHAR}
	// [250923변경사항]#{fileTargetId} →#{fileTargetId,jdbcType=NUMERIC}
    @Insert("""
        INSERT INTO file_tbl (
          file_id,
          member_id,
          file_target_type,
          file_target_id,
          file_name,
          file_path,
          file_type,
          file_ext,
          file_size
        ) VALUES (
          seq_file_id.NEXTVAL,
          #{memberId},
          #{fileTargetType},
          #{fileTargetId,jdbcType=NUMERIC},
          #{fileName},
          #{filePath,jdbcType=VARCHAR},
          #{fileType},
          #{fileExt,jdbcType=VARCHAR},
          #{fileSize,jdbcType=NUMERIC}
        )
    """)
    @SelectKey(statement = "SELECT seq_file_id.CURRVAL FROM dual",
               keyProperty = "fileId",
               before = false,
               resultType = Long.class)
    int uploadFile(FileUploadRequest req);

    /** 특정 대상별 파일 목록 조회 */
    @Select("""
        SELECT
          file_id		   AS fileId,
          member_id        AS memberId,
          file_target_type AS fileTargetType,
          file_target_id   AS fileTargetId,
          file_name        AS fileName,
          file_path        AS filePath,
          file_type        AS fileType,
          file_ext         AS fileExt,
          file_size        AS fileSize,
          file_reg_date    AS fileRegDate
        FROM file_tbl
        WHERE file_target_type = #{targetType}
          AND file_target_id = #{targetId}
        ORDER BY file_id DESC
    """)
    List<FileResponse> listFilesByTarget(@Param("targetType") String targetType,
                                         @Param("targetId") String targetId);

    /** 파일 삭제 */
    @Delete("DELETE FROM file_tbl WHERE file_id = #{fileId}")
    int deleteFileById(@Param("fileId") Long fileId);
}

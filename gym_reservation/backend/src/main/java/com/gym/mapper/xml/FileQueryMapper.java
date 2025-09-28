package com.gym.mapper.xml;

import com.gym.domain.file.FileRequest;
import com.gym.domain.file.FileResponse;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * MyBatis 매퍼 인터페이스
 * - XML 매퍼(FileQueryMapper.xml)와 1:1 매핑
 * - 주의: namespace는 XML의 namespace와 동일한 FQN 여야 함 (com.gym.mapper.xml.FileQueryMapper)
 * - 아래 메서드들은 XML에 정의된 id와 동일해야 합니다.
 */
@Mapper
public interface FileQueryMapper {

    /**
     * 목록/조건 조회 (fileId가 우선 조건)
     * parameter: com.gym.domain.file.FileRequest
     * result: List<FileResponse>
     */
    List<FileResponse> selectFiles(FileRequest request);

    /**
     * 파일명 단건 조회: 기존 방식 유지(파일명으로 최신 1건 조회)
     * parameter: fileName(String)
     * result: FileResponse
     * (원본 보존 — 필요 시 유지)
     */
    FileResponse selectFileByName(@Param("fileName") String fileName);

    /**
     * ★추가: 파일 PK(file_id)로 단건 조회
     * parameter: fileId(Long)
     * result: FileResponse
     * - 미리보기/다운로드에서 파일명 대신 fileId를 사용하도록 변경했으므로 필요
     */
    FileResponse selectFileById(@Param("fileId") Long fileId);

    /**
     * 총 개수
     * parameter: FileRequest 또는 파라미터 맵(타겟타입/타겟ID)
     * result: long
     */
    long countFiles(FileRequest request);
}

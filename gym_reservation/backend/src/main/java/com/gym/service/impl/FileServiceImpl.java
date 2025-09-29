package com.gym.service.impl;

import com.gym.domain.file.*;
import com.gym.mapper.annotation.FileMapper;
import com.gym.mapper.xml.FileQueryMapper;
import com.gym.service.FileService;

import lombok.extern.slf4j.Slf4j;

import org.springframework.stereotype.Service;
import java.util.List;

/**
 * 파일 서비스 구현체
 * - 실제 DB 연동은 Mapper 호출
 */
@Service
// [205923추가]
@Slf4j
public class FileServiceImpl implements FileService {
	
	// 파일 등록/삭제용 매퍼 (어노테이션 기반)
    private final FileMapper fileMapper;
    // 파일 조회용 매퍼 (XML 기반)
    private final FileQueryMapper fileQueryMapper;

    // 생성자 주입 (스프링이 FileMapper, FileQueryMapper를 자동으로 넣어줌)
    public FileServiceImpl(FileMapper fileMapper, FileQueryMapper fileQueryMapper) {
        this.fileMapper = fileMapper;
        this.fileQueryMapper = fileQueryMapper;
    }
    
    // 업로드 기능
    // 파라미터: FileUploadRequest (파일명, 경로, 크기 등)
    // 반환값: int (INSERT 성공 시 영향받은 행 수, 보통 1)
    @Override
    public int uploadFile(FileUploadRequest request) {
        return fileMapper.uploadFile(request);
    }

    
    /*
    // 특정 대상별 파일 조회 기능
    // 파라미터: targetType(대상 구분), targetId(대상 ID)
    // 반환값: List<FileResponse> (대상에 속한 파일 목록)
    @Override
    public List<FileResponse> listFilesByTarget(String targetType, String targetId) {
        return fileMapper.listFilesByTarget(targetType, targetId);
    } */
    
    // 삭제 기능
    // 파라미터: fileId (삭제할 파일의 PK)
    // 반환값: int (DELETE 성공 시 영향받은 행 수, 보통 1)
    @Override
    public int deleteFileById(Long fileId) {
        return fileMapper.deleteFileById(fileId);
    }

		// 파일 목록 조회 기능
    // 파라미터: FileRequest (검색 조건: 파일명, 대상 타입, 페이징 등)
    // 반환값: List<FileResponse> (조회된 파일 목록)
    @Override
    public List<FileResponse> listFiles(FileRequest req) {
    	//[250923추가]
    	log.info("listFiles:{}", req );
        return fileQueryMapper.selectFiles(req);
    }
}

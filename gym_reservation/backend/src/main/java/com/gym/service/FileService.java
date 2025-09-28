package com.gym.service;

import com.gym.domain.file.*;
import java.util.List;

/**
 * 파일 서비스 인터페이스
 * - Controller에서 호출되는 추상 메서드
 */
public interface FileService {
    
	int uploadFile(FileUploadRequest request);	// 파일 등록
    
    int deleteFileById(Long fileId);	// 파일 삭제
    
    // List<FileResponse> listFilesByTarget(String targetType, String targetId);
    List<FileResponse> listFiles(FileRequest req);	// 목록 조회
}

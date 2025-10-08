package com.gym.domain.file;

import lombok.Data;

/**
 * 파일 요청 DTO
 * - 파일 업로드/등록/수정 시 요청 값 전달용
 * - DB 테이블 컬럼명과 동일하게 작성
 */
@Data
public class FileRequest {
    private Long fileId;			// 파일 ID (PK)
    private String memberId;		// [250923] 업로더(회원ID)
    //private String fileTargetId;	// 대상 ID ← ✅ 이 필드가 없어서 오류 발생
    private Long fileTargetId;		// [250923] 대상 고유 ID
    private String fileTargetType;	// 파일이 속한 대상 타입 (예: CONTENT)
    private Long targetId;			// 대상 ID (예: 콘텐츠 ID)
    private String fileName;		// 원본 파일명
    private String filePath;		// 저장 경로
    private Long fileSize;			// 파일 크기
    private String createdBy;		// 등록자 ID

}

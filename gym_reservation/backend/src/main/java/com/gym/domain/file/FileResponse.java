package com.gym.domain.file;

import lombok.*;

import java.time.LocalDateTime;

/**
 * 파일 응답 DTO
 * - DB SELECT 결과를 클라이언트에게 반환
 * - file_tbl 컬럼과 1:1 매핑
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString
public class FileResponse {

    private Long fileId;			// 파일 고유번호 (PK)
    private String memberId;		// [250923] 업로더(회원ID)
    private String fileTargetType;	// 대상 종류
    //private String fileTargetId;	// 대상 ID
    private Long fileTargetId;		// [250923] 대상 고유 ID
    private String fileName;		// 파일명
    private String filePath;		// 저장 경로
    private String fileType;		// 용도
    private String fileExt;			// 확장자
    private Long fileSize;			// 파일 크기
    private LocalDateTime fileRegDate;	// 등록일

}

package com.gym.domain.file;

import lombok.Data;
import java.util.Date;

/**
 * file_tbl 과 매핑되는 엔티티 클래스
 */
@Data
public class File {
    private Long fileId;			// 파일 고유번호 (PK)
    private String memberId;		// [250923] 업로더(회원ID)
    private String fileTargetType;	// 대상 종류 (board/content/facility)
    //private String fileTargetId;	// 대상 ID
    private Long fileTargetId;		// [250923] 대상 고유 ID
    private String fileName;		// 원본 파일명
    private String filePath;		// 저장 경로
    private String fileType;		// 파일 용도
    private String fileExt;			// 확장자
    private Long fileSize;			// 크기 (byte)
    private Date fileRegDate;		// 등록일
}

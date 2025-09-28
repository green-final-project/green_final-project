package com.gym.domain.file;

import lombok.*;

/**
 * 파일 업로드 요청 DTO
 * - Swagger POST /api/files 요청 JSON 과 매핑됨
 * - DB INSERT 에 사용
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString
public class FileUploadRequest {

    private Long fileId;             // PK 회수용 (INSERT 후 @SelectKey 로 CURRVAL 채워짐)
    private String memberId;		// [250923] 업로더(회원ID)
    private String fileTargetType;   // 대상 종류 (board/content/facility 등)
    //private String fileTargetId;  // 대상 고유 ID
    private Long fileTargetId;		// [250923] 대상 고유 ID
    private String fileName;         // 원본 파일명
    private String filePath;         // 저장 경로 (절대/상대/URL)
    private String fileType;         // 파일 용도 ('썸네일' | '본문')
    private String fileExt;          // 확장자 (jpg, png, pdf 등)
    private Long fileSize;           // 파일 크기(byte)
     
}

package com.gym.controller;

import com.gym.common.ApiResponse;                       // 공통 응답 래퍼
import com.gym.domain.file.FileRequest;                 // 조회 요청 DTO
import com.gym.domain.file.FileResponse;                // 조회 응답 DTO
import com.gym.domain.file.FileUploadRequest;           // 업로드 요청 DTO(DDL 컬럼만 사용)
import com.gym.service.FileService;                     // 파일 저장/삭제 서비스
import com.gym.mapper.xml.FileQueryMapper;              // 조회 전용 매퍼(XML)
import io.swagger.v3.oas.annotations.Operation;         // Swagger 문서
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
// ⚠️ Swagger의 @ApiResponse 와 우리 ApiResponse 이름 충돌 방지: 아래 3가지는 FQN 사용 예정
// import io.swagger.v3.oas.annotations.responses.ApiResponse;
// import io.swagger.v3.oas.annotations.media.Content;
// import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.log4j.Log4j2;

import org.springframework.http.HttpHeaders;             // 응답 헤더 구성
import org.springframework.http.HttpStatus;             // 상태 코드
import org.springframework.http.MediaType;              // MIME 타입
import org.springframework.http.ContentDisposition;     // Content-Disposition
import org.springframework.http.ResponseEntity;         // ResponseEntity
import org.springframework.web.bind.annotation.*;        // Rest 컨트롤러 어노테이션
import org.springframework.web.multipart.MultipartFile; // 업로드 파일 타입
import org.springframework.core.io.InputStreamResource;  // 스트리밍 리소스

import java.io.FileInputStream;                         // 파일 스트림
import java.io.IOException;
import java.io.InputStream;
import java.io.File;                                    // [FIX-250923] File 사용 추가 import
//import java.net.URLEncoder;                             // [FIX-250923] 파일명 인코딩
//import java.nio.charset.StandardCharsets;               // [FIX-250923] UTF-8 상수
import java.nio.file.Files;                             // MIME 추론
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

// 🔒 로그인 사용자 ID 추출용 (JWT 필터가 SecurityContext에 Authentication 저장한다는 전제)
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;

// ✅ 설정값 선택 적용을 위한 주입(없으면 자동으로 OS 임시폴더 사용)
import org.springframework.beans.factory.annotation.Value;

@Tag(name = "07.File", description = "첨부파일 API (업로드/조회/삭제/미리보기/다운로드)")
@RestController
@RequestMapping("/api/files")
@Log4j2
public class FileController {

    private final FileService fileService;          // 업로드/삭제 처리
    private final FileQueryMapper fileQueryMapper;  // 조회 전용(XML): 파일명→경로/메타 조회

    @Value("${file.upload-dir:}")                   // ✅ 존재하면 사용, 없으면 빈 문자열
    private String configuredUploadRoot;            //    (고정 경로 강제가 아님)

    // 생성자 주입(정적 호출 방지)
    public FileController(FileService fileService, FileQueryMapper fileQueryMapper) {
        this.fileService = fileService;
        this.fileQueryMapper = fileQueryMapper;
    }
    // [FIX-250923] ⛔ 중복 생성자 제거(두 개가 동시에 있으면 빈 주입 충돌/컴파일 경고 유발)
    // public FileController(FileQueryMapper fileQueryMapper) {
    //     this.fileQueryMapper = fileQueryMapper;
    // }

    // ---------------------------------------------------------------------
    // 0) 멀티파트(파일 선택) 업로드 — Swagger에서 파일 버튼 노출
    //    - PK는 시퀀스 자동 증가(컨트롤러 입력 불필요)
    //    - 업로더(member_id)는 로그인 토큰에서 자동 주입
    // ---------------------------------------------------------------------
    @Operation(summary = "파일 업로드(Multipart)", description = "파일 선택으로 업로드. PK는 시퀀스 자동 증가.")
    @PostMapping(path = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ApiResponse<Integer> uploadFileMultipart(
            @Parameter(description = "업로드할 파일", required = true)
            @RequestParam("file") MultipartFile file,                      // 실제 파일

            @Parameter(description = "파일 대상 구분(content/board/facility 등)", required = true)
            @RequestParam("fileTargetType") String fileTargetType,         // DDL: file_target_type NOT NULL

            // [250923수정사항] Swagger에서 'PK 입력' 오해 방지: 파일 자신의 PK 아님. 필수값으로 전환(요구 반영)
            @Parameter(description = "연결 대상 ID(예: 게시글ID, 파일 PK 아님)", required = true)
            @RequestParam(value = "fileTargetId") Long fileTargetId,       // DDL: file_target_id (NOT NULL로 운영)

            @Parameter(description = "파일 용도(본문/썸네일), 미입력 시 '본문'", required = false)
            @RequestParam(value = "fileType", required = false) String fileType // DDL CHECK('썸네일','본문')
    ) throws IOException {

        // (0) 로그인 회원ID 추출(토큰→SecurityContext) ---------------------------
        final String memberId = getLoginMemberId(); // 예: "hong1"
        if (memberId == null || memberId.isBlank()) {
            // 인증 필수: 토큰 없으면 업로드 불가
            throw new AuthenticationCredentialsNotFoundException("인증 필요: 로그인 후 업로드하세요.");
        }

        // (1) 업로드 파일 메타데이터 추출 ---------------------------------------
        final String originalName = file.getOriginalFilename(); // DDL: file_name
        final long size = file.getSize();                       // DDL: file_size
        String fileExt = "";
        if (originalName != null && originalName.lastIndexOf('.') > -1) {
            fileExt = originalName.substring(originalName.lastIndexOf('.') + 1); // DDL: file_ext
        }

        // (2) DDL CHECK 보정: '썸네일'/'본문' 외 값 또는 null → '본문'
        if (fileType == null || (!"본문".equals(fileType) && !"썸네일".equals(fileType))) {
            fileType = "본문";
        }

        // Oracle은 빈 문자열("")을 NULL로 처리 → NOT NULL 컬럼 보호 위해 실제 경로 생성
        String savedRelPath; // DB에 넣을 경로(상대 또는 절대)
        {
            java.time.LocalDate now = java.time.LocalDate.now();
            String y = String.valueOf(now.getYear());
            String m = String.format("%02d", now.getMonthValue());

            // ✅ 고정 경로 강제 금지: ① 설정값이 있으면 그 경로 사용, ② 없으면 OS 임시폴더 사용
            final Path root;
            if (configuredUploadRoot != null && !configuredUploadRoot.isBlank()) {
                root = Paths.get(configuredUploadRoot).toAbsolutePath().normalize();   // 설정값 사용(운영 편의)
            } else {
                String tmp = System.getProperty("java.io.tmpdir");                     // OS 임시폴더
                root = Paths.get(tmp, "app_uploads").toAbsolutePath().normalize();     // 환경 무관 안전
            }

            Files.createDirectories(root);                 // 루트 생성(부모 포함)
            Path dir  = root.resolve(y).resolve(m);        // 연/월
            Files.createDirectories(dir);                  // 경로 보장

            // 파일명 안전화(경로침투 방지)
            String baseName = (originalName == null || originalName.isBlank()) ? "unnamed" : originalName;
            baseName = baseName.replace("\\", "/");
            if (baseName.contains("/")) baseName = baseName.substring(baseName.lastIndexOf('/') + 1);
            baseName = baseName.replaceAll("[\\r\\n\\t]", "");

            String uuidName = java.util.UUID.randomUUID().toString() + "_" + baseName; // 중복 방지
            Path dest = dir.resolve(uuidName);

            file.transferTo(dest.toFile()); // 실제 저장
            savedRelPath = dest.toString().replace(File.separatorChar, '/');
        }

        // (3) DTO 구성(DDL 존재 컬럼만) -----------------------------------------
        FileUploadRequest req = new FileUploadRequest();
        req.setMemberId(memberId);            // 업로더 회원ID
        req.setFileTargetType(fileTargetType);
        req.setFileTargetId(fileTargetId);    // [요구 반영] 필수 입력으로 운영
        req.setFileName(originalName);
        req.setFileType(fileType);
        req.setFileExt(fileExt);
        req.setFileSize(size);
        req.setFilePath(savedRelPath);        // NOT NULL 보장

        // (4) 업로드 처리(INSERT(seq_file_id.NEXTVAL))
        int affected = fileService.uploadFile(req);
        return ApiResponse.ok(affected);
    }

    // ---------------------------------------------------------------------
    // 1) (기존) JSON 업로드 — 유지(비활성화)
    // ---------------------------------------------------------------------
    /*
    @Operation(summary = "파일 업로드(JSON)", description = "파일 메타데이터를 JSON으로 업로드(호환 유지).")
    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    public ApiResponse<Integer> uploadFile(@RequestBody FileUploadRequest request) {
        return ApiResponse.ok(fileService.uploadFile(request));
    }
    */

    // ---------------------------------------------------------------------
    // 2) 파일 목록 조회 — 기존 유지
    // ---------------------------------------------------------------------
    @Operation(summary = "파일 목록", description = "파일 ID/파일명/파일 종류(content/board/facility 등)로 검색합니다.")
    @GetMapping
    public ApiResponse<List<FileResponse>> listFiles(
            @Parameter(description = "파일 ID") @RequestParam(name = "fileId", required = false) Long fileId,
            @Parameter(description = "파일명(부분일치)") @RequestParam(name = "fileName", required = false) String fileName,
            @Parameter(description = "파일 종류(content/board/facility 등)") @RequestParam(name = "fileTargetType", required = false) String fileTargetType
    ) {
        FileRequest req = new FileRequest();
        req.setFileId(fileId);
        req.setFileName(fileName);
        req.setFileTargetType(fileTargetType);
        return ApiResponse.ok(fileService.listFiles(req));
    }

    // ---------------------------------------------------------------------
    // 3) 파일 미리보기 — /files/{fileId}/preview (inline)
    //    ★수정: 경로/파라미터를 파일명에서 파일ID 기준으로 변경 (fileId로 조회하여 실제 경로/메타 사용)
    // ---------------------------------------------------------------------
    @Operation(summary = "파일 미리보기", description = "브라우저에서 바로 열 수 있도록 inline으로 바이너리 응답합니다.")
    @GetMapping(value = "/{fileId}/preview")
    public ResponseEntity<InputStreamResource> previewById(
            @Parameter(description = "파일ID(PK)", required = true)
            @PathVariable("fileId") Long fileId   // ★수정: 기존 filename(String) → fileId(Long)
    ) throws IOException {

        // (1) DB에서 fileId로 경로/메타 조회  ★수정: selectFileById 사용
        FileResponse fileInfo = fileQueryMapper.selectFileById(fileId); // ★수정
        if (fileInfo == null || fileInfo.getFilePath() == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }

        // (2) 실제 경로에서 파일 스트림 열기
        Path path = Paths.get(fileInfo.getFilePath());
        if (!Files.exists(path)) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
        InputStream in = new FileInputStream(path.toFile());

        // (3) MIME 추론(없으면 octet-stream)
        String mime = Files.probeContentType(path);
        if (mime == null || mime.isBlank()) mime = MediaType.APPLICATION_OCTET_STREAM_VALUE;

        // (4) 헤더 구성 — inline
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType(mime));

        // 파일명은 DB의 원본명 사용(인코딩 처리 필요)
        String originalFileName = fileInfo.getFileName() == null ? path.getFileName().toString() : fileInfo.getFileName();
        // ContentDisposition의 filename은 파이어폭스/사파리/등 브라우저별 차이를 고려할 수 있으나,
        // 여기서는 기본 inline + 원본파일명 설정(인코딩은 아래 다운로드 로직과 동일 방식을 참고)
        headers.setContentDisposition(ContentDisposition.inline().filename(originalFileName).build());

        return new ResponseEntity<>(new InputStreamResource(in), headers, HttpStatus.OK);
    }

    // ---------------------------------------------------------------------
    // 4) 파일 다운로드 — /api/files/download (attachment)
    //    ★수정: fileName 파라미터 대신 fileId 파라미터로 변경
    // ---------------------------------------------------------------------
    @Operation(
            summary = "파일 다운로드(파일ID로 조회)",
            description = "DB의 file_tbl에서 파일ID로 검색해, 저장 경로(file_path)의 실제 파일을 첨부로 내려줍니다."
    )
    @GetMapping(value = "/download")
    public void downloadById(
            HttpServletRequest request,
            HttpServletResponse response,
            @Parameter(description = "다운로드할 파일의 PK(fileId)", required = true)
            @RequestParam(name = "fileId") Long fileId   // ★수정: 기존 fileName(String) → fileId(Long)
    ) throws Exception {

        // 0) 로그기록
        log.info("다운로드 요청 fileId:{}", fileId); // ★수정: 로그 메시지 변경

        // 1) DB 조회: fileId로 단건 조회
        FileResponse file = fileQueryMapper.selectFileById(fileId); // ★수정
        if (file == null || file.getFilePath() == null) {
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
            return;
        }

        // 2) 실제 파일 존재 확인
        File f = new File(file.getFilePath());
        if (!f.exists() || !f.isFile()) {
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
            return;
        }

        // 3) MIME 추론 (없으면 octet-stream)
        String mimeType = java.net.URLConnection.guessContentTypeFromName(f.getName());
        if (mimeType == null || mimeType.isBlank()) {
            mimeType = "application/octet-stream";
        }

        // 4) 브라우저별 한글 파일명 처리 (예시 코드 방식 반영)
        String ua = request.getHeader("User-Agent");
        String original = file.getFileName();
        String contentDisposition;

        if (ua != null && (ua.contains("MSIE") || ua.contains("Trident"))) {
            // IE 계열: URLEncoder + 공백 보정
            String enc = java.net.URLEncoder.encode(original, java.nio.charset.StandardCharsets.UTF_8.name())
                            .replaceAll("\\+", "%20");
            contentDisposition = "attachment;filename=" + enc + ";";
        } else {
            // 그외: RFC 5987(UTF-8) + 기본 filename
            String enc = java.net.URLEncoder.encode(original, java.nio.charset.StandardCharsets.UTF_8.name())
                            .replaceAll("\\+", "%20");
            contentDisposition = "attachment; filename=\"" + original + "\"; filename*=UTF-8''" + enc;
        }

        // 5) 헤더 세팅 (예시 코드 스타일)
        response.setHeader("Content-Disposition", contentDisposition);
        response.setHeader("Content-Transfer-Encoding", "binary");
        response.setHeader("Pragma", "no-cache");
        response.setHeader("Expires", "-1");
        response.setContentType(mimeType);
        response.setContentLengthLong(f.length());

        // 6) 스트리밍
        try (InputStream in = new java.io.BufferedInputStream(new java.io.FileInputStream(f))) {
            org.springframework.util.FileCopyUtils.copy(in, response.getOutputStream());
        }
    }

    // ---------------------------------------------------------------------
    // 5) 파일 삭제 — 기존 유지
    // ---------------------------------------------------------------------
    @Operation(summary = "파일 삭제", description = "파일 ID로 파일을 삭제합니다.")
    @DeleteMapping("/{fileId}")
    public ApiResponse<Integer> deleteFileById(
            @Parameter(description = "삭제할 파일 ID", required = true)
            @PathVariable("fileId") Long fileId) {
        return ApiResponse.ok(fileService.deleteFileById(fileId));
    }

    // ---------------------------------------------------------------------
    // 🔒 공용: 로그인 회원ID 추출 (JWT 필터가 Authentication 주입해야 동작)
    // ---------------------------------------------------------------------
    private String getLoginMemberId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication(); // 현재 스레드 인증
        if (auth == null || !auth.isAuthenticated()) return null;                     // 인증 안됨
        return auth.getName();                                                        // username(=memberId)
    }
}

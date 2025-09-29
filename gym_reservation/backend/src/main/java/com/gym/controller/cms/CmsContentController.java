
package com.gym.controller.cms;

import com.gym.common.ApiResponse;
import com.gym.domain.content.ContentCreateRequest;
import com.gym.domain.content.ContentResponse;
import com.gym.domain.content.ContentSearchRequest;
import com.gym.domain.content.ContentUpdateRequest;
import com.gym.security.dto.SecuRoleDTO;
import com.gym.security.dto.SecuUserDTO;
import com.gym.service.ContentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import io.swagger.v3.oas.annotations.media.Schema;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/cms/contents")
@RequiredArgsConstructor
@Tag(name = "04.Contents-CMS", description = "CMS 콘텐츠 관리(목록/등록/수정/삭제)")
@Log4j2
public class CmsContentController {

	private final ContentService contentService;

	/**
	 * 콘텐츠 목록 조회(GET) - 입력: memberId(작성자ID), contentTitle(콘텐츠명), page, size - 모두 선택
	 * 입력(미입력 시 전체 조회) - 페이징은 컨트롤러에서 subList로 간단 처리
	 */
	@Operation(summary = "콘텐츠 목록", description = "작성자ID/콘텐츠명/페이지/사이즈로 조회(미입력 시 전체)")
	@GetMapping
	public ApiResponse<Map<String, Object>> listContents(
			@RequestParam(value = "memberId", required = false) String memberId,
			@RequestParam(value = "contentTitle", required = false) String contentTitle,
			@RequestParam(value = "page", defaultValue = "0") int page,
			@RequestParam(value = "size", defaultValue = "10") int size) {
		if (page < 0)
			page = 0;
		if (size <= 0)
			size = 10;

		// 컨트롤러에서 DTO 생성 → 서비스 시그니처 준수
		ContentSearchRequest req = new ContentSearchRequest();
		req.setMemberId(memberId);
		req.setContentTitle(contentTitle);

		// 서비스 호출(시그니처 그대로)
		List<ContentResponse> all = contentService.listContents(req);

		// 간단 페이징
		int total = all.size();
		int from = Math.min(page * size, total);
		int to = Math.min(from + size, total);
		List<ContentResponse> items = all.subList(from, to);

		// 응답 payload
		Map<String, Object> payload = new LinkedHashMap<>();
		payload.put("items", items);
		payload.put("total", total);
		payload.put("page", page);
		payload.put("size", size);
		payload.put("hasNext", to < total);

		return ApiResponse.ok(payload);
	}

	/**
	 * 콘텐츠 등록 (POST) - 입력: 콘텐츠 제목, 내용, 번호 - 콘텐츠 구분은 이용안내와 상품/시설안내 중 선택 -
	 * memberId(작성자ID)는 자동으로 로그인한 계정 ID로 등록
	 */
	@Operation(summary = "콘텐츠 등록", description = "텍스트박스 입력 폼으로 등록(작성자ID는 로그인ID로 고정)")
	@PostMapping(consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
	public ResponseEntity<ApiResponse<Long>> createContent(
			// 주의사항!! @RequestParam에서 defaultValue를 쓰면 필수입력사항(*)+빨간색 텍스트 적용이 안됨, 그 점 고려해야 함

			@Parameter(name = "contentTitle", description = "콘텐츠 제목", required = true, 
					   schema = @Schema(type = "string", example = "제목"))
					   @RequestParam(value = "contentTitle", required = true) String contentTitle,
					   
			@Parameter(name = "contentContent", description = "콘텐츠 내용", required = true,
					   schema = @Schema(type = "string", example = "내용"))
					   @RequestParam(value = "contentContent", required = true) String contentContent,

			@Parameter(name = "contentType", description = "콘텐츠 구분(이용안내 / 상품/시설안내)",
					   schema = @Schema(type = "string", allowableValues = {"이용안내", "상품/시설안내" }, example = "이용안내"))
					   @RequestParam("contentType") String contentType,

			@Parameter(name = "contentUse", description = "사용여부(Y/N)", 
					   schema = @Schema(type = "string", allowableValues = {"Y", "N" }, example = "Y"))
					   @RequestParam("contentUse") String contentUse,

			@Parameter(name = "contentNum", description = "콘텐츠번호(중복 불가, 2자리)", required = true,
					   schema = @Schema(type = "string", example = "00"))
					   @RequestParam(value = "contentNum", required = true) Integer contentNum,

			Authentication auth) {
		
		// TODO----------------- 로그인 및 권한 로그 검토 -----------------
		// TODO SecurityContextHolder.getContext()=SecurityContext를 기점으로
		// TODO 토큰으로 인증된 정보를 가져옴
		Authentication auths = SecurityContextHolder.getContext().getAuthentication();

		// TODO 로그인한 사용자 정보
		Object principal = auths.getPrincipal();
		log.info("/api/cms/contents/ post:{}", principal); // 해당 컨트롤럴의 @RequestMapping에 있는 경로 붙어넣기

		// TODO 로그인 여부
		boolean loginYN = auths.isAuthenticated();
		log.info("/api/cms/contents/ post loninY/N:{}", loginYN); // 해당 컨트롤럴의 @RequestMapping에 있는 경로 붙어넣기

		// TODO 권한(ROLE) 정보 가져오기 (admin인지, user인지)
		SecuUserDTO secuUserDTO = (SecuUserDTO) principal;
		Collection<SecuRoleDTO> coll = (Collection<SecuRoleDTO>) secuUserDTO.getAuthorities();
		log.info("role:" + ((SecuRoleDTO) coll.toArray()[0]).getAuthority());
		
		ResponseEntity<ApiResponse<Long>> responseEntity = null;

		if (auth == null || auth.getName() == null)
			throw new AccessDeniedException("로그인이 필요합니다.");
		String loginId = auth.getName(); // 작성자ID = 로그인ID

		ContentCreateRequest req = new ContentCreateRequest();
		req.setContentTitle(contentTitle);
		req.setContentContent(contentContent);
		req.setMemberId(loginId);
		req.setContentType(contentType);
		req.setContentUse(contentUse);
		req.setContentNum(contentNum);

		try {
			Long pk = contentService.createContent(req);
			// return ApiResponse.ok(pk);
			responseEntity = new ResponseEntity<>(ApiResponse.ok(pk), HttpStatus.OK);
		} catch (RuntimeException ex) {
			Throwable cause = ex.getCause();
			// 1) 스프링 매핑 예외로 들어온 경우
			// 가독성 + 테스트를 위해서 ResponseEntity 기준으로 변경함
			if (cause instanceof DuplicateKeyException || cause instanceof DataIntegrityViolationException) {
				responseEntity = new ResponseEntity<>(ApiResponse.fail(-1, "콘텐츠번호가 중복됩니다."),
						HttpStatus.CONFLICT);
				throw new ResponseStatusException(HttpStatus.CONFLICT, "콘텐츠번호가 중복됩니다.", ex);
			}
			// 2) 드라이버가 원문만 던지는 경우(ORA-00001 또는 제약명 매칭)
			// 가독성 + 테스트를 위해서 ResponseEntity 기준으로 변경함
			String msg = ex.getMessage();
			log.info("errror msg:" + msg);
			if (msg != null && (msg.contains("ORA-00001") || msg.contains("CONTENTS_TBL_NUM_UN"))) {
				responseEntity = new ResponseEntity<>(ApiResponse.fail(-1, "콘텐츠번호가 중복됩니다."), HttpStatus.OK);
			}
		}
		return responseEntity;
	}


	/** 수정(PUT, application/x-www-form-urlencoded) */
	@Operation(summary = "콘텐츠 수정", description = "수정할 콘텐츠 번호 입력 후, 텍스트박스 입력 폼으로 수정(작성자ID는 로그인ID로 고정)")
	@PutMapping(value = "/{contentId}", consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
	public ResponseEntity<ApiResponse<Integer>> updateContent(@PathVariable("contentId") Long contentId,

	        @Parameter(name = "contentTitle", description = "콘텐츠 제목(미입력 시 기존값 유지)", required = false,
	        	       schema = @Schema(type = "string", example = "제목"))
	        	       @RequestParam(value = "contentTitle", required = false) String contentTitle,

	        @Parameter(name = "contentContent", description = "콘텐츠 내용(미입력 시 기존값 유지)", required = false,
	        		   schema = @Schema(type = "string", example = "내용"))
	       			   @RequestParam(name = "contentContent", required = false) String contentContent,

	        @Parameter(name = "contentType", description = "콘텐츠 구분(이용안내 / 상품/시설안내)",
			   		   schema = @Schema(type = "string", allowableValues = {"이용안내", "상품/시설안내" }, example = "이용안내"))
			   		   @RequestParam(name = "contentType", required = false) String contentType,

			@Parameter(name = "contentUse", description = "사용여부(Y/N)", 
			   		   schema = @Schema(type = "string", allowableValues = {"Y", "N" }, example = "Y"))
			   		   @RequestParam(name = "contentUse", required = false) String contentUse,

	        @Parameter(name = "contentNum", description = "콘텐츠번호(중복 불가, 미입력 시 기존값 유지)", required = false,
	        		   schema = @Schema(type = "string", example = "00"))
	        		   @RequestParam(name = "contentNum", required = false) Integer contentNum,

	        Authentication auth) {

	    // ---------- 로그인 및 권한 로그(등록과 동일 포맷) ----------
	    var auths = SecurityContextHolder.getContext().getAuthentication();
	    Object principal = auths != null ? auths.getPrincipal() : null;
	    log.info("/api/cms/contents/ put:{}", principal);
	    boolean loginYN = auths != null && auths.isAuthenticated();
	    log.info("/api/cms/contents/ put loninY/N:{}", loginYN);
	    if (principal instanceof com.gym.security.dto.SecuUserDTO s) {
	        var coll = (java.util.Collection<com.gym.security.dto.SecuRoleDTO>) s.getAuthorities();
	        if (!coll.isEmpty()) {
	            log.info("role:" + ((com.gym.security.dto.SecuRoleDTO) coll.toArray()[0]).getAuthority());
	        }
	    }
	    // -------------------------------------------------------

	    if (auth == null || auth.getName() == null) {
	        throw new AccessDeniedException("로그인이 필요합니다.");
	    }
	    String loginId = auth.getName(); // 작성자ID는 로그인ID로 고정

	    // 1) 기존 데이터 조회 (NULL로 들어온 항목은 기존값 유지하기 위함)
	    var curr = contentService.getContentById(contentId);
	    if (curr == null) {
	        // UI 일관성 위해 200으로 내려도 되고, 필요하면 NOT_FOUND로 바꿔도 됨
	        return ResponseEntity.ok(ApiResponse.fail(-404, "콘텐츠가 존재하지 않습니다."));
	    }

	    // 2) 업데이트 요청 DTO 구성 (NULL 또는 빈 문자열 -> 기존값 대입)
	    ContentUpdateRequest req = new ContentUpdateRequest();
	    req.setContentId(contentId);
	    req.setMemberId(loginId); // 트리거가 UPDATE 시 작성자 권한 검사하므로 로그인ID로 고정

	    req.setContentTitle(orKeep(contentTitle, curr.getContentTitle()));
	    req.setContentContent(orKeep(contentContent, curr.getContentContent()));
	    req.setContentType(orKeep(contentType, curr.getContentType()));
	    req.setContentUse(orKeep(contentUse, curr.getContentUse()));
	    req.setContentNum((contentNum != null) ? contentNum : curr.getContentNum());

	    try {
	        int affected = contentService.updateContent(req);
	        return ResponseEntity.ok(ApiResponse.ok(affected));
	    } catch (RuntimeException ex) {
	        // 등록과 동일하게: 제약 위반을 200 OK + fail 코드로 바인딩
	        Throwable cause = ex.getCause();
	        if (cause instanceof DuplicateKeyException || cause instanceof DataIntegrityViolationException) {
	            return ResponseEntity.ok(ApiResponse.fail(-1, "콘텐츠번호가 중복됩니다."));
	        }

	        String msg = ex.getMessage();
	        log.info("update error msg: {}", msg);
	        if (msg != null && (msg.contains("ORA-00001") || msg.contains("CONTENTS_TBL_NUM_UN"))) {
	            return ResponseEntity.ok(ApiResponse.fail(-1, "콘텐츠번호가 중복됩니다."));
	        }

	        // 기타 알 수 없는 예외
	        return ResponseEntity.ok(ApiResponse.fail(-500, "서버 오류가 발생했습니다."));
	    }
	}

	// CmsContentController 내부에 유틸 추가
	private static String orKeep(String incoming, String current) {
	    return (incoming == null || incoming.trim().isEmpty()) ? current : incoming;
	}


	/** 삭제(DELETE) */
	@Operation(summary = "콘텐츠 삭제", description = "PK로 삭제")
	@DeleteMapping("/{contentId}")
	public ApiResponse<Integer> deleteContent(@PathVariable("contentId") Long contentId) {
		return ApiResponse.ok(contentService.deleteContentById(contentId));
	}
}

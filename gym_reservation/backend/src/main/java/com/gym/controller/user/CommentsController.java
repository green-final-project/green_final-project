package com.gym.controller.user;

import com.gym.domain.comments.CommentsCreateRequest; // DTO: 생성/수정 요청
import com.gym.domain.comments.CommentsResponse; // DTO: 조회 응답
import com.gym.service.CommentsService; // 서비스 인터페이스
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

import org.springframework.http.MediaType; // 폼 입력(consumes) 지정
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication; // 로그인 사용자 조회
import org.springframework.security.core.GrantedAuthority; // 권한 문자열 조회
import org.springframework.security.core.context.SecurityContextHolder; // 보안 컨텍스트 접근
import org.springframework.web.bind.annotation.*;

import java.util.Collection;
import java.util.List;

import com.gym.service.PostService; // 게시글 서비스 주입[250925추가]

/**
 * 댓글 컨트롤러
 * - 변경 범위: 컨트롤러만 수정 (Service/Impl/Mapper 불변)
 * - 등록: 폼 입력으로 전환, memberId는 로그인ID로 강제 (폼에 memberId 없음)
 * - 수정: 작성자 본인만 가능 (로그인ID == 댓글의 memberId)
 * - 삭제: 작성자 본인만, 단 관리자 권한 보유자는 타인 댓글도 삭제 가능
 */
@Tag(name = "13.Comment")
@RestController
@RequestMapping("/api/posts/{postId}/comments")
@RequiredArgsConstructor
public class CommentsController {

    private final CommentsService commentsService;
    private final PostService postService; // 게시글 존재 여부 확인용[250925추가]
    
    /** 현재 로그인ID(username)를 반환, 비로그인 시 null */
    private String currentLoginId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication(); // 보안 컨텍스트에서 인증 객체 획득
        return (auth == null) ? null : auth.getName(); // username = 회원ID
    }

    /** 관리자 권한 여부 판정(ROLE_ADMIN / "관리자" / "최고관리자" 등 문자열 포함 시 true) */
    private boolean hasAdminRole() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication(); // 인증 객체
        if (auth == null) return false;
        Collection<? extends GrantedAuthority> roles = auth.getAuthorities(); // 권한 목록
        if (roles == null) return false;
        for (GrantedAuthority ga : roles) {
            String r = ga.getAuthority();
            if (r == null) continue;
            String up = r.toUpperCase();
            // 프로젝트 내 권한 문자열을 폭넓게 수용(ROLE_ADMIN / 관리자 / 최고관리자)
            //if (up.contains("ADMIN") || r.contains("관리자") || r.contains("최고관리자")) {
            if (up.contains("ADMIN") || r.contains("관리자") || r.contains("책임자")) { // [251002]
                return true;
            }
        }
        return false;
    }

    // ------------------------------------------------------------------
    // 1) 등록(폼 입력) — memberId는 로그인ID로 강제, 폼에서는 내용만 받음
    // ------------------------------------------------------------------
    @Operation(summary = "댓글 등록(폼 입력)", description = "memberId는 로그인ID로 고정되며 폼에서 받지 않습니다.")
    @PostMapping(consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    public ResponseEntity<Long> createComments(
            @PathVariable("postId") Long postId, // URL 경로의 게시글ID
            @Parameter(description = "댓글 내용", required = true)
            @RequestParam("commentContent") String commentContent	// 폼 파라미터(내용만 입력)
    ) {
    	// ---------------- 차단 기능 ----------------
    	// 로그인 여부 체크
    	String loginId = currentLoginId(); // 로그인ID 획득
        if (loginId == null) return ResponseEntity.status(401).build(); // 비로그인 차단
        // 게시글 유무 체크
        if (postService.getPostById(postId) == null) {
            return ResponseEntity.status(404).build(); // 존재하지 않는 게시글 등록할 경우 차단
        }
        
        // 서비스 시그니처 준수: createComments(CommentsCreateRequest request)
        CommentsCreateRequest req = new CommentsCreateRequest(); // DTO 인스턴스 생성
        // 이하 세터명은 DTO 표준 형태로 가정(필드명: postId, memberId, commentContent)
        req.setPostId(postId); // 게시글ID 설정
        req.setMemberId(loginId); // 작성자ID = 로그인ID
        req.setContent(commentContent); // 내용 설정

        Long createdId = commentsService.createComments(req); // 서비스 호출(변경 없음)
        return ResponseEntity.ok(createdId); // 생성된 댓글ID 반환
    }
    
    
    

    // ------------------------------------------------------------------
    // 2) 목록 조회 — 시그니처 유지(getCommentsByPost)
    // ------------------------------------------------------------------
    @Operation(summary = "댓글 목록 조회", description = "특정 게시글의 댓글 전체 조회")
    @GetMapping
    public ResponseEntity<List<CommentsResponse>> getCommentsByPost(@PathVariable("postId") Long postId) {
        return ResponseEntity.ok(commentsService.getCommentsByPost(postId)); // 서비스 호출
    }

    // ------------------------------------------------------------------
    // 3) 수정(폼 입력) — 작성자 본인만 가능 (로그인ID == 댓글의 memberId)
    // ------------------------------------------------------------------
    @Operation(summary = "댓글 수정(폼 입력)", description = "작성자 본인만 수정 가능합니다.")
    @PutMapping(value = "/{commentsId}", consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    public ResponseEntity<Void> updateComments(
            @PathVariable("postId") Long postId, // 경로의 게시글ID(검증용)
            @PathVariable("commentsId") Long commentsId, // 수정 대상 댓글ID
            @Parameter(description = "수정할 댓글 내용", required = true)
            @RequestParam("commentContent") String commentContent // 폼 파라미터(내용만 입력)
    ) {
        String loginId = currentLoginId(); // 로그인ID
        if (loginId == null) return ResponseEntity.status(401).build(); // 비로그인 차단

        // 단건 조회로 소유자 확인 (시그니처 유지: getCommentsById)
        CommentsResponse target = commentsService.getCommentsById(commentsId);  // 대상 댓글 조회
        if (target == null || !postId.equals(target.getPostId())) { // 게시글-댓글 매칭 검증
            return ResponseEntity.status(404).build(); // 대상 없음
        }
        if (!loginId.equals(target.getMemberId())) { // 본인 여부 확인
            return ResponseEntity.status(403).build(); // 본인이 아니면 거부
        }

        // 서비스 시그니처 준수: updateCommentsByMember(String memberId, Long commentsId, CommentsCreateRequest request)
        CommentsCreateRequest req = new CommentsCreateRequest(); // DTO 인스턴스
        req.setPostId(postId); // 게시글ID
        req.setMemberId(loginId); // 작성자(회원)ID
        req.setContent(commentContent); // 수정 내용

        commentsService.updateCommentsByMember(loginId, commentsId, req);// 서비스 호출
        return ResponseEntity.ok().build(); // 200 OK(바디 없음)
    }

    // ------------------------------------------------------------------
    // 4) 삭제 — 기본: 본인만 / 예외: 관리자 권한은 타인 댓글 삭제 허용
    // ------------------------------------------------------------------
    @Operation(summary = "댓글 삭제", description = "작성자 본인만 삭제 가능, 관리자 권한은 타인 댓글도 삭제 가능")
    @DeleteMapping("/{commentsId}")
    public ResponseEntity<Void> deleteComments(
            @PathVariable("postId") Long postId, // 경로의 게시글ID(검증용)
            @PathVariable("commentsId") Long commentsId // 삭제 대상 댓글ID
    ) {
        String loginId = currentLoginId(); // 로그인ID
        if (loginId == null) return ResponseEntity.status(401).build(); // 비로그인 차단

        // 단건 조회로 소유자/게시글 매칭 확인
        CommentsResponse target = commentsService.getCommentsById(commentsId); // 대상 댓글
        if (target == null || !postId.equals(target.getPostId())) { // 게시글-댓글 매칭 검증
            return ResponseEntity.status(404).build(); // 대상 없음
        }

        // 권한 판정: 본인이거나, 관리자 권한 보유 시 삭제 허용
        boolean owner = loginId.equals(target.getMemberId()); // 본인 여부
        boolean admin = hasAdminRole(); // 관리자 여부
        if (!owner && !admin) return ResponseEntity.status(403).build(); // 권한 없음

        commentsService.deleteComments(commentsId); // 서비스 호출(시그니처 유지)
        return ResponseEntity.ok().build(); // 200 OK
    }
}

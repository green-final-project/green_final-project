package com.gym.controller.cms;

//도메인/DTO
import com.gym.domain.comments.CommentsResponse; // [250925] 댓글 응답 DTO
import com.gym.domain.post.PostResponse; // 게시글 DTO(요청/응답 겸용)

//서비스
import com.gym.service.CommentsService; // [250925] 댓글 서비스(삭제 시 선삭제 호출)
import com.gym.service.PostService; // 게시글 서비스(목록/단건/등록/수정/삭제/조회수 증가)

//Swagger 문서화
import io.swagger.v3.oas.annotations.Operation; // 스웨거 요약
import io.swagger.v3.oas.annotations.Parameter; // 파라미터 문서화
import io.swagger.v3.oas.annotations.media.Schema; // 스키마/허용값 문서화
import io.swagger.v3.oas.annotations.tags.Tag; // 태그 그룹

//세션/요청(조회수 중복 방지 옵션용)
import jakarta.servlet.http.HttpServletRequest; // [250925추가사항] 상세조회 세션 중복방지용 request
import jakarta.servlet.http.HttpSession; // [250925추가사항] 세션

//스프링/보안/웹
import lombok.RequiredArgsConstructor; // 생성자 주입
import org.springframework.http.HttpStatus; // HTTP 상태코드
import org.springframework.http.MediaType; // 요청 Content-Type 지정
import org.springframework.security.core.Authentication; // 인증 주체
import org.springframework.security.core.GrantedAuthority; // 권한 문자열
import org.springframework.security.core.context.SecurityContextHolder; // 보안 컨텍스트
import org.springframework.web.bind.annotation.*; // @RestController 등
import org.springframework.web.server.ResponseStatusException; // 예외→상태코드 변환

import java.util.*; // List, Set, Collection, Locale 등

/**
 * CMS용 게시글 컨트롤러
 * - 폼 전송 기반 등록/수정/삭제 + 조회(목록/단건)
 * - 흐름: Controller(CmsPostController) → Service(PostService/CommentsService) → Mapper → SQL
 * - 정책(CMS):
 *   1) 작성자ID는 로그인ID 자동 세팅(등록 시 1회), 수정 시 불변
 *   2) 수정/삭제는 작성자 본인 또는 관리자/책임자만
 *   3) 비밀글은 작성자 또는 관리자/책임자만
 *   4) CMS는 공지 여부 입력 허용, postType은 "일반/공지" 허용
 */
@RestController
@RequestMapping("/api/cms/boards/{boardId}/posts")
@RequiredArgsConstructor
@Tag(name = "10.Post-CMS", description = "CMS용 게시글 API (폼 전송 + 조회)")
public class CmsPostController {

    private final PostService postService; // 게시글 서비스(Controller→Service 진입점)
    private final CommentsService commentsService; // 댓글 서비스(삭제 시 선삭제 호출)

    /** 목록 조회
     * Controller.listPosts → PostService.getPostsByBoard → Mapper → SQL
     */
    @Operation(summary = "CMS 게시판별 게시글 목록 조회")
    @GetMapping
    public List<PostResponse> listPosts(
            @Parameter(description = "게시판ID(경로변수)") @PathVariable("boardId") Long boardId,
            @Parameter(description = "페이지 번호(기본 1)") @RequestParam(name = "page", defaultValue = "1") int page,
            @Parameter(description = "페이지 크기(기본 10)") @RequestParam(name = "size", defaultValue = "10") int size,
            @Parameter(description = "제목/내용 검색어(선택)") @RequestParam(name = "keyword", required = false) String keyword,
            @Parameter(description = "공지글만 조회 여부(선택)") @RequestParam(name = "notice", required = false) Boolean notice
    ) {
        return postService.getPostsByBoard(boardId, page, size, keyword, notice); // Service 호출
    }

    /** 단건 조회 + 비밀글 접근제어 + 조회수 증가
     * Controller.getPost → PostService.getPostById → 접근검사 → PostService.increaseViewCount
     */
    @Operation(summary = "CMS 게시글 단건 조회")
    @GetMapping("/{postId}")
    public PostResponse getPost(
            @Parameter(description = "게시판ID(경로변수)") @PathVariable("boardId") Long boardId,
            @Parameter(description = "게시글ID(경로변수)") @PathVariable("postId") Long postId,
            HttpServletRequest request // 세션 중복방지 옵션에서 사용 가능
    ) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication(); // 인증 주체 조회
        String loginId = (auth != null ? auth.getName() : null); // 로그인ID 또는 null
        boolean adminOrManager = hasAnyAuthority(auth, "관리자", "책임자", "ROLE_ADMIN", "ROLE_MANAGER", "admin", "manager"); // 관리자/책임자 여부

        PostResponse post = postService.getPostById(postId); // 단건 조회(Service→Mapper→SQL)

        // 게시판-게시글 매칭 검증
        if (post == null || !post.getBoardId().equals(boardId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "요청한 게시판에 속한 게시글이 아닙니다.");
        }

        // 비밀글 접근 제한(작성자 또는 관리자/책임자만)
        if (Boolean.TRUE.equals(post.getPostSecret())) {
            boolean writer = (loginId != null && loginId.equals(post.getMemberId()));
            if (!(writer || adminOrManager)) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "비밀게시글은 작성자 또는 관리자/책임자만 조회할 수 있습니다.");
            }
        }

        // [250925] 게시글 조회수 기능 추가: CMS에서도 단건 조회 시 조회수 증가
        // Controller → PostService.increaseViewCount(postId) → Mapper → SQL(update)
        postService.increaseViewCount(postId);

        /* 세션 기반 중복 방지(비활성화)
        HttpSession session = request.getSession();
        String key = "viewed_post_" + postId;
        if (session.getAttribute(key) == null) {
            postService.increaseViewCount(postId);
            session.setAttribute(key, System.currentTimeMillis());
        }
        */

        return post; // 단건 응답
    }

    /** 등록(폼 전송)
     * Controller.createPost → PostService.createPost → Mapper → SQL(insert)
     * CMS 정책: 공지 입력 허용, postType은 "일반/공지" 허용
     */
    @Operation(summary = "CMS 게시글 등록(폼)")
    @PostMapping(consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    public Long createPost(
            @Parameter(description = "게시판ID(경로변수)") @PathVariable("boardId") Long boardId,
            @Parameter(name = "postTitle", description = "게시글 제목(필수)") @RequestParam("postTitle") String postTitle,
            @Parameter(name = "postContent", description = "게시글 내용(필수)") @RequestParam("postContent") String postContent,
            @Parameter(
                    name = "postType",
                    description = "게시글 구분(selectbox: 일반/공지)",
                    schema = @Schema(type = "string", allowableValues = {"일반","공지"}, example = "일반")
            ) @RequestParam(name = "postType", required = false) String postType,
            @Parameter(
                    name = "postNotice",
                    description = "공지 여부(true/false, 기본 false)",
                    schema = @Schema(type = "string", allowableValues = {"true","false"}, example = "false")
            ) @RequestParam(name = "postNotice", required = false, defaultValue = "false") boolean postNotice,
            @Parameter(
                    name = "postSecret",
                    description = "비밀글 여부(true/false, 기본 false)",
                    schema = @Schema(type = "string", allowableValues = {"true","false"}, example = "false")
            ) @RequestParam(name = "postSecret", required = false, defaultValue = "false") boolean postSecret
    ) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication(); // 인증 주체
        if (auth == null || auth.getName() == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "로그인이 필요합니다.");
        }
        String loginId = auth.getName(); // 작성자ID
        String normalizedType = normalizeType(postType); // "일반/공지" 값 정규화

        // Controller→Service 전달 DTO 구성
        PostResponse req = PostResponse.builder()
                .boardId(boardId)
                .postTitle(postTitle)
                .postContent(postContent)
                .memberId(loginId)
                .postNotice(postNotice)
                .postSecret(postSecret)
                .postType(normalizedType)
                .build();

        return postService.createPost(req); // 등록 처리(Service→Mapper→SQL)
    }

    /** 수정(폼 전송)
     * Controller.updatePost → PostService.updatePost → Mapper → SQL(update)
     * CMS 정책: 작성자ID 불변, 공지/일반 select 허용
     */
    @Operation(summary = "CMS 게시글 수정(폼)")
    @PutMapping(value = "/{postId}", consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    public String updatePost(
            @Parameter(description = "게시판ID(경로변수)") @PathVariable("boardId") Long boardId,
            @Parameter(description = "게시글ID(경로변수)") @PathVariable("postId") Long postId,
            @Parameter(name = "postTitle", description = "게시글 제목(선택: 미입력 시 기존값 유지)") @RequestParam(name = "postTitle", required = false) String postTitle,
            @Parameter(name = "postContent", description = "게시글 내용(선택: 미입력 시 기존값 유지)") @RequestParam(name = "postContent", required = false) String postContent,
            @Parameter(
                    name = "postType",
                    description = "게시글 구분(selectbox: 일반/공지)",
                    schema = @Schema(type = "string", allowableValues = {"일반","공지"})
            ) @RequestParam(name = "postType", required = false) String postType,
            @Parameter(
                    name = "postNotice",
                    description = "공지 여부(선택, true/false)",
                    schema = @Schema(type = "string", allowableValues = {"true","false"})
            ) @RequestParam(name = "postNotice", required = false) Boolean postNotice,
            @Parameter(
                    name = "postSecret",
                    description = "비밀글 여부(선택, true/false)",
                    schema = @Schema(type = "string", allowableValues = {"true","false"})
            ) @RequestParam(name = "postSecret", required = false) Boolean postSecret
    ) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication(); // 인증 주체
        if (auth == null || auth.getName() == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "로그인이 필요합니다.");
        }
        String loginId = auth.getName(); // 로그인ID
        boolean adminOrManager = hasAnyAuthority(auth, "관리자", "책임자", "ROLE_ADMIN", "ROLE_MANAGER", "admin", "manager"); // 권한 체크

        PostResponse origin = postService.getPostById(postId); // 원글 조회
        if (origin == null || !origin.getBoardId().equals(boardId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "요청한 게시판에 속한 게시글이 아닙니다.");
        }

        String writerId = origin.getMemberId(); // 작성자ID
        if (!adminOrManager && !loginId.equals(writerId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "작성자 본인 또는 관리자/책임자만 수정할 수 있습니다.");
        }

        String normalizedType = (postType == null ? null : normalizeType(postType)); // 선택 입력 시 정규화
        // 수정 요청 DTO 구성(미입력 필드는 Service/Mapper에서 기존값 유지 처리 가정)
        PostResponse req = PostResponse.builder()
                .postId(postId)
                .boardId(boardId)
                .postTitle(postTitle)
                .postContent(postContent)
                .postNotice(postNotice)
                .postSecret(postSecret)
                .postType(normalizedType)
                .memberId(writerId)
                .build();

        postService.updatePost(req); // 수정 처리(Service→Mapper→SQL)
        return "게시글이 수정되었습니다.(CMS)";
    }

    /** 삭제
     * Controller.deletePost → (선삭제) CommentsService.deleteComments* → (본삭제) PostService.deletePostById
     * 정책: 본인 또는 관리자/책임자만 삭제 가능
     */
    @Operation(summary = "CMS 게시글 삭제")
    @DeleteMapping("/{postId}")
    public String deletePost(
            @Parameter(description = "게시판ID(경로변수)") @PathVariable("boardId") Long boardId,
            @Parameter(description = "게시글ID(경로변수)") @PathVariable("postId") Long postId
    ) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication(); // 인증 주체
        if (auth == null || auth.getName() == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "로그인이 필요합니다.");
        }
        String loginId = auth.getName(); // 로그인ID
        boolean adminOrManager = hasAnyAuthority(auth, "관리자", "책임자", "ROLE_ADMIN", "ROLE_MANAGER", "admin", "manager"); // 권한 체크

        PostResponse origin = postService.getPostById(postId); // 원글 조회
        if (origin == null || !origin.getBoardId().equals(boardId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "요청한 게시판에 속한 게시글이 아닙니다.");
        }

        String writerId = origin.getMemberId(); // 작성자ID
        if (!adminOrManager && !loginId.equals(writerId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "작성자 본인 또는 관리자/책임자만 삭제할 수 있습니다.");
        }

        // [250925] 게시글 삭제 시 댓글 일괄 삭제: 댓글 조회 후 루프 삭제(연쇄 정합성 확보)
        // Controller → CommentsService.getCommentsByPost → CommentsService.deleteComments(id) → Mapper → SQL
        List<CommentsResponse> comments = commentsService.getCommentsByPost(postId);
        for (CommentsResponse c : comments) {
            commentsService.deleteComments(c.getCommentsId());
        }

        postService.deletePostById(postId); // 본삭제(Service→Mapper→SQL)
        return "게시글이 삭제되었습니다.(CMS)";
    }

    /** postType 허용값 정규화 유틸("일반"/"공지")
     * 입력값이 유효하지 않으면 "일반"으로 고정
     */
    private String normalizeType(String postType) {
        if (postType == null) return "일반";
        String v = postType.trim();
        if ("일반".equals(v) || "공지".equals(v)) return v;
        return "일반";
    }

    /** 권한 문자열 포함 여부 검사 유틸
     * 다국어/ROLE_ 접두/소문자까지 폭넓게 허용
     * 사용처: getPost/updatePost/deletePost의 관리자/책임자 판정
     */
    private boolean hasAnyAuthority(Authentication auth, String... roles) {
        if (auth == null) return false;
        Collection<? extends GrantedAuthority> authorities = auth.getAuthorities();
        if (authorities == null) return false;

        Set<String> wanted = new HashSet<>();
        for (String r : roles) {
            if (r == null) continue;
            String s = r.trim().toLowerCase(Locale.ROOT);
            wanted.add(s);
            if (!s.startsWith("role_")) {
                wanted.add("role_" + s);
            }
        }
        // 한글 별칭 매핑
        wanted.add("관리자");
        wanted.add("책임자");

        for (GrantedAuthority ga : authorities) {
            String g = ga.getAuthority();
            if (g == null) continue;
            String s = g.trim();
            if (wanted.contains(s)) return true;
            String sl = s.toLowerCase(Locale.ROOT);
            if (wanted.contains(sl)) return true;
            if (!sl.startsWith("role_") && wanted.contains("role_" + sl)) return true;
        }
        return false;
    }
}

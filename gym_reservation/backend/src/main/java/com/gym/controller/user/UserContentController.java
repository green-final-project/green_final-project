package com.gym.controller.user;

import com.gym.common.ApiResponse;
import com.gym.domain.content.ContentResponse;
import com.gym.service.ContentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/contents")
@RequiredArgsConstructor
@Tag(name = "04.Contents-User", description = "사용자용 콘텐츠 단건 조회")
public class UserContentController {

    private final ContentService contentService;

    /** 콘텐츠 단건 조회(permitAll) */
    @Operation(summary = "콘텐츠 단건 조회", description = "콘텐츠 ID로 조회")
    @GetMapping("/{contentId}")
    public ApiResponse<ContentResponse> getContentById(@PathVariable("contentId") Long contentId) {
        return ApiResponse.ok(contentService.getContentById(contentId));
    }
}

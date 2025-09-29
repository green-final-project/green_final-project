package com.gym.service;

import com.gym.domain.content.*;
import java.util.List;

import org.springframework.security.access.annotation.Secured;
import org.springframework.security.access.prepost.PreAuthorize;

/**
 * 콘텐츠 서비스 인터페이스
 * - Controller에서 호출하는 추상 메서드
 */
public interface ContentService {
	//@PreAuthorize("hasRole('ADMIN')")
	//@Secured("ROLE_ADMIN")
	Long createContent(ContentCreateRequest request);  				  // 등록 (PK 반환)
    
    
    //@PreAuthorize("hasRole('ADMIN')") 
    List<ContentResponse> listContents(ContentSearchRequest request); // 목록 조회
    
    //@PreAuthorize("hasRole('ADMIN')")
    int updateContent(ContentUpdateRequest request);                  // 수정
    
    //@PreAuthorize("hasRole('ADMIN')")
    int deleteContentById(Long contentId);                            // 삭제

    ContentResponse getContentById(Long contentId);                   // 단건 조회
}

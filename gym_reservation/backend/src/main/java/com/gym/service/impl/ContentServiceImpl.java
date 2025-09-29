package com.gym.service.impl;

import com.gym.domain.content.*;
import com.gym.mapper.annotation.ContentMapper;
import com.gym.service.ContentService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.DuplicateKeyException;

import java.util.List;

/**
 * 콘텐츠 서비스 구현체
 * - DB 트랜잭션 단위로 CRUD 수행
 * - INSERT 성공 시에만 PK 증가 후 Swagger 응답으로 반환
 */
@Service
@RequiredArgsConstructor
public class ContentServiceImpl implements ContentService {

    private final ContentMapper contentMapper;

    /**
     * 콘텐츠 등록 (PK 반환)
     * - INSERT 성공 시 오라클 시퀀스 NEXTVAL → CURRVAL 사용
     * - 실패 시 롤백 처리되어 PK 증가하지 않음
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createContent(ContentCreateRequest request) {
    	
    	Long result = -1L; //[추가] 비정상(-1L)으로 지정
    	
        try {
            int affected = contentMapper.createContent(request);
            if (affected == 0) {
                throw new RuntimeException("콘텐츠 등록 실패");
            }
            //return contentMapper.getLastContentId();
            result = contentMapper.getLastContentId();
        } catch (DuplicateKeyException e) {
            // 그대로 던져서 ControllerAdvice가 409로 매핑하게 한다
            throw e;
            
        } catch (DataIntegrityViolationException e) {
            // Oracle 제약명으로 중복 상황 식별해 DuplicateKey로 승격
            String msg = e.getMostSpecificCause() != null ? e.getMostSpecificCause().getMessage() : "";
            if (msg.contains("CONTENTS_TBL_NUM_UN") || msg.contains("ORA-00001")) {
                throw new DuplicateKeyException("콘텐츠번호가 중복됩니다.(구현)", e);
            }
            // 나머지는 그대로 던져 전역 핸들러가 처리
            throw e;
            
        }
        return result;
    }
    
    

    /**
     * 콘텐츠 단건 조회
     * - CMS 관리자/책임자/관리자 권한 필요
     * - 일반 사용자도 조회 가능(권한 구분은 Controller 단에서 처리)
     */
    @Override
    public ContentResponse getContentById(Long contentId) {
        return contentMapper.getContentById(contentId);
    }

    /**
     * 콘텐츠 목록 조회
     * - ContentSearchRequest 조건이 Mapper에 구현되어 있어야 함
     */
    @Override
    public List<ContentResponse> listContents(ContentSearchRequest request) {
        // [old] 단순 전체 조회
        // return contentMapper.listContents(request);

        // [250917] 검색 조건이 없다면 전체조회, 조건이 있다면 조건검색
        if (request == null) {
            return contentMapper.listContents(new ContentSearchRequest()); // 빈 조건으로 전체조회
        }
        return contentMapper.listContents(request);
    }

    /**
     * 콘텐츠 수정
     */
    @Override
    @Transactional(rollbackFor = Exception.class) // [250917] 트랜잭션 보장 추가
    public int updateContent(ContentUpdateRequest request) {
        int affected = contentMapper.updateContent(request);
        if (affected == 0) {
            throw new RuntimeException("콘텐츠 수정 실패: ID=" + request.getContentId());
        }
        return affected;
    }

    /**
     * 콘텐츠 삭제
     */
    @Override
    @Transactional(rollbackFor = Exception.class) // [250917] 트랜잭션 보장 추가
    public int deleteContentById(Long contentId) {
        int affected = contentMapper.deleteContentById(contentId);
        if (affected == 0) {
            throw new RuntimeException("콘텐츠 삭제 실패: ID=" + contentId);
        }
        return affected;
    }
    
    
}

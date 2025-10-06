package com.gym.service.impl;

import com.gym.common.PageResponse;
import com.gym.domain.facility.*;
import com.gym.mapper.annotation.FacilityMapper;
import com.gym.mapper.xml.FacilityQueryMapper;
import com.gym.service.FacilityService;

import lombok.extern.log4j.Log4j2;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Log4j2
@Service
public class FacilityServiceImpl implements FacilityService {

    private final FacilityMapper facilityMapper; // 시설 CRUD용 Mapper(INSERT, UPDATE, DELETE, SELECT 단건)
    private final FacilityQueryMapper facilityQueryMapper; //시설 검색/카운트용 Mapper(XML 기반)

    // 생성자 주입: Spring이 Mapper구현체를 자동 주입
    public FacilityServiceImpl(FacilityMapper facilityMapper, // 어노테이션매퍼 (등록/수정/삭제)
    						   FacilityQueryMapper facilityQueryMapper // xml매퍼 (간단조회)
    ) {
        this.facilityMapper = facilityMapper;
        this.facilityQueryMapper = facilityQueryMapper;
    }

    @Override
    @Transactional(rollbackFor = Exception.class) // 트랜잭션 시작: INSERT 실패/예외 시 전체 롤백
    public Long createFacility(FacilityCreateRequest req) {
        // [1] 필수값 검증: 시설명 누락 방지(빈 문자열 포함)
        if (req.getFacilityName() == null || req.getFacilityName().isBlank())
            throw new IllegalArgumentException("facilityName is required");

        // [2] 필수값 검증: 최대/최소 인원 모두 있어야 함
        if (req.getFacilityPersonMax() == null || req.getFacilityPersonMin() == null)
            throw new IllegalArgumentException("personMax/personMin are required");

        // [3] 도메인 규칙: 최소인원 <= 최대인원 보장
        if (req.getFacilityPersonMin() > req.getFacilityPersonMax())
            throw new IllegalArgumentException("facilityPersonMin <= facilityPersonMax");

        // [4] VO 생성: 요청값 → 엔티티 필드 매핑(+ 기본값 적용)
        Facility f = Facility.builder()
                .facilityName(req.getFacilityName())                 // 시설명(필수)
                .memberId(req.getMemberId())                         // 담당자ID(허용 스펙에 따라 필수/선택)
                .facilityPhone(req.getFacilityPhone())               // 연락처
                .facilityContent(req.getFacilityContent())           // 시설 설명
                .facilityImagePath(req.getFacilityImagePath())       // 이미지 경로
                .facilityPersonMax(req.getFacilityPersonMax())       // 최대 인원(필수)
                .facilityPersonMin(req.getFacilityPersonMin())       // 최소 인원(필수)
                .facilityUse(req.getFacilityUse() != null ?          // 사용여부(null 허용 시 기본 true)
                        req.getFacilityUse() : true)
                .facilityOpenTime(req.getFacilityOpenTime())         // 개장시간 "HH:mm" 문자열(매퍼에서 TO_DATE 처리)
                .facilityCloseTime(req.getFacilityCloseTime())       // 폐장시간 "HH:mm" 문자열
                .facilityMoney(req.getFacilityMoney() != null ?      // 이용료(null → 0L 기본)
                        req.getFacilityMoney() : 0L)
                .facilityType(req.getFacilityType())                 // 시설종류(스펙 값 사용)
                .build();

        // [5] INSERT 수행: 시퀀스(NEXTVAL)로 PK 생성, @SelectKey로 방금 PK(CURRVAL) 주입
        int affected = facilityMapper.insertFacility(f);

        // [6] 영향 행수 검증: 1건이 아니면 예외(트랜잭션 롤백)
        if (affected != 1) throw new RuntimeException("INSERT failed");

        // [7] PK 반환: mapper의 @SelectKey가 f.facilityId 에 CURRVAL 주입 완료
        return f.getFacilityId();
    }


    //------------------------------------ 단건조회(시설정보상세) ----------------------------------
    @Override
    @Transactional(readOnly = true)	// 읽기 전용 
    public FacilityResponse getFacilityById(Long facilityId) { // 단건조회, 반환: FacilityResponse DTO
        Facility f = facilityMapper.selectFacilityById(facilityId); // Mapper 호출, f는 시설의 약어를 띈 변수
        if (f == null) throw new RuntimeException("NOT_FOUND: facility " + facilityId); // 없으면 예외
        return toResp(f); // Facility → FacilityResponse 변환
    }

    //------------------------------------ 목록조회 ----------------------------------
    @Override
    @Transactional(readOnly = true) // 일기 전용
    public PageResponse<FacilityResponse> searchFacilities( // 조건 검색 기능으로 변환
	    	//String name, Boolean facilityUse, Integer page, Integer size, String sort) {
	    	String name,		// 시설명 
	    	Boolean facilityUse,// 시설 사용여부
	    	Integer page,		// 시설 목록 페이지 번호
	    	Integer size,		// 시설 목록 페이지의 크기
	    	String sort,		// 시설 목록 정렬 
	    	String type 		// 시설 카테고리 ⚠️[251001] 검색 카테고리(category) 필터 추가
	    	) {
    		
    		log.info("searchFacilities:{}", name); // 백엔드 콘솔 검색로그 기록 ⚠️ [251001] 로그 추가
    		
    		// 목록 검색 필터링 기능, 시설명, 사용여부, 페이지, 페이지크기, 정렬기준, 카테고리
	        List<Facility> items = facilityQueryMapper.selectFacilities
	        		(name, facilityUse, page, size, sort, type); // ⚠️ [251001] 검색 카테고리 필터 추가
	        long total = facilityQueryMapper.countFacilities(name, facilityUse, type); // ⚠️ [251001] 검색 카테고리 필터 추가
	        
	        log.info("total:{}", total); // 모든 백엔드 로그기록 출력 ⚠️ [251001] 로그 추가
	        
	        // PageResponse.of(변환된 DTO 목록, 총건수, 페이지번호, 페이지크기)
	        return PageResponse.of(items.stream().map(this::toResp).toList(), total, page, size); 
	    }

    //------------------------------------ 시설정보 수정 ----------------------------------
    @Override
    @Transactional(rollbackFor = Exception.class) // 트랜젝션 시작함 
    public void updateFacility(Long facilityId, FacilityUpdateRequest req) {// 시설 정보 수정, 변환값 없음
        Facility target = facilityMapper.selectFacilityById(facilityId);	// 수정대상 조회
        if (target == null) throw new RuntimeException("NOT_FOUND: facility " + facilityId); // 없으면 예외처리함

        // 입력된 값만 업데이트 (null은 무시)
        if (req.getFacilityName() != null)       target.setFacilityName(req.getFacilityName());
        if (req.getMemberId() != null)           target.setMemberId(req.getMemberId());
        if (req.getFacilityPhone() != null)      target.setFacilityPhone(req.getFacilityPhone());
        if (req.getFacilityContent() != null)    target.setFacilityContent(req.getFacilityContent());
        if (req.getFacilityImagePath() != null)  target.setFacilityImagePath(req.getFacilityImagePath());
        if (req.getFacilityPersonMax() != null)  target.setFacilityPersonMax(req.getFacilityPersonMax());
        if (req.getFacilityPersonMin() != null)  target.setFacilityPersonMin(req.getFacilityPersonMin());
        if (req.getFacilityUse() != null)        target.setFacilityUse(req.getFacilityUse());
        if (req.getFacilityOpenTime() != null)   target.setFacilityOpenTime(req.getFacilityOpenTime());
        if (req.getFacilityCloseTime() != null)  target.setFacilityCloseTime(req.getFacilityCloseTime());
        if (req.getFacilityMoney() != null)      target.setFacilityMoney(req.getFacilityMoney());

        // 최소인원이 최대인원보다 클 경우 에러 발생 Rock
        if (target.getFacilityPersonMin() != null && target.getFacilityPersonMax() != null
                && target.getFacilityPersonMin() > target.getFacilityPersonMax())
            throw new IllegalArgumentException("facilityPersonMin <= facilityPersonMax");

        int affected = facilityMapper.updateFacility(target); // DB 업데이트 실행
        if (affected == 0) throw new RuntimeException("UPDATE failed"); // 실패 시 예외처리
    }

    //------------------------------------ 시설정보 삭제 ----------------------------------
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteFacilityById(Long facilityId) {	// 시설정보 삭제, 반환 없음
        int affected = facilityMapper.deleteFacilityById(facilityId);	// 삭제 실행
        if (affected == 0) throw new RuntimeException("NOT_FOUND: facility " + facilityId); // 삭제대상 없으면 예외처리
    }

    //------------------------------------ 시설사용여부 변경 ----------------------------------
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void changeFacilityUse(Long facilityId, boolean facilityUse) {	 // [사용 여부 변경
        int affected = facilityMapper.updateFacilityUse(facilityId, facilityUse);	// 변경 후, 반영
        if (affected == 0) throw new RuntimeException("NOT_FOUND: facility " + facilityId);	// 실패 시, 예외처리
    }

    //------------------------------------ 시설정보 존재 여부 확인 ----------------------------------
    @Override
    @Transactional(readOnly = true)
    public boolean existsFacilityById(Long facilityId) {	// 시설 존재 여부 확인
        return facilityMapper.existsFacilityById(facilityId);	// Mapper 실행하기
    }

    //------------------------------------ 시설정보 카운트(개수조회) ----------------------------------
    @Override
    @Transactional(readOnly = true)
    public long countFacilities(String name, Boolean facilityUse, String type) {	// 시설 총 개수
        return facilityQueryMapper.countFacilities(name, facilityUse, type);		// mapper 실행
    } //⚠️ [251001] 검색 카테고리(category) 필터 추가

    //-------------------------  DTO 변환(Facility → FacilityResponse) -----------------------------
    private FacilityResponse toResp(Facility f) {
        return FacilityResponse.builder()
                .facilityId(f.getFacilityId())
                .facilityName(f.getFacilityName())
                .memberId(f.getMemberId())
                .facilityPhone(f.getFacilityPhone())
                .facilityContent(f.getFacilityContent())
                .facilityImagePath(f.getFacilityImagePath())
                .facilityPersonMax(f.getFacilityPersonMax())
                .facilityPersonMin(f.getFacilityPersonMin())
                .facilityUse(f.isFacilityUse())
                .facilityRegDate(f.getFacilityRegDate())
                .facilityModDate(f.getFacilityModDate())
                .facilityOpenTime(f.getFacilityOpenTime())
                .facilityCloseTime(f.getFacilityCloseTime())
                .facilityMoney(f.getFacilityMoney())
                .build();
    }
}

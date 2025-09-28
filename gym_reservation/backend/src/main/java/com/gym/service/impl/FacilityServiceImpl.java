package com.gym.service.impl;

import com.gym.common.PageResponse;
import com.gym.domain.facility.*;
import com.gym.mapper.annotation.FacilityMapper;
import com.gym.mapper.xml.FacilityQueryMapper;
import com.gym.service.FacilityService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class FacilityServiceImpl implements FacilityService {

    private final FacilityMapper facilityMapper;
    private final FacilityQueryMapper facilityQueryMapper;

    public FacilityServiceImpl(FacilityMapper facilityMapper, FacilityQueryMapper facilityQueryMapper) {
        this.facilityMapper = facilityMapper;
        this.facilityQueryMapper = facilityQueryMapper;
    }

    @Override
    @Transactional(rollbackFor = Exception.class) // ✅ 트랜잭션 시작: INSERT 실패/예외 시 전체 롤백
    public Long createFacility(FacilityCreateRequest req) {
        // ✅ [1] 필수값 검증: 시설명 누락 방지(빈 문자열 포함)
        if (req.getFacilityName() == null || req.getFacilityName().isBlank())
            throw new IllegalArgumentException("facilityName is required");

        // ✅ [2] 필수값 검증: 최대/최소 인원 모두 있어야 함
        if (req.getFacilityPersonMax() == null || req.getFacilityPersonMin() == null)
            throw new IllegalArgumentException("personMax/personMin are required");

        // ✅ [3] 도메인 규칙: 최소인원 <= 최대인원 보장
        if (req.getFacilityPersonMin() > req.getFacilityPersonMax())
            throw new IllegalArgumentException("facilityPersonMin <= facilityPersonMax");

        // ✅ [4] VO 생성: 요청값 → 엔티티 필드 매핑(+ 기본값 적용)
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

        // ✅ [5] INSERT 수행: 시퀀스(NEXTVAL)로 PK 생성, @SelectKey로 방금 PK(CURRVAL) 주입
        int affected = facilityMapper.insertFacility(f);

        // ✅ [6] 영향 행수 검증: 1건이 아니면 예외(트랜잭션 롤백)
        if (affected != 1) throw new RuntimeException("INSERT failed");

        // ✅ [7] PK 반환: mapper의 @SelectKey가 f.facilityId 에 CURRVAL 주입 완료
        return f.getFacilityId();
    }


    @Override
    @Transactional(readOnly = true)
    public FacilityResponse getFacilityById(Long facilityId) {
        Facility f = facilityMapper.selectFacilityById(facilityId);
        if (f == null) throw new RuntimeException("NOT_FOUND: facility " + facilityId);
        return toResp(f);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<FacilityResponse> searchFacilities(String name, Boolean facilityUse,
                                                           Integer page, Integer size, String sort) {
        List<Facility> items = facilityQueryMapper.selectFacilities(name, facilityUse, page, size, sort);
        long total = facilityQueryMapper.countFacilities(name, facilityUse);
        return PageResponse.of(items.stream().map(this::toResp).toList(), total, page, size);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateFacility(Long facilityId, FacilityUpdateRequest req) {
        Facility target = facilityMapper.selectFacilityById(facilityId);
        if (target == null) throw new RuntimeException("NOT_FOUND: facility " + facilityId);

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

        if (target.getFacilityPersonMin() != null && target.getFacilityPersonMax() != null
                && target.getFacilityPersonMin() > target.getFacilityPersonMax())
            throw new IllegalArgumentException("facilityPersonMin <= facilityPersonMax");

        int affected = facilityMapper.updateFacility(target);
        if (affected == 0) throw new RuntimeException("UPDATE failed");
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteFacilityById(Long facilityId) {
        int affected = facilityMapper.deleteFacilityById(facilityId);
        if (affected == 0) throw new RuntimeException("NOT_FOUND: facility " + facilityId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void changeFacilityUse(Long facilityId, boolean facilityUse) {
        int affected = facilityMapper.updateFacilityUse(facilityId, facilityUse);
        if (affected == 0) throw new RuntimeException("NOT_FOUND: facility " + facilityId);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existsFacilityById(Long facilityId) {
        return facilityMapper.existsFacilityById(facilityId);
    }

    @Override
    @Transactional(readOnly = true)
    public long countFacilities(String name, Boolean facilityUse) {
        return facilityQueryMapper.countFacilities(name, facilityUse);
    }

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

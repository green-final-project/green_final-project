package com.gym.service;

import com.gym.common.PageResponse;
import com.gym.domain.facility.*;
import java.util.List;

public interface FacilityService {

    Long createFacility(FacilityCreateRequest req);                           // PK 반환

    FacilityResponse getFacilityById(Long facilityId);

    /** 검색(목록). PageResponse는 [임시추가]로 최소 구조 제공(아래 3-3 참조) */
    PageResponse<FacilityResponse> searchFacilities(
            String name, 
            Boolean facilityUse, 
            Integer page, 
            Integer size, 
            String sort,
            String type // ⚠️ [251001] category → type 변경
    		);

    void updateFacility(Long facilityId, FacilityUpdateRequest req);

    void deleteFacilityById(Long facilityId);

    void changeFacilityUse(Long facilityId, boolean facilityUse);

    boolean existsFacilityById(Long facilityId);

    long countFacilities(String name, Boolean facilityUse, String type); // ⚠️ [251001] category → type 변경
}

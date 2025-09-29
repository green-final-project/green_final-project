package com.gym.service;

import com.gym.domain.closedday.ClosedDay;
import com.gym.domain.closedday.ClosedDayResponse;
import java.time.LocalDate;
import java.util.List;

public interface ClosedDayService {

	/*
	 	휴무일(ClosedDay) 관련 서비스 인터페이스
	 	- 컨트롤러와 매퍼 사이에서 비즈니스 로직을 담당
	 	- 주로 CRUD 기능 제공
	 */
	// 휴무일 등록 
    Long createClosedDay(ClosedDay closedDay);
        
    // 휴무일 조회
    List<ClosedDayResponse> findClosedDaysByFacility(Long facilityId, LocalDate fromDate, LocalDate toDate);

    // 휴무일 삭제
    void deleteClosedDayById(Long closedId);

    // 휴무일 수정 
    void updateClosedDay(Long closedId, ClosedDay update);
}

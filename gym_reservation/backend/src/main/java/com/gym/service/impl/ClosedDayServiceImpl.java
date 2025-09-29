package com.gym.service.impl;

import com.gym.domain.closedday.ClosedDay;
import com.gym.domain.closedday.ClosedDayResponse;
import com.gym.mapper.xml.ClosedDayMapper;
import com.gym.service.ClosedDayService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDate;
import java.util.List;

/**
 * ClosedDayService 구현체
 */
@Service
public class ClosedDayServiceImpl implements ClosedDayService {

	private final ClosedDayMapper closedDayMapper; // DB매퍼 의존성 주입

    public ClosedDayServiceImpl(ClosedDayMapper closedDayMapper) {
        this.closedDayMapper = closedDayMapper;
    }

    // 휴무일 등록
    // 상단의 DB매퍼 의존성을 통해 insert 함
    @Override
    @Transactional
    public Long createClosedDay(ClosedDay closedDay) {
        closedDayMapper.insertClosedDay(closedDay);
        return closedDay.getClosedId(); // 시퀀스 생성 → closedId 반환함 
    }

    // 휴무일 조회
    // 시설ID, 시작일, 종료일 필터링
    @Override
    @Transactional(readOnly = true)
    public List<ClosedDayResponse> findClosedDaysByFacility(Long facilityId, LocalDate fromDate, LocalDate toDate) {
        return closedDayMapper.selectClosedDaysByFacility(facilityId, fromDate, toDate);
    }

    // 휴무일 삭제
    @Override
    @Transactional
    public void deleteClosedDayById(Long closedId) {
        int deleted = closedDayMapper.deleteClosedDayById(closedId);
        // 대상이 없을 경우(0) → Alert 메시지 노출
        if (deleted == 0) {
            throw new RuntimeException("해당 휴무일(ClosedId=" + closedId + ")이 존재하지 않습니다.");
        }
    }

    // 휴무일 수정
    @Override
    @Transactional
    public void updateClosedDay(Long closedId, ClosedDay update) {
        int updated = closedDayMapper.updateClosedDay(closedId, update);
        // 대상이 없을 경우(0) → Alert 메시지 노출
        if (updated == 0) {
            throw new RuntimeException("수정할 휴무일이 없습니다. ID=" + closedId);
        }
    }
}

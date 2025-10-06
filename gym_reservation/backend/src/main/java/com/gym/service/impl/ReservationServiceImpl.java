package com.gym.service.impl;

import com.gym.domain.reservation.Reservation;
import com.gym.domain.reservation.ReservationCreateRequest;
import com.gym.domain.reservation.ReservationResponse;
import com.gym.domain.reservation.ReservationSearchRequest;
import com.gym.domain.reservation.ReservationUpdateRequest;
import com.gym.mapper.annotation.MemberMapper; 
import com.gym.mapper.annotation.ReservationMapper;
import com.gym.mapper.xml.ReservationQueryMapper;  
import com.gym.service.ReservationService;
import lombok.RequiredArgsConstructor;

import org.springframework.jdbc.core.JdbcTemplate; //[250919] 추가
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

//[250925추가] 문자 전송 서비스 주입(기존 서비스 인터페이스 사용, 시그니처 변경 금지)
import com.gym.service.MessageService; // 메시지 서비스(프로젝트 보유 인터페이스 사용)
import com.gym.domain.message.Message; // 메시지 엔티티 (이력 저장용)


@Service
@RequiredArgsConstructor
public class ReservationServiceImpl implements ReservationService {

    private final ReservationMapper reservationMapper;	// CUD 매퍼
    private final ReservationQueryMapper reservationQueryMapper;	// 조회 매퍼(XML)
    private final MemberMapper memberMapper; // 회원 검증
    private final JdbcTemplate jdbcTemplate; //[250919] 추가 (부트 자동 구성)
    private final MessageService messageService; // [250925추가] 메시지 서비스 빈 주입(기존 구현체 사용, 새로운 메서드 생성 금지)
    
    
    // 예약신청
    @Override
    @Transactional
    public Long createReservation(ReservationCreateRequest request) {
        // 1) 회원 존재 확인: 없으면 중단
        if (!memberMapper.existsMemberById(request.getMemberId())) {
            throw new IllegalArgumentException("존재하지 않는 회원 ID: " + request.getMemberId());
        }

        // 2) 문자열 → 도메인 타입 변환
        //    wantDate: "yyyy-MM-dd" → LocalDate (※ atStartOfDay() 쓰지 않음)
        //    시작/종료: "yyyy-MM-dd HH:mm:ss" → LocalDateTime
        DateTimeFormatter d  = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        DateTimeFormatter dt = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

        LocalDate wantDate = LocalDate.parse(request.getWantDate(), d);
        LocalDateTime start = LocalDateTime.parse(request.getResvStartTime(), dt);
        LocalDateTime end   = LocalDateTime.parse(request.getResvEndTime(), dt);

        // INSERT 전에 겹치는 예약 여부 확인 (완료 상태만 막히도록 XML에서 resv_status='완료' 조건 포함)
        if (reservationQueryMapper.existsOverlapReservation(
                request.getFacilityId(), start, end)) {
            throw new IllegalStateException("이미 예약되어 있는 상태입니다.");
        }

        Reservation entity = Reservation.builder()
                .memberId(request.getMemberId())		// 회원ID
                .facilityId(request.getFacilityId())	// 시설ID
                .resvContent(request.getResvContent())	// 요구사항
                .wantDate(wantDate)	// 원하는 날짜
                .resvPersonCount(request.getResvPersonCount()) // 인원
                .resvStartTime(start)	// 원하는 날짜 + 시작시간
                .resvEndTime(end)	// 원하는 날짜 + 종료시간
                .resvStatus("대기")  // 등록 시 자동으로 '대기'
                .build();


        // 3) INSERT 수행(성공 시 entity.resvId 채워짐)
        reservationMapper.insertReservation(entity);

        // 4) 생성된 PK 반환
        return entity.getResvId();
    } 

    /**
     * [251002 신규] 예약 단건조회
     * - 입력: resvId(PK)
     * - 출력: ReservationResponse (단일 예약 상세정보)
     * - 사용처: 예약 상세확인, 결제단계 연동
     */
    @Override
    @Transactional(readOnly = true)
    public ReservationResponse getReservation(Long resvId) {
        return reservationQueryMapper.getReservation(resvId);
    }

    
    // 검색 조회
    @Override
    @Transactional(readOnly = true)
    public List<ReservationResponse> listReservations(ReservationSearchRequest req) {
        // userId/facilityId가 모두 null/빈값이면 전체 조회됨(XML 동적 where)
        return reservationQueryMapper.listReservations(req);
    }

    @Override
    @Transactional
    public int updateReservationByUser(Long resvId, String userId,
                                       ReservationUpdateRequest request) {
        // 1) 소유권 확인: resvId + userId 일치 여부
        if (!reservationMapper.existsByIdAndMemberId(resvId, userId)) {
            throw new IllegalArgumentException("NOT_FOUND_OR_FORBIDDEN: reservation=" + resvId + ", user=" + userId);
        }

        // 2) 부분수정 엔티티 구성(null 필드는 미반영)
        Reservation patch = Reservation.builder()
                .resvId(resvId)
                .memberId(userId)
                .resvContent(request.getResvContent())
                .resvPersonCount(request.getResvPersonCount())
                .resvStatus(request.getResvStatus())
                .build();

        return reservationMapper.updateByIdAndMemberId(patch);
    }

    // 예약정보 삭제하기...이제 안씀
    @Override
    @Transactional
    public int deleteReservationByUser(Long resvId, String userId) {
        // 1) 소유권 확인
        if (!reservationMapper.existsByIdAndMemberId(resvId, userId)) {
            throw new IllegalArgumentException("NOT_FOUND_OR_FORBIDDEN: reservation=" + resvId + ", user=" + userId);
        }
        // 2) 삭제
        return reservationMapper.deleteByIdAndMemberId(resvId, userId);
    }
    
    /**
     * [250919 신규] 취소신청 — resv_cancel='Y' 로만 업데이트 (idempotent)
     * - 매퍼 파일 수정 금지: JDBC 한 줄 UPDATE
     * - 소유자 검증: (resv_id, userId) 일치 필수
     * - 이미 'Y'면 영향행 0 → 에러 아님
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public int requestReservationCancel(Long resvId, String userId, String resvCancelReason) {
        if (!reservationMapper.existsByIdAndMemberId(resvId, userId)) {
            throw new IllegalArgumentException(
                "NOT_FOUND_OR_FORBIDDEN: reservation=" + resvId + ", user=" + userId
            );
        }

    // 2) 매퍼로 컬럼 업데이트 (resv_cancel='Y', resv_cancel_reason=입력값)
    // [250925변경사항] ※ 기존 반환값을 변수에 담아 성공 여부 확인 후 문자 발송(커밋 경로에서만)
    // return reservationMapper.updateCancelRequest(resvId, userId, resvCancelReason); // 기존꺼
    int updated = reservationMapper.updateCancelRequest(resvId, userId, resvCancelReason); // [250925추가] 반환값 변수화

    // [250925추가] 취소 성공 시 신청자에게 문자 발송(서비스 계층에서 처리, 트리거/DDL 무관)
    if (updated == 1) { // 업데이트 성공(영향행 1건)일 때만
    	// 2-1) 예약자(member_id) 조회
    	String applicantId = jdbcTemplate.queryForObject(
    	        "SELECT r.member_id FROM reservation_tbl r WHERE r.resv_id = ?",
    	        String.class,   // requiredType 먼저
    	        resvId          // 가변인자
    	); // 신청자 ID

    	// 2-2) 휴대폰 번호 조회(member_tbl.mobile)
    	String mobile = jdbcTemplate.queryForObject(
    	        //"SELECT m.mobile FROM member_tbl m WHERE m.member_id = ?",
    			//[251005]  m.mobile → m.member_mobile 변경 
    			"SELECT m.member_mobile FROM member_tbl m WHERE m.member_id = ?",
    	        String.class,   // requiredType 먼저
    	        applicantId     // 가변인자
    	); // 신청자 휴대폰

        // 2-3) 문자 전송(기존 MessageService 메서드 시그니처 사용, 새 메서드 생성 금지)
        if (mobile != null && !mobile.isEmpty()) {
        	Message msg = Message.builder()
                    .memberId(applicantId)          // 수신자ID
                    .resvId(resvId)                 // 관련 예약ID
                    .messageType("예약취소")        // 유형 (사양에 맞춰 "예약취소")
                    .messageContent("예약신청 취소되었습니다.") // 본문
                    .build();

            messageService.sendMessage(msg); // ✅ 기존 시그니처 그대로 사용
        }
    }
    
    return updated; // 기존 반환 계약 유지
}

}

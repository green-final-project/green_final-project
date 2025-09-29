package com.gym.mapper.xml;

import com.gym.domain.message.Message;
import com.gym.domain.message.MessageResponse;

import org.apache.ibatis.annotations.Mapper;

import java.util.List;
import java.util.Map;

/**
 * MyBatis 매퍼 인터페이스
 * SQL Mapper XML과 연동되어 DB 작업 수행
 */
@Mapper
public interface MessageMapper {

    /**
     * 메시지 저장 (insert)
     * @param message 저장 대상 메시지 정보
     * @return 저장된 메시지 고유 ID (생성된 키값)
     */
    int insertMessage(Message message);

    /**
     * 전체 메시지 목록 조회 (단일 엔드포인트용 필터)
     * @param params 검색 파라미터 맵 (startDate, endDate, messageType, receiverId)
     * @return 메시지 전체 리스트
     */
    List<MessageResponse> selectAllMessages(Map<String, Object> params);
}

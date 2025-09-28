package com.gym.service.impl;

import com.gym.domain.message.Message;
import com.gym.domain.message.MessageResponse;
import com.gym.mapper.xml.MessageMapper;
import com.gym.service.MessageService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 메시지 관련 비즈니스 로직 구현 클래스
 * - 메시지 저장 시 현재 시간 세팅, DB 삽입 및 로그 기록
 * - 조회 및 카운트 기능 구현
 */
@Service
public class MessageServiceImpl implements MessageService {

    private static final Logger logger = LoggerFactory.getLogger(MessageServiceImpl.class);

    private final MessageMapper messageMapper;

    /**
     * 생성자 기반 의존성 주입
     * @param messageMapper 메시지 매퍼
     */
    public MessageServiceImpl(MessageMapper messageMapper) {
        this.messageMapper = messageMapper;
    }

    @Override
    public void sendMessage(Message message) {
        // 메시지 발송 시 현재 서버시간으로 발송일시 세팅 (나노초는 0으로)
        message.setMessageDate(LocalDateTime.now().withNano(0));

        // DB에 메시지 저장 (생성된 PK 리턴)
        //Long id = messageMapper.insertMessage(message);
        //message.setMessageId(id);
        int affected = messageMapper.insertMessage(message); // 영향 행수

        // 메시지 전송 내용 로그 기록
        logger.info("문자전송 요청 - 수신자 ID: {}, 유형: {}, 내용: {}, 발송 시간: {}",
            message.getMemberId(),
            message.getMessageType(),
            message.getMessageContent(),
            message.getMessageDate());
    }

    @Override
    public List<MessageResponse> getAllMessages(String startDate, String endDate, String messageType, String receiverId) {
        Map<String, Object> params = new HashMap<>();  // 기존 스타일 유지(Map)
        params.put("startDate", startDate);   // YYYY-MM-DD 문자열
        params.put("endDate", endDate);       // YYYY-MM-DD 문자열
        params.put("messageType", messageType); // '예약확인'/'예약취소'/'휴관공지'
        params.put("receiverId", receiverId); // 회원ID
        return messageMapper.selectAllMessages(params); // ✅ 동일 id 재사용
    }
}

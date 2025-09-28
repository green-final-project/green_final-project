package com.gym.service;

import java.util.List;
import com.gym.domain.payment.*;

public interface PaymentService {

	Long create(PaymentCreateRequest req); // 등록 → PK

	List<PaymentResponse> findList(PaymentSearchRequest req); // 목록 검색

	void updateStatus(Long paymentId, String status); // 상태 변경
}

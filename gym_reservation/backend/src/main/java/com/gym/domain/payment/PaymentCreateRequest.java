package com.gym.domain.payment;

import lombok.*;
import jakarta.validation.constraints.*;
import com.fasterxml.jackson.annotation.JsonIgnore;

// 결제 정보 등록 검증 도메인 
@Getter 
@Setter
@NoArgsConstructor 
@AllArgsConstructor
@Builder 
@ToString
public class PaymentCreateRequest {

 @NotBlank(message = "회원아이디(memberId)를 입력해주세요.")
 @Size(max = 20, message = "회원아이디(memberId)는 20자 이하여야 합니다.")
 private String memberId;

 private Long accountId;  // 계좌 결제 시 필수
 private Long cardId;     // 카드 결제 시 필수

 @NotNull(message = "신청정보ID(resvId)를 입력하세요.")
 private Long resvId;

 @NotNull(message = "결제금액(paymentMoney)을 입력하세요.")
 @Positive(message = "결제금액(paymentMoney)은 최소 1 이상이어야 합니다.")
 private Long paymentMoney;

 // 결제수단 
 @Pattern(regexp = "^(계좌|카드)$",
          message = "결제수단(paymentMethod)은 '계좌' 또는 '카드'만 허용됩니다.")
 private String paymentMethod;

 // null 허용(없으면 '예약' 처리)
 @Pattern(regexp = "^(완료|예약|취소)$",
          message = "결제상태(paymentStatus)는 '완료','예약','취소' 중 하나여야 합니다.")
 private String paymentStatus;

// ⚠️ [251004 추가] 카드 할부 개월수
// - 계좌 결제 시 무조건 0
// - 카드 결제 시 0(일시불), 2~6, 12만 허용
 @Min(value = 0, message = "할부 개월수(cardInstallment)는 0 이상이어야 합니다.")
 @Max(value = 12, message = "할부 개월수(cardInstallment)는 12 이하만 허용됩니다.")
 private Integer cardInstallment = 0;

 // --------------------------------------- 검증 항목들 ---------------------------------------
 
 // 결재수단 검증
@AssertTrue(message = "결제수단이 '계좌'면 accountId만, '카드'면 cardId만 지정하세요.")
@JsonIgnore
public boolean isMethodConsistent() {
    if (paymentMethod == null) return true; 
    // 계좌(accountId) 값을 입력했을 경우 → 결제수단(paymentMethod)은 계좌로 설정해야 함 카드(cardId)는 null로 설정
    if ("계좌".equals(paymentMethod)) return accountId != null && cardId == null;
    // 카드(cardId) 값을 입력했을 경우 → 결제수단(paymentMethod)은 카드로 설정해야 함 계좌(accountId)는 null로 설정
    if ("카드".equals(paymentMethod)) return cardId != null && accountId == null;
    return true; // @Pattern이 차단
}
 
// ⚠️ [251004 추가] 카드 할부 유효성 검증
@AssertTrue(message = "카드 결제 시 할부 개월수는 0(일시불), 2, 3, 4, 5, 6, 12만 가능합니다.")
@JsonIgnore
public boolean isInstallmentValid() {
    if (paymentMethod == null) return true;
    if ("계좌".equals(paymentMethod)) return cardInstallment == 0; // 계좌는 0으로 고정, 0 외에는 false
    if ("카드".equals(paymentMethod)) // 카드는 0(일시불), 2~6개월, 12개월 중에서만 true로 처리
        return cardInstallment != null &&
               (cardInstallment == 0 || 
                cardInstallment == 2 ||
                cardInstallment == 3 || 
                cardInstallment == 4 ||
                cardInstallment == 5 || 
                cardInstallment == 6 ||
                cardInstallment == 12);
    return true;
}
}

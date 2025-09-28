package com.gym.domain.closedday;

import java.time.LocalDate;
import lombok.*;

@Getter @Setter @ToString
@NoArgsConstructor @AllArgsConstructor @Builder
public class ClosedDayResponse {
    
    private Long closedId; // 휴무일 고유번호
    private Long facilityId; // 시설 ID
    private String facilityName; // 시설명 (JOIN으로 가져올 정보)
    private LocalDate closedDate; // 휴무일 날짜
    private String closedContent; // 휴무 사유
}

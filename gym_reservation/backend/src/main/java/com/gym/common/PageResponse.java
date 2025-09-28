package com.gym.common;


import lombok.*;
import java.util.List;

@Getter 			// Lombok: Getter 자동 생성
@Setter				// Lombok: Setter 자동 생성
@NoArgsConstructor	// Lombok: 기본 생성자 자동 생성
@AllArgsConstructor	// Lombok: 모든 필드를 매개변수로 받는 생성자 자동 생성
@Builder			// Lombok: Builder 패턴 자동 생성

/**
 * 공통 페이지 응답 클래스
 * - 목록 조회 시 페이징 처리를 위한 응답 객체
 * - 제네릭 타입 <T>를 사용하여 다양한 데이터 타입을 담을 수 있음
 */
public class PageResponse<T> {

    // 실제 데이터 목록 (예: 회원, 시설 목록 등)
    private List<T> items;

    // 전체 데이터 건수 (페이징 처리를 위한 총 개수)
    private long total;

    // 현재 페이지 번호 (0 또는 1부터 시작하는 페이지 인덱스)
    private Integer page;

    // 한 페이지에 보여줄 데이터 개수
    private Integer size;

    // 객체 생성 메서드
    public static <T> PageResponse<T> of(List<T> items,	// 데이터 목록
                                         long total,	// 전체 데이터
                                         Integer page,	// 현제 페이지 번호
                                         Integer size)	// 페이지당 데이터 개수 
    	{
        // Builder 객체 생성
        return PageResponse.<T> builder()
        		//코드			주석설명				빌더 코드 예시
                .items(items)	// 데이터 목록 설정 		.items(List.of("A", "B"))
                .total(total)	// 전체 건수 설정		.total(100)
                .page(page)		// 현재 페이지 번호		.page(1) 
                .size(size)		// 페이지 크기 설정		.size(10) 
                .build();		// 최종 객체 생성		.build(); 
    }
}
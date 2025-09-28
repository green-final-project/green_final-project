package com.gym.controller.cms;

import com.gym.common.ApiResponse;
import com.gym.domain.card.CardCreateRequest;
import com.gym.domain.card.CardResponse;
import com.gym.service.CardService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.MediaType;
//import io.swagger.v3.oas.annotations.media.Schema;
//import io.swagger.v3.oas.annotations.Parameter;
import org.springframework.dao.DataIntegrityViolationException;

import java.util.*;

/**
 * CMS 카드 API (관리자/최고관리자) - SecurityConfig: "/api/cms/cards/**" ->
 * hasAnyAuthority("admin","superadmin")
 */
@Tag(name = "06.Card-CMS", description = "CMS 카드 API (관리자/최고관리자)")
@RestController
@RequiredArgsConstructor
@Slf4j
@RequestMapping("/api/cms/cards")
public class CmsCardController {

	private final CardService cardService;

	@Operation(summary = "CMS 카드 등록", description = "임의 회원 대상 등록 (폼 입력 방식)")
	@PostMapping(consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
	public ApiResponse<Long> create(
	        @Parameter(name = "memberId", description = "회원ID", required = true)
	        @RequestParam("memberId") String memberId,
	        @Parameter(name = "cardBank", description = "카드사명", required = true)
	        @RequestParam("cardBank") String cardBank,
	        @Parameter(name = "cardNumber", description = "카드번호", required = true)
	        @RequestParam("cardNumber") String cardNumber,
	        @Parameter(name = "cardApproval", description = "승인번호(APPR-1021)")
	        @RequestParam(name = "cardApproval", required = false) String cardApproval,
	        @Parameter(
	            name = "cardMain",
	            description = "대표 여부",
	            required = true,
	            schema = @io.swagger.v3.oas.annotations.media.Schema(
	                type = "string", allowableValues = {"true","false"}, example = "false"
	            )
	        )
	        @RequestParam(name = "cardMain", defaultValue = "false") boolean cardMain
	) {
	    log.info("[POST]/api/cms/cards memberId={}, cardBank={}, cardNumber={}, cardApproval={}, cardMain={}",
	            memberId, cardBank, cardNumber, cardApproval, cardMain);

	    CardCreateRequest req = new CardCreateRequest();
	    req.setMemberId(memberId);
	    req.setCardBank(cardBank);
	    req.setCardNumber(cardNumber);
	    req.setCardApproval(cardApproval);
	    req.setCardMain(cardMain);

	    try {
	        Long pk = cardService.createCard(req);
	        return ApiResponse.ok(pk);
	    } catch (DataIntegrityViolationException | IllegalArgumentException ex) {
	        // UNIQUE 등 제약 위반 또는 서비스 사전검사 실패 → 409 + 고정 메시지
	        return ApiResponse.fail(409, "이미 등록된 카드번호입니다.");
	    }
	}

	
	/**
	 * 대표카드 변경(PATCH /api/cms/cards/{cardId}/main?memberId=xxx) - 서비스/매퍼 수정 없이 사용:
	 * unsetOtherMains → setCardToMain 순서로 내부 처리됨
	 */
	@Operation(summary = "CMS 대표카드 설정", description = "특정 회원의 대표카드를 지정합니다.")
	@PatchMapping("/{cardId}/main")
	public ApiResponse<Void> setMainCard(@Parameter(description = "대표로 지정할 카드 PK") @PathVariable("cardId") Long cardId,
			@Parameter(description = "카드 소유 회원ID") @RequestParam("memberId") String memberId) {
		log.info("[PATCH]/api/cms/cards/{}/main?memberId={}", cardId, memberId);
		try {
			cardService.setMainCard(cardId, memberId);
			return ApiResponse.ok();
		} catch (RuntimeException e) {
			// 존재하지 않거나 다른 회원 카드를 지정한 경우 등
			String msg = String.valueOf(e.getMessage());
			if (msg.contains("NOT_FOUND") || msg.contains("mismatch")) {
				return ApiResponse.fail(404, "대상 카드를 찾을 수 없거나 회원이 일치하지 않습니다.");
			}
			// 유니크/트리거 충돌 등 기타는 400으로 응답
			return ApiResponse.fail(400, "대표카드 설정에 실패했습니다.");
		}
	}

	/**
	 * 특정 회원 카드 조회(GET /api/cms/cards?memberId=xxx&page=0&size=10) - 컨트롤러 레벨에서만 간단
	 * 페이징(기본 size=10) - 서비스/매퍼 그대로 사용(listCardsByMember)
	 */
	@Operation(summary = "CMS 특정 회원 카드 목록", description = "memberId로 카드 목록을 조회합니다(간단 페이징).")
	@GetMapping
	public ApiResponse<Map<String, Object>> listByMember(@RequestParam("memberId") String memberId,
			@RequestParam(value = "page", defaultValue = "0") int page,
			@RequestParam(value = "size", defaultValue = "10") int size) {
		log.info("[GET]/api/cms/cards?memberId={}&page={}&size={}", memberId, page, size);

		if (page < 0)
			page = 0;
		if (size <= 0)
			size = 10;

		List<CardResponse> all = cardService.listCardsByMember(memberId); // 전체 가져와서 슬라이스
		int total = all.size();
		int from = Math.min(page * size, total);
		int to = Math.min(from + size, total);
		List<CardResponse> items = all.subList(from, to);

		Map<String, Object> payload = new LinkedHashMap<>();
		payload.put("items", items);
		payload.put("total", total);
		payload.put("page", page);
		payload.put("size", size);
		payload.put("hasNext", to < total);

		return ApiResponse.ok(payload);
	}
}

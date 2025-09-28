package com.gym.controller.user;

import com.gym.common.ApiResponse;
import com.gym.domain.card.*;
import com.gym.service.CardService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Parameter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.core.Authentication;    // [250917] 본인 확인용
import org.springframework.security.access.AccessDeniedException; // [250917] 접근차단 예외
import io.swagger.v3.oas.annotations.media.Schema; // [250917] 입력폼
import org.springframework.http.MediaType; // [250917] 입력폼

import java.util.List;

/**
 * 카드 API (등록/목록/대표설정/삭제)
 * - 경로/메서드/파라미터/반환: 사용자 표와 1:1 일치
 * - [250917] 등록/대표설정/삭제는 로그인 본인 소유만 허용
 */
@Tag(name = "06.Card-User", description = "카드 API (등록/목록/대표설정/삭제)")
@RestController
@RequiredArgsConstructor
@Slf4j
public class UserCardController {

    private final CardService cardService;

    /** 1) 등록(POST /api/cards) — CardCreateRequest → PK(Long) */
    // [old]
    /*
    @Operation(summary = "카드 등록", description = "card_tbl INSERT (시퀀스/제약 준수)")
    @PostMapping("/api/cards")
    public ApiResponse<Long> createCard(@RequestBody CardCreateRequest req) {
        log.info("[POST]/api/cards req={}", req);
        Long pk = cardService.createCard(req);
        return ApiResponse.ok(pk);
    }
    */
    // 1) 등록(POST /api/cards) — 입력폼(form-urlencoded) + 본인 계정 고정
    @Operation(summary = "카드 등록", description = "card_tbl INSERT (폼 입력, 작성자ID는 로그인ID로 자동 설정)")
    @PostMapping(value = "/api/cards", consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    public ApiResponse<Long> createCard(
            // 카드사명: 필수 입력 파라미터(폼에서 값을 받음)
            @Parameter(name = "cardBank", description = "카드사명", required = true)
            @RequestParam("cardBank") String cardBank,

            // 카드번호: 필수 입력 파라미터(UNIQUE 등 제약이 걸려 있을 수 있음)
            @Parameter(name = "cardNumber", description = "카드번호", required = true)
            @RequestParam("cardNumber") String cardNumber,

            // 승인번호: 선택 입력(없을 수 있음)
            @Parameter(name = "cardApproval", description = "승인번호(APPR-1021)")
            @RequestParam(name = "cardApproval", required = false) String cardApproval,

            // 대표 여부: 기본값은 false, true로 오면 등록 직후 대표 전환 로직을 추가로 수행
            @Parameter(
                name = "cardMain",
                description = "대표 여부(true/false)",
                schema = @Schema(type = "string", allowableValues = {"true","false"}, example = "false")
            )
            @RequestParam(name = "cardMain", defaultValue = "false") boolean cardMain,

            // 스프링 시큐리티 인증 객체: 필터에서 인증이 통과된 요청만 컨트롤러에 도달
            Authentication auth
    ) {
        // 로그인 사용자ID 추출: 작성자ID로 강제 사용(폼에서 memberId를 받지 않으므로 스푸핑 불가)
        final String loginId = auth.getName(); // 작성자ID = 로그인ID

        // 운영 로그: 입력값 기록(카드번호 등 민감정보는 운영 정책에 따라 마스킹 권장)
        log.info("[POST]/api/cards loginId={}, cardBank={}, cardNumber={}, cardApproval={}, cardMain={}",
                loginId, cardBank, cardNumber, cardApproval, cardMain);

        // DTO 구성: 컨트롤러는 폼 파라미터와 로그인ID를 DTO에 담아 서비스로 전달
        CardCreateRequest req = new CardCreateRequest();
        req.setMemberId(loginId);       // 소유자ID를 로그인ID로 고정
        req.setCardBank(cardBank);      // 카드사명 매핑
        req.setCardNumber(cardNumber);  // 카드번호 매핑
        req.setCardApproval(cardApproval); // 승인번호 매핑(선택)
        req.setCardMain(cardMain);      // 대표 여부 전달(등록 직후 대표 전환 분기에도 사용)

        try {
            // 1) 카드 등록 시도: DB에 INSERT 수행(서비스 내부에서 매퍼 호출)
            Long pk = cardService.createCard(req);

            // 2) 대표 카드로 요청된 경우: 방금 생성된 PK를 대상으로 대표 전환
            //    - setMainCard(cardId, memberId)는 대상 카드는 'Y', 같은 회원의 나머지 카드는 자동 'N'으로 만드는 비즈니스 로직
            //    - 단일 대표 보장을 위해 INSERT와 별개로 분리 호출(매퍼 수정 없이 일관성 유지)
            if (cardMain) {
                cardService.setMainCard(pk, loginId);
            }

            // 정상 처리 응답: 생성된 카드의 PK 반환
            return ApiResponse.ok(pk);

        } catch (org.springframework.dao.DataIntegrityViolationException ex) {
            // 3-A) DB 제약 위반(UNIQUE 등) 발생 시: 500 대신 사용자 친화 메시지로 변환
            //      - 본인 ID 소유 목록에 동일 카드번호가 있는지 조회하여 메시지 분기
            boolean sameUserHasCard = false;
            try {
                // 본인 소유 카드 목록 조회(매퍼 수정 없이 기존 서비스 조회 사용)
                java.util.List<CardResponse> myCards = cardService.listCardsByMember(loginId);
                if (myCards != null) {
                    for (CardResponse c : myCards) {
                        // 동일 카드번호가 본인 목록에 존재하면 true
                        if (cardNumber.equals(c.getCardNumber())) { sameUserHasCard = true; break; }
                    }
                }
            } catch (Exception ignore) {
                // 목록 조회 실패 시에도 아래 공통 분기 로직은 수행(메시지 결정에는 영향 없음)
            }
            // 본인 ID 기준 중복이면 "이미 등록된 카드입니다.", 그렇지 않으면 "본인 카드인지 확인 부탁드립니다."
            return sameUserHasCard
                    ? ApiResponse.fail(409, "이미 등록된 카드입니다.")
                    : ApiResponse.fail(409, "본인 카드인지 확인 부탁드립니다.");

        } catch (IllegalArgumentException ex) {
            // 3-B) 서비스 사전검사에서 IllegalArgumentException을 던진 경우(예: "cardNumber already exists")
            //      - 위와 동일한 방식으로 본인 소유 여부를 검사하여 메시지 분기
            boolean sameUserHasCard = false;
            try {
                java.util.List<CardResponse> myCards = cardService.listCardsByMember(loginId);
                if (myCards != null) {
                    for (CardResponse c : myCards) {
                        if (cardNumber.equals(c.getCardNumber())) { sameUserHasCard = true; break; }
                    }
                }
            } catch (Exception ignore) { }
            return sameUserHasCard
                    ? ApiResponse.fail(409, "이미 등록된 카드입니다.")
                    : ApiResponse.fail(409, "본인 카드인지 확인 부탁드립니다.");
        }
    }




    /** 2) 회원별 목록(GET /api/members/{memberId}/cards) — List<CardResponse> */
    @Operation(summary = "카드 목록", description = "memberId 기준 SELECT")
    @GetMapping("/api/members/{memberId}/cards")
    // [old]
    /*
    public ApiResponse<List<CardResponse>> listByMember(
            @Parameter(description = "회원ID") @PathVariable("memberId") String memberId) {
        log.info("[GET]/api/members/{}/cards", memberId);
        return ApiResponse.ok(cardService.listCardsByMember(memberId));
    }
    */
    // [250917] 본인 카드 목록만 조회 가능
    public ApiResponse<List<CardResponse>> listByMember(
            @Parameter(description = "회원ID") @PathVariable("memberId") String memberId,
            Authentication auth) {
        log.info("[GET]/api/members/{}/cards", memberId);
        if (!auth.getName().equals(memberId)) throw new AccessDeniedException("본인 카드만 조회할 수 있습니다.");
        return ApiResponse.ok(cardService.listCardsByMember(memberId));
    }

    /** 3) 대표카드 설정(PATCH /api/cards/{cardId}/main?memberId=xxx) — void */
    // [old]
    /*
    @Operation(summary = "대표카드 설정", description = "대상만 'Y', 나머지 자동 'N'")
    @PatchMapping("/api/cards/{cardId}/main")
    public ApiResponse<Void> setMainCard(
            @Parameter(description = "대표로 지정할 카드 PK") @PathVariable("cardId") Long cardId,
            @Parameter(description = "카드 소유 회원ID") @RequestParam("memberId") String memberId) {
        log.info("[PATCH]/api/cards/{}/main?memberId={}", cardId, memberId);
        cardService.setMainCard(cardId, memberId);
        return ApiResponse.ok();
    }
    */
    // [250917] 본인 카드만 대표로 설정 가능
    // 3) 대표카드 설정(PATCH /api/cards/{cardId}/main) — 본인만 가능
    @Operation(summary = "대표카드 설정", description = "대상만 'Y', 나머지 자동 'N' (본인만 가능)")
    @PatchMapping("/api/cards/{cardId}/main")
    public ApiResponse<Void> setMainCard(
            @Parameter(description = "대표로 지정할 카드 PK")
            @PathVariable("cardId") Long cardId,
            Authentication auth // 인증 정보(컨트롤러 진입 시 인증 보장)
    ) {
        // 로그인ID 추출(폼으로 받지 않음)
        final String loginId = auth.getName();

        // 로깅(민감정보 제외)
        log.info("[PATCH]/api/cards/{}/main loginId={}", cardId, loginId);

        // 서비스 호출: 서비스 내부에서 카드 소유자=loginId 검증 및 대표카드 전환 처리
        cardService.setMainCard(cardId, loginId);

        // 표준 응답
        return ApiResponse.ok();
    }


    /** 4) 삭제(DELETE /api/cards/{cardId}) — void */
    @Operation(summary = "카드 삭제", description = "PK로 단건 삭제(트리거/참조 제약 주의)")
    @DeleteMapping("/api/cards/{cardId}")
    // [old]
    /*
    public ApiResponse<Void> deleteCardById(
            @Parameter(description = "삭제할 카드 PK") @PathVariable("cardId") Long cardId) {
        log.info("[DELETE]/api/cards/{}", cardId);
        cardService.deleteCardById(cardId);
        return ApiResponse.ok();
    }
    */
    // [250917] 본인 카드만 삭제 가능(서비스에서 소유자 최종 검증)
    /*public ApiResponse<Void> deleteCardById(
            @Parameter(description = "삭제할 카드 PK") @PathVariable("cardId") Long cardId,
            Authentication auth) {
        log.info("[DELETE]/api/cards/{}", cardId);
        cardService.deleteCardByIdForOwner(cardId, auth.getName());   // 소유자 검증 포함 삭제
        return ApiResponse.ok();
    }*/
    public ApiResponse<Void> deleteCardById(
            @Parameter(description = "삭제할 카드 PK") @PathVariable("cardId") Long cardId,
            Authentication auth) {
        log.info("[DELETE]/api/cards/{}", cardId);
        try {
            cardService.deleteCardByIdForOwner(cardId, auth.getName()); // 소유자 검증 포함 삭제
            return ApiResponse.ok();
        } catch (RuntimeException e) {
            // DB 트리거가 터질 때 ORA-20041, ORA-20042 같은 코드가 메시지에 포함됨
            if (e.getMessage() != null && e.getMessage().contains("대표카드")) {
                return ApiResponse.fail(400, "대표카드는 삭제할 수 없습니다."); 
            }
            // 나머지 메시지 전송
            throw e;
        }
    }

}

-- =========================================================
-- 🔧 공통: 스키마 고정(DDL에 스키마 접두어가 없음)
-- =========================================================
-- ALTER SESSION SET CURRENT_SCHEMA = gym;

--------------------------------------------------------------------------------
-- 0) 초기화(선택): 기존 트리거/인덱스/테이블 제거 
-- 에러 초기화 목적
--------------------------------------------------------------------------------
/*
BEGIN
  EXECUTE IMMEDIATE 'DROP TRIGGER trg_card_require_main_on_del';
EXCEPTION WHEN OTHERS THEN IF SQLCODE != -4080 THEN RAISE; END IF;
END;
/
BEGIN
  EXECUTE IMMEDIATE 'DROP TRIGGER trg_card_require_main_on_upd';
EXCEPTION WHEN OTHERS THEN IF SQLCODE != -4080 THEN RAISE; END IF;
END;
/
BEGIN
  EXECUTE IMMEDIATE 'DROP INDEX uidx_card_one_main_per_member';
EXCEPTION WHEN OTHERS THEN IF SQLCODE NOT IN (-1418,-942) THEN RAISE; END IF;
END;
/
-- DROP TABLE card_tbl CASCADE CONSTRAINTS;
*/

--------------------------------------------------------------------------------
-- 1) 결제카드정보(card_tbl) 생성
--    - 엑셀 사양 1:1 반영: PK/NOT NULL/UNIQUE(card_number)/CHECK(Y,N)/FK(member)
--------------------------------------------------------------------------------
CREATE TABLE card_tbl (
    card_id       NUMBER        NOT NULL,                  -- 카드 고유번호 (PK)
    member_id     VARCHAR2(20)  NOT NULL,                  -- 소유자 회원ID (FK → member_tbl.member_id)
    card_bank     VARCHAR2(50)  NOT NULL,                  -- 카드사명 (신한, 현대 등)
    card_number   VARCHAR2(20)  NOT NULL,                  -- 카드번호(전역 UNIQUE)
    card_approval VARCHAR2(20),                            -- 승인번호(모의결제 등)
    card_main     CHAR(1)       DEFAULT 'N' NOT NULL,      -- 대표카드 여부(Y/N) - 기본값 'N'
    card_reg_date DATE          DEFAULT SYSDATE            -- 등록일 - 기본값 SYSDATE
);

-- 📌 컬럼 주석(엑셀 사양 그대로)
COMMENT ON COLUMN card_tbl.card_id       IS '카드 고유번호 (PK)';
COMMENT ON COLUMN card_tbl.member_id     IS '카드 소유자 회원 ID (FK)';
COMMENT ON COLUMN card_tbl.card_bank     IS '카드사명 (신한, 현대 등)';
COMMENT ON COLUMN card_tbl.card_number   IS '카드번호';
COMMENT ON COLUMN card_tbl.card_approval IS '카드 승인번호';
COMMENT ON COLUMN card_tbl.card_main     IS '대표 카드 여부 (Y/N)';
COMMENT ON COLUMN card_tbl.card_reg_date IS '카드 등록일';

-- 📌 제약조건
ALTER TABLE card_tbl ADD CONSTRAINT card_tbl_pk  PRIMARY KEY (card_id);            -- PK
ALTER TABLE card_tbl ADD CONSTRAINT card_main_ch CHECK (card_main IN ('Y','N'));   -- 대표여부 Y/N
ALTER TABLE card_tbl ADD CONSTRAINT card_number_un UNIQUE (card_number);           -- 카드번호 전역 UNIQUE

-- 📌 FK
ALTER TABLE card_tbl
  ADD CONSTRAINT fk_card_member
  FOREIGN KEY (member_id)
  REFERENCES member_tbl(member_id);
  
--------------------------------------------------------------------------------
-- 2) 트리거
--------------------------------------------------------------------------------
-- 2-A) “회원별 대표카드 정확히 1개” 강제
--    - (A) 함수기반 UNIQUE 인덱스: member_id 당 card_main='Y' 1개만 허용
--    - (B) 트리거: 대표카드가 0개가 되는 UPDATE/DELETE 금지
--------------------------------------------------------------------------------
-- (A) 함수기반 유니크 인덱스: card_main='Y' 인 행만 유니크 키 생성
--     'Y'가 아닌 행은 인덱스 키가 NULL → 중복 허용
CREATE UNIQUE INDEX uidx_card_one_main_per_member
  ON card_tbl ( CASE WHEN card_main = 'Y' THEN member_id END );

-- (B1) 대표행 삭제 방지
CREATE OR REPLACE TRIGGER trg_card_require_main_on_del
BEFORE DELETE ON card_tbl
FOR EACH ROW
DECLARE
    v_cnt NUMBER;
BEGIN
    IF :OLD.card_main = 'Y' THEN
        SELECT COUNT(*)
          INTO v_cnt
          FROM card_tbl
         WHERE member_id = :OLD.member_id
           AND card_main = 'Y'
           AND card_id  <> :OLD.card_id;

        IF v_cnt = 0 THEN
            RAISE_APPLICATION_ERROR(
                -20041,
                '대표카드 최소 1개 유지 규칙 위반: 삭제 전 다른 대표카드를 먼저 지정하세요.'
            );
        END IF;
    END IF;
END;
/

-- 대표 → 보조 하향/소유자 이관 시 원 소유자 대표 0개 방지
CREATE OR REPLACE TRIGGER trg_card_require_main_on_upd
BEFORE UPDATE ON card_tbl
FOR EACH ROW
DECLARE
    v_cnt NUMBER;
BEGIN
    -- ① 'Y' → 'N'
    IF :OLD.card_main = 'Y' AND :NEW.card_main = 'N' THEN
        SELECT COUNT(*)
          INTO v_cnt
          FROM card_tbl
         WHERE member_id = :OLD.member_id
           AND card_main = 'Y'
           AND card_id  <> :OLD.card_id;

        IF v_cnt = 0 THEN
            RAISE_APPLICATION_ERROR(
                -20042,
                '대표카드 최소 1개 유지 규칙 위반: 먼저 다른 카드를 대표로 지정한 후 본 카드를 해제하세요.'
            );
        END IF;
    END IF;

    -- ② 대표카드인 행을 다른 회원에게 이관(member_id 변경)
    IF :OLD.card_main = 'Y' AND :OLD.member_id <> :NEW.member_id THEN
        SELECT COUNT(*)
          INTO v_cnt
          FROM card_tbl
         WHERE member_id = :OLD.member_id
           AND card_main = 'Y'
           AND card_id  <> :OLD.card_id;

        IF v_cnt = 0 THEN
            RAISE_APPLICATION_ERROR(
                -20043,
                '대표카드 이관 불가: 원 소유 회원이 대표카드 0개가 됩니다. 먼저 다른 대표카드를 지정하세요.'
            );
        END IF;
    END IF;
END;
/
-- SHOW ERRORS;



/* ============================================================================
  2-B) 신규 등록 자동 대표 지정 (ADD만 수행)
     - 첫 카드 등록이면 :NEW.card_main 을 'Y'로 강제
     - 대표('Y')로 들어온 건은 문장 종료 후 해당 회원의 나머지를 자동으로 'N'
     - 기존 객체(테이블/제약/트리거)는 손대지 않음
============================================================================ */
CREATE OR REPLACE TRIGGER trg_card_auto_main_on_ins        -- 트리거명(INSERT 전용)
FOR INSERT ON card_tbl                                      -- 대상 테이블
COMPOUND TRIGGER                                            -- COMPOUND: 각 단계 분리

  /* 회원별 ‘이번 문장에서 대표로 들어온 카드ID’를 기억할 맵(다건 INSERT 대비) */
  TYPE t_map IS TABLE OF NUMBER INDEX BY VARCHAR2(64);      -- 키: member_id, 값: card_id
  g_main_map t_map;                                         -- 전역 컬렉션

  BEFORE EACH ROW IS                                         -- 행 단위(INSERT 시 마다)
    v_cnt NUMBER;                                            -- 회원 보유 카드 수
  BEGIN
    /* NULL 방지: 외부에서 NULL로 오면 기본 'N' 처리 */
    IF :NEW.card_main IS NULL THEN
      :NEW.card_main := 'N';                                 -- NOT NULL 준수
    END IF;

    /* 첫 카드 등록이면 자동 대표 강제 */
    SELECT COUNT(*) INTO v_cnt
      FROM card_tbl
     WHERE member_id = :NEW.member_id;                       -- 해당 회원의 기존 카드 수

    IF v_cnt = 0 THEN
      :NEW.card_main := 'Y';                                 -- 첫 등록 → 무조건 대표
    END IF;

    /* 대표('Y')로 들어오는 행은 사후 정리를 위해 기록 */
    IF :NEW.card_main = 'Y' THEN
      g_main_map(:NEW.member_id) := :NEW.card_id;            -- 시퀀스 사용 시 INSERT문에서 세팅됨
    END IF;
  END BEFORE EACH ROW;

  AFTER STATEMENT IS                                         -- 문장 종료 후(표준화 정리)
    k VARCHAR2(64);                                          -- member_id 키
    v NUMBER;                                                -- 대표 card_id
  BEGIN
    /* 대표로 들어온 회원에 대해: 해당 카드만 'Y', 나머지는 'N' 으로 일괄 정리 */
    k := g_main_map.FIRST;
    WHILE k IS NOT NULL LOOP
      v := g_main_map(k);
      UPDATE card_tbl
         SET card_main = CASE WHEN card_id = v THEN 'Y' ELSE 'N' END
       WHERE member_id = k;                                  -- 같은 회원의 모든 카드 대상
      k := g_main_map.NEXT(k);
    END LOOP;
  END AFTER STATEMENT;

END;
/

/* ============================================================================
  2-C) 대표 변경 시 자동 정리 (ADD만 수행)
     - UPDATE 로 어떤 카드가 'Y' 가 되면 → 그 회원의 나머지는 자동 'N'
     - 대표를 'N' 으로 내리는 UPDATE 자체는 허용(기존 BEFORE UPDATE 트리거가 0개 금지 검증)
     - 기존 트리거와 충돌하지 않도록 문장 종료 후 정리만 수행
============================================================================ */

DROP TRIGGER trg_card_require_main_on_upd;
DROP TRIGGER trg_card_require_main_on_del;



CREATE OR REPLACE TRIGGER trg_card_require_main
FOR UPDATE OR DELETE ON card_tbl
COMPOUND TRIGGER
  TYPE t_mid_tab IS TABLE OF card_tbl.member_id%TYPE;
  g_mids t_mid_tab := t_mid_tab();

  TYPE t_seen IS TABLE OF BOOLEAN INDEX BY VARCHAR2(64);
  g_seen t_seen;

  AFTER EACH ROW IS
  BEGIN
    IF DELETING THEN
      IF :OLD.card_main = 'Y' THEN
        g_mids.EXTEND; g_mids(g_mids.COUNT) := :OLD.member_id;
      END IF;
    ELSIF UPDATING THEN
      IF (:OLD.card_main = 'Y' AND :NEW.card_main = 'N')
         OR (:OLD.card_main = 'Y' AND :OLD.member_id <> :NEW.member_id) THEN
        g_mids.EXTEND; g_mids(g_mids.COUNT) := :OLD.member_id;
      END IF;
    END IF;
  END AFTER EACH ROW;

  AFTER STATEMENT IS
  BEGIN
    IF g_mids.COUNT > 0 THEN
      FOR i IN 1 .. g_mids.COUNT LOOP
        IF NOT g_seen.EXISTS(g_mids(i)) THEN
          g_seen(g_mids(i)) := TRUE;
          DECLARE v_cnt NUMBER;
          BEGIN
            SELECT COUNT(*) INTO v_cnt
              FROM card_tbl
             WHERE member_id = g_mids(i)
               AND card_main = 'Y';
            IF v_cnt = 0 THEN
              RAISE_APPLICATION_ERROR(
                -20042,
                '대표카드 최소 1개 유지 규칙 위반: 먼저 다른 카드를 대표로 지정하세요.'
              );
            END IF;
          END;
        END IF;
      END LOOP;
    END IF;
  END AFTER STATEMENT;
END;
/





--------------------------------------------------------------------------------
-- 3) 더미 데이터(재실행 대비 삭제 후 삽입)
--------------------------------------------------------------------------------
DELETE FROM card_tbl WHERE card_id IN (1,2,3,4);
COMMIT;

-- hong1: 대표 1개 + 보조 1개
INSERT INTO card_tbl (card_id, member_id, card_bank, card_number, card_approval, card_main, card_reg_date)
VALUES (1, 'hong1', '신한카드', '9400-1111-2222-3333', 'APPR-1001', 'Y', SYSDATE);

INSERT INTO card_tbl (card_id, member_id, card_bank, card_number, card_approval, card_main, card_reg_date)
VALUES (2, 'hong1', '현대카드', '9400-4444-5555-6666', 'APPR-1002', 'N', SYSDATE);

-- hong2: 대표 1개
INSERT INTO card_tbl (card_id, member_id, card_bank, card_number, card_approval, card_main, card_reg_date)
VALUES (3, 'hong2', '국민카드', '5500-7777-8888-9999', 'APPR-2001', 'Y', SYSDATE);

-- (참고) 대표 중복 시도 → 함수기반 UNIQUE 충돌(ORA-00001)로 실패해야 정상
-- INSERT INTO card_tbl (card_id, member_id, card_bank, card_number, card_main) 
-- VALUES (4, '홍1', '롯데카드', '1234-0000-0000-0000', 'Y');

COMMIT;


-- card_tbl PK(card_id) 자동 증가용 시퀀스 생성
BEGIN
  EXECUTE IMMEDIATE '
    CREATE SEQUENCE seq_card_id   -- 시퀀스 이름
    START WITH 1                  -- 시작 값 (1부터 시작)
    INCREMENT BY 1                -- 1씩 증가
    NOCACHE                       -- 캐시 안씀(환경에 따라 CACHE 20도 가능)
    NOCYCLE                       -- 끝까지 가면 멈춤(다시 1로 안돌아감)
  ';
EXCEPTION
  WHEN OTHERS THEN
    IF SQLCODE != -955 THEN RAISE; END IF;  -- 이미 있으면 무시
END;
/

SELECT seq_card_id.NEXTVAL FROM dual;   -- 1, 2, 3 ... 값이 나오면 정상

BEGIN
  EXECUTE IMMEDIATE 'DROP TRIGGER trg_card_auto_main_on_upd';
EXCEPTION
  WHEN OTHERS THEN
    IF SQLCODE != -4080 THEN RAISE; END IF; -- 없으면 무시
END;
/

-------------------------------------------------------------------------------
-- 시퀀스 생성
BEGIN
  EXECUTE IMMEDIATE q'[
    CREATE SEQUENCE seq_card_id START WITH 1 INCREMENT BY 1 NOCACHE NOCYCLE
  ]';
EXCEPTION WHEN OTHERS THEN IF SQLCODE != -955 THEN RAISE; END IF;
END;
/
-- 1. (재귀 방지) 예전 업데이트 트리거만 안전 삭제
--    - 로그의 ORA-00036 원인: `TRG_CARD_AUTO_MAIN_ON_UPD`
BEGIN EXECUTE IMMEDIATE 'DROP TRIGGER trg_card_auto_main_on_upd'; EXCEPTION WHEN OTHERS THEN NULL; END;
/






--------------------------------------------------------------------------------
-- 4) 확인 조회
--------------------------------------------------------------------------------
SELECT
    c.card_id                              AS "카드번호(PK)",
    c.member_id                            AS "회원ID",
    c.card_bank                            AS "카드사",
    c.card_number                          AS "카드번호",
    NVL(c.card_approval,'-')               AS "승인번호",
    CASE c.card_main WHEN 'Y' THEN '대표' ELSE '보조' END AS "대표여부",
    TO_CHAR(c.card_reg_date,'YYYY-MM-DD HH24:MI') AS "등록일"
FROM card_tbl c
ORDER BY c.card_id;
--c.member_id, 
--------------------------------------------------------------------------------
-- 5-1) 💀 데이터 초기화 (안전 모드) 💀
--      - 예제 더미(card_id 1~4 등)만 정리 / 구조·제약 유지
--------------------------------------------------------------------------------
DELETE FROM card_tbl WHERE card_id IN (1,2,3,4);
COMMIT;

--------------------------------------------------------------------------------
-- 5-2) 💀 ddl 블록까지 안전 삭제 💀
--      - 실제 구조 제거 (테스트 종료 시 사용)
--------------------------------------------------------------------------------
/*
BEGIN EXECUTE IMMEDIATE 'DROP TRIGGER trg_card_require_main_on_del';  EXCEPTION WHEN OTHERS THEN NULL; END;
/
BEGIN EXECUTE IMMEDIATE 'DROP TRIGGER trg_card_require_main_on_upd';  EXCEPTION WHEN OTHERS THEN NULL; END;
/
BEGIN EXECUTE IMMEDIATE 'DROP INDEX uidx_card_one_main_per_member';   EXCEPTION WHEN OTHERS THEN NULL; END;
/
BEGIN EXECUTE IMMEDIATE 'DROP TABLE card_tbl CASCADE CONSTRAINTS';    EXCEPTION WHEN OTHERS THEN NULL; END;
/
*/

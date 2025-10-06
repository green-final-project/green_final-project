-----------------------------------------------------------
-- 1) 결제계좌정보(account_tbl) 생성
-----------------------------------------------------------
CREATE TABLE account_tbl (
    account_id       NUMBER        NOT NULL,            -- 회원 계좌 고유번호 (PK)
    member_id        VARCHAR2(20)  NOT NULL,            -- 계좌 소유자 회원 ID (FK → member_tbl.member_id)
    account_bank     VARCHAR2(50)  NOT NULL,            -- 계좌 은행명
    account_number   VARCHAR2(20)  NOT NULL,            -- 실제 계좌번호
    account_main     CHAR(1)       DEFAULT 'N' NOT NULL,-- 대표 계좌 여부(Y/N) - 기본값 'N'
    account_reg_date DATE          DEFAULT SYSDATE      -- 등록일 SYSDATE
);

-- 컬럼 주석(엑셀 사양 그대로)
COMMENT ON COLUMN account_tbl.account_id       IS '회원 계좌 고유번호 (PK)';
COMMENT ON COLUMN account_tbl.member_id        IS '계좌 소유자 회원 ID (FK)';
COMMENT ON COLUMN account_tbl.account_bank     IS '계좌 은행명 (국민, 카카오 등)';
COMMENT ON COLUMN account_tbl.account_number   IS '실제 계좌번호';
COMMENT ON COLUMN account_tbl.account_main     IS '대표 계좌 여부 (Y/N)';
COMMENT ON COLUMN account_tbl.account_reg_date IS '등록일';

-- 제약조건
ALTER TABLE account_tbl ADD CONSTRAINT account_tbl_pk  PRIMARY KEY (account_id);            -- PK
ALTER TABLE account_tbl ADD CONSTRAINT account_main_ch CHECK (account_main IN ('Y','N'));   -- 대표여부 Y/N
ALTER TABLE account_tbl ADD CONSTRAINT account_number_un UNIQUE (account_number);           -- 계좌번호 전역 UNIQUE (중복되면 에러 발생)

-- FK
ALTER TABLE account_tbl
  ADD CONSTRAINT fk_account_member
  FOREIGN KEY (member_id)
  REFERENCES member_tbl(member_id);
  
-- 시퀀스 지정
CREATE SEQUENCE seq_account_id
   MINVALUE 1 MAXVALUE 9999999999999999999999999999
   INCREMENT BY 1 
   START WITH 1 -- 원래는 5 -> 실행 후 1로 바꿔야 함
   NOORDER
   NOCYCLE
   NOKEEP
   NOSCALE
   NOCACHE
   GLOBAL ;
    
-----------------------------------------------------------
-- 2) 회원별 대표계좌 정확히 1개
--    함수기반 UNIQUE 인덱스: member_id 당 account_main='Y' 1개만 허용
--    트리거: 대표계좌가 0개가 되는 UPDATE/DELETE 금지
-----------------------------------------------------------

-- 함수기반 유니크 인덱스: account_main='Y'인 행만 대상
-- 'Y'가 아닌 행은 인덱스 키가 NULL → 중복 허용
CREATE UNIQUE INDEX uidx_account_one_main_per_member
  ON account_tbl ( CASE WHEN account_main = 'Y' THEN member_id END );

-- 대표행 삭제 방지: 삭제하려는 행이 대표('Y')이고, 동일 회원의 다른 대표가 없으면 차단
CREATE OR REPLACE TRIGGER trg_account_require_main_on_del
BEFORE DELETE ON account_tbl
FOR EACH ROW
DECLARE
    v_cnt NUMBER;
BEGIN
    IF :OLD.account_main = 'Y' THEN
        SELECT COUNT(*)
          INTO v_cnt
          FROM account_tbl
         WHERE member_id   = :OLD.member_id
           AND account_main = 'Y'
           AND account_id  <> :OLD.account_id;
        IF v_cnt = 0 THEN
            RAISE_APPLICATION_ERROR(
                -20031,
                '대표계좌 최소 1개 유지 규칙 위반: 삭제 전 다른 대표계좌를 먼저 지정해야 합니다.'
            );
        END IF;
    END IF;
END;
/

-- 대표 → 보조로 내릴 때 방지: UPDATE로 'Y'→'N' 변경 시, 동일 회원의 다른 대표가 없으면 차단
CREATE OR REPLACE TRIGGER trg_account_require_main_on_upd
BEFORE UPDATE ON account_tbl
FOR EACH ROW
DECLARE
    v_cnt NUMBER;
BEGIN
    -- 대표 → 보조로 내릴경우
    IF :OLD.account_main = 'Y' AND :NEW.account_main = 'N' THEN
        SELECT COUNT(*)
          INTO v_cnt
          FROM account_tbl
         WHERE member_id   = :OLD.member_id
           AND account_main = 'Y'
           AND account_id  <> :OLD.account_id;
        IF v_cnt = 0 THEN
            RAISE_APPLICATION_ERROR(
                -20032,
                '대표계좌 최소 1개 유지 규칙 위반: 먼저 다른 계좌를 대표로 지정한 후 본 계좌를 해제하세요.'
            );
        END IF;
    END IF;

    -- 대표인 행을 다른 회원에게 이관(member_id 변경)하는 경우: 원 소유자 측 대표 0개 방지
    IF :OLD.account_main = 'Y' AND :OLD.member_id <> :NEW.member_id THEN
        SELECT COUNT(*)
          INTO v_cnt
          FROM account_tbl
         WHERE member_id   = :OLD.member_id
           AND account_main = 'Y'
           AND account_id  <> :OLD.account_id;
        IF v_cnt = 0 THEN
            RAISE_APPLICATION_ERROR(
                -20033,
                '대표계좌 이관 불가: 원 소유 회원이 대표계좌 0개가 됩니다. 먼저 다른 대표계좌를 지정하세요.'
            );
        END IF;
    END IF;
END;
/
-- SHOW ERRORS;

-----------------------------------------------------------
-- 등록 규칙 트리거 추가
-- 사용자 ID 기준으로 첫 계좌 등록은 자동으로 대표계좌값이 Y로 등록
-- 두 번째부터는 대표계좌 Y/N 선택 가능
-- 신규계좌 혹은 수정된 계정에서 대표계좌를 Y로 등록하면 기존 Y는 자동으로 N 처리
-----------------------------------------------------------
BEGIN
  EXECUTE IMMEDIATE 'DROP INDEX uidx_account_one_main_per_member';  -- member_id 당 'Y' 1개 강제 인덱스
EXCEPTION
  WHEN OTHERS THEN
    -- ORA-01418: specified index does not exist, ORA-00942: table or view does not exist
    IF SQLCODE NOT IN (-1418, -942) THEN RAISE; END IF;             -- 없으면 무시, 다른 오류만 전파
END;
/

-- 예전 트리거 정리
BEGIN EXECUTE IMMEDIATE 'DROP TRIGGER trg_account_require_main_on_upd'; EXCEPTION WHEN OTHERS THEN NULL; END;
/
BEGIN EXECUTE IMMEDIATE 'DROP TRIGGER trg_account_require_main_on_del'; EXCEPTION WHEN OTHERS THEN NULL; END;
/
BEGIN EXECUTE IMMEDIATE 'DROP TRIGGER trg_account_require_main';        EXCEPTION WHEN OTHERS THEN NULL; END;
/

-- 대표 최소 1개 유지(컴파운드 트리거)
CREATE OR REPLACE TRIGGER trg_account_require_main
FOR UPDATE OR DELETE ON account_tbl
COMPOUND TRIGGER
  TYPE t_mid_tab IS TABLE OF account_tbl.member_id%TYPE;
  g_mids t_mid_tab := t_mid_tab();
  TYPE t_seen IS TABLE OF BOOLEAN INDEX BY VARCHAR2(64);
  g_seen t_seen;

  PROCEDURE remember(p_mid account_tbl.member_id%TYPE) IS
  BEGIN
    IF p_mid IS NOT NULL THEN
      g_mids.EXTEND;
      g_mids(g_mids.COUNT) := p_mid;
    END IF;
  END;

  AFTER EACH ROW IS
  BEGIN
    IF DELETING THEN
      IF :OLD.account_main = 'Y' THEN
        remember(:OLD.member_id);
      END IF;
    ELSIF UPDATING THEN
      IF (:OLD.account_main = 'Y' AND :NEW.account_main = 'N')
         OR (:OLD.account_main = 'Y' AND :OLD.member_id <> :NEW.member_id) THEN
        remember(:OLD.member_id);
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
              FROM account_tbl
             WHERE member_id=g_mids(i)
               AND account_main='Y';
            IF v_cnt=0 THEN
              RAISE_APPLICATION_ERROR(-20032,'대표계좌 최소 1개 유지 규칙 위반: 먼저 다른 계좌를 대표로 지정하세요.');
            END IF;
          END;
        END IF;
      END LOOP;
    END IF;
  END AFTER STATEMENT;
END;
/


---------------- 250917 CMS관리자 대표계좌 삭제 방지 트리거 --------------------------
DECLARE
  v_exists NUMBER := 0;
BEGIN
  SELECT COUNT(*) INTO v_exists
  FROM ALL_TRIGGERS
  WHERE OWNER = SYS_CONTEXT('USERENV','CURRENT_SCHEMA')
    AND TRIGGER_NAME = 'TRG_ACCOUNT_BLOCK_DELETE_MAIN';
  IF v_exists = 0 THEN NULL; END IF;

  EXECUTE IMMEDIATE q'[
    CREATE OR REPLACE TRIGGER TRG_ACCOUNT_BLOCK_DELETE_MAIN
    BEFORE DELETE ON account_tbl
    FOR EACH ROW
    BEGIN
      IF :OLD.account_main = 'Y' THEN
        RAISE_APPLICATION_ERROR(-20041, '대표계좌는 삭제할 수 없습니다.');
      END IF;
    END;
  ]';
END;
/
COMMIT;
---------------- 250917 CMS관리자 대표계정 삭제 방지 트리거 --------------------------

----------------------------------------
-- 4) 확인 조회
----------------------------------------
SELECT
    a.account_id AS "계좌번호(PK)",
    a.member_id  AS "회원ID",
    a.account_bank AS "은행명",
    a.account_number AS "계좌번호",
    CASE a.account_main WHEN 'Y' THEN '대표' ELSE '보조' END AS "대표여부",
    TO_CHAR(a.account_reg_date,'YYYY-MM-DD HH24:MI') AS "등록일"
FROM account_tbl a
ORDER BY a.account_id;

-- 중복 확인
SELECT account_number, COUNT(*) cnt
FROM account_tbl
GROUP BY account_number
HAVING COUNT(*) > 1;

DELETE FROM account_tbl t
 WHERE t.rowid IN (
   SELECT rid
   FROM (
     SELECT rowid rid,
            ROW_NUMBER() OVER (PARTITION BY account_number ORDER BY account_id) rn
     FROM account_tbl
   )
   WHERE rn > 1
);
COMMIT;

--------------------------------------------------------------------------------
-- 5-1) 💀 데이터 초기화 (안전 모드) 💀
--------------------------------------------------------------------------------
DELETE FROM account_tbl WHERE account_id IN (1,2,3,4);
COMMIT;

--------------------------------------------------------------------------------
-- 5-2) 💀 ddl 블록까지 안전 삭제 💀
--------------------------------------------------------------------------------
/*
BEGIN EXECUTE IMMEDIATE 'DROP TRIGGER trg_account_require_main_on_del'; EXCEPTION WHEN OTHERS THEN NULL; END;
/
BEGIN EXECUTE IMMEDIATE 'DROP TRIGGER trg_account_require_main_on_upd'; EXCEPTION WHEN OTHERS THEN NULL; END;
/
BEGIN EXECUTE IMMEDIATE 'DROP INDEX uidx_account_one_main_per_member';  EXCEPTION WHEN OTHERS THEN NULL; END;
/
BEGIN EXECUTE IMMEDIATE 'DROP TABLE account_tbl CASCADE CONSTRAINTS';    EXCEPTION WHEN OTHERS THEN NULL; END;
/
*/

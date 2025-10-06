-- ======================================================================
--   paylog_type 허용: ('결제','취소','대기') 만 사용
--  trg_payment_pk_seq    : 결제 PK 자동 부여
--  trg_payment_to_paylog : INSERT/UPDATE시, 로그 기록
-- ======================================================================

----------------------------------------
-- 1) 결제 테이블
----------------------------------------
CREATE TABLE payment_tbl (
    payment_id      NUMBER        NOT NULL,
    member_id       VARCHAR2(20)  NOT NULL,
    account_id      NUMBER        NULL,
    card_id         NUMBER        NULL,
    resv_id         NUMBER        NOT NULL,
    payment_money   NUMBER        NOT NULL,
    payment_method  VARCHAR2(20)  DEFAULT '계좌' NOT NULL,    -- 기본값 계좌
    payment_status  VARCHAR2(20)  DEFAULT '예약' NOT NULL,    -- 기본값 예약
    payment_date    DATE          DEFAULT SYSDATE NOT NULL
);

ALTER TABLE payment_tbl
  ADD CONSTRAINT payment_tbl_pk PRIMARY KEY (payment_id);

ALTER TABLE payment_tbl
  ADD CONSTRAINT payment_method_ch CHECK (payment_method IN ('카드','계좌'));

ALTER TABLE payment_tbl
  ADD CONSTRAINT payment_status_ch CHECK (payment_status IN ('완료','예약','취소'));

-- 계좌/카드 중 택1 규칙
ALTER TABLE payment_tbl
  ADD CONSTRAINT payment_method_fk_rule CHECK (
       (payment_method = '계좌' AND account_id IS NOT NULL AND card_id IS NULL)
    OR (payment_method = '카드' AND card_id   IS NOT NULL AND account_id IS NULL)
  );

-- FK (부모 테이블은 기존에 있어야 함)
ALTER TABLE payment_tbl
  ADD CONSTRAINT fk_payment_member      FOREIGN KEY (member_id)  REFERENCES member_tbl(member_id);

ALTER TABLE payment_tbl
  ADD CONSTRAINT fk_payment_account     FOREIGN KEY (account_id) REFERENCES account_tbl(account_id) ON DELETE SET NULL;

ALTER TABLE payment_tbl
  ADD CONSTRAINT fk_payment_card        FOREIGN KEY (card_id)    REFERENCES card_tbl(card_id)       ON DELETE SET NULL;

ALTER TABLE payment_tbl
  ADD CONSTRAINT fk_payment_reservation FOREIGN KEY (resv_id)    REFERENCES reservation_tbl(resv_id);

-- 인덱스
CREATE INDEX idx_payment_member ON payment_tbl(member_id);
CREATE INDEX idx_payment_resv   ON payment_tbl(resv_id);
CREATE INDEX idx_payment_date   ON payment_tbl(payment_date);
CREATE INDEX idx_payment_acc    ON payment_tbl(account_id);
CREATE INDEX idx_payment_card   ON payment_tbl(card_id);

----------------------------------------
-- 2) 결제 로그 테이블
----------------------------------------
CREATE TABLE paylog_tbl (
    paylog_id               NUMBER         NOT NULL,
    payment_id              NUMBER         NOT NULL,
    paylog_type             VARCHAR2(20)   NOT NULL, -- '결제','취소','대기'
    paylog_before_status    VARCHAR2(20),
    paylog_after_status     VARCHAR2(20),
    paylog_money            NUMBER,
    paylog_method           VARCHAR2(20),
    paylog_manager          VARCHAR2(20),
    paylog_memo             VARCHAR2(200),
    paylog_date             DATE
    DEFAULT SYSDATE NOT NULL
);

ALTER TABLE paylog_tbl
  ADD CONSTRAINT paylog_tbl_pk PRIMARY KEY (paylog_id);

ALTER TABLE paylog_tbl
  ADD CONSTRAINT fk_paylog_payment FOREIGN KEY (payment_id)
      REFERENCES payment_tbl(payment_id) ON DELETE CASCADE;

-- 결제 상태 3가지 
ALTER TABLE paylog_tbl
  ADD CONSTRAINT paylog_type_ch CHECK (paylog_type IN ('결제','취소','대기'));

CREATE INDEX idx_paylog_payment ON paylog_tbl(payment_id);
CREATE INDEX idx_paylog_date    ON paylog_tbl(paylog_date);

------------------------------------------------
-- 3) 시퀀스 (현 데이터 기준 MAX+1로 동기화 생성)
------------------------------------------------
DECLARE
v_next NUMBER;   -- 임시 변수: 다음 시퀀스 시작 값을 담기 위한 숫자 변수
BEGIN

-- [PK값] 결제 PK 시퀀스(payment_seq) 재생성

SELECT NVL(MAX(payment_id), 0) + 1  -- payment_tbl의 최대 PK값 1씩 카운트
INTO v_next -- v_next 변수에 담음
FROM payment_tbl;

-- 기존 payment_seq 시퀀스가 있으면 삭제, 없으면 무시
BEGIN 
EXECUTE IMMEDIATE 'DROP SEQUENCE payment_seq'; 
EXCEPTION 
WHEN OTHERS THEN NULL;  -- 예외 발생(존재하지 않음 등) 시 무시
END;

-- 새로운 payment_seq 시퀀스를 v_next 값부터 시작하도록 생성
EXECUTE IMMEDIATE 
'CREATE SEQUENCE payment_seq START WITH '||v_next||' INCREMENT BY 1 NOCACHE NOCYCLE';

-- [PK값] 결제로그 PK 시퀀스(paylog_seq) 재생성

SELECT NVL(MAX(paylog_id), 0) + 1   -- 현재 paylog_tbl의 최대 PK값 + 1을 구함
INTO v_next -- v_next 변수에 담음
FROM paylog_tbl;

-- 기존 paylog_seq 시퀀스가 있으면 삭제, 없으면 무시
BEGIN 
EXECUTE IMMEDIATE 'DROP SEQUENCE paylog_seq'; 
EXCEPTION 
WHEN OTHERS THEN NULL;  -- 예외 발생 시 무시
END;

-- 새로운 paylog_seq 시퀀스를 v_next 값부터 시작하도록 생성
EXECUTE IMMEDIATE 
'CREATE SEQUENCE paylog_seq START WITH '||v_next||' INCREMENT BY 1 NOCACHE NOCYCLE';
END;
/
----------------------------------------
-- 4) 트리거
----------------------------------------

-- 4-1) 결제 PK 자동 세팅 (보조 안전망)
CREATE OR REPLACE TRIGGER trg_payment_pk_seq
BEFORE INSERT ON payment_tbl
FOR EACH ROW
BEGIN
  IF :NEW.payment_id IS NULL THEN
    :NEW.payment_id := payment_seq.NEXTVAL;
  END IF;
END;
/
-- (정상 경로는 애플리케이션 INSERT에서 payment_seq.NEXTVAL 사용)


-- 4-2) 결제 → 결제로그 자동 기록(INSERT/UPDATE)
CREATE OR REPLACE TRIGGER trg_payment_to_paylog
AFTER INSERT OR UPDATE ON payment_tbl
FOR EACH ROW
DECLARE
  v_type VARCHAR2(20);
BEGIN
  IF :NEW.payment_status = '완료' THEN
    v_type := '결제';
  ELSIF :NEW.payment_status = '취소' THEN
    v_type := '취소';
  ELSE
    v_type := '대기';
  END IF;

  IF INSERTING THEN
    INSERT INTO paylog_tbl(
      paylog_id, payment_id, paylog_type,
      paylog_before_status, paylog_after_status,
      paylog_money, paylog_method, paylog_date
    ) VALUES (
      paylog_seq.NEXTVAL,          -- 로그 PK 시퀀스 사용(유지)
      :NEW.payment_id, v_type,
      NULL, :NEW.payment_status,
      :NEW.payment_money, :NEW.payment_method, SYSDATE
    );

  ELSIF UPDATING THEN
    IF NVL(:NEW.payment_status,'§') <> NVL(:OLD.payment_status,'§')
       OR NVL(:NEW.payment_money,-1) <> NVL(:OLD.payment_money,-1)
       OR NVL(:NEW.payment_method,'§') <> NVL(:OLD.payment_method,'§')
    THEN
      INSERT INTO paylog_tbl(
        paylog_id, payment_id, paylog_type,
        paylog_before_status, paylog_after_status,
        paylog_money, paylog_method, paylog_date
      ) VALUES (
        paylog_seq.NEXTVAL,
        :OLD.payment_id, v_type,
        :OLD.payment_status, :NEW.payment_status,
        :NEW.payment_money, :NEW.payment_method, SYSDATE
      );
    END IF;
  END IF;
END;
/
ALTER TRIGGER trg_payment_to_paylog ENABLE;


SELECT trigger_name, status
  FROM user_triggers
 WHERE trigger_name IN ('TRG_PAYMENT_PK_SEQ','TRG_PAYMENT_TO_PAYLOG');
 
----------------------------------------
-- 5) 점검 쿼리
----------------------------------------
-- 결제 PK값 증가 트리거 비활성화
ALTER TRIGGER trg_payment_pk_seq DISABLE;
-- 결제 PK값 증가 트리거 활성화
ALTER TRIGGER trg_payment_pk_seq ENABLE;
-- 상태 확인
SELECT trigger_name, status
  FROM user_triggers
 WHERE trigger_name IN ('TRG_PAYMENT_PK_SEQ','TRG_PAYMENT_TO_PAYLOG'); -- 결제정보/결제로그 둘 다 활성화되어 있어야 함
 
-- 결제→결제로그 기록 트리거 활성화
ALTER TRIGGER trg_payment_to_paylog ENABLE;
-- 결제→결제로그 기록 트리거 비활성화
ALTER TRIGGER trg_payment_to_paylog DISABLE;
-- 상태 확인
SELECT sequence_name, last_number
  FROM user_sequences
 WHERE sequence_name IN ('PAYMENT_SEQ','PAYLOG_SEQ'); -- 둘 다 값이 같아야 함(그래야지, 제대로 전송된거니까)


/* =====================================================================
    [250922 추가사항]
    - 시설이용 예약취소 = 결제취소 동기화 트리거
    - 결제금액 계산 트리거
 * ===================================================================== */
 
-- 결제 취소 시 예약 상태 자동 '취소' 동기화 트리거
-- payment_tbl.payment_status 가 '취소' 로 변경되는 UPDATE
CREATE OR REPLACE TRIGGER trg_payment_cancel_to_reservation
AFTER UPDATE OF payment_status ON payment_tbl   -- 결제상태 변경 후 실행
FOR EACH ROW
WHEN ( NEW.payment_status = '취소'                   -- 새 상태가 '취소'일 때만
       AND NVL(OLD.payment_status, '§') <> '취소' )  -- 이전 상태가 '취소'가 아니었다면(상태가 실제로 취소로 바뀐 경우)
BEGIN
  -- 예약건(resv_id) 상태를 '취소'로 설정
  UPDATE reservation_tbl
  SET resv_status = '취소'                       -- 예약상태를 '취소'로 강제
  WHERE resv_id = :NEW.resv_id                    -- 결제건이 참조하는 예약 PK
  AND resv_status <> '취소';                    -- 이미 '취소'면 불필요 업데이트 방지
END;
/
ALTER TRIGGER trg_payment_cancel_to_reservation ENABLE;  -- 활성화 보장



CREATE OR REPLACE TRIGGER trg_payment_done_to_reservation
AFTER UPDATE OF payment_status ON payment_tbl         -- 결제상태 변경 후 실행
FOR EACH ROW
WHEN ( NEW.payment_status = '완료'                     -- 새 상태가 '완료'일 때만
       AND NVL(OLD.payment_status, '§') <> '완료' )    -- 이전 상태는 '완료'가 아니어야 함(실제 전이)
BEGIN
  -- 예약건 상태를 '완료'로 설정
  UPDATE reservation_tbl
     SET resv_status = '완료'                          -- 예약상태를 '완료'로 강제
   WHERE resv_id = :NEW.resv_id                        -- 해당 결제건이 참조하는 예약PK
     AND resv_status <> '완료'                         -- 이미 완료면 스킵(불필요 UPDATE 방지)
     AND resv_status <> '취소';                        -- 취소된 예약은 되살리지 않음(정책 2)
END;
/
ALTER TRIGGER trg_payment_done_to_reservation ENABLE;  -- 활성화 보장


-- resv_id로 결제금액 산출 트리거
-- 시설이용금액 * 예약신청 이용시간
-- facility_money * (resv_end_time - resv_start_time)
-- 2-1) 기존 동일 트리거가 있으면 무시하고 진행(재실행 안전)
BEGIN
EXECUTE IMMEDIATE 'DROP TRIGGER trg_payment_calc_amount';
EXCEPTION
WHEN OTHERS THEN
IF SQLCODE != -4080 THEN RAISE; END IF;  -- ORA-04080: 존재하지 않음 → 무시
END;
/

-- 2-1) 생성: payment_tbl에 대해 BEFORE INSERT/UPDATE (payment_money를 자동 세팅)
CREATE OR REPLACE TRIGGER trg_payment_calc_amount
BEFORE INSERT OR UPDATE OF resv_id, payment_money ON payment_tbl
FOR EACH ROW
DECLARE
v_amount   NUMBER;    -- 최종 결제금액(원)
BEGIN
  /* INSERT이거나, UPDATE에서 payment_money가 NULL/0 이면 자동 계산
     - 백엔드가 금액을 채워서 들어오면 그 값을 존중(최소 변경 원칙)  */
  IF INSERTING OR (:NEW.payment_money IS NULL OR :NEW.payment_money = 0) THEN
    SELECT ROUND(
             f.facility_money *
             ((CAST(r.resv_end_time AS DATE) - CAST(r.resv_start_time AS DATE)) * 24)
           )
      INTO v_amount
      FROM reservation_tbl r
      JOIN facility_tbl    f ON f.facility_id = r.facility_id
     WHERE r.resv_id = :NEW.resv_id;

    IF v_amount IS NULL OR v_amount <= 0 THEN
      RAISE_APPLICATION_ERROR(-20031, '결제금액 계산 실패(0 이하)');
    END IF;

    :NEW.payment_money := v_amount;  -- 자동 세팅
  END IF;
END;
/
ALTER TRIGGER trg_payment_calc_amount ENABLE;

----------------------------------------
-- 2025년 10월 4일 추가사항: 카드 할부 기능 추가
----------------------------------------

-- 1. 결제 테이블에 card_installment(카드 할부) 컬럼 추가
ALTER TABLE payment_tbl
ADD card_installment NUMBER(2) DEFAULT 0 NOT NULL; -- 2자리 숫자, 필수입력사항으로 설정

-- 2. 컬럼 주석
COMMENT ON COLUMN payment_tbl.card_installment IS
'카드 결제 시 할부 개월수 (0=일시불, 2~12개월만 허용)';

-- 3. 결제수단 제약조건
-- 카드는 0개월(일시불), 2~6개월, 12개월로 설정, 계좌는 0으로 고정
ALTER TABLE payment_tbl
ADD CONSTRAINT chk_payment_card_installment
CHECK ((payment_method = '카드' AND card_installment IN (0, 2, 3, 4, 5, 6, 12))
    OR (payment_method = '계좌' AND card_installment = 0) );

-- 4. 결제로그 테이블에 카드할부 로그 기록 컬럼 추가
ALTER TABLE paylog_tbl
ADD card_installment NUMBER(2);
COMMENT ON COLUMN paylog_tbl.card_installment IS
'결제 시 카드 할부 개월수 (0=일시불, 2~12개월)';

-- 5. 기존 트리거 수정 (결제 → 결제로그 기록)
CREATE OR REPLACE TRIGGER trg_payment_to_paylog
AFTER INSERT OR UPDATE ON payment_tbl
FOR EACH ROW
DECLARE
v_type VARCHAR2(20);
BEGIN
IF :NEW.payment_status = '완료' THEN
v_type := '결제';
ELSIF :NEW.payment_status = '취소' THEN
v_type := '취소';
ELSE
v_type := '대기';
END IF;

IF INSERTING THEN
INSERT INTO paylog_tbl(
paylog_id,
payment_id,
paylog_type,
paylog_before_status,
paylog_after_status,
paylog_money,
paylog_method,
card_installment,         -- ✅ [추가] 카드 할부 기록
paylog_date
) VALUES (
paylog_seq.NEXTVAL,
    :NEW.payment_id,
v_type,
NULL,
    :NEW.payment_status,
    :NEW.payment_money,
    :NEW.payment_method,
    :NEW.card_installment,    -- ✅ [추가]
SYSDATE
);

ELSIF UPDATING THEN
IF NVL(:NEW.payment_status,'§') <> NVL(:OLD.payment_status,'§')
    OR NVL(:NEW.payment_money,-1) <> NVL(:OLD.payment_money,-1)
    OR NVL(:NEW.payment_method,'§') <> NVL(:OLD.payment_method,'§')
    OR NVL(:NEW.card_installment,-1) <> NVL(:OLD.card_installment,-1) -- ✅ [추가]
THEN
INSERT INTO paylog_tbl(
    paylog_id,
    payment_id,
    paylog_type,
    paylog_before_status,
    paylog_after_status,
    paylog_money,
    paylog_method,
    card_installment,        -- ✅ [추가]
    paylog_date
) VALUES (
    paylog_seq.NEXTVAL,
    :OLD.payment_id,
    v_type,
    :OLD.payment_status,
    :NEW.payment_status,
    :NEW.payment_money,
    :NEW.payment_method,
    :NEW.card_installment,   -- ✅ [추가]
    SYSDATE
);
END IF;
END IF;
END;
/
ALTER TRIGGER trg_payment_to_paylog ENABLE;

----------------------------------------
-- 6) 조회
----------------------------------------
-- 결제 목록
SELECT
  p.payment_id     AS "결제ID",
  p.member_id      AS "회원ID",
  p.account_id     AS "계좌ID",
  p.card_id        AS "카드ID",
  p.resv_id        AS "예약ID",
  f.facility_name   AS "시설명",
  p.payment_money  AS "결제금액",
  p.payment_method AS "결제방식",
  p.payment_status AS "결제상태",
  r.resv_status    AS "예약상태", 
  P.card_installment AS "카드할부", 
  TO_CHAR(p.payment_date,'YYYY-MM-DD HH24:MI') AS "결제일시"
FROM payment_tbl p
JOIN reservation_tbl r
  ON p.resv_id = r.resv_id
JOIN facility_tbl f
  ON r.facility_id = f.facility_id
ORDER BY p.payment_id;


-- 결제 로그
SELECT
  l.paylog_id      AS "로그ID",
  l.payment_id     AS "결제ID",
  l.paylog_type    AS "로그유형",
  l.paylog_before_status AS "이전상태",
  l.paylog_after_status  AS "이후상태",
  l.paylog_money   AS "금액",
  l.paylog_method  AS "방식",
  l.paylog_manager AS "담당자",
  l.paylog_memo    AS "메모",
  l.card_installment AS "카드할부개월",
  TO_CHAR(l.paylog_date,'YYYY-MM-DD HH24:MI') AS "로그일시"
FROM paylog_tbl l
ORDER BY l.paylog_id DESC;


-------------------------------------------------------------------------------
-- 7-1) 💀 데이터 초기화 
-------------------------------------------------------------------------------
-- 1) 자식 비우기
TRUNCATE TABLE GYM.paylog_tbl;

-- 2) FK 비활성화
ALTER TABLE GYM.paylog_tbl DISABLE CONSTRAINT fk_paylog_payment;

-- 3) 부모 비우기
TRUNCATE TABLE GYM.payment_tbl;

-- 4) FK 재활성화
ALTER TABLE GYM.paylog_tbl ENABLE CONSTRAINT fk_paylog_payment;

-- 5) 시퀀스 재생성(스키마 접두사 필수)
BEGIN EXECUTE IMMEDIATE 'DROP SEQUENCE GYM.paylog_seq';  EXCEPTION WHEN OTHERS THEN NULL; END;
/
CREATE SEQUENCE GYM.paylog_seq  START WITH 1 INCREMENT BY 1 NOCACHE NOCYCLE;
/
BEGIN EXECUTE IMMEDIATE 'DROP SEQUENCE GYM.payment_seq'; EXCEPTION WHEN OTHERS THEN NULL; END;
/
CREATE SEQUENCE GYM.payment_seq START WITH 1 INCREMENT BY 1 NOCACHE NOCYCLE;
/

-- 6) 트리거 활성화 보장
ALTER TRIGGER GYM.trg_payment_pk_seq    ENABLE;
ALTER TRIGGER GYM.trg_payment_to_paylog ENABLE;

-- 7) 점검(반드시 같은 스키마로)
SELECT COUNT(*) FROM GYM.payment_tbl;
SELECT COUNT(*) FROM GYM.paylog_tbl;
SELECT sequence_name, last_number
  FROM all_sequences
 WHERE sequence_owner='GYM'
   AND sequence_name IN ('PAYMENT_SEQ','PAYLOG_SEQ');


-------------------------------------------------------------------------------
-- 7-2) 💀 DDL 안전 삭제 (자식 → 부모)
-------------------------------------------------------------------------------
-- 1) 트리거 제거
BEGIN EXECUTE IMMEDIATE 'DROP TRIGGER trg_payment_to_paylog'; EXCEPTION WHEN OTHERS THEN NULL; END;
/
BEGIN EXECUTE IMMEDIATE 'DROP TRIGGER trg_payment_pk_seq';    EXCEPTION WHEN OTHERS THEN NULL; END;
/

-- 2) 시퀀스 제거
BEGIN EXECUTE IMMEDIATE 'DROP SEQUENCE paylog_seq';  EXCEPTION WHEN OTHERS THEN NULL; END;
/
BEGIN EXECUTE IMMEDIATE 'DROP SEQUENCE payment_seq'; EXCEPTION WHEN OTHERS THEN NULL; END;
/

-- 3) 테이블 제거(자식 → 부모)
BEGIN EXECUTE IMMEDIATE 'DROP TABLE paylog_tbl CASCADE CONSTRAINTS';  EXCEPTION WHEN OTHERS THEN NULL; END;
/
BEGIN EXECUTE IMMEDIATE 'DROP TABLE payment_tbl CASCADE CONSTRAINTS'; EXCEPTION WHEN OTHERS THEN NULL; END;
/

-- =========================================================
-- 🔧 공통: 스키마 고정 (DDL에 스키마 접두어 없음)
-- =========================================================
-- ALTER SESSION SET CURRENT_SCHEMA = gym;

-- 연결 자체 확인
SELECT 1 FROM DUAL;

-- 표준 계정 존재 여부(있으면 테스트 로그에 객체 내용 출력)
SELECT member_id, member_name, member_role, member_joindate
  FROM member_tbl
 WHERE member_id IN ('hong10','1234');
--------------------------------------------------------------------------------
-- 1) member_tbl  ← 대부분 테이블의 부모
--    ✨ 메인결제수단 컬럼(member_manipay) 포함 버전
--------------------------------------------------------------------------------
CREATE TABLE member_tbl (
    member_id        VARCHAR2(20)    NOT NULL,                        -- 회원 ID (PK)
    member_pw        VARCHAR2(20)    NOT NULL,                        -- 비밀번호
    member_name      VARCHAR2(100)   NOT NULL,                        -- 이름
    member_gender    CHAR(1)         NOT NULL,                        -- 성별 ('m','f')
    member_email     VARCHAR2(50)    NOT NULL,  -- 이메일
    member_mobile    VARCHAR2(13)    NOT NULL,                        -- 휴대폰 번호
    member_phone     VARCHAR2(13),                              -- 일반 전화번호
    zip              CHAR(5),                                   -- 우편번호
    road_address     NVARCHAR2(50),                             -- 도로명 주소
    jibun_address    NVARCHAR2(50),                             -- 지번 주소
    detail_address   NVARCHAR2(50),                             -- 상세 주소
    member_birthday  DATE,                                      -- 생년월일
    member_manipay   VARCHAR2(20)   DEFAULT 'account' NOT NULL, -- 주요 결제수단('account','card')
    member_joindate  DATE           DEFAULT SYSDATE NOT NULL,         -- 가입일 (기본값 SYSDATE)
    member_role      VARCHAR2(10)   DEFAULT 'user'    NOT NULL,       -- 권한 ('user','admin')
    admin_type       VARCHAR2(20)   DEFAULT '관리자'                   -- 관리자 역할 세분화(책임자/관리자/강사)
);

-------------------------------------------------------------------------------
-- [추가] 2025-09-15  암호화 기능 
-- 목적: BCrypt 해시(60바이트) 저장을 위해 길이 확장, 그 외 컬럼/제약/트리거는 미변경
ALTER TABLE member_tbl
MODIFY (member_pw VARCHAR2(60 BYTE));  -- 컬럼명 그대로 유지, 길이만 60로

-- 1) PW 컬럼 길이 적용 확인(= 60)
SELECT column_name, data_type, data_length
FROM   user_tab_columns
WHERE  table_name='MEMBER_TBL' AND column_name='MEMBER_PW';

-- 2) 더미 계정 PW가 평문이면 아직 인코딩 전(가입/수정시 BCrypt encode() 필요)
SELECT member_id, member_pw, LENGTH(member_pw) AS len
FROM   member_tbl
WHERE  member_id LIKE 'hong%';

--------------------------------------------------------------------------------
-- 2) 컬럼/테이블 주석
--------------------------------------------------------------------------------
COMMENT ON TABLE  member_tbl                      IS '회원정보';
COMMENT ON COLUMN member_tbl.member_id            IS '회원 ID (PK)';
COMMENT ON COLUMN member_tbl.member_pw            IS '비밀번호';
COMMENT ON COLUMN member_tbl.member_name          IS '이름';
COMMENT ON COLUMN member_tbl.member_gender        IS '성별 (m/f)';
COMMENT ON COLUMN member_tbl.member_email         IS '이메일';
COMMENT ON COLUMN member_tbl.member_mobile        IS '휴대폰 번호';
COMMENT ON COLUMN member_tbl.member_phone         IS '일반 전화번호';
COMMENT ON COLUMN member_tbl.zip                  IS '우편번호';
COMMENT ON COLUMN member_tbl.road_address         IS '도로명 주소';
COMMENT ON COLUMN member_tbl.jibun_address        IS '지번 주소';
COMMENT ON COLUMN member_tbl.detail_address       IS '상세 주소';
COMMENT ON COLUMN member_tbl.member_birthday      IS '생년월일';
COMMENT ON COLUMN member_tbl.member_manipay       IS '주요 결제수단 (account=계좌 / card=카드)';
COMMENT ON COLUMN member_tbl.member_joindate      IS '가입일 (기본값 SYSDATE)';
COMMENT ON COLUMN member_tbl.member_role          IS '권한 (user/admin), 기본값 user';
COMMENT ON COLUMN member_tbl.admin_type           IS '관리자 역할(책임자/관리자/강사), 기본값 관리자';

--------------------------------------------------------------------------------
-- 3) 제약조건
--------------------------------------------------------------------------------
-- 회원ID, PK값 선정(자동으로 UNIQUE 선정됨) 
ALTER TABLE member_tbl ADD CONSTRAINT member_tbl_pk     PRIMARY KEY (member_id);

-- 성별 선택 (남/녀)
ALTER TABLE member_tbl ADD CONSTRAINT member_gender_ch  CHECK (member_gender IN ('m','f'));

-- 계정 권한 (일반/관리자)
ALTER TABLE member_tbl ADD CONSTRAINT member_role_ch    CHECK (member_role   IN ('user','admin'));

-- 관리자 권한 (책임자, 관리자, 강사)
ALTER TABLE member_tbl ADD CONSTRAINT admin_type_ch
  CHECK ( member_role <> 'admin' OR admin_type IN ('책임자','관리자','강사') );

-- 주요 결제수단(계좌, 카드)
ALTER TABLE member_tbl ADD CONSTRAINT member_manipay_ch CHECK (member_manipay IN ('account','card'));

-- UNIQUE(DB값 중복금지 선정)...(이메일/휴대폰)
ALTER TABLE member_tbl ADD CONSTRAINT member_email_un  UNIQUE (member_email);
ALTER TABLE member_tbl ADD CONSTRAINT member_mobile_un UNIQUE (member_mobile);

--------------------------------------------------------------------------------
-- 4) 트리거: 주요 결제수단 무결성 검증
--------------------------------------------------------------------------------
BEGIN
  EXECUTE IMMEDIATE 'DROP TRIGGER trg_member_manipay_chk';
EXCEPTION
  WHEN OTHERS THEN
    IF SQLCODE != -4080 THEN RAISE; END IF;
END;
/

-- 주요결제수단 전용 트리거 
CREATE OR REPLACE TRIGGER trg_member_manipay_chk
BEFORE UPDATE OF member_manipay ON member_tbl
FOR EACH ROW
DECLARE
    v_cnt NUMBER;
BEGIN
    IF :NEW.member_manipay = 'account' THEN
        SELECT COUNT(*) INTO v_cnt
          FROM account_tbl
         WHERE member_id    = :NEW.member_id
           AND account_main = 'Y';
        IF v_cnt = 0 THEN
            RAISE_APPLICATION_ERROR(-20061,
              '주요 결제수단이 계좌로 설정되었으나 대표계좌가 없습니다. 먼저 대표계좌를 지정하세요.');
        END IF;

    ELSIF :NEW.member_manipay = 'card' THEN
        SELECT COUNT(*) INTO v_cnt
          FROM card_tbl
         WHERE member_id = :NEW.member_id
           AND card_main = 'Y';
        IF v_cnt = 0 THEN
            RAISE_APPLICATION_ERROR(-20062,
              '주요 결제수단이 카드로 설정되었으나 대표카드가 없습니다. 먼저 대표카드를 지정하세요.');
        END IF;
    END IF;
END;
/
-- ✅ 결과: member_manipay 변경 시 반드시 대표 결제수단이 존재해야 함

-- 비활성화 쿼리
ALTER TRIGGER trg_member_manipay_chk DISABLE;
-- 활성화 쿼리 
ALTER TRIGGER trg_member_manipay_chk ENABLE;
-- 활성화 유무 체크
SELECT trigger_name, status
  FROM user_triggers
 WHERE trigger_name = 'TRG_MEMBER_MANIPAY_CHK';


--------------------------------------------------------------------------------
-- 4) 트리거(2) 250927 추가사항
--------------------------------------------------------------------------------
-- 기존 회원-연쇄삭제 트리거 제거(자식테이블을 만지면 ORA-04091 유발)
BEGIN EXECUTE IMMEDIATE 'DROP TRIGGER TRG_MEMBER_CASCADE_DELETE'; EXCEPTION WHEN OTHERS THEN NULL; END; -- 없으면 무시
/

-- 우회 플래그 패키지(세션 전역 변수) 준비
CREATE OR REPLACE PACKAGE trg_control AS g_skip_account_trigger BOOLEAN := FALSE; END trg_control; -- 회원 일괄삭제 동안 대표계좌 삭제검사 스킵
/

-- 대표계좌 단건 삭제 금지 트리거(평소엔 막고, 일괄삭제 때만 스킵)
CREATE OR REPLACE TRIGGER trg_account_block_delete_main
BEFORE DELETE ON account_tbl
FOR EACH ROW
BEGIN
IF trg_control.g_skip_account_trigger THEN RETURN; END IF; -- 회원 일괄삭제 중이면 통과
IF :OLD.account_main = 'Y' THEN RAISE_APPLICATION_ERROR(-20041,'대표계좌는 단독 삭제 불가'); END IF; -- 상시엔 대표계좌 삭제 금지
END;
/

-- FK를 ON DELETE CASCADE로 재정의(삭제 전파를 DB가 수행)
--    ※ 기존 이름과 다르면 실제 이름으로 바꿔서 실행
ALTER TABLE account_tbl  DROP CONSTRAINT fk_account_member; -- 계좌→회원 FK 제거(재생성 준비)
ALTER TABLE account_tbl  ADD  CONSTRAINT fk_account_member  FOREIGN KEY (member_id) REFERENCES member_tbl(member_id) ON DELETE CASCADE; -- 회원삭제→계좌 자동삭제
ALTER TABLE card_tbl     DROP CONSTRAINT fk_card_member; -- 카드→회원 FK 제거
ALTER TABLE card_tbl     ADD  CONSTRAINT fk_card_member   FOREIGN KEY (member_id) REFERENCES member_tbl(member_id) ON DELETE CASCADE; -- 회원삭제→카드 자동삭제
ALTER TABLE payment_tbl  DROP CONSTRAINT fk_payment_account; -- 결제→계좌 FK 제거
ALTER TABLE payment_tbl  ADD  CONSTRAINT fk_payment_account FOREIGN KEY (account_id) REFERENCES account_tbl(account_id) ON DELETE CASCADE; -- 계좌삭제→결제 자동삭제
ALTER TABLE payment_tbl  DROP CONSTRAINT fk_payment_card; -- 결제→카드 FK 제거
ALTER TABLE payment_tbl  ADD  CONSTRAINT fk_payment_card    FOREIGN KEY (card_id)    REFERENCES card_tbl(card_id)    ON DELETE CASCADE; -- 카드삭제→결제 자동삭제
ALTER TABLE payment_tbl  DROP CONSTRAINT fk_payment_member; -- 결제→회원 직접 FK 제거(중복 경로 제거로 충돌 방지)
-- 필요 시: ALTER TABLE payment_tbl DROP COLUMN member_id; -- 컬럼 자체가 쓸모없다면 주석 해제

-- 회원 삭제 트리거(경량): 자식 DML 절대 금지, 우회 플래그만 토글
-- → FK CASCADE가 알아서 삭제함(뮤테이팅 방지)
CREATE OR REPLACE TRIGGER trg_member_cascade_flag
FOR DELETE ON member_tbl
COMPOUND TRIGGER
BEFORE STATEMENT IS
BEGIN
trg_control.g_skip_account_trigger := TRUE; -- 회원삭제 시작: 대표계좌 단건 금지 트리거 잠시 우회
END BEFORE STATEMENT;
AFTER STATEMENT IS
BEGIN
trg_control.g_skip_account_trigger := FALSE; -- 회원삭제 종료: 우회 종료(항상 원복)
END AFTER STATEMENT;
END;
/
CREATE OR REPLACE TRIGGER trg_member_cascade_flag
FOR DELETE ON member_tbl
COMPOUND TRIGGER
  BEFORE STATEMENT IS
  BEGIN
    trg_control.g_skip_account_trigger := TRUE; -- 회원삭제 시작: 대표계좌 단건 금지 트리거 잠시 우회
  END BEFORE STATEMENT;
  AFTER STATEMENT IS
  BEGIN
    trg_control.g_skip_account_trigger := FALSE; -- 회원삭제 종료: 우회 종료(항상 원복)
  END AFTER STATEMENT;
END;
/


-- ─────────────────────────────────────────────────────────────────
-- 5) 검증(읽기 전용) : 모두 CASCADE인지, 상태 정상인지 확인
-- ─────────────────────────────────────────────────────────────────
SELECT uc.constraint_name,uc.table_name,
       '-> '||(SELECT p.table_name FROM user_constraints p WHERE p.constraint_name=uc.r_constraint_name) AS parent,
       uc.delete_rule
FROM user_constraints uc
WHERE uc.constraint_type='R'
  AND uc.table_name IN ('ACCOUNT_TBL','CARD_TBL','PAYMENT_TBL')
ORDER BY uc.table_name,uc.constraint_name; -- delete_rule가 모두 CASCADE여야 정상

SELECT trigger_name,status,triggering_event,trigger_type
FROM user_triggers
WHERE trigger_name IN ('TRG_ACCOUNT_BLOCK_DELETE_MAIN','TRG_MEMBER_CASCADE_FLAG'); -- 둘 다 ENABLED면 정상


--------------------------------------------------------------------------------
-- 5) 더미데이터 생성 (hong1 ~ hong10)
--------------------------------------------------------------------------------
BEGIN
  FOR i IN 1..10 LOOP
    DELETE FROM member_tbl WHERE member_id = 'hong' || TO_CHAR(i);  -- 재실행 안전

    INSERT INTO member_tbl (
      member_id, member_pw, member_name, member_gender,
      member_email, member_mobile, member_phone,
      zip, road_address, jibun_address, detail_address,
      member_birthday, member_joindate, member_role, admin_type
    ) VALUES (
      'hong' || TO_CHAR(i),
      '1234',
      '홍길동' || TO_CHAR(i),
      CASE WHEN MOD(i,2)=0 THEN 'm' ELSE 'f' END,
      'hong' || TO_CHAR(i) || '@example.com',
      '010-' || LPAD(TO_CHAR(1000 + i),4,'0') || '-' || LPAD(TO_CHAR(1000 + i),4,'0'),
      '02-'  || LPAD(TO_CHAR(500  + i),3,'0') || '-' || LPAD(TO_CHAR(1000 + i),4,'0'),
      '08395',
      N'서울특별시 구로구 새말로9길 45',
      N'서울특별시 구로구 구로동 123-45',
      N'101동 ' || TO_CHAR(100 + i) || '호',
      DATE '1990-01-01' + (i-1)*30,
      SYSDATE,
      'user',
      NULL
    );
  END LOOP;
  COMMIT;
END;
/

--------------------------------------------------------------------------------
-- 6) 권한/관리자 유형 부여
--------------------------------------------------------------------------------

-- hong1~7까진 일반회원
UPDATE member_tbl SET member_role='user',  admin_type=NULL  
WHERE member_id IN ('hong1','hong2','hong3','hong4','hong5','hong6','hong7');

-- hong 8~10까지 책임자, 강사, 관리자 권한 부여
UPDATE member_tbl SET member_role='admin', admin_type='책임자' WHERE member_id='hong8';
UPDATE member_tbl SET member_role='admin', admin_type='강사'   WHERE member_id='hong9';
UPDATE member_tbl SET member_role='admin', admin_type='관리자' WHERE member_id='hong10';

COMMIT;


--------------------------------------------------------------------------------
-- 🔧 [신규 트리거] 회원 주소/연락처 입력값 자동 NULL 보정
-- 목적: zip, member_phone, road_address, jibun_address 컬럼이
--       '', 'string', 공백 등일 때 자동으로 NULL로 변환
--------------------------------------------------------------------------------
BEGIN
  EXECUTE IMMEDIATE 'DROP TRIGGER trg_member_null_cleanup';
EXCEPTION
  WHEN OTHERS THEN
    IF SQLCODE != -4080 THEN RAISE; END IF; -- ORA-04080: 존재하지 않음 무시
END;
/

CREATE OR REPLACE TRIGGER trg_member_null_cleanup
BEFORE INSERT OR UPDATE ON member_tbl
FOR EACH ROW
BEGIN
  -- ✅ zip 컬럼 보정
  IF :NEW.zip IS NOT NULL THEN
    IF TRIM(:NEW.zip) IS NULL OR LOWER(TRIM(:NEW.zip)) = 'string' THEN
      :NEW.zip := NULL;
    END IF;
  END IF;

  -- ✅ member_phone 컬럼 보정
  IF :NEW.member_phone IS NOT NULL THEN
    IF TRIM(:NEW.member_phone) IS NULL OR LOWER(TRIM(:NEW.member_phone)) = 'string' THEN
      :NEW.member_phone := NULL;
    END IF;
  END IF;

  -- ✅ road_address 컬럼 보정
  IF :NEW.road_address IS NOT NULL THEN
    IF TRIM(:NEW.road_address) IS NULL OR LOWER(TRIM(:NEW.road_address)) = 'string' THEN
      :NEW.road_address := NULL;
    END IF;
  END IF;

  -- ✅ jibun_address 컬럼 보정
  IF :NEW.jibun_address IS NOT NULL THEN
    IF TRIM(:NEW.jibun_address) IS NULL OR LOWER(TRIM(:NEW.jibun_address)) = 'string' THEN
      :NEW.jibun_address := NULL;
    END IF;
  END IF;
END;
/
--------------------------------------------------------------------------------
-- 🔧 [신규 트리거] 회원 주소·연락처 입력값 자동 NULL 보정 (5컬럼 대상)
-- 목적: zip, member_phone, road_address, jibun_address, detail_address 컬럼이
--       '', 공백, 'string', 'STRING' 등일 때 NULL로 자동 변환
-- 적용대상: INSERT, UPDATE 시점
-- 작성일자: [2025-10-07]
--------------------------------------------------------------------------------
BEGIN
  EXECUTE IMMEDIATE 'DROP TRIGGER trg_member_null_cleanup';
EXCEPTION
  WHEN OTHERS THEN
    IF SQLCODE != -4080 THEN RAISE; END IF;  -- ORA-04080: trigger does not exist → 무시
END;
/
CREATE OR REPLACE TRIGGER trg_member_null_cleanup
BEFORE INSERT OR UPDATE ON member_tbl
FOR EACH ROW
BEGIN
  -- ✅ zip 보정
  IF :NEW.zip IS NOT NULL THEN
    IF TRIM(:NEW.zip) IS NULL OR LOWER(TRIM(:NEW.zip)) = 'string' THEN
      :NEW.zip := NULL;
    END IF;
  END IF;

  -- ✅ member_phone 보정
  IF :NEW.member_phone IS NOT NULL THEN
    IF TRIM(:NEW.member_phone) IS NULL OR LOWER(TRIM(:NEW.member_phone)) = 'string' THEN
      :NEW.member_phone := NULL;
    END IF;
  END IF;

  -- ✅ road_address 보정
  IF :NEW.road_address IS NOT NULL THEN
    IF TRIM(:NEW.road_address) IS NULL OR LOWER(TRIM(:NEW.road_address)) = 'string' THEN
      :NEW.road_address := NULL;
    END IF;
  END IF;

  -- ✅ jibun_address 보정
  IF :NEW.jibun_address IS NOT NULL THEN
    IF TRIM(:NEW.jibun_address) IS NULL OR LOWER(TRIM(:NEW.jibun_address)) = 'string' THEN
      :NEW.jibun_address := NULL;
    END IF;
  END IF;

  -- ✅ detail_address 보정
  IF :NEW.detail_address IS NOT NULL THEN
    IF TRIM(:NEW.detail_address) IS NULL OR LOWER(TRIM(:NEW.detail_address)) = 'string' THEN
      :NEW.detail_address := NULL;
    END IF;
  END IF;
END;
/

SELECT trigger_name, status, triggering_event, trigger_type
  FROM user_triggers
 WHERE trigger_name = 'TRG_MEMBER_NULL_CLEANUP';

--------------------------------------------------------------------------------
-- 7) 확인 조회
--------------------------------------------------------------------------------
SELECT
    member_id           AS "회원ID",
    member_name         AS "회원명",
    CASE member_gender WHEN 'm' THEN '남' WHEN 'f' THEN '여' END "성별",
    member_phone        AS "연락처",
    member_mobile       AS "휴대폰",
    member_email        AS "이메일",
    TO_CHAR(member_birthday, 'YYYY-MM-DD') AS "생년월일",
    member_manipay      AS "주요결제수단",
    TO_CHAR(member_joindate, 'YYYY-MM-DD') AS "가입일",
    member_role         AS "권한",
    admin_type          AS "관리자유형",
    member_pw
FROM member_tbl;
-- ORDER BY member_id;

SELECT
    member_id AS "회원ID",
    member_pw AS "패스워드",
    member_role         AS "권한",
    admin_type          AS "관리자유형"
FROM member_tbl;

--------------------------------------------------------------------------------
-- 테스트 계정들 전부 비번 1234로 설정
UPDATE member_tbl
   SET member_pw = '1234'
WHERE member_id IN ('hong1','hong2','hong3','hong4','hong5','hong6','hong7','hong8','hong9','hong10');

COMMIT;

--------------------------------------------------------------------------------
--  목적: member_role ≠ 'admin' 인 INSERT에서는 admin_type을 강제로 NULL 처리
--          → 모든 아이디(hong99 포함 전체)에 대해 admin_type이 NULL이어도 등록 가능
--          → 기존 CHECK(admin_type_ch) 규칙 보존(관리자만 값 강제)
--------------------------------------------------------------------------------

-- 0) 재실행 안전: 동일명 트리거 존재 시만 삭제 (ORA-04080 무시)
BEGIN
  EXECUTE IMMEDIATE 'DROP TRIGGER trg_member_admin_type_on_ins';
EXCEPTION
  WHEN OTHERS THEN
    IF SQLCODE != -4080 THEN RAISE; END IF;  -- ORA-04080: trigger does not exist
END;
/
-- 1) 트리거 생성: INSERT 시점에만 admin_type 정규화
CREATE OR REPLACE TRIGGER trg_member_admin_type_on_ins
BEFORE INSERT ON member_tbl
FOR EACH ROW
BEGIN
  -- ✅ 관리자가 아닌 경우(일반 회원): admin_type은 항상 NULL로 강제
  IF :NEW.member_role <> 'admin' THEN
    :NEW.admin_type := NULL;  -- 필드에 값이 들어와도 무시하고 NULL 저장
  END IF;

  -- ✅ 관리자(admin)인 경우: 기존 CHECK 제약에 따름
  --    admin_type은 ('책임자','관리자','강사') 중 하나여야 하며 NULL 불가(제약으로 보장)
END;
/


--------------------------------------------------------------------------------
-- 8-1) 💀 데이터 초기화 (안전 모드) 💀
--      - 더미(hong1~hong10) 회원만 정리 / 구조·제약 유지
--------------------------------------------------------------------------------
DELETE FROM member_tbl WHERE member_id LIKE 'hong%';
COMMIT;

--------------------------------------------------------------------------------
-- 8-2) 💀 ddl 블록까지 안전 삭제 💀
--      - 실제 구조 제거 (테스트 종료 시 사용)
--------------------------------------------------------------------------------
BEGIN EXECUTE IMMEDIATE 'DROP TRIGGER trg_member_manipay_chk'; EXCEPTION WHEN OTHERS THEN NULL; END;
/
BEGIN EXECUTE IMMEDIATE 'DROP TABLE member_tbl CASCADE CONSTRAINTS'; EXCEPTION WHEN OTHERS THEN NULL; END;
/

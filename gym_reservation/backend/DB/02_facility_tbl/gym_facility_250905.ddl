-- =========================================================
-- 🔧 공통: 스키마 고정 (DDL에 스키마 접두어 없음)
-- =========================================================
-- ALTER SESSION SET CURRENT_SCHEMA = gym;    -- 필요 시 사용

/* ========================================================================== *
 * 🏟️ 시설 DDL 통합 스크립트 (카테고리/시간당 이용료 반영)
 *   1) facility_tbl 생성(제약/인덱스/주석) + FK(member_tbl.member_id)
 *   2) 트리거:
 *      - trg_facility_insert : 담당자 권한 검증 (ADMIN + 강사만 허용)
 *      - trg_facility_mod_ts : 등록/수정 시각 자동 관리
 *   3) 계정/데이터:
 *      - hong9(ADMIN+강사)로 정상 INSERT
 *      - hong1(일반)로 농구장B 시도 → 트리거 차단(재현 유지)
 *   4) 더미데이터: 풋살장A/농구장A 성공, 농구장B 실패(차단)
 *   5) 확인 쿼리
 *   ※ facility_money = "1시간 이용료(원)"
 *   ※ 카테고리 = ('수영장','농구장','풋살장','배드민턴장','볼링장')
 * ========================================================================== */

--------------------------------------------------------------------------------
-- 0) 재실행 안전 드롭 (없으면 무시)
--------------------------------------------------------------------------------
BEGIN
  EXECUTE IMMEDIATE 'DROP TRIGGER trg_facility_insert';
EXCEPTION
  WHEN OTHERS THEN IF SQLCODE != -4080 THEN RAISE; END IF;  -- ORA-04080: 트리거 없음 → 무시
END;
/
BEGIN
  EXECUTE IMMEDIATE 'DROP TRIGGER trg_facility_mod_ts';
EXCEPTION
  WHEN OTHERS THEN IF SQLCODE != -4080 THEN RAISE; END IF;
END;
/
BEGIN
  EXECUTE IMMEDIATE 'DROP TABLE facility_tbl CASCADE CONSTRAINTS';
EXCEPTION
  WHEN OTHERS THEN IF SQLCODE != -942 THEN RAISE; END IF;   -- ORA-00942: 테이블 없음 → 무시
END;
/

--------------------------------------------------------------------------------
-- 1) 시설 테이블 생성 + 제약/인덱스/주석
--------------------------------------------------------------------------------
CREATE TABLE facility_tbl (
    facility_id         NUMBER         NOT NULL,                 -- 시설 고유 번호(PK)
    facility_name       VARCHAR2(100)  NOT NULL,                 -- 시설명
    member_id           VARCHAR2(20)   NOT NULL,                 -- 담당자 회원ID(FK→member_tbl.member_id)
    facility_phone      VARCHAR2(20),                            -- 연락처
    facility_content    CLOB,                                    -- 설명(HTML/텍스트)
    facility_image_path VARCHAR2(200),                           -- 이미지 경로
    facility_person_max NUMBER,                                  -- 최대 인원
    facility_person_min NUMBER,                                  -- 최소 인원
    facility_use        CHAR(1)        DEFAULT 'Y' NOT NULL,     -- 사용 여부(Y/N)
    facility_reg_date   DATE           DEFAULT SYSDATE NOT NULL, -- 등록일(기본 SYSDATE)
    facility_mod_date   DATE,                                    -- 수정일(UPDATE 시 자동)
    facility_open_time  DATE,                                    -- 운영 시작 시간
    facility_close_time DATE,                                    -- 운영 종료 시간
    facility_money      NUMBER(10)     DEFAULT 0 NOT NULL,       -- 1시간 이용료(원, 기본값 0)
    facility_type       VARCHAR2(50)   DEFAULT '수영장' NOT NULL -- 상위 카테고리(5종 고정)
);

COMMENT ON TABLE  facility_tbl                     IS '시설 마스터';
COMMENT ON COLUMN facility_tbl.facility_id         IS '시설 고유 번호';
COMMENT ON COLUMN facility_tbl.facility_name       IS '시설명';
COMMENT ON COLUMN facility_tbl.member_id           IS '담당자 회원ID(ADMIN+강사만 허용)';
COMMENT ON COLUMN facility_tbl.facility_phone      IS '연락처';
COMMENT ON COLUMN facility_tbl.facility_content    IS '설명(HTML/텍스트)';
COMMENT ON COLUMN facility_tbl.facility_image_path IS '이미지 경로';
COMMENT ON COLUMN facility_tbl.facility_person_max IS '최대 인원';
COMMENT ON COLUMN facility_tbl.facility_person_min IS '최소 인원';
COMMENT ON COLUMN facility_tbl.facility_use        IS '사용 여부(Y/N)';
COMMENT ON COLUMN facility_tbl.facility_reg_date   IS '등록일(기본 SYSDATE)';
COMMENT ON COLUMN facility_tbl.facility_mod_date   IS '수정일(UPDATE 시 자동)';
COMMENT ON COLUMN facility_tbl.facility_open_time  IS '운영 시작 시간';
COMMENT ON COLUMN facility_tbl.facility_close_time IS '운영 종료 시간';
COMMENT ON COLUMN facility_tbl.facility_money      IS '1시간 이용료(원, 기본값 0)';
COMMENT ON COLUMN facility_tbl.facility_type       IS '상위 카테고리(수영장/농구장/풋살장/배드민턴장/볼링장)';

-- 기본 제약
ALTER TABLE facility_tbl ADD CONSTRAINT facility_tbl_pk    PRIMARY KEY (facility_id);                          -- PK
ALTER TABLE facility_tbl ADD CONSTRAINT facility_use_ch    CHECK (facility_use IN ('Y','N'));                  -- 사용여부
ALTER TABLE facility_tbl ADD CONSTRAINT facility_person_ch CHECK (facility_person_max >= facility_person_min); -- 인원검증
ALTER TABLE facility_tbl ADD CONSTRAINT facility_type_ck   CHECK (facility_type IN
  ('수영장','농구장','풋살장','배드민턴장','볼링장'));                                                      -- 카테고리 제한

-- FK
ALTER TABLE facility_tbl
  ADD CONSTRAINT fk_facility_member
  FOREIGN KEY (member_id)
  REFERENCES member_tbl(member_id);                                                                           -- 담당자 FK

-- 인덱스
CREATE INDEX idx_facility_member ON facility_tbl(member_id);     -- 담당자 조회 성능
CREATE INDEX idx_facility_use    ON facility_tbl(facility_use);  -- 사용여부 필터
CREATE INDEX idx_facility_type   ON facility_tbl(facility_type); -- 카테고리 필터

--------------------------------------------------------------------------------
-- 2) 트리거 #1 : 담당자 권한 검증 (ADMIN + 강사만 허용)
--------------------------------------------------------------------------------
CREATE OR REPLACE TRIGGER trg_facility_insert
BEFORE INSERT OR UPDATE ON facility_tbl
FOR EACH ROW
DECLARE
    v_role_raw   member_tbl.member_role%TYPE;   -- 원시 권한값(예: 'ADMIN')
    v_type_raw   member_tbl.admin_type%TYPE;    -- 원시 관리자유형(예: '강사')
    v_role_norm  VARCHAR2(20);                  -- 정규화 권한값(대문자)
    v_type_norm  VARCHAR2(40);                  -- 정규화 유형값
BEGIN
    SELECT member_role, admin_type
      INTO v_role_raw, v_type_raw
      FROM member_tbl
     WHERE member_id = :NEW.member_id;

    v_role_norm := UPPER(TRIM(NVL(v_role_raw, '')));
    v_type_norm := TRIM(NVL(v_type_raw, ''));

    IF v_role_norm <> 'ADMIN' OR v_type_norm <> '강사' THEN
        RAISE_APPLICATION_ERROR(-20001, '시설 담당자는 member_role=ADMIN 이고 admin_type=강사 인 계정만 가능합니다.');
    END IF;

EXCEPTION
    WHEN NO_DATA_FOUND THEN
        RAISE_APPLICATION_ERROR(-20002, '시설 담당자(member_id)가 회원 테이블에 존재하지 않습니다.');
END;
/
-- ✅ ADMIN + 강사만 통과, 그 외/미존재는 ORA-20001/20002 발생

--------------------------------------------------------------------------------
-- 3) 트리거 #2 : 등록/수정 시각 자동 관리
--------------------------------------------------------------------------------
CREATE OR REPLACE TRIGGER trg_facility_mod_ts
BEFORE INSERT OR UPDATE ON facility_tbl
FOR EACH ROW
BEGIN
  IF INSERTING THEN
    :NEW.facility_reg_date := NVL(:NEW.facility_reg_date, SYSDATE); -- 등록일 기본값 보정
    :NEW.facility_mod_date := NULL;                                  -- 신규는 수정일 없음
  ELSIF UPDATING THEN
    :NEW.facility_mod_date := SYSDATE;                               -- 수정 시각 자동 기록
  END IF;
END;
/
-- ✅ 실제 UPDATE시에만 수정일 기록


--------------------------------------------------------------------------------
-- 추가) 자동 시퀀스 생성
--------------------------------------------------------------------------------

-- PK용 시퀀스
CREATE SEQUENCE facility_seq START WITH 1 INCREMENT BY 1 NOCACHE;

-- PK 자동 세팅 트리거
CREATE OR REPLACE TRIGGER trg_facility_pk
BEFORE INSERT ON facility_tbl
FOR EACH ROW
BEGIN
  IF :NEW.facility_id IS NULL THEN
    :NEW.facility_id := facility_seq.NEXTVAL;
  END IF;
END;
/


--------------------------------------------------------------------------------
-- 4) 더미데이터 (원문 계정/값 보존, 카테고리만 지정)
--------------------------------------------------------------------------------
BEGIN
  DELETE FROM facility_tbl WHERE facility_id IN (1,2,3);  -- 재실행 대비 동일 PK만 정리
  COMMIT;
EXCEPTION WHEN OTHERS THEN NULL;  -- 오류 무시(재실행 안전)
END;
/

-- (1) 풋살장A  → 정상 (담당자 hong9 : ADMIN+강사)
INSERT INTO facility_tbl (
    facility_id, facility_name, member_id, facility_phone,
    facility_content, facility_image_path,
    facility_person_max, facility_person_min,
    facility_use, facility_reg_date,
    facility_open_time, facility_close_time,
    facility_money, facility_type
) VALUES (
    1, '풋살장A', 'hong9', '031-1111-1111',
    '풋살장A입니다.', NULL,
    50, 20,
    'Y', SYSDATE,
    TRUNC(SYSDATE) + (8/24),
    TRUNC(SYSDATE) + (22/24),
    80000, '풋살장'
);

-- (2) 농구장A  → 정상 (담당자 hong9)
INSERT INTO facility_tbl (
    facility_id, facility_name, member_id, facility_phone,
    facility_content, facility_image_path,
    facility_person_max, facility_person_min,
    facility_use, facility_reg_date,
    facility_open_time, facility_close_time,
    facility_money, facility_type
) VALUES (
    2, '농구장A', 'hong9', '031-2222-2222',
    '농구장A입니다.', NULL,
    50, 20,
    'Y', SYSDATE,
    TRUNC(SYSDATE) + (8/24),
    TRUNC(SYSDATE) + (22/24),
    50000, '농구장'
);

-- (3) 농구장B  → 강사권한 없음(hong1) → 트리거 차단(의도된 오류)
INSERT INTO facility_tbl (
     facility_id, facility_name, member_id, facility_phone,
     facility_content, facility_image_path,
     facility_person_max, facility_person_min,
     facility_use, facility_reg_date,
     facility_open_time, facility_close_time,
     facility_money, facility_type
 ) VALUES (
     3, '농구장B', 'hong1', '031-3333-3333',
     '농구장B', NULL,
     50, 20,
     'Y', SYSDATE,
     TRUNC(SYSDATE) + (8/24),
     TRUNC(SYSDATE) + (22/24),
     100000, '농구장'
 );
COMMIT;
-- ✅ 결과: (1)(2) 성공, (3)은 ORA-20001로 차단되지만 스크립트는 계속 실행됨

--------------------------------------------------------------------------------
-- 5) 확인 쿼리
--------------------------------------------------------------------------------
-- 목록 확인
SELECT
    f.facility_id         AS "시설번호",
    f.facility_name       AS "시설명",
    f.member_id           AS "담당자ID",
    f.facility_type       AS "카테고리",
    f.facility_money      AS "1시간이용료(원)",
    f.facility_person_max AS "최대인원",
    f.facility_person_min AS "최소인원",
    CASE f.facility_use WHEN 'Y' THEN '사용' ELSE '미사용' END AS "사용여부",
    TO_CHAR(f.facility_reg_date, 'YYYY-MM-DD HH24:MI')           AS "등록일",
    NVL(TO_CHAR(f.facility_mod_date, 'YYYY-MM-DD HH24:MI'), '-') AS "수정일",
    TO_CHAR(f.facility_open_time,  'HH24:MI')                    AS "운영시작",
    TO_CHAR(f.facility_close_time, 'HH24:MI')                    AS "운영종료"
FROM facility_tbl f
ORDER BY f.facility_id;

-- 카테고리별 건수(정확 집계)
SELECT facility_type, COUNT(*) AS cnt
FROM facility_tbl
GROUP BY facility_type
ORDER BY facility_type;

-- 농구장만 별도 확인(농구장A만 1건이어야 정상)
SELECT COUNT(*) AS "농구장_건수"
FROM facility_tbl
WHERE facility_type = '농구장';

--------------------------------------------------------------------------------
-- 6) 💀(선택) 데이터만 초기화   ※ 필요할 때만 수동 실행💀
--------------------------------------------------------------------------------
-- DELETE FROM facility_tbl; COMMIT;

--------------------------------------------------------------------------------
-- 7) 💀(선택) 구조까지 제거     ※ 테스트 종료 시 사용💀
--------------------------------------------------------------------------------
/*
BEGIN EXECUTE IMMEDIATE 'DROP TRIGGER trg_facility_insert';  EXCEPTION WHEN OTHERS THEN NULL; END;
/
BEGIN EXECUTE IMMEDIATE 'DROP TRIGGER trg_facility_mod_ts';  EXCEPTION WHEN OTHERS THEN NULL; END;
/
BEGIN EXECUTE IMMEDIATE 'DROP TABLE facility_tbl CASCADE CONSTRAINTS'; EXCEPTION WHEN OTHERS THEN NULL; END;
/
*/

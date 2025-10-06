-- =========================================================
-- 🔧 공통: 스키마 고정 (DDL에 스키마 접두어 없음)
-- =========================================================
-- ALTER SESSION SET CURRENT_SCHEMA = gym;

--------------------------------------------------------------------------------
-- 0) 재실행 안전 드롭 (DROP TABLE, DROP SEQUENCE, DROP INDEX)
--    - 기존 객체가 있으면 삭제, 없으면 무시
--------------------------------------------------------------------------------
BEGIN EXECUTE IMMEDIATE 'DROP TABLE file_tbl CASCADE CONSTRAINTS';
EXCEPTION WHEN OTHERS THEN
    IF SQLCODE != -942 THEN -- ORA-00942: 테이블이 존재하지 않음
        RAISE;
    END IF;
END;
/
BEGIN EXECUTE IMMEDIATE 'DROP SEQUENCE seq_file_id';
EXCEPTION WHEN OTHERS THEN
    IF SQLCODE != -2289 THEN -- ORA-02289: 시퀀스가 존재하지 않음
        RAISE;
    END IF;
END;
/
BEGIN EXECUTE IMMEDIATE 'DROP INDEX idx_file_target';
EXCEPTION WHEN OTHERS THEN
    IF SQLCODE != -1418 THEN -- ORA-01418: 인덱스 존재하지 않음
        RAISE;
    END IF;
END;
/

--------------------------------------------------------------------------------
-- 1) 첨부파일(file_tbl) 테이블 생성
--------------------------------------------------------------------------------
CREATE TABLE file_tbl (
    file_id           NUMBER         NOT NULL,                 -- 파일 고유번호(PK)
    file_target_type  VARCHAR2(20)   NOT NULL,                 -- 대상 종류(board/content/facility 등)
    file_target_id    VARCHAR2(20)   NOT NULL,                 -- 대상의 고유 ID(문자)
    file_name         VARCHAR2(200)  NOT NULL,                 -- 업로드 원본 파일명
    file_path         VARCHAR2(500)  NOT NULL,                 -- 서버/로컬 저장 경로
    file_type         VARCHAR2(50)   DEFAULT '본문' NOT NULL,  -- 파일 용도 ('썸네일'|'본문')
    file_ext          VARCHAR2(20),                             -- 확장자 (jpg, png, pdf 등)
    file_size         NUMBER,                                   -- 파일 크기(byte)
    file_reg_date     DATE           DEFAULT SYSDATE NOT NULL   -- 파일 등록일
);

-- 📌 COMMENT
COMMENT ON TABLE  file_tbl                  IS '첨부파일 정보(단순 저장소. FK 연결 불필요)';
COMMENT ON COLUMN file_tbl.file_id          IS '파일 고유번호 (PK)';
COMMENT ON COLUMN file_tbl.file_target_type IS '첨부 대상 종류 (board/content/facility 등)';
COMMENT ON COLUMN file_tbl.file_target_id   IS '첨부 대상의 고유 ID(문자, FK 아님)';
COMMENT ON COLUMN file_tbl.file_name        IS '원본 파일 이름';
COMMENT ON COLUMN file_tbl.file_path        IS '저장 경로(절대/상대/URL)';
COMMENT ON COLUMN file_tbl.file_type        IS '파일 용도 (''썸네일''|''본문'')';
COMMENT ON COLUMN file_tbl.file_ext         IS '확장자 (jpg, png, pdf 등)';
COMMENT ON COLUMN file_tbl.file_size        IS '파일 크기 (byte)';
COMMENT ON COLUMN file_tbl.file_reg_date    IS '파일 등록일';

-- 📌 제약조건
ALTER TABLE file_tbl ADD CONSTRAINT file_tbl_pk  PRIMARY KEY (file_id);
ALTER TABLE file_tbl ADD CONSTRAINT file_type_ch CHECK (file_type IN ('썸네일','본문'));

-- 📌 인덱스
CREATE INDEX idx_file_target ON file_tbl (file_target_type, file_target_id);

--------------------------------------------------------------------------------
-- 2) 시퀀스 생성 (file_id 자동 증가) - 재실행 안전 처리
--------------------------------------------------------------------------------
BEGIN
  EXECUTE IMMEDIATE q'[
	  CREATE SEQUENCE seq_file_id START WITH 1 INCREMENT BY 1 NOCACHE NOCYCLE
  ]';
EXCEPTION WHEN OTHERS THEN
  IF SQLCODE != -955 THEN -- ORA-00955: 이미 존재
    RAISE;
  END IF;
END;
/

--------------------------------------------------------------------------------
-- 3) 샘플 데이터 삽입
--------------------------------------------------------------------------------
INSERT INTO file_tbl (
    file_id, file_target_type, file_target_id,
    file_name, file_path, file_type, file_ext, file_size
) VALUES (
    seq_file_id.NEXTVAL,
    'content',   -- 어느 모듈의 파일인지
    '1001',      -- 대상 PK
    '001.jpg',
    'd:\developer_project\gym_reservation\backend\file\images\001.jpg',
    '본문',
    'jpg',
    2048
);
COMMIT;


--------------------------------------------------------------------------------
-- [250923추가] 올린이(회원ID) 컬럼 추가
--------------------------------------------------------------------------------
BEGIN
  EXECUTE IMMEDIATE q'[
    ALTER TABLE file_tbl ADD (member_id VARCHAR2(20))
  ]';  -- 컬럼 1개 추가만 수행(제약/인덱스/FK는 건드리지 않음)
EXCEPTION
  WHEN OTHERS THEN
    IF SQLCODE != -1430 THEN  -- ORA-01430: column being added already exists
      RAISE;                  -- 그 외 오류는 그대로 발생시켜 문제를 드러냄
    END IF;
END;
/

-- 📌 컬럼 설명 주석(선택)
BEGIN
  EXECUTE IMMEDIATE q'[
    COMMENT ON COLUMN file_tbl.member_id IS '파일 업로더 회원ID(hong1~10 등)'
  ]';
EXCEPTION
  WHEN OTHERS THEN
    NULL; -- 주석은 필수 아님, 실패해도 무시
END;
/

--------------------------------------------------------------------------------
-- 4) 확인 쿼리
--------------------------------------------------------------------------------
SELECT file_id, member_id, file_target_type, file_target_id, file_name, file_ext, file_size,
       file_path, TO_CHAR(file_reg_date,'YYYY-MM-DD HH24:MI') AS reg_dt
  FROM file_tbl
 ORDER BY file_id;

--------------------------------------------------------------------------------
-- 5) 💀 데이터 초기화/삭제 블록 💀
--------------------------------------------------------------------------------
-- 샘플 데이터만 삭제
-- DELETE FROM file_tbl WHERE file_target_type='content' AND file_target_id='1001';
-- COMMIT;

-- 전체 데이터 삭제
-- DELETE FROM file_tbl;
-- COMMIT;

-- 구조 자체 삭제 (테스트 종료 시)
-- DROP SEQUENCE seq_file_id;
-- DROP INDEX idx_file_target;
-- DROP TABLE file_tbl CASCADE CONSTRAINTS;

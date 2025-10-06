-- =========================================================
-- 🔧 공통: 스키마 고정 (DDL에 스키마 접두어 없음)
-- =========================================================
-- ALTER SESSION SET CURRENT_SCHEMA = gym;


/*
8월 23일 기준으로 비밀글 컬럼 추가
*/
--------------------------------------------------------------------------------
-- 0) 재실행 안전 드롭
--------------------------------------------------------------------------------
BEGIN
  EXECUTE IMMEDIATE 'DROP TABLE post_tbl CASCADE CONSTRAINTS';
EXCEPTION
  WHEN OTHERS THEN
    IF SQLCODE != -942 THEN RAISE; END IF;  -- ORA-00942: 테이블 없음 → 무시
END;
/
BEGIN
  EXECUTE IMMEDIATE 'DROP SEQUENCE post_seq';
EXCEPTION
  WHEN OTHERS THEN
    IF SQLCODE != -2289 THEN RAISE; END IF; -- ORA-02289: 시퀀스 없음 → 무시
END;
/

--------------------------------------------------------------------------------
-- 1) post_tbl 테이블 생성  (+ post_secret 추가)
--------------------------------------------------------------------------------
CREATE TABLE post_tbl (
    post_id         NUMBER          NOT NULL,                 -- 게시글 고유번호 (PK)
    board_id        NUMBER          NOT NULL,                 -- 게시판 ID (FK → board_tbl.board_id)
    post_title      VARCHAR2(200)   NOT NULL,                 -- 게시글 제목
    post_content    CLOB            NOT NULL,                 -- 게시글 내용 (HTML 가능)
    member_id       VARCHAR2(20)    NOT NULL,                 -- 작성자 ID (FK → member_tbl.member_id)
    post_reg_date   DATE DEFAULT SYSDATE NOT NULL,            -- 등록일 (기본값 SYSDATE)
    post_mod_date   DATE,                                     -- 수정일 (수정시 갱신)
    post_view_count NUMBER DEFAULT 0,                         -- 조회수 (기본값 0)
    post_notice     CHAR(1) DEFAULT 'N' NOT NULL,             -- 공지글 여부 ('Y'/'N') 기본값 'N'
    post_secret     CHAR(1) DEFAULT 'N' NOT NULL,             -- ✅ 비밀글 여부 ('Y'/'N') 기본값 'N'
    post_type       VARCHAR2(20) DEFAULT '일반' NOT NULL      -- 게시글 유형 ('공지','일반')
);

--------------------------------------------------------------------------------
-- 2) 컬럼/테이블 주석
--------------------------------------------------------------------------------
COMMENT ON TABLE  post_tbl                 IS '게시글';
COMMENT ON COLUMN post_tbl.post_id         IS '게시글 고유번호 (PK)';
COMMENT ON COLUMN post_tbl.board_id        IS '게시판 ID (FK → board_tbl.board_id)';
COMMENT ON COLUMN post_tbl.post_title      IS '게시글 제목';
COMMENT ON COLUMN post_tbl.post_content    IS '게시글 내용 (HTML 가능)';
COMMENT ON COLUMN post_tbl.member_id       IS '작성자 ID (FK → member_tbl.member_id)';
COMMENT ON COLUMN post_tbl.post_reg_date   IS '등록일 (기본값 SYSDATE)';
COMMENT ON COLUMN post_tbl.post_mod_date   IS '수정일 (수정시 갱신)';
COMMENT ON COLUMN post_tbl.post_view_count IS '조회수 (기본값 0)';
COMMENT ON COLUMN post_tbl.post_notice     IS '공지글 여부 (기본값 N)';
COMMENT ON COLUMN post_tbl.post_secret     IS '비밀글 여부 (Y/N, 기본값 N)';
COMMENT ON COLUMN post_tbl.post_type       IS '게시글 유형 (공지/일반)';

--------------------------------------------------------------------------------
-- 3) 제약조건
--------------------------------------------------------------------------------
-- 게시글 id를 PK값으로 선정
ALTER TABLE post_tbl ADD CONSTRAINT post_tbl_pk PRIMARY KEY (post_id);

-- 게시판 id 외래키 설정
ALTER TABLE post_tbl ADD CONSTRAINT fk_post_board
  FOREIGN KEY (board_id) REFERENCES board_tbl(board_id);

-- 회원 id 외래키 설정
ALTER TABLE post_tbl ADD CONSTRAINT fk_post_member
  FOREIGN KEY (member_id) REFERENCES member_tbl(member_id);

-- 공지글 여부의 제약 조건 (기본값 N)
ALTER TABLE post_tbl ADD CONSTRAINT post_notice_CH
  CHECK (post_notice IN ('Y','N'));

-- ✅ 비밀글 여부 제약 조건 (기본값 N)
ALTER TABLE post_tbl ADD CONSTRAINT post_secret_CH
  CHECK (post_secret IN ('Y','N'));

-- 게시글 유형의 제약 조건 (기본값 일반)
ALTER TABLE post_tbl ADD CONSTRAINT post_type_CH
  CHECK (post_type IN ('공지','일반'));

--------------------------------------------------------------------------------
-- 4) 시퀀스 생성
--------------------------------------------------------------------------------
--CREATE SEQUENCE post_seq
--  START WITH 1
--  INCREMENT BY 1
--  NOCACHE
--  NOCYCLE;

-- PK 증가 규칙(전송 실패했을 시 증가X)
  BEGIN
  EXECUTE IMMEDIATE q'[
     CREATE SEQUENCE post_seq START WITH 1 INCREMENT BY 1 NOCACHE NOCYCLE
  ]';
EXCEPTION WHEN OTHERS THEN IF SQLCODE != -955 THEN RAISE; END IF;
END;
/

-- 시퀀스 생성 확인
--SELECT sequence_name FROM user_sequences WHERE sequence_name = 'POST_SEQ';


--------------------------------------------------------------------------------
-- 4.1) PK값 자동 증가(전송 실패했을 시 증가X) 트리거 추가
--------------------------------------------------------------------------------
CREATE OR REPLACE TRIGGER trg_post_before_insert
BEFORE INSERT ON post_tbl
FOR EACH ROW
BEGIN
  IF :NEW.post_id IS NULL THEN
    SELECT post_seq.NEXTVAL INTO :NEW.post_id FROM dual;
  END IF;
END;
/

--------------------------------------------------------------------------------
-- 5) 더미 데이터 생성  (post_secret은 기본값 'N' 자동 적용)
--------------------------------------------------------------------------------
-- 공지글 
INSERT INTO post_tbl (
    post_id, board_id, post_title, post_content, member_id, post_notice, post_type, post_secret
) VALUES (
    post_seq.NEXTVAL, 1, '첫 번째 공지사항', '게시판 공지사항입니다.', 'hong10', 'Y', '공지', 'N'
);

-- 일반글
INSERT INTO post_tbl (
    post_id, board_id, post_title, post_content, member_id, post_notice, post_type, post_secret
) VALUES (
    post_seq.NEXTVAL, 1, '첫 번째 일반글', '일반 게시글 테스트입니다.', 'hong1', 'N', '일반', 'N'
);

-- 비밀글
INSERT INTO post_tbl (
    post_id, board_id, post_title, post_content, member_id, post_notice, post_type, post_secret
) VALUES (
    post_seq.NEXTVAL, 1, '첫 번째 비밀글', '일반 게시글(비밀) 테스트입니다.', 'hong1', 'N', '일반', 'Y'
);

COMMIT;
--------------------------------------------------------------------------------
-- 250910 시퀀스 수정
/* 전송 실패해도 PK값 증가했던 이유 : 처리 방식이 이중 경로로 되어 있음
    - DDL: `trg_post_before_insert` 트리거에서 `post_seq.NEXTVAL`을 호출함.
    - XML: `<selectKey order="BEFORE"> SELECT post_seq.NEXTVAL …`로 또 NEXTVAL호출 */
--------------------------------------------------------------------------------
-- 기존 트리거 삭제 및 재실행
BEGIN
EXECUTE IMMEDIATE 'DROP TRIGGER trg_post_before_insert';
EXCEPTION WHEN OTHERS THEN NULL;
END;
/

-- PostMapper.xml에서 
-- 1. BEFORE selectKey 제거
-- 2. VALUES에 직접 post_seq.NEXTVAL
-- 3. AFTER selectKey로 CURRVAL 조회

-- 시퀀스는 심플하게 유지
BEGIN
  EXECUTE IMMEDIATE q'[CREATE SEQUENCE post_seq START WITH 1 INCREMENT BY 1 NOCACHE NOCYCLE]';
EXCEPTION WHEN OTHERS THEN IF SQLCODE != -955 THEN RAISE; END IF;
END;
/
--------------------------------------------------------------------------------
-- 2025년 9월 11일
-------------------------------------------------------------------------------

-- 1) 기존 트리거 제거 : 재실행 안전(없으면 무시)

BEGIN
  EXECUTE IMMEDIATE 'DROP TRIGGER trg_post_before_insert';     -- 이전 트리거 제거
EXCEPTION
  WHEN OTHERS THEN NULL;                                       -- 없으면 조용히 통과
END;
/

-- 2) 검증 + PK할당 트리거
--    핵심: post_type 검증에 "실패"하면 RAISE → NEXTVAL 호출 자체가 없음
--          검증 "성공" 후에만 post_seq.NEXTVAL로 PK 채번 
CREATE OR REPLACE TRIGGER trg_post_before_insert
BEFORE INSERT ON post_tbl
FOR EACH ROW
DECLARE
  v_post_type VARCHAR2(20);                                     -- 입력된 게시글 유형 임시 변수
BEGIN
  -- (A) post_type 사전검증 : '공지' 또는 '일반'만 허용(대소문자 구분)
  v_post_type := :NEW.post_type;

  IF v_post_type IS NULL
     OR v_post_type NOT IN ('공지','일반') THEN
    -- 🔒 검증 실패 : 시퀀스를 호출하지 않고 즉시 차단 → PK 증가 없음
    RAISE_APPLICATION_ERROR(-20001, 'post_type 값은 ''공지'' 또는 ''일반''만 허용됩니다.');
  END IF;

  -- (B) PK 자동할당 : 검증을 통과했을 때만 시퀀스 호출(NEXTVAL 소비)
  IF :NEW.post_id IS NULL THEN
    SELECT post_seq.NEXTVAL INTO :NEW.post_id FROM dual;        -- PK 채번(검증 성공 후에만)
  END IF;
END;
/


-------------------------------------------------------------------------------
--개선 시퀀스
-- 게시판별(post_id) 독립 증가 + 실패 시 번호 증가 없음 구성
-------------------------------------------------------------------------------
-- 0) 기존 전역 시퀀스/트리거 제거
BEGIN
EXECUTE IMMEDIATE 'DROP TRIGGER trg_post_before_insert';
EXCEPTION
WHEN OTHERS THEN
IF SQLCODE != -4080 THEN RAISE; END IF;  -- ORA-04080: 트리거 없음 → 무시
END;
/
BEGIN
 EXECUTE IMMEDIATE 'DROP SEQUENCE post_seq';
EXCEPTION
WHEN OTHERS THEN
IF SQLCODE != -2289 THEN RAISE; END IF;  -- ORA-02289: 시퀀스 없음 → 무시
END;
/

-- 1) 게시판별 채번 테이블 생성(재실행 안전)
--    - board_id별로 마지막 부여된 post_id를 보관
BEGIN
EXECUTE IMMEDIATE q'[
    CREATE TABLE board_post_seq_tbl (
      board_id      NUMBER       NOT NULL, -- 게시판ID
      last_post_id  NUMBER       NOT NULL, -- 마지막 게시글ID
      CONSTRAINT board_post_seq_pk PRIMARY KEY (board_id)
    )
  ]';
EXCEPTION
  WHEN OTHERS THEN
    IF SQLCODE != -955 THEN RAISE; END IF;  -- ORA-00955: 이미 존재 → 무시
END;
/

-- 2) post_tbl의 PK를 (board_id, post_id) 복합키로 전환
--    - 재실행 안전: 기존 PK 있으면 제거 후 재생성
DECLARE
  v_exists NUMBER;
BEGIN
  -- 기존 PK 명 확인 후 제거 시도(이름이 post_tbl_pk라고 가정)
  BEGIN
    EXECUTE IMMEDIATE 'ALTER TABLE post_tbl DROP CONSTRAINT post_tbl_pk';
  EXCEPTION
    WHEN OTHERS THEN
      IF SQLCODE != -2443 THEN RAISE; END IF; -- ORA-02443: 제약 없음 → 무시
  END;

  -- 복합 PK 생성: 동일 게시판 내에서만 post_id 유일
  EXECUTE IMMEDIATE '
    ALTER TABLE post_tbl
      ADD CONSTRAINT post_tbl_pk
      PRIMARY KEY (board_id, post_id)
  ';
END;
/

-- 3) BEFORE INSERT 트리거(검증 선행 → 성공 시에만 증가)
--    - 핵심: 검증 실패 시 RAISE로 즉시 중단 → 채번 행 업데이트도 롤백 → 번호 증가 없음
--    - 성공 경로에서만 board_post_seq_tbl의 last_post_id를 +1 후 NEW.post_id에 반영
CREATE OR REPLACE TRIGGER trg_post_before_insert
BEFORE INSERT ON post_tbl
FOR EACH ROW
DECLARE
  v_last NUMBER;             -- 현재 게시판의 마지막 post_id
BEGIN
  -- (A) 입력값 검증: 허용값 위반 시 즉시 차단(번호 증가 없음)
  IF :NEW.post_type IS NULL OR :NEW.post_type NOT IN ('공지','일반') THEN
    RAISE_APPLICATION_ERROR(-20001, 'post_type 값은 ''공지'' 또는 ''일반''만 허용됩니다.');
  END IF;

  IF :NEW.post_notice NOT IN ('Y','N') THEN
    RAISE_APPLICATION_ERROR(-20002, 'post_notice 값은 ''Y'' 또는 ''N''만 허용됩니다.');
  END IF;

  IF :NEW.post_secret NOT IN ('Y','N') THEN
    RAISE_APPLICATION_ERROR(-20003, 'post_secret 값은 ''Y'' 또는 ''N''만 허용됩니다.');
  END IF;

  IF :NEW.board_id IS NULL THEN
    RAISE_APPLICATION_ERROR(-20004, 'board_id는 NULL일 수 없습니다.');
  END IF;

  -- (B) 게시판별 채번 행을 잠금 후 획득(없으면 생성)
  BEGIN
    SELECT last_post_id
      INTO v_last
      FROM board_post_seq_tbl
     WHERE board_id = :NEW.board_id
     FOR UPDATE; -- 행잠금으로 동시성 보장
  EXCEPTION
    WHEN NO_DATA_FOUND THEN
      INSERT INTO board_post_seq_tbl(board_id, last_post_id)
      VALUES (:NEW.board_id, 0);

      SELECT last_post_id
        INTO v_last
        FROM board_post_seq_tbl
       WHERE board_id = :NEW.board_id
       FOR UPDATE;
  END;

  -- (C) PK 채번: 검증 통과 후에만 증가·반영
  IF :NEW.post_id IS NULL THEN
    v_last := v_last + 1;                       -- 다음 번호
    :NEW.post_id := v_last;                     -- NEW 행에 적용
    UPDATE board_post_seq_tbl
       SET last_post_id = v_last
     WHERE board_id = :NEW.board_id;            -- 카운터 갱신(동일 트랜잭션 내)
  END IF;
END;
/

--------------------------------------------------------------------------------
-- 6) 확인 조회
--------------------------------------------------------------------------------
SELECT
    p.board_id         AS "게시판ID (FK)",
    p.post_id          AS "게시글ID (PK)",
    p.post_title       AS "제목",
    p.member_id        AS "작성자ID (FK)",
    CASE p.post_notice WHEN 'Y' THEN '공지' ELSE '일반' END AS "공지여부",
    p.post_secret      AS "비밀글(Y/N)",
    p.post_type        AS "게시글유형",
    p.post_content     AS "게시글내용",
    p.post_view_count  AS "조회수",
    TO_CHAR(p.post_reg_date,'YYYY-MM-DD HH24:MI')           AS "등록일",
    NVL(TO_CHAR(p.post_mod_date,'YYYY-MM-DD HH24:MI'), '-') AS "수정일"
FROM post_tbl p
ORDER BY p.post_id;

-- 1) post_tbl 데이터 삭제
DELETE FROM post_tbl;

-- 2) 게시판별 시퀀스 카운터도 초기화(필요 시)
DELETE FROM board_post_seq_tbl;

-- 3) 트랜잭션 확정
COMMIT;
--------------------------------------------------------------------------------
-- 6-2) 확인 조회 (+첨부파일 포함)
--------------------------------------------------------------------------------
SELECT
    p.board_id         AS "게시판ID (FK)",                -- 게시판 FK
    p.post_id          AS "게시글ID (PK)",                -- 게시글 PK
    p.post_title       AS "제목",                         -- 제목
    p.member_id        AS "작성자ID (FK)",                -- 작성자
    CASE p.post_notice WHEN 'Y' THEN '공지' ELSE '일반' END AS "공지여부",  -- 공지/일반
    p.post_secret      AS "비밀글(Y/N)",                  -- 비밀글 여부
    p.post_type        AS "게시글유형",                   -- 유형
    p.post_content     AS "게시글내용",                   -- 내용
    p.post_view_count  AS "조회수",                       -- 조회수
    TO_CHAR(p.post_reg_date,'YYYY-MM-DD HH24:MI')           AS "등록일",   -- 등록일
    NVL(TO_CHAR(p.post_mod_date,'YYYY-MM-DD HH24:MI'), '-') AS "수정일",   -- 수정일

    /* 🔽 첨부 개수: file_tbl에서 대상이 'post'이고 post_id 일치하는 행 수 */
    (
      SELECT COUNT(*)
      FROM file_tbl f
      WHERE f.file_target_type = 'post'                   -- 표준화된 타겟 구분값(운영에서 쓰는 값으로 통일)
        AND f.file_target_id   = TO_CHAR(p.post_id)       -- file_target_id(VARCHAR2) ↔ post_id(NUMBER) 매칭
    ) AS "첨부개수",

    /* 🔽 첨부 파일 목록: 파일명(파일ID)들을 콤마로 연결. 없으면 '-' 처리 */
    NVL((
      SELECT LISTAGG(f.file_name || ' (' || f.file_id || ')', ', ')
             WITHIN GROUP (ORDER BY f.file_id)
      FROM file_tbl f
      WHERE f.file_target_type = 'post'
        AND f.file_target_id   = TO_CHAR(p.post_id)
    ), '-') AS "첨부파일목록(파일명(파일ID))"

FROM post_tbl p
ORDER BY p.post_id;


--------------------------------------------------------------------------------
-- 7-1) 💀 데이터 초기화 (안전 모드) 💀
--      - 데이터만 삭제 / 구조·제약 유지
--------------------------------------------------------------------------------
DELETE FROM post_tbl;
COMMIT;

-- 시퀀스 재시작(선택)
BEGIN
  EXECUTE IMMEDIATE 'DROP SEQUENCE post_seq;
EXCEPTION
  WHEN OTHERS THEN
    IF SQLCODE != -2289 THEN RAISE; END IF; -- ORA-02289: 시퀀스 없음 → 무시
END;
/
CREATE SEQUENCE post_seq START WITH 1 INCREMENT BY 1 NOCACHE NOCYCLE;

--------------------------------------------------------------------------------
-- 7-2) 💀 ddl 블록까지 안전 삭제 💀
--      - 실제 구조 제거 (테스트 종료 시 사용)
--------------------------------------------------------------------------------

BEGIN EXECUTE IMMEDIATE 'DROP TABLE post_tbl CASCADE CONSTRAINTS'; EXCEPTION WHEN OTHERS THEN NULL; END;
/
BEGIN EXECUTE IMMEDIATE 'DROP SEQUENCE post_seq';                 EXCEPTION WHEN OTHERS THEN NULL; END;
/


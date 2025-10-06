-- =========================================================
-- 콘텐츠 테이블 + 트리거 + 시퀀스
-- =========================================================

-- 0) 기존 객체 안전 삭제
BEGIN EXECUTE IMMEDIATE 'DROP TABLE contents_tbl CASCADE CONSTRAINTS'; EXCEPTION WHEN OTHERS THEN IF SQLCODE != -942 THEN NULL; END IF; END;
/
BEGIN EXECUTE IMMEDIATE 'DROP TRIGGER trg_contents_writer_admin'; EXCEPTION WHEN OTHERS THEN IF SQLCODE != -4080 THEN NULL; END IF; END;
/
BEGIN EXECUTE IMMEDIATE 'DROP TRIGGER trg_contents_mod_ts'; EXCEPTION WHEN OTHERS THEN IF SQLCODE != -4080 THEN NULL; END IF; END;
/
BEGIN EXECUTE IMMEDIATE 'DROP SEQUENCE seq_content_id'; EXCEPTION WHEN OTHERS THEN IF SQLCODE != -2289 THEN NULL; END IF; END;
/

-- 1) 테이블 생성
CREATE TABLE contents_tbl (
    content_id         NUMBER          NOT NULL,
    content_title      VARCHAR2(100)   NOT NULL,
    content_content    CLOB,
    member_id          VARCHAR2(20)    NOT NULL,
    content_reg_date   DATE            DEFAULT SYSDATE,
    content_mod_date   DATE,
    content_use        CHAR(1)         DEFAULT 'Y' NOT NULL,
    content_num        NUMBER(2),
    content_type       VARCHAR2(50)    DEFAULT '이용안내' NOT NULL
);

ALTER TABLE contents_tbl ADD CONSTRAINT contents_tbl_pk PRIMARY KEY (content_id);
ALTER TABLE contents_tbl ADD CONSTRAINT content_use_ch CHECK (content_use IN ('Y','N'));
ALTER TABLE contents_tbl ADD CONSTRAINT content_type_ch CHECK (content_type IN ('이용안내','상품/시설안내'));
ALTER TABLE contents_tbl
  ADD CONSTRAINT fk_contents_member FOREIGN KEY (member_id)
  REFERENCES member_tbl(member_id);

-- 2) 관리자만 작성 가능 트리거
CREATE OR REPLACE TRIGGER trg_contents_writer_admin
BEFORE INSERT OR UPDATE ON contents_tbl
FOR EACH ROW
DECLARE
    v_role member_tbl.member_role%TYPE;
BEGIN
    SELECT member_role INTO v_role
      FROM member_tbl
     WHERE member_id = :NEW.member_id;

    IF UPPER(NVL(v_role, '')) <> 'ADMIN' THEN
        RAISE_APPLICATION_ERROR(-20011, '콘텐츠 작성자는 관리자만 가능합니다.');
    END IF;
EXCEPTION
    WHEN NO_DATA_FOUND THEN
        RAISE_APPLICATION_ERROR(-20012, '작성자가 회원 테이블에 존재하지 않습니다.');
END;
/

-- 3) 수정일 자동 갱신 트리거
CREATE OR REPLACE TRIGGER trg_contents_mod_ts
BEFORE INSERT OR UPDATE ON contents_tbl
FOR EACH ROW
BEGIN
  IF INSERTING THEN
    :NEW.content_mod_date := NULL;
    :NEW.content_reg_date := NVL(:NEW.content_reg_date, SYSDATE);
  ELSIF UPDATING THEN
    :NEW.content_mod_date := SYSDATE;
  END IF;
END;
/

-----------------------------------------------------------
-- 4) 시퀀스 생성
ALTER SESSION SET CURRENT_SCHEMA = GYM;
BEGIN
  EXECUTE IMMEDIATE '
    CREATE SEQUENCE seq_content_id
      START WITH 1
      INCREMENT BY 1
      NOCACHE
      NOCYCLE
  ';
EXCEPTION
  WHEN OTHERS THEN
    IF SQLCODE != -955 THEN  -- ORA-00955: 이미 존재
      RAISE;
    END IF;
END;
/

SELECT sequence_name, increment_by, cache_size
FROM   user_sequences
WHERE  sequence_name = 'SEQ_CONTENT_ID';  -- Oracle은 기본 대문자 저장

SELECT owner, object_name, object_type, status
FROM   all_objects
WHERE  object_type = 'SEQUENCE'
AND    object_name = 'SEQ_CONTENT_ID';

INSERT INTO contents_tbl (
  content_id, content_title, content_content, member_id,
  content_use, content_num, content_type, content_reg_date
) VALUES (
  seq_content_id.NEXTVAL,               -- ★ 시퀀스 정상 동작 확인
  '이용 안내(시퀀스 확인)', '<p>본문</p>', 'hong10',  -- hong10은 admin(작성 가능)
  'Y', 99, '이용안내', SYSDATE
);
COMMIT;


SELECT content_id, content_title, member_id, content_type,
       TO_CHAR(content_reg_date,'YYYY-MM-DD HH24:MI') AS reg_at
FROM   contents_tbl
WHERE  content_title LIKE '이용 안내(시퀀스 확인)%'
ORDER  BY content_id DESC;

-----------------------------------------------------------

-- 5) 테스트 데이터
INSERT INTO contents_tbl (content_id, content_title, content_content, member_id, content_use, content_num, content_type)
VALUES (seq_content_id.NEXTVAL, '테스트 콘텐츠', '<p>테스트 본문입니다</p>', 'hong10', 'Y', 10, '이용안내');

COMMIT;


----------------------------------------------------------

-- 250918추가사항 콘텐츠번호(content_num) 중복 방지 유니크 제약
BEGIN
EXECUTE IMMEDIATE '
    ALTER TABLE contents_tbl
    ADD CONSTRAINT contents_tbl_num_un UNIQUE (content_num)
    ';
EXCEPTION
WHEN OTHERS THEN
-- ORA-02261: 동일한 고유키/PK가 이미 존재 → 재실행 시 무시
IF SQLCODE != -2261 THEN
RAISE;
END IF;
END;
/
/*
주의사항: 만약 이미 중복값이 있을 경우에는 실행 안됨, 
제약 존재 여부 부분에서 확인을 하고 적용이 안되어 있다면 중복항목 제거 후 재실행
*/ 

-- 제약 존재 여부
SELECT constraint_name, constraint_type, status
FROM   user_constraints
WHERE  table_name='CONTENTS_TBL'
AND    constraint_name='CONTENTS_TBL_NUM_UN';

-- 중복 상태 점검(제약 추가 전/후 참고용)
SELECT content_num, COUNT(*) cnt
FROM   contents_tbl
GROUP  BY content_num
HAVING COUNT(*) > 1;

-------------------------------------------------------------------
-- 6) 확인 조회 (조회 결과 확인용)
SELECT
    content_id      AS "콘텐츠ID",
    content_num     AS "콘텐츠번호",
    content_title   AS "제목",
    content_type    AS "카테고리",
    content_num     AS "정렬번호",
    member_id       AS "작성자ID",
    CASE content_use WHEN 'Y' THEN '사용' ELSE '미사용' END AS "사용여부",
    TO_CHAR(content_reg_date, 'YYYY-MM-DD HH24:MI') AS "작성일",
    NVL(TO_CHAR(content_mod_date, 'YYYY-MM-DD HH24:MI'), '-') AS "수정일"
FROM contents_tbl
ORDER BY content_num;

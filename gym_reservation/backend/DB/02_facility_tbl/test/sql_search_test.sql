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
    TO_CHAR(f.facility_close_time, 'HH24:MI')                    AS "운영종료",
    f.facility_image_path AS "이미지 경로"
FROM facility_tbl f
ORDER BY f.facility_id;


-------------------------------------------------------------------------------



SELECT
      f.facility_id,
      f.facility_name,
      f.member_id,
      f.facility_phone,
      f.facility_content,
      f.facility_image_path,
      f.facility_person_max,
      f.facility_person_min,
      f.facility_use,
      f.facility_reg_date,
      f.facility_mod_date,
      TO_CHAR(f.facility_open_time,'HH24:MI') AS facility_open_time,
      TO_CHAR(f.facility_close_time,'HH24:MI') AS facility_close_time,
      f.facility_money,
      f.facility_type 
FROM facility_tbl f
    WHERE  f.facility_name LIKE '%' || '풋살장A' || '%'
        AND f.facility_type = '풋살장' 
	    ORDER BY f.facility_id
      OFFSET 0 * 10 ROWS FETCH NEXT 10 ROWS ONLY;







--------------------------------------------------------------------------------
SELECT
      f.facility_id,
      f.facility_name,
      f.member_id,
      f.facility_phone,
      f.facility_content,
      f.facility_image_path,
      f.facility_person_max,
      f.facility_person_min,
      f.facility_use,
      f.facility_reg_date,
      f.facility_mod_date,
      TO_CHAR(f.facility_open_time,'HH24:MI') AS facility_open_time,
      TO_CHAR(f.facility_close_time,'HH24:MI') AS facility_close_time,
      f.facility_money,
      f.facility_type 
    FROM facility_tbl f
     WHERE  f.facility_name LIKE '%' || '풋살장A' || '%'

        AND f.facility_type = '풋살장' 

	    ORDER BY f.facility_id

      OFFSET 1 * 10 ROWS FETCH NEXT 10 ROWS ONLY;
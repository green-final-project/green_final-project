package com.gym.config.type;

import org.apache.ibatis.type.BaseTypeHandler;	// MyBatis 기본 TypeHandler 상속 클래스
import org.apache.ibatis.type.JdbcType;			// JDBC 타입(enum)
import java.sql.*;								// JDBC 관련 클래스(PreparedStatement, ResultSet, CallableStatement)


/**
 * BooleanYNTypeHandler
 *
 * 목적:
 * - DB의 CHAR(1) 컬럼 값('Y' 또는 'N') ↔ Java의 boolean(true/false) 변환 자동 처리
 * - 예) account_main CHAR(1) → Account.accountMain(boolean)
 *
 * 적용 방법:
 * - application.yml 에 mybatis.type-handlers-package: com.gym.config.type 추가
 * - 이후 매퍼(@Mapper, XML)에서 #{accountMain} 사용 시 자동으로 Y/N ↔ boolean 변환됨
 */

public class BooleanYNTypeHandler extends BaseTypeHandler<Boolean> {  // 🔄 Y/N ↔ boolean

	/**
     * setNonNullParameter
     * - Java → DB 입력 시 실행
     * - PreparedStatement(INSERT/UPDATE 등 파라미터 바인딩)에 boolean 값을 세팅
     *
     * @param ps        SQL 실행용 PreparedStatement 객체
     * @param i         파라미터 인덱스 (1부터 시작)
     * @param parameter 자바에서 전달된 boolean 값 (true/false)
     * @param jdbcType  JDBC 타입(enum) — 여기서는 사용 안 함
     *
     * 동작:
     * - parameter가 true → 'Y'
     * - parameter가 false 또는 null → 'N'
     */
    @Override
    public void setNonNullParameter(PreparedStatement ps, int i, Boolean parameter, JdbcType jdbcType)
            throws SQLException {
        ps.setString(i, parameter != null && parameter ? "Y" : "N");   // true→'Y', false/null→'N'
    }
    
/*=======================================================================================================*/    

    /**
     * getNullableResult(ResultSet, String)
     * - DB → Java 조회 시 실행 (컬럼명 기반)
     *
     * @param rs          SELECT 결과셋(ResultSet)
     * @param columnName  컬럼명
     * @return            'Y'면 true, 나머지(false/null 포함)는 false
     * v는 value의 약자
     */
    @Override
    public Boolean getNullableResult(ResultSet rs, String columnName) 
    		throws SQLException {
        String v = rs.getString(columnName);	// 컬럼값
        return "Y".equalsIgnoreCase(v);			// 'Y'면 true
    }

/*=======================================================================================================*/
    
    /**
     * getNullableResult(ResultSet, int)
     * - DB → Java 조회 시 실행 (컬럼 인덱스 기반)
     *
     * @param rs           SELECT 결과셋(ResultSet)
     * @param columnIndex  컬럼 인덱스 (1부터 시작)
     * @return             'Y'면 true, 아니면 false
     */
    @Override
    public Boolean getNullableResult(ResultSet rs, int columnIndex) 
    		throws SQLException {
        String v = rs.getString(columnIndex);
        return "Y".equalsIgnoreCase(v);
    }

    
/*=======================================================================================================*/
    
    /**
     * getNullableResult(CallableStatement, int)
     * - 프로시저/함수 OUT 파라미터 조회 시 실행
     *
     * @param cs           CallableStatement (프로시저 호출 객체)
     * @param columnIndex  OUT 파라미터 인덱스
     * @return             'Y'면 true, 아니면 false
     */
    @Override
    public Boolean getNullableResult(CallableStatement cs, int columnIndex) 
    		throws SQLException {
        String v = cs.getString(columnIndex);
        return "Y".equalsIgnoreCase(v);	// 
    }
}

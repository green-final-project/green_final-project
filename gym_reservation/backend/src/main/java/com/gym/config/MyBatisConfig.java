package com.gym.config;  // ⚙️ 설정 클래스 패키지

import javax.sql.DataSource;
// 💾 커넥션 풀(DataSource)
import org.apache.ibatis.session.SqlSessionFactory;
// ⚙️ MyBatis 핵심 팩토리
import org.mybatis.spring.SqlSessionFactoryBean;
// ⚙️ 부트-마이바티스 연동 팩토리 빈
import org.mybatis.spring.SqlSessionTemplate;
// ⚙️ Thread-safe SqlSession
import org.mybatis.spring.annotation.MapperScan;
// ⚙️ 매퍼 스캔 애노테이션
import org.springframework.context.annotation.Bean;
// ⚙️ @Bean 등록
import org.springframework.context.annotation.Configuration;
// ⚙️ 설정 클래스 표시
import org.springframework.core.io.support.PathMatchingResourcePatternResolver; 
// ⚙️ XML 매퍼 경로 로딩용

/**
 * MyBatis 전역 설정
 * - 매퍼 스캔: annotation, xml 두 경로를 동시에 스캔
 * - typeAliasesPackage: 도메인 패키지 별칭 등록
 */
@Configuration
@MapperScan(basePackages = {
        "com.gym.mapper.annotation",    // 🌟 어노테이션 매퍼 패키지
        "com.gym.mapper.xml"            // 🌟 XML 매퍼 인터페이스 패키지(시그니처만)
})
public class MyBatisConfig {

    /** SqlSessionFactory 빈 등록: DataSource 주입 + 타입 별칭 경로 지정 */
    @Bean
    public SqlSessionFactory
    
    sqlSessionFactory(DataSource dataSource) throws Exception {
    	
        SqlSessionFactoryBean factoryBean = new SqlSessionFactoryBean();
        // 팩토리 bean 생성
        
        factoryBean.setDataSource(dataSource);    
        // DB 연결 주입
        
        factoryBean.setTypeAliasesPackage("com.gym.domain");
        // DTO/VO 별칭 루트
        
        
        //-----------★ 추가: XML 매퍼 경로 지정(없으면 XML SQL을 못 읽음)-----------
        factoryBean.setMapperLocations(
            new PathMatchingResourcePatternResolver()
                .getResources("classpath*:mappers/**/*.xml") // 📂 mappers/ 이하 모두
        );
        //------------------------------------------------------------------------
        
        // MyBatis 전역 설정(스네이크→카멜 자동 매핑 켬)
        org.apache.ibatis.session.Configuration conf =
                new org.apache.ibatis.session.Configuration(); // 설정 객체 생성
        conf.setMapUnderscoreToCamelCase(true);	// member_id → memberId 자동 매핑
        factoryBean.setConfiguration(conf);		// 팩토리에 설정 주입
        return factoryBean.getObject();
        // 팩토리 반환
    }

    /** SqlSessionTemplate 빈: 안전한 SqlSession 주입용 */
    @Bean
    public SqlSessionTemplate
    	sqlSessionTemplate(SqlSessionFactory sqlSessionFactory) {
        return new SqlSessionTemplate(sqlSessionFactory); // 세션템플릿 반환
    }
}

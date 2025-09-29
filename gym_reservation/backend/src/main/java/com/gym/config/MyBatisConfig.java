package com.gym.config;

import org.apache.ibatis.session.SqlSessionFactory;
import org.mybatis.spring.SqlSessionFactoryBean;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;

import javax.sql.DataSource;

@Configuration
@MapperScan(basePackages = {
        "com.gym.mapper.annotation",   // 어노테이션 매퍼
        "com.gym.mapper.xml",          // XML 매퍼 인터페이스 패키지
        "com.gym.security.mapper"      // [250915] 로그인용 매퍼 추가
})
public class MyBatisConfig {

    @Bean
    public SqlSessionFactory sqlSessionFactory(DataSource dataSource) throws Exception {
        SqlSessionFactoryBean bean = new SqlSessionFactoryBean();
        bean.setDataSource(dataSource); // 커넥션 풀 주입

        PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();

        // mapperLocations 설정
        // bean.setMapperLocations(resolver.getResources("classpath*:/mappers/**/*.xml")); // old [원본 보존]
        bean.setMapperLocations(resolver.getResources("classpath:/mappers/**/*.xml"));   // [250926] 안정화: 중복 스캔 차단

        // [25093] 언더스코어→카멜 자동 매핑
        org.apache.ibatis.session.Configuration conf = new org.apache.ibatis.session.Configuration();
        conf.setMapUnderscoreToCamelCase(true);
        bean.setConfiguration(conf);

        return bean.getObject();
    }
}

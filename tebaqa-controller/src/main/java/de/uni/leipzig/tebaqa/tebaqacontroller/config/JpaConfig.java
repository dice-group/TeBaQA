package de.uni.leipzig.tebaqa.tebaqacontroller.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

import javax.sql.DataSource;

@Configuration
public class JpaConfig {

    @Autowired Environment env;

//    @Bean
//    public DataSource getDataSource()
//    {
//        DataSourceBuilder dataSourceBuilder = DataSourceBuilder.create();
//        dataSourceBuilder.driverClassName(env.getProperty("spring.datasource.driver-class-name"));
//        dataSourceBuilder.url(env.getProperty("spring.datasource.url"));
//        dataSourceBuilder.username(env.getProperty("spring.datasource.username"));
//        dataSourceBuilder.password(env.getProperty("spring.datasource.password"));
//        dataSourceBuilder.
//        return dataSourceBuilder.build();
//    }

    @Bean
    public DataSource getDataSource()
    {
        DataSourceBuilder dataSourceBuilder = DataSourceBuilder.create();
        dataSourceBuilder.driverClassName("org.h2.Driver");
        dataSourceBuilder.url("jdbc:h2:file:./tebaqa-db;IGNORECASE=TRUE");
        dataSourceBuilder.username("sa");
        dataSourceBuilder.password("password");
        return dataSourceBuilder.build();
    }
}
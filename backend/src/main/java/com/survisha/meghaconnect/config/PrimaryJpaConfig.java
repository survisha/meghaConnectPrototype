package com.survisha.meghaconnect.config;

import com.zaxxer.hikari.HikariDataSource;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.orm.jpa.EntityManagerFactoryBuilder;
import org.springframework.context.annotation.*;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.orm.jpa.*;
import org.springframework.transaction.PlatformTransactionManager;
import javax.persistence.EntityManagerFactory;
import javax.sql.DataSource;

@Configuration
@EnableJpaRepositories(basePackages="com.survisha.meghaconnect.repository",
        entityManagerFactoryRef="entityManagerFactory", transactionManagerRef="transactionManager")
public class PrimaryJpaConfig {
    @Bean @Primary @ConfigurationProperties("spring.datasource")
    public DataSourceProperties dataSourceProperties(){return new DataSourceProperties();}

    @Bean @Primary @ConfigurationProperties("spring.datasource.hikari")
    public HikariDataSource dataSource(@Qualifier("dataSourceProperties") DataSourceProperties p){return p.initializeDataSourceBuilder().type(HikariDataSource.class).build();}

    @Bean @Primary
    public LocalContainerEntityManagerFactoryBean entityManagerFactory(EntityManagerFactoryBuilder builder,@Qualifier("dataSource") DataSource ds){
        return builder.dataSource(ds).packages("com.survisha.meghaconnect.entity").persistenceUnit("primary").build();
    }

    @Bean @Primary
    public PlatformTransactionManager transactionManager(@Qualifier("entityManagerFactory") EntityManagerFactory emf){return new JpaTransactionManager(emf);}
}

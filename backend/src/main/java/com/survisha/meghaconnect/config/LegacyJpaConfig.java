package com.survisha.meghaconnect.config;

import com.zaxxer.hikari.HikariDataSource;
import org.flywaydb.core.Flyway;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.orm.jpa.EntityManagerFactoryBuilder;
import org.springframework.context.annotation.*;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.orm.jpa.*;
import org.springframework.transaction.PlatformTransactionManager;
import javax.persistence.EntityManagerFactory;
import java.util.Map;

@Configuration
@EnableJpaRepositories(basePackages="com.survisha.meghaconnect.legacy.repository",
        entityManagerFactoryRef="legacyEntityManagerFactory", transactionManagerRef="legacyTransactionManager")
public class LegacyJpaConfig {
    @Bean @ConfigurationProperties("legacy.datasource")
    public DataSourceProperties legacyDataSourceProperties(){return new DataSourceProperties();}

    @Bean @ConfigurationProperties("legacy.datasource.hikari")
    public HikariDataSource legacyDataSource(@Qualifier("legacyDataSourceProperties") DataSourceProperties p){return p.initializeDataSourceBuilder().type(HikariDataSource.class).build();}

    @Bean(initMethod="migrate")
    public Flyway legacyFlyway(@Qualifier("legacyDataSource") HikariDataSource ds){return Flyway.configure().dataSource(ds).locations("classpath:db/legacy-migration").baselineOnMigrate(true).load();}

    @Bean
    public LocalContainerEntityManagerFactoryBean legacyEntityManagerFactory(EntityManagerFactoryBuilder builder,@Qualifier("legacyDataSource") HikariDataSource ds){
        return builder.dataSource(ds).packages("com.survisha.meghaconnect.legacy.entity").persistenceUnit("legacy")
                .properties(Map.of(
                        "hibernate.hbm2ddl.auto", "none",
                        "hibernate.jdbc.batch_size", "500",
                        "hibernate.order_inserts", "true",
                        "hibernate.physical_naming_strategy", "org.springframework.boot.orm.jpa.hibernate.SpringPhysicalNamingStrategy",
                        "hibernate.implicit_naming_strategy", "org.springframework.boot.orm.jpa.hibernate.SpringImplicitNamingStrategy"
                )).build();
    }

    @Bean
    public PlatformTransactionManager legacyTransactionManager(@Qualifier("legacyEntityManagerFactory") EntityManagerFactory emf){return new JpaTransactionManager(emf);}
}

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
import javax.sql.DataSource;
import java.util.Map;

@Configuration
@EnableJpaRepositories(basePackages="com.survisha.meghaconnect.repository",
        entityManagerFactoryRef="entityManagerFactory", transactionManagerRef="transactionManager")
public class PrimaryJpaConfig {
    @Bean @Primary @ConfigurationProperties("spring.datasource")
    public DataSourceProperties dataSourceProperties(){return new DataSourceProperties();}

    @Bean @Primary @ConfigurationProperties("spring.datasource.hikari")
    public HikariDataSource dataSource(@Qualifier("dataSourceProperties") DataSourceProperties p){return p.initializeDataSourceBuilder().type(HikariDataSource.class).build();}

    @Bean(name = "primaryFlyway", initMethod = "migrate")
    @Primary
    public Flyway primaryFlyway(@Qualifier("dataSource") HikariDataSource dataSource) {
        return Flyway.configure()
                .dataSource(dataSource)
                .locations("classpath:db/migration")
                .baselineOnMigrate(true)
                .validateOnMigrate(false)
                .load();
    }

    @Bean @Primary
    @DependsOn("primaryFlyway")
    public LocalContainerEntityManagerFactoryBean entityManagerFactory(EntityManagerFactoryBuilder builder,@Qualifier("dataSource") DataSource ds){
        return builder.dataSource(ds)
                .packages("com.survisha.meghaconnect.entity")
                .persistenceUnit("primary")
                .properties(namingStrategyProperties())
                .build();
    }

    private Map<String, Object> namingStrategyProperties() {
        return Map.of(
                "hibernate.physical_naming_strategy",
                "org.springframework.boot.orm.jpa.hibernate.SpringPhysicalNamingStrategy",
                "hibernate.implicit_naming_strategy",
                "org.springframework.boot.orm.jpa.hibernate.SpringImplicitNamingStrategy"
        );
    }

    @Bean @Primary
    public PlatformTransactionManager transactionManager(@Qualifier("entityManagerFactory") EntityManagerFactory emf){return new JpaTransactionManager(emf);}
}

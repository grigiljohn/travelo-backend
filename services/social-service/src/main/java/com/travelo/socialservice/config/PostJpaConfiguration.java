package com.travelo.socialservice.config;

import jakarta.persistence.EntityManagerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;
import org.springframework.context.annotation.Primary;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;
import org.springframework.transaction.PlatformTransactionManager;

import javax.sql.DataSource;
import java.util.Properties;

@Configuration
@EnableJpaRepositories(
        basePackages = {
                "com.travelo.postservice.repository",
                "com.travelo.planservice.repository",
                "com.travelo.circlesservice.repository"
        },
        entityManagerFactoryRef = "postEntityManagerFactory",
        transactionManagerRef = "postTransactionManager"
)
public class PostJpaConfiguration {

    @Bean(name = "postEntityManagerFactory")
    @Primary
    @DependsOn("postFlyway")
    public LocalContainerEntityManagerFactoryBean postEntityManagerFactory(
            @Qualifier("postDataSource") DataSource dataSource) {
        LocalContainerEntityManagerFactoryBean em = new LocalContainerEntityManagerFactoryBean();
        em.setDataSource(dataSource);
        em.setPackagesToScan(
                "com.travelo.postservice.entity",
                "com.travelo.planservice.persistence",
                "com.travelo.circlesservice.persistence"
        );
        HibernateJpaVendorAdapter adapter = new HibernateJpaVendorAdapter();
        adapter.setShowSql(false);
        em.setJpaVendorAdapter(adapter);
        em.setJpaProperties(postHibernateProperties());
        return em;
    }

    @Bean(name = "postTransactionManager")
    @Primary
    public PlatformTransactionManager postTransactionManager(
            @Qualifier("postEntityManagerFactory") EntityManagerFactory emf) {
        return new JpaTransactionManager(emf);
    }

    private static Properties postHibernateProperties() {
        Properties p = new Properties();
        p.put("hibernate.hbm2ddl.auto", "none");
        p.put("hibernate.dialect", "org.hibernate.dialect.PostgreSQLDialect");
        p.put("hibernate.format_sql", "true");
        p.put("hibernate.jdbc.lob.non_contextual_creation", "true");
        p.put("hibernate.physical_naming_strategy", "org.hibernate.boot.model.naming.CamelCaseToUnderscoresNamingStrategy");
        p.put("hibernate.implicit_naming_strategy", "org.hibernate.boot.model.naming.ImplicitNamingStrategyJpaCompliantImpl");
        return p;
    }
}

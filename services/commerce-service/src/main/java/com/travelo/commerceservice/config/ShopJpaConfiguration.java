package com.travelo.commerceservice.config;

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
        basePackages = "com.travelo.shopservice.repository",
        entityManagerFactoryRef = "shopEntityManagerFactory",
        transactionManagerRef = "shopTransactionManager"
)
public class ShopJpaConfiguration {

    @Bean(name = "shopEntityManagerFactory")
    @Primary
    @DependsOn("shopFlyway")
    public LocalContainerEntityManagerFactoryBean shopEntityManagerFactory(
            @Qualifier("shopDataSource") DataSource dataSource) {
        LocalContainerEntityManagerFactoryBean em = new LocalContainerEntityManagerFactoryBean();
        em.setDataSource(dataSource);
        em.setPackagesToScan("com.travelo.shopservice.entity");
        HibernateJpaVendorAdapter adapter = new HibernateJpaVendorAdapter();
        adapter.setShowSql(false);
        em.setJpaVendorAdapter(adapter);
        em.setJpaProperties(shopHibernateProperties());
        return em;
    }

    @Bean(name = "shopTransactionManager")
    @Primary
    public PlatformTransactionManager shopTransactionManager(
            @Qualifier("shopEntityManagerFactory") EntityManagerFactory emf) {
        return new JpaTransactionManager(emf);
    }

    private static Properties shopHibernateProperties() {
        Properties p = new Properties();
        p.put("hibernate.hbm2ddl.auto", "validate");
        p.put("hibernate.dialect", "org.hibernate.dialect.PostgreSQLDialect");
        p.put("hibernate.physical_naming_strategy", "org.hibernate.boot.model.naming.CamelCaseToUnderscoresNamingStrategy");
        p.put("hibernate.implicit_naming_strategy", "org.hibernate.boot.model.naming.ImplicitNamingStrategyJpaCompliantImpl");
        return p;
    }
}

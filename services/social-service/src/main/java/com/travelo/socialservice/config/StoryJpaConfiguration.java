package com.travelo.socialservice.config;

import jakarta.persistence.EntityManagerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;
import org.springframework.transaction.PlatformTransactionManager;

import javax.sql.DataSource;
import java.util.Properties;

@Configuration
@EnableJpaRepositories(
        basePackages = "com.travelo.storyservice.repository",
        entityManagerFactoryRef = "storyEntityManagerFactory",
        transactionManagerRef = "storyTransactionManager"
)
public class StoryJpaConfiguration {

    @Bean(name = "storyEntityManagerFactory")
    @DependsOn("storyFlyway")
    public LocalContainerEntityManagerFactoryBean storyEntityManagerFactory(
            @Qualifier("storyDataSource") DataSource dataSource) {
        LocalContainerEntityManagerFactoryBean em = new LocalContainerEntityManagerFactoryBean();
        em.setDataSource(dataSource);
        em.setPackagesToScan("com.travelo.storyservice.entity");
        HibernateJpaVendorAdapter adapter = new HibernateJpaVendorAdapter();
        adapter.setShowSql(false);
        em.setJpaVendorAdapter(adapter);
        em.setJpaProperties(storyHibernateProperties());
        return em;
    }

    @Bean(name = "storyTransactionManager")
    public PlatformTransactionManager storyTransactionManager(
            @Qualifier("storyEntityManagerFactory") EntityManagerFactory emf) {
        return new JpaTransactionManager(emf);
    }

    private static Properties storyHibernateProperties() {
        Properties p = new Properties();
        p.put("hibernate.hbm2ddl.auto", "validate");
        p.put("hibernate.dialect", "org.hibernate.dialect.PostgreSQLDialect");
        p.put("hibernate.format_sql", "true");
        return p;
    }
}

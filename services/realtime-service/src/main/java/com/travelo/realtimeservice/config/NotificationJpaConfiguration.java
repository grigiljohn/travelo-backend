package com.travelo.realtimeservice.config;

import jakarta.persistence.EntityManagerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
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
        basePackages = "com.travelo.notificationservice.repository",
        entityManagerFactoryRef = "notificationEntityManagerFactory",
        transactionManagerRef = "notificationTransactionManager"
)
public class NotificationJpaConfiguration {

    @Bean(name = "notificationEntityManagerFactory")
    @DependsOn("notificationFlyway")
    public LocalContainerEntityManagerFactoryBean notificationEntityManagerFactory(
            @Qualifier("notificationDataSource") DataSource dataSource,
            @Value("${realtime.notification.hibernate-default-schema:public}") String hibernateSchema) {
        LocalContainerEntityManagerFactoryBean em = new LocalContainerEntityManagerFactoryBean();
        em.setDataSource(dataSource);
        em.setPackagesToScan("com.travelo.notificationservice.entity");
        HibernateJpaVendorAdapter adapter = new HibernateJpaVendorAdapter();
        adapter.setShowSql(false);
        em.setJpaVendorAdapter(adapter);
        em.setJpaProperties(notificationHibernateProperties(hibernateSchema));
        return em;
    }

    @Bean(name = "notificationTransactionManager")
    public PlatformTransactionManager notificationTransactionManager(
            @Qualifier("notificationEntityManagerFactory") EntityManagerFactory emf) {
        return new JpaTransactionManager(emf);
    }

    private static Properties notificationHibernateProperties(String hibernateSchema) {
        Properties p = new Properties();
        p.put("hibernate.hbm2ddl.auto", "validate");
        p.put("hibernate.dialect", "org.hibernate.dialect.PostgreSQLDialect");
        if (hibernateSchema != null && !hibernateSchema.isBlank() && !"public".equalsIgnoreCase(hibernateSchema.trim())) {
            p.put("hibernate.default_schema", hibernateSchema.trim());
        }
        return p;
    }
}

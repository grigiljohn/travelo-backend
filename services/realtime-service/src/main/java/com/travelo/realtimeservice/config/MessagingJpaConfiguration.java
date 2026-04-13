package com.travelo.realtimeservice.config;

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
        basePackages = "com.travelo.messagingservice.repository",
        entityManagerFactoryRef = "messagingEntityManagerFactory",
        transactionManagerRef = "messagingTransactionManager"
)
public class MessagingJpaConfiguration {

    @Bean(name = "messagingEntityManagerFactory")
    @Primary
    @DependsOn("messagingFlyway")
    public LocalContainerEntityManagerFactoryBean messagingEntityManagerFactory(
            @Qualifier("messagingDataSource") DataSource dataSource) {
        LocalContainerEntityManagerFactoryBean em = new LocalContainerEntityManagerFactoryBean();
        em.setDataSource(dataSource);
        em.setPackagesToScan("com.travelo.messagingservice.entity");
        HibernateJpaVendorAdapter adapter = new HibernateJpaVendorAdapter();
        adapter.setShowSql(false);
        em.setJpaVendorAdapter(adapter);
        em.setJpaProperties(messagingHibernateProperties());
        return em;
    }

    @Bean(name = "messagingTransactionManager")
    @Primary
    public PlatformTransactionManager messagingTransactionManager(
            @Qualifier("messagingEntityManagerFactory") EntityManagerFactory emf) {
        return new JpaTransactionManager(emf);
    }

    private static Properties messagingHibernateProperties() {
        Properties p = new Properties();
        p.put("hibernate.hbm2ddl.auto", "validate");
        p.put("hibernate.dialect", "org.hibernate.dialect.PostgreSQLDialect");
        return p;
    }
}

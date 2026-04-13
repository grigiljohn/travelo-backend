package com.travelo.commerceservice.config;

import jakarta.persistence.EntityManagerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;
import org.springframework.transaction.PlatformTransactionManager;

import javax.sql.DataSource;
import java.util.Properties;

@Configuration
@EnableJpaRepositories(
        basePackages = "com.travelo.adservice.repository",
        entityManagerFactoryRef = "adEntityManagerFactory",
        transactionManagerRef = "adTransactionManager"
)
public class AdJpaConfiguration {

    @Bean(name = "adEntityManagerFactory")
    public LocalContainerEntityManagerFactoryBean adEntityManagerFactory(
            @Qualifier("adDataSource") DataSource dataSource) {
        LocalContainerEntityManagerFactoryBean em = new LocalContainerEntityManagerFactoryBean();
        em.setDataSource(dataSource);
        em.setPackagesToScan("com.travelo.adservice.entity");
        HibernateJpaVendorAdapter adapter = new HibernateJpaVendorAdapter();
        adapter.setShowSql(false);
        em.setJpaVendorAdapter(adapter);
        em.setJpaProperties(adHibernateProperties());
        return em;
    }

    @Bean(name = "adTransactionManager")
    public PlatformTransactionManager adTransactionManager(
            @Qualifier("adEntityManagerFactory") EntityManagerFactory emf) {
        return new JpaTransactionManager(emf);
    }

    private static Properties adHibernateProperties() {
        Properties p = new Properties();
        p.put("hibernate.hbm2ddl.auto", "update");
        p.put("hibernate.dialect", "org.hibernate.dialect.PostgreSQLDialect");
        p.put("hibernate.format_sql", "true");
        p.put("hibernate.jdbc.lob.non_contextual_creation", "true");
        p.put("hibernate.physical_naming_strategy", "org.hibernate.boot.model.naming.CamelCaseToUnderscoresNamingStrategy");
        return p;
    }
}

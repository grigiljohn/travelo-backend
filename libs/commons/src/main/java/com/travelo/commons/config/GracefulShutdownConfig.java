package com.travelo.commons.config;

import org.apache.catalina.connector.Connector;
import org.apache.tomcat.util.threads.ThreadPoolExecutor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.web.embedded.tomcat.TomcatConnectorCustomizer;
import org.springframework.boot.web.embedded.tomcat.TomcatServletWebServerFactory;
import org.springframework.boot.web.servlet.server.ConfigurableServletWebServerFactory;
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.ContextClosedEvent;

import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;

/**
 * Configuration for graceful shutdown.
 * Allows in-flight requests to complete before shutting down.
 */
@Configuration
public class GracefulShutdownConfig {

    private static final int GRACEFUL_SHUTDOWN_TIMEOUT_SECONDS = 30;

    @Bean
    public GracefulShutdown gracefulShutdown() {
        return new GracefulShutdown(GRACEFUL_SHUTDOWN_TIMEOUT_SECONDS);
    }

    @Bean
    public ConfigurableServletWebServerFactory webServerFactory(GracefulShutdown gracefulShutdown) {
        TomcatServletWebServerFactory factory = new TomcatServletWebServerFactory();
        factory.addConnectorCustomizers(gracefulShutdown);
        return factory;
    }

    /**
     * Graceful shutdown implementation for Tomcat.
     */
    public static class GracefulShutdown implements TomcatConnectorCustomizer, ApplicationListener<ContextClosedEvent> {

        private static final Logger logger = LoggerFactory.getLogger(GracefulShutdown.class);
        private volatile Connector connector;
        private final int waitTime;

        public GracefulShutdown(int waitTime) {
            this.waitTime = waitTime;
        }

        @Override
        public void customize(Connector connector) {
            this.connector = connector;
        }

        @Override
        public void onApplicationEvent(@org.springframework.lang.NonNull ContextClosedEvent event) {
            if (connector != null) {
                try {
                    logger.info("Starting graceful shutdown...");
                    connector.pause();
                    Executor executor = connector.getProtocolHandler().getExecutor();
                    if (executor instanceof ThreadPoolExecutor threadPoolExecutor) {
                        threadPoolExecutor.shutdown();
                        if (!threadPoolExecutor.awaitTermination(waitTime, TimeUnit.SECONDS)) {
                            logger.warn("Graceful shutdown timeout exceeded, forcing shutdown");
                            threadPoolExecutor.shutdownNow();
                        } else {
                            logger.info("Graceful shutdown completed");
                        }
                    }
                } catch (Exception e) {
                    logger.error("Error during graceful shutdown", e);
                }
            }
        }
    }
}


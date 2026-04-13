package com.travelo.messagingservice.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import io.swagger.v3.oas.models.tags.Tag;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI openAPI(@Value("${spring.application.name:realtime-service}") String appName,
                          @Value("${server.port:8098}") int port) {
        return new OpenAPI()
                .info(new Info()
                        .title("Travelo Messaging API (realtime-service)")
                        .description("""
                                Messaging REST API (part of realtime-service).
                                
                                This service handles direct messages, conversations, and messaging operations.
                                
                                **Base URL:** `http://localhost:%d/api/v1`
                                
                                **Authentication:** Most endpoints require JWT Bearer token authentication.
                                """.formatted(port))
                        .version("v1")
                        .contact(new Contact()
                                .name("Travelo Development Team")
                                .email("dev@travelo.com"))
                        .license(new License()
                                .name("Proprietary")
                                .url("https://travelo.com/license")))
                .servers(List.of(
                        new Server()
                                .url("http://localhost:" + port)
                                .description("Local Development Server"),
                        new Server()
                                .url("http://api.travelo.com:8080/messaging-service")
                                .description("API Gateway (Production)"),
                        new Server()
                                .url("https://api.travelo.com/messaging-service")
                                .description("Production Server (HTTPS)")
                ))
                .addSecurityItem(new SecurityRequirement().addList("Bearer Authentication"))
                .components(new io.swagger.v3.oas.models.Components()
                        .addSecuritySchemes("Bearer Authentication", new SecurityScheme()
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")
                                .description("JWT token obtained from authentication service")))
                .tags(List.of(
                        new Tag().name("Messages").description("Message management operations"),
                        new Tag().name("Conversations").description("Conversation management")
                ));
    }
}

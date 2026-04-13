package com.travelo.authservice.config;

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
    public OpenAPI openAPI(@Value("${spring.application.name:identity-service}") String appName,
                          @Value("${server.port:8081}") int port) {
        return new OpenAPI()
                .info(new Info()
                        .title("Travelo Identity Service API")
                        .description("""
                                Authentication (`/api/v1/auth/...`) and user profiles (`/api/v1/users/...`) in one deployable.
                                
                                **Auth base:** `http://localhost:%d/api/v1/auth`
                                
                                **Users base:** `http://localhost:%d/api/v1/users`
                                
                                **Authentication:** Most endpoints require JWT Bearer token authentication.
                                Use the `/register` endpoint to create an account and receive tokens.
                                """.formatted(port, port))
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
                                .url("http://api.travelo.com:8080/auth-service")
                                .description("API Gateway (Production)"),
                        new Server()
                                .url("https://api.travelo.com/auth-service")
                                .description("Production Server (HTTPS)")
                ))
                .addSecurityItem(new SecurityRequirement().addList("Bearer Authentication"))
                .components(new io.swagger.v3.oas.models.Components()
                        .addSecuritySchemes("Bearer Authentication", new SecurityScheme()
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")
                                .description("JWT token obtained from /register or /login endpoints")))
                .tags(List.of(
                        new Tag().name("Authentication").description("User authentication and registration"),
                        new Tag().name("OTP").description("Email verification OTP operations")
                ));
    }
}

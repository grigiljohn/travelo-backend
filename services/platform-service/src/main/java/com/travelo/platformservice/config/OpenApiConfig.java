package com.travelo.platformservice.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI openAPI(@Value("${spring.application.name:platform-service}") String appName,
                          @Value("${server.port:8099}") int port) {
        return new OpenAPI()
                .info(new Info()
                        .title("Travelo Platform Service")
                        .description("""
                                Combined deployable for admin, analytics, AI orchestration, and gateway helpers.
                                Clients should keep using API gateway path prefixes:
                                `/platform-service`, `/analytics-service`, `/gateway-helpers`, `/ai-orchestrator-service`.
                                The legacy `/admin-service` prefix has been retired — route admin
                                calls through `/platform-service/**`.

                                **Direct base URL:** `http://localhost:%d`
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
                                .description("Direct (development)"),
                        new Server()
                                .url("http://api.travelo.com:8080/platform-service")
                                .description("Gateway → platform-service prefix"),
                        new Server()
                                .url("http://api.travelo.com:8080/analytics-service")
                                .description("Gateway → analytics prefix"),
                        new Server()
                                .url("http://api.travelo.com:8080/ai-orchestrator-service")
                                .description("Gateway → AI orchestrator prefix"),
                        new Server()
                                .url("http://api.travelo.com:8080/gateway-helpers")
                                .description("Gateway → gateway-helpers prefix")
                ))
                .addSecurityItem(new SecurityRequirement().addList("Bearer Authentication"))
                .components(new io.swagger.v3.oas.models.Components()
                        .addSecuritySchemes("Bearer Authentication", new SecurityScheme()
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")
                                .description("JWT from auth service")));
    }
}

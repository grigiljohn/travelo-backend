package com.travelo.searchservice.config;

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
    public OpenAPI openAPI(@Value("${spring.application.name:discovery-service}") String appName,
                          @Value("${server.port:8088}") int port) {
        return new OpenAPI()
                .info(new Info()
                        .title("Travelo Discovery Service API (search)")
                        .description("""
                                Elasticsearch-backed search and discovery. Deployed as discovery-service; gateway may use `/search-service` or `/discovery-service` prefix.
                                
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
                                .url("http://api.travelo.com:8080/search-service")
                                .description("API Gateway (Production)"),
                        new Server()
                                .url("https://api.travelo.com/search-service")
                                .description("Production (legacy search prefix)"),
                        new Server()
                                .url("http://api.travelo.com:8080/discovery-service")
                                .description("API Gateway (discovery prefix)"),
                        new Server()
                                .url("https://api.travelo.com/discovery-service")
                                .description("Production (HTTPS, discovery prefix)")
                ))
                .addSecurityItem(new SecurityRequirement().addList("Bearer Authentication"))
                .components(new io.swagger.v3.oas.models.Components()
                        .addSecuritySchemes("Bearer Authentication", new SecurityScheme()
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")
                                .description("JWT token obtained from authentication service")))
                .tags(List.of(
                        new Tag().name("Search").description("Search operations")
                ));
    }
}

package com.travelo.adservice.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.servers.Server;
import io.swagger.v3.oas.models.tags.Tag;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI openAPI(@Value("${spring.application.name:commerce-service}") String appName,
                          @Value("${server.port:8097}") int port) {
        return new OpenAPI()
                .info(new Info()
                        .title("Travelo Ads Manager API")
                        .description("""
                                Complete API documentation for Travelo Ads Manager Backend.
                                
                                This API provides comprehensive functionality for managing advertising campaigns,
                                ad sets, ads, analytics, billing, assets, targeting, and optimization.
                                
                                **Base URL:** `http://localhost:%d/api/v1`
                                
                                **Authentication:** Most endpoints require JWT Bearer token authentication.
                                """.formatted(port))
                        .version("2.0")
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
                                .url("https://api.travelo.com")
                                .description("Production Server")
                ))
                .tags(List.of(
                        new Tag().name("Campaigns").description("Campaign management operations"),
                        new Tag().name("Ad Groups").description("Ad group (ad-set) management operations"),
                        new Tag().name("Ads").description("Ad management operations"),
                        new Tag().name("Analytics").description("Analytics and reporting operations"),
                        new Tag().name("Billing").description("Billing and payment operations"),
                        new Tag().name("Assets").description("Asset library management"),
                        new Tag().name("Targeting").description("Audience targeting operations"),
                        new Tag().name("Optimization").description("Campaign optimization tools"),
                        new Tag().name("Admin").description("Admin panel operations (ADMIN role required)"),
                        new Tag().name("Notifications").description("Notification management"),
                        new Tag().name("Authentication").description("Authentication and authorization"),
                        new Tag().name("Users").description("User management operations")
                ));
    }
}

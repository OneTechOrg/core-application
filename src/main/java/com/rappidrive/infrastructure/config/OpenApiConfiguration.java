package com.rappidrive.infrastructure.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * OpenAPI/Swagger configuration.
 */
@Configuration
public class OpenApiConfiguration {

    private static final String BEARER_AUTH_SCHEME = "bearer-jwt";
    
    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI()
            .info(new Info()
                .title("RappiDrive API")
                .description("White-label ride-hailing platform API")
                .version("1.0.0")
                .contact(new Contact()
                    .name("RappiDrive Team")
                    .email("api@rappidrive.com"))
                .license(new License()
                    .name("Proprietary")
                    .url("https://rappidrive.com/license")))
            .servers(List.of(
                new Server()
                    .url("http://localhost:8080")
                    .description("Development server"),
                new Server()
                    .url("https://api-staging.rappidrive.com")
                    .description("Staging server"),
                new Server()
                    .url("https://api.rappidrive.com")
                    .description("Production server")
            ))
            .components(new Components().addSecuritySchemes(
                BEARER_AUTH_SCHEME,
                new SecurityScheme()
                    .type(SecurityScheme.Type.HTTP)
                    .scheme("bearer")
                    .bearerFormat("JWT")
                    .description("JWT Bearer token")
            ))
            .addSecurityItem(new SecurityRequirement().addList(BEARER_AUTH_SCHEME));
    }
}

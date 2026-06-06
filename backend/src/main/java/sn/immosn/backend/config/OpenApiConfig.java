package sn.immosn.backend.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {
    @Bean
    public OpenAPI customOpenAPI() {
        final String securitySchemeName = "bearerAuth";
            return new OpenAPI()
                .info(new Info()
                    .title("ImmoSN API")
                    .version("1.0.0")
                    .description("""
                            ImmoSN - Plateforme immobilière sénégalaise
                            Fonctionnalités :
                            - Gestion des annonces immobilières
                            - Leads et prospects
                            - Messagerie client / agent
                            - Visites et contrats
                            - Signalements et litiges
                            Endpoints publics :
                            - GET /annonces
                            - GET /annonces/{id}
                            - POST /auth/login
                            - POST /auth/register
                            Endpoints protégés :
                            - Administration (ADMIN / SUPER_ADMIN)
                            - Création / modification annonces
                            - Leads, contrats, dashboard
                            """))
                .components(new Components()
                    .addSecuritySchemes(securitySchemeName,
                        new SecurityScheme()
                            .type(SecurityScheme.Type.HTTP)
                            .scheme("bearer")
                            .bearerFormat("JWT")));
}
}
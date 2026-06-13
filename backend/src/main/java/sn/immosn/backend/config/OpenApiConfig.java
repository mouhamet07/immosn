package sn.immosn.backend.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.ExternalDocumentation;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
            .info(new Info()
                .title("ImmoSN API")
                .version("v1")
                .description("""
                    ## Plateforme Immobilière Sénégalaise

                    ImmoSN est une plateforme SaaS immobilière permettant :

                    - **Publication d'annonces** — vente et location de biens immobiliers au Sénégal
                    - **Recherche multicritère** — filtres combinables : type de bien, prix, localisation, commodités
                    - **Favoris** — sauvegarde d'annonces par les clients
                    - **Demandes de visite** — planification et suivi complet des visites
                    - **Messagerie** — discussions client/agent par annonce
                    - **Contrats** — création, suivi, résiliation et prolongation
                    - **Signalements** — remontée et traitement des litiges contractuels
                    - **Administration** — tableau de bord, gestion des leads et des comptes admin

                    ---

                    ## Authentification JWT

                    Les endpoints protégés utilisent **JWT Bearer Token**.

                    1. Appelez `POST /api/v1/auth/login` avec vos identifiants
                    2. Récupérez le champ `data.accessToken` dans la réponse
                    3. Cliquez sur le bouton **Authorize**  en haut de cette page
                    4. Saisissez : `Bearer <votre_token>`
                    5. Tous les appels suivants incluront automatiquement le token

                    ---

                    ## Rôles et niveaux d'accès

                    | Rôle | Description | Périmètre |
                    |------|-------------|-----------|
                    | `PUBLIC` | Aucune authentification requise | Annonces, recherche, référentiels, localisation |
                    | `CLIENT` | Utilisateur inscrit | Favoris, visites, discussions, contrats propres, signalements |
                    | `ADMIN` | Gestionnaire de la plateforme | Annonces, leads, contrats, signalements, messagerie, dashboard |
                    | `SUPER_ADMIN` | Administrateur système | Tout ADMIN + gestion des comptes administrateurs |

                    ---

                    ## Format des réponses

                    Toutes les réponses suivent l'enveloppe `RestResponse<T>` :
                    ```json
                    {
                        "success": true,
                        "status": 200,
                        "message": "Opération réalisée avec succès",
                        "data": { },
                        "timestamp": "2024-01-15T10:30:00"
                    }
                    ```

                    Les listes paginées utilisent `PagedResponse<T>` :
                    ```json
                    {
                        "data": [ ],
                        "totalElements": 48,
                        "totalPages": 4,
                        "currentPage": 0,
                        "pageSize": 12,
                        "isFirst": true,
                        "isLast": false
                    }
                    ```
                    """)
                .contact(new Contact()
                    .name("ImmoSN Support Technique")
                    .email("support@immosn.sn")
                    .url("https://immosn.sn"))
                .license(new License()
                    .name("Propriétaire — ImmoSN © 2024")
                    .url("https://immosn.sn/legal")))
            .externalDocs(new ExternalDocumentation()
                .description("Documentation complète ImmoSN")
                .url("https://docs.immosn.sn"))
            .components(new Components()
                .addSecuritySchemes("bearerAuth",
                    new SecurityScheme()
                        .name("bearerAuth")
                        .type(SecurityScheme.Type.HTTP)
                        .scheme("bearer")
                        .bearerFormat("JWT")
                        .description(
                            "Token JWT obtenu via POST /api/v1/auth/login. " +
                            "Saisissez uniquement le token, le préfixe 'Bearer ' est ajouté automatiquement.")));
    }


    @Bean
    public GroupedOpenApi authGroup() {
        return GroupedOpenApi.builder()
            .group("01-authentification")
            .displayName("AUTHENTIFICATION")
            .pathsToMatch("/api/v1/auth/**")
            .build();
    }

    @Bean
    public GroupedOpenApi annoncesGroup() {
        return GroupedOpenApi.builder()
            .group("02-annonces")
            .displayName("ANNONCES")
            .pathsToMatch("/api/v1/annonces/**")
            .build();
    }

    @Bean
    public GroupedOpenApi typesBienGroup() {
        return GroupedOpenApi.builder()
            .group("03-types-bien")
            .displayName("TYPES DE BIENS")
            .pathsToMatch("/api/v1/types-bien/**")
            .build();
    }

    @Bean
    public GroupedOpenApi commoditesGroup() {
        return GroupedOpenApi.builder()
            .group("04-commodites")
            .displayName("COMMODITES")
            .pathsToMatch("/api/v1/commodites/**")
            .build();
    }

    @Bean
    public GroupedOpenApi favorisGroup() {
        return GroupedOpenApi.builder()
            .group("05-favoris")
            .displayName("FAVORIS")
            .pathsToMatch("/api/v1/favoris/**")
            .build();
    }

    @Bean
    public GroupedOpenApi visitesGroup() {
        return GroupedOpenApi.builder()
            .group("06-visites")
            .displayName("VISITES")
            .pathsToMatch("/api/v1/visites/**")
            .build();
    }

    @Bean
    public GroupedOpenApi discussionsGroup() {
        return GroupedOpenApi.builder()
            .group("07-discussions")
            .displayName("DISCUSSIONS")
            .pathsToMatch("/api/v1/discussions/**")
            .build();
    }

    @Bean
    public GroupedOpenApi contratsGroup() {
        return GroupedOpenApi.builder()
            .group("08-contrats")
            .displayName("CONTRATS")
            .pathsToMatch("/api/v1/contrats/**")
            .build();
    }

    @Bean
    public GroupedOpenApi signalementsGroup() {
        return GroupedOpenApi.builder()
            .group("09-signalements")
            .displayName("SIGNALEMENTS")
            .pathsToMatch("/api/v1/signalements/**")
            .build();
    }

    @Bean
    public GroupedOpenApi leadsGroup() {
        return GroupedOpenApi.builder()
            .group("10-leads")
            .displayName("LEADS")
            .pathsToMatch("/api/v1/leads/**")
            .build();
    }

    @Bean
    public GroupedOpenApi administrationGroup() {
        return GroupedOpenApi.builder()
            .group("11-administration")
            .displayName("ADMINISTRATION")
            .pathsToMatch("/api/v1/admin/**")
            .build();
    }

    @Bean
    public GroupedOpenApi locationsGroup() {
        return GroupedOpenApi.builder()
            .group("12-localisation")
            .displayName("LOCALISATION")
            .pathsToMatch("/api/v1/locations/**")
            .build();
    }
}

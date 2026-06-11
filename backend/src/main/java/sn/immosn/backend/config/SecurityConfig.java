package sn.immosn.backend.config;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import sn.immosn.backend.auth.data.jwt.JwtAuthentificationFilter;
import sn.immosn.backend.auth.data.jwt.JwtTokenProvider;
import sn.immosn.backend.auth.service.AuthUserDetailService;

import java.util.List;

/**
 * Configuration de sécurité centralisée — deny-by-default.
 *
 * Règles RBAC :
 *   CLIENT      → lecture annonces, favoris, discussions, visites, contrats propres
 *   ADMIN       → gestion annonces, visites, leads, contrats, signalements, messages
 *   SUPER_ADMIN → tout ADMIN + gestion des comptes ADMIN
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtTokenProvider       jwtTokenProvider;
    private final AuthUserDetailService  authUserDetailService;

    @Value("${app.cors.allowed-origins:http://localhost:5173,http://localhost:80}")
    private List<String> allowedOrigins;

    //  Beans 
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public JwtAuthentificationFilter jwtAuthentificationFilter() {
        return new JwtAuthentificationFilter(jwtTokenProvider, authUserDetailService);
    }

    @Bean
    public DaoAuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider(authUserDetailService);
        provider.setPasswordEncoder(passwordEncoder());
        return provider;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    //  CORS centralisé 
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(allowedOrigins);
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("Authorization", "Content-Type", "Accept"));
        config.setAllowCredentials(true);
        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/**", config);
        return source;
    }

    //  Filter chain — deny-by-default 
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .headers(headers -> headers.frameOptions(frame -> frame.disable()))
            .authorizeHttpRequests(authz -> authz

                //  Actuator health : sans auth (Docker healthcheck) 
                .requestMatchers("/actuator/health").permitAll()

                //  Public
                .requestMatchers(HttpMethod.POST, "/api/v1/auth/register").permitAll()
                .requestMatchers(HttpMethod.POST, "/api/v1/auth/login").permitAll()
                .requestMatchers(HttpMethod.POST, "/api/v1/auth/logout").permitAll()
                .requestMatchers(HttpMethod.GET,  "/api/v1/annonces").permitAll()
                .requestMatchers(HttpMethod.GET,  "/api/v1/annonces/admin").hasAnyRole("ADMIN", "SUPER_ADMIN")
                .requestMatchers(HttpMethod.GET,  "/api/v1/annonces/admin/**").hasAnyRole("ADMIN", "SUPER_ADMIN")
                .requestMatchers(HttpMethod.GET,  "/api/v1/annonces/{id}").permitAll()
                .requestMatchers(HttpMethod.POST, "/api/v1/annonces/search").permitAll()
                .requestMatchers(HttpMethod.GET,  "/api/v1/commodites").permitAll()
                .requestMatchers(HttpMethod.GET,  "/api/v1/commodites/paged").permitAll()
                .requestMatchers(HttpMethod.GET,  "/api/v1/types-bien").permitAll()
                .requestMatchers(HttpMethod.GET,  "/api/v1/types-bien/paged").permitAll()
                .requestMatchers(HttpMethod.GET,  "/api/v1/locations/**").permitAll()

                //  ADMIN ou SUPER_ADMIN
                .requestMatchers(HttpMethod.POST,   "/api/v1/annonces").hasAnyRole("ADMIN", "SUPER_ADMIN")
                .requestMatchers(HttpMethod.PUT,    "/api/v1/annonces/**").hasAnyRole("ADMIN", "SUPER_ADMIN")
                .requestMatchers(HttpMethod.DELETE, "/api/v1/annonces/**").hasAnyRole("ADMIN", "SUPER_ADMIN")
                .requestMatchers(HttpMethod.PATCH,  "/api/v1/annonces/**").hasAnyRole("ADMIN", "SUPER_ADMIN")
                .requestMatchers(HttpMethod.POST,   "/api/v1/commodites/**").hasAnyRole("ADMIN", "SUPER_ADMIN")
                .requestMatchers(HttpMethod.PUT,    "/api/v1/commodites/**").hasAnyRole("ADMIN", "SUPER_ADMIN")
                .requestMatchers(HttpMethod.DELETE, "/api/v1/commodites/**").hasAnyRole("ADMIN", "SUPER_ADMIN")
                .requestMatchers(HttpMethod.PATCH,  "/api/v1/commodites/**").hasAnyRole("ADMIN", "SUPER_ADMIN")
                .requestMatchers(HttpMethod.POST,   "/api/v1/types-bien/**").hasAnyRole("ADMIN", "SUPER_ADMIN")
                .requestMatchers(HttpMethod.PUT,    "/api/v1/types-bien/**").hasAnyRole("ADMIN", "SUPER_ADMIN")
                .requestMatchers(HttpMethod.DELETE, "/api/v1/types-bien/**").hasAnyRole("ADMIN", "SUPER_ADMIN")
                .requestMatchers(HttpMethod.PATCH,  "/api/v1/types-bien/**").hasAnyRole("ADMIN", "SUPER_ADMIN")
                .requestMatchers("/api/v1/admin/**").hasAnyRole("ADMIN", "SUPER_ADMIN")
                .requestMatchers("/api/v1/leads/**").hasAnyRole("ADMIN", "SUPER_ADMIN")

                //  Gestion ADMIN : SUPER_ADMIN UNIQUEMENT 
                // ADMIN ne peut PAS créer d'autres admins ni les archiver
                .requestMatchers(HttpMethod.POST,  "/api/v1/auth/admin").hasRole("SUPER_ADMIN")
                .requestMatchers(HttpMethod.GET,   "/api/v1/auth/admins").hasRole("SUPER_ADMIN")
                .requestMatchers(HttpMethod.PATCH, "/api/v1/auth/admins/**").hasRole("SUPER_ADMIN")

                //  Endpoints authentifiés (granularité via @PreAuthorize)
                .requestMatchers("/api/v1/auth/profile").authenticated()
                .requestMatchers("/api/v1/discussions/**").authenticated()
                .requestMatchers("/api/v1/visites/**").authenticated()
                .requestMatchers("/api/v1/contrats/**").authenticated()
                .requestMatchers("/api/v1/signalements/**").authenticated()
                .requestMatchers("/api/v1/favoris/**").authenticated()

                //  Swagger UI + OpenAPI docs 
                .requestMatchers("/swagger-ui/**", "/swagger-ui.html", "/v3/api-docs/**").permitAll()

                //  H2 console : développement uniquement 
                .requestMatchers("/h2-console/**").permitAll()

                //  Deny-by-default : tout le reste exige authentification
                .anyRequest().authenticated()
            )
            .authenticationProvider(authenticationProvider())
            .addFilterBefore(jwtAuthentificationFilter(), UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}

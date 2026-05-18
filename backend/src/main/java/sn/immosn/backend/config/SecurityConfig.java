package sn.immosn.backend.config;

import sn.immosn.backend.auth.data.jwt.JwtAuthentificationFilter;
import sn.immosn.backend.auth.data.jwt.JwtTokenProvider;
import sn.immosn.backend.auth.service.AuthUserDetailService;
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

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    private final JwtTokenProvider jwtTokenProvider;
    private final AuthUserDetailService authUserDetailService ;

    public SecurityConfig(JwtTokenProvider jwtTokenProvider, AuthUserDetailService authUserDetailService) {
        this.jwtTokenProvider = jwtTokenProvider;
        this.authUserDetailService = authUserDetailService;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(authz -> authz
                // Auth : public
                .requestMatchers("/api/v1/auth/register", "/api/v1/auth/login", "/api/v1/auth/logout").permitAll()
                .requestMatchers("/h2-console/**").permitAll()
                // Lecture annonces/referentiels : public
                .requestMatchers(HttpMethod.GET, "/api/v1/annonces/**").permitAll()
                .requestMatchers(HttpMethod.POST, "/api/v1/annonces/search").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/v1/commodites/**").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/v1/type-bien/**").permitAll()
                // Mutations annonces : admin seulement
                .requestMatchers(HttpMethod.POST, "/api/v1/annonces").hasRole("ADMIN")
                .requestMatchers(HttpMethod.PUT, "/api/v1/annonces/**").hasRole("ADMIN")
                .requestMatchers(HttpMethod.DELETE, "/api/v1/annonces/**").hasRole("ADMIN")
                .requestMatchers(HttpMethod.PATCH, "/api/v1/annonces/**").hasRole("ADMIN")
                // Gestion référentiels : admin seulement
                .requestMatchers(HttpMethod.POST, "/api/v1/commodites/**").hasRole("ADMIN")
                .requestMatchers(HttpMethod.PUT, "/api/v1/commodites/**").hasRole("ADMIN")
                .requestMatchers(HttpMethod.DELETE, "/api/v1/commodites/**").hasRole("ADMIN")
                .requestMatchers(HttpMethod.POST, "/api/v1/type-bien/**").hasRole("ADMIN")
                .requestMatchers(HttpMethod.PUT, "/api/v1/type-bien/**").hasRole("ADMIN")
                .requestMatchers(HttpMethod.DELETE, "/api/v1/type-bien/**").hasRole("ADMIN")
                // Auth admin : admin seulement
                .requestMatchers("/api/v1/auth/admin", "/api/v1/auth/admins").hasRole("ADMIN")
                // Profil : authentifié
                .requestMatchers("/api/v1/auth/profile").authenticated()
                // Discussions : authentifié
                .requestMatchers("/api/v1/discussions/**").authenticated()
                // Sprint 3 — Visites : authentifié (granularité via @PreAuthorize)
                .requestMatchers("/api/v1/visites/**").authenticated()
                // Sprint 3 — Leads : ADMIN only
                .requestMatchers("/api/v1/leads/**").hasRole("ADMIN")
                // Sprint 3 — Contrats : authentifié
                .requestMatchers("/api/v1/contrats/**").authenticated()
                // Sprint 3 — Signalements : authentifié
                .requestMatchers("/api/v1/signalements/**").authenticated()
                // Sprint 4 — Dashboard admin : ADMIN only
                .requestMatchers("/api/v1/admin/**").hasRole("ADMIN")
                // Sprint 4 — Favoris : CLIENT authentifié
                .requestMatchers("/api/v1/favoris/**").authenticated()
                .anyRequest().authenticated()
            )
                .headers(headers -> headers.frameOptions(frameOptions -> frameOptions.disable()))
                .authenticationProvider(authenticationProvider())
                .addFilterBefore(jwtAuthentificationFilter(), UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public JwtAuthentificationFilter jwtAuthentificationFilter() {
        return new JwtAuthentificationFilter(jwtTokenProvider, authUserDetailService);
    }

    @Bean
    public DaoAuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider(authUserDetailService);
        authProvider.setPasswordEncoder(passwordEncoder());
        return authProvider;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }
}
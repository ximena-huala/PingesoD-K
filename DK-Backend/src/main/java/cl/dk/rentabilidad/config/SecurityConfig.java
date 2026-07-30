package cl.dk.rentabilidad.config;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.core.env.Environment;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.annotation.web.configurers.HeadersConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter;
import org.springframework.web.cors.CorsConfigurationSource;

import java.util.Arrays;

/**
 * Configuración de seguridad empresarial para la API REST.
 *
 * <ul>
 *   <li>Autenticación stateless con JWT</li>
 *   <li>CORS restrictivo por origen configurado</li>
 *   <li>Headers de seguridad HTTP</li>
 *   <li>Swagger solo accesible en perfil {@code dev}</li>
 *   <li>Endpoints {@code /api/dev/**} abiertos solo en perfil {@code dev} (Falabella)</li>
 * </ul>
 */
@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtFilter jwtFilter;
    private final JwtAuthenticationEntryPoint authenticationEntryPoint;
    private final JwtAccessDeniedHandler accessDeniedHandler;
    private final CorsConfigurationSource corsConfigurationSource;
    private final Environment environment;

    @Value("${springdoc.swagger-ui.enabled:true}")
    private boolean swaggerUiEnabled;

    /**
     * Cadena de seguridad solo para el perfil dev: deja /api/dev/** abierto sin JWT.
     */
    @Bean
    @Order(1)
    @Profile("dev")
    public SecurityFilterChain devFilterChain(HttpSecurity http) throws Exception {
        http
                .securityMatcher("/api/dev/**")
                .csrf(AbstractHttpConfigurer::disable)
                .cors(cors -> cors.configurationSource(corsConfigurationSource))
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth.anyRequest().permitAll());
        return http.build();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .cors(cors -> cors.configurationSource(corsConfigurationSource))
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint(authenticationEntryPoint)
                        .accessDeniedHandler(accessDeniedHandler))
                .headers(headers -> headers
                        .contentTypeOptions(Customizer.withDefaults())
                        .frameOptions(HeadersConfigurer.FrameOptionsConfig::deny)
                        .referrerPolicy(referrer ->
                                referrer.policy(ReferrerPolicyHeaderWriter.ReferrerPolicy.NO_REFERRER))
                        .httpStrictTransportSecurity(hsts -> hsts
                                .includeSubDomains(true)
                                .maxAgeInSeconds(31_536_000))
                        .contentSecurityPolicy(csp ->
                                csp.policyDirectives("default-src 'none'; frame-ancestors 'none'"))
                )
                .authorizeHttpRequests(auth -> {
                    auth.requestMatchers("/api/auth/login").permitAll();
                    if (swaggerHabilitado()) {
                        auth.requestMatchers(
                                "/swagger-ui.html",
                                "/swagger-ui/**",
                                "/v3/api-docs/**"
                        ).permitAll();
                    }
                    auth.anyRequest().authenticated();
                })
                .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);
    }

    private boolean swaggerHabilitado() {
        return swaggerUiEnabled && Arrays.asList(environment.getActiveProfiles()).contains("dev");
    }
}

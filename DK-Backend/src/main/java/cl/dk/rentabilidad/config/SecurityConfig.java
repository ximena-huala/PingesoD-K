package cl.dk.rentabilidad.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * Configuración central de Spring Security.
 *
 * La aplicación usa autenticación stateless (sin sesiones) basada en JWT.
 * Cada petición debe incluir el token en el header Authorization.
 *
 * Rutas públicas (sin token):
 *   - POST /api/auth/login → único endpoint abierto
 *
 * Todo lo demás requiere token JWT válido.
 */
@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtFilter jwtFilter;

    /**
     * Cadena de seguridad solo para el perfil dev: deja /api/dev/** abierto sin JWT.
     *
     * Esas rutas (las de FalabellaDevController) solo existen en dev, así que esta
     * cadena también. Fuera de dev ni se crea, y /api/dev/** cae en la cadena
     * principal que sí pide token. El @Order(1) la pone antes que la general, pero
     * con securityMatcher solo aplica a /api/dev/**.
     */
    @Bean
    @Order(1)
    @Profile("dev")
    public SecurityFilterChain devFilterChain(HttpSecurity http) throws Exception {
        http
                .securityMatcher("/api/dev/**")
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth.anyRequest().permitAll());
        return http.build();
    }

    /**
     * Configura la cadena de filtros de seguridad.
     *
     * Decisiones de diseño:
     * - CSRF deshabilitado: la app es una API REST stateless, no usa formularios
     * - Sesiones deshabilitadas: cada petición se autentica con su propio JWT
     * - BCrypt como encoder: estándar seguro para hashing de contraseñas
     */
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                // Deshabilitamos CSRF porque usamos JWT, no cookies de sesión
                .csrf(AbstractHttpConfigurer::disable)

                // Sin sesiones: cada petición es independiente (stateless)
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

                // Definimos qué rutas son públicas y cuáles requieren autenticación
                .authorizeHttpRequests(auth -> auth
                        // Solo el login es público
                        .requestMatchers("/api/auth/login").permitAll()
                        // Todo lo demás requiere token JWT válido
                        .anyRequest().authenticated()
                )

                // Agregamos nuestro filtro JWT antes del filtro estándar de Spring
                .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    /**
     * Bean del encoder de contraseñas con BCrypt.
     * Se usa en AuthService para comparar la contraseña ingresada
     * contra el hash almacenado en la base de datos.
     *
     * El factor de costo por defecto (10) es adecuado para producción.
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}

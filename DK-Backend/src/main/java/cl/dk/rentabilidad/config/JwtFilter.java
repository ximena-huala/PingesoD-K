package cl.dk.rentabilidad.config;

import cl.dk.rentabilidad.repository.UsuarioRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

/**
 * Filtro JWT que intercepta cada petición HTTP antes de llegar al controller.
 *
 * Proceso:
 * 1. Extrae el token del header Authorization: Bearer {token}
 * 2. Valida que el token sea válido y no haya expirado
 * 3. Carga el usuario desde la BD y lo registra en el SecurityContext
 * 4. Spring Security permite la petición si el usuario está autenticado
 *
 * Este filtro se ejecuta UNA sola vez por petición (OncePerRequestFilter).
 */
@Component
@RequiredArgsConstructor
public class JwtFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;
    private final UsuarioRepository usuarioRepository;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        // 1. Leemos el header Authorization de la petición
        String authHeader = request.getHeader("Authorization");

        // 2. Si no hay header o no empieza con "Bearer ", dejamos pasar sin autenticar
        //    Spring Security rechazará la petición si el endpoint lo requiere
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        // 3. Extraemos el token quitando el prefijo "Bearer "
        String token = authHeader.substring(7);

        // 4. Validamos el token y autenticamos si es válido
        if (jwtUtil.esValido(token)) {

            // Extraemos el email del subject del token
            String email = jwtUtil.extraerEmail(token);

            // Verificamos que el usuario exista en BD y esté activo
            usuarioRepository.findByEmail(email)
                    .filter(u -> u.getActivo())
                    .ifPresent(usuario -> {

                        // Construimos el objeto de autenticación de Spring Security
                        // Sin roles diferenciados (todos son admin según acuerdo con cliente)
                        UsernamePasswordAuthenticationToken auth =
                                new UsernamePasswordAuthenticationToken(
                                        User.withUsername(usuario.getEmail())
                                                .password("")
                                                .authorities("ROLE_ADMIN")
                                                .build(),
                                        null,
                                        List.of()
                                );

                        // Agregamos detalles de la petición (IP, session, etc.)
                        auth.setDetails(new WebAuthenticationDetailsSource()
                                .buildDetails(request));

                        // Registramos la autenticación en el contexto de Spring Security
                        SecurityContextHolder.getContext().setAuthentication(auth);
                    });
        }

        // 5. Continuamos con el siguiente filtro en la cadena
        filterChain.doFilter(request, response);
    }
}
package cl.dk.rentabilidad.config;

import cl.dk.rentabilidad.exception.UnauthorizedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;

/**
 * Utilidad para obtener el usuario autenticado desde el contexto de Spring Security.
 */
public final class SecurityContextHelper {

    private SecurityContextHelper() {
    }

    /**
     * @return email del usuario autenticado (subject del JWT)
     * @throws UnauthorizedException si no hay sesión válida
     */
    public static String obtenerEmailAutenticado() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !authentication.isAuthenticated()) {
            throw new UnauthorizedException("No autenticado");
        }

        Object principal = authentication.getPrincipal();
        if (principal instanceof User user) {
            return user.getUsername();
        }

        throw new UnauthorizedException("No autenticado");
    }
}

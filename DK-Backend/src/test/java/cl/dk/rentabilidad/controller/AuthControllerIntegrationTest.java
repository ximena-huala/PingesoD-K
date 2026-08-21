package cl.dk.rentabilidad.controller;

import cl.dk.rentabilidad.config.JwtFilter;
import cl.dk.rentabilidad.config.SecurityConfig;
import cl.dk.rentabilidad.dto.LoginResponse;
import cl.dk.rentabilidad.exception.UnauthorizedException;
import cl.dk.rentabilidad.service.AuthService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Prueba de INTEGRACIÓN de la capa web del login: integra
 * {@link AuthController} + Spring MVC + validación (@Valid) +
 * {@code GlobalExceptionHandler} + serialización JSON.
 *
 * Verifica el flujo HTTP real (request → respuesta) sin base de datos:
 * el servicio se mockea, y se excluye la seguridad para aislar la capa web.
 */
@WebMvcTest(controllers = AuthController.class,
        excludeAutoConfiguration = SecurityAutoConfiguration.class,
        excludeFilters = @ComponentScan.Filter(
                type = FilterType.ASSIGNABLE_TYPE,
                classes = {SecurityConfig.class, JwtFilter.class}))
@AutoConfigureMockMvc(addFilters = false)
class AuthControllerIntegrationTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;

    @MockitoBean AuthService authService;

    @Test
    @DisplayName("POST /api/auth/login con credenciales válidas responde 200 y el token")
    void loginValidoDevuelveToken() throws Exception {
        when(authService.login(any(), any())).thenReturn(
                LoginResponse.builder().token("jwt-123").nombre("Kevin").email("kevin@dk.cl").build());

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                Map.of("email", "kevin@dk.cl", "password", "changeme"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("jwt-123"))
                .andExpect(jsonPath("$.email").value("kevin@dk.cl"))
                .andExpect(jsonPath("$.nombre").value("Kevin"));
    }

    @Test
    @DisplayName("POST /api/auth/login con email mal formado responde 400 (validación)")
    void emailInvalidoDevuelve400() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                Map.of("email", "no-es-email", "password", "changeme"))))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /api/auth/login sin contraseña responde 400 (validación)")
    void sinPasswordDevuelve400() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"kevin@dk.cl\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /api/auth/login con credenciales inválidas responde 401")
    void credencialesInvalidasDevuelve401() throws Exception {
        when(authService.login(any(), any()))
                .thenThrow(new UnauthorizedException("Credenciales inválidas"));

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                Map.of("email", "kevin@dk.cl", "password", "mala"))))
                .andExpect(status().isUnauthorized());
    }
}

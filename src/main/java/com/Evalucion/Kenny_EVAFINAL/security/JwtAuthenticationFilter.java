package com.Evalucion.Kenny_EVAFINAL.security;

import com.Evalucion.Kenny_EVAFINAL.exception.ApiError;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Filtro JWT: intercepta cada solicitud <b>una sola vez</b> (OncePerRequestFilter),
 * extrae y valida el token y, si es correcto, publica la autenticacion en el
 * SecurityContext para que Spring Security autorice por rol.
 *
 * <p>Flujo: {@code Authorization: Bearer TOKEN} &rarr; validar firma y expiracion
 * &rarr; cargar el usuario desde base de datos (el rol autoritativo es el de la BD,
 * no el del token) &rarr; crear {@code Authentication} &rarr; continuar la cadena.</p>
 *
 * <p>Nunca se registra el token completo en los logs.</p>
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String PREFIJO_BEARER = "Bearer ";

    private final JwtService jwtService;
    private final CustomUserDetailsService userDetailsService;
    private final ObjectMapper objectMapper;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        String header = request.getHeader(HttpHeaders.AUTHORIZATION);

        if (header == null || !header.startsWith(PREFIJO_BEARER)) {
            filterChain.doFilter(request, response);
            return;
        }

        String token = header.substring(PREFIJO_BEARER.length()).trim();

        try {
            if (!jwtService.esValido(token)) {
                rechazar(response, request, "Token invalido o expirado");
                return;
            }

            String email = jwtService.extraerEmail(token);
            UserDetails userDetails = userDetailsService.loadUserByUsername(email);

            var autenticacion = new UsernamePasswordAuthenticationToken(
                    userDetails, null, userDetails.getAuthorities());
            autenticacion.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

            SecurityContextHolder.getContext().setAuthentication(autenticacion);
        } catch (JwtException | IllegalArgumentException | UsernameNotFoundException e) {
            log.warn("Token rechazado en {}: {}", request.getRequestURI(), e.getMessage());
            rechazar(response, request, "Token invalido o expirado");
            return;
        }

        filterChain.doFilter(request, response);
    }

    private void rechazar(HttpServletResponse response, HttpServletRequest request, String mensaje)
            throws IOException {
        response.setStatus(HttpStatus.UNAUTHORIZED.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        ApiError error = ApiError.of(HttpStatus.UNAUTHORIZED.value(), "INVALID_TOKEN", mensaje,
                request.getRequestURI());
        objectMapper.writeValue(response.getOutputStream(), error);
    }
}

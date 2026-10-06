package com.Evalucion.Kenny_EVAFINAL.config;

import com.Evalucion.Kenny_EVAFINAL.enums.Rol;
import com.Evalucion.Kenny_EVAFINAL.security.JwtAuthenticationFilter;
import com.Evalucion.Kenny_EVAFINAL.security.RestAccessDeniedHandler;
import com.Evalucion.Kenny_EVAFINAL.security.RestAuthenticationEntryPoint;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

/**
 * Configuracion de seguridad.
 *
 * <p>Decisiones:</p>
 * <ul>
 *   <li>CSRF deshabilitado: la API es stateless y se autentica con Bearer token,
 *       por lo que no existen cookies que proteger contra CSRF.</li>
 *   <li>Sesiones {@code STATELESS}: no se usa HttpSession en ninguna parte.</li>
 *   <li>El filtro JWT se situa antes de {@code UsernamePasswordAuthenticationFilter}.</li>
 *   <li>La autorizacion por rol vive aqui y se refuerza con {@code @EnableMethodSecurity}
 *       para poder usar anotaciones en el futuro.</li>
 * </ul>
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final RestAuthenticationEntryPoint authenticationEntryPoint;
    private final RestAccessDeniedHandler accessDeniedHandler;

    @Value("${app.cors.allowed-origins:http://localhost:3000,http://localhost:4200}")
    private List<String> origenesPermitidos;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .formLogin(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .logout(AbstractHttpConfigurer::disable)
                .exceptionHandling(experiencia -> experiencia
                        .authenticationEntryPoint(authenticationEntryPoint)
                        .accessDeniedHandler(accessDeniedHandler))
                .authorizeHttpRequests(auth -> auth
                        // --- Publicos ---
                        .requestMatchers(HttpMethod.POST, "/api/v1/auth/**").permitAll()

                        // --- Catalogo ---
                        .requestMatchers(HttpMethod.GET, "/api/v1/libros/**").authenticated()
                        .requestMatchers(HttpMethod.POST, "/api/v1/libros").hasRole(Rol.ADMIN.name())
                        .requestMatchers(HttpMethod.PUT, "/api/v1/libros/**").hasRole(Rol.ADMIN.name())
                        .requestMatchers(HttpMethod.DELETE, "/api/v1/libros/**").hasRole(Rol.ADMIN.name())

                        // --- Prestamos / devoluciones ---
                        .requestMatchers(HttpMethod.POST, "/api/v1/prestamos")
                        .hasAnyRole(Rol.ADMIN.name(), Rol.BIBLIOTECARIO.name())
                        .requestMatchers(HttpMethod.PATCH, "/api/v1/prestamos/*/devolucion")
                        .hasAnyRole(Rol.ADMIN.name(), Rol.BIBLIOTECARIO.name())
                        .requestMatchers(HttpMethod.GET, "/api/v1/prestamos/mis-prestamos")
                        .hasRole(Rol.LECTOR.name())
                        .requestMatchers(HttpMethod.GET, "/api/v1/prestamos/atrasados")
                        .hasAnyRole(Rol.ADMIN.name(), Rol.BIBLIOTECARIO.name())

                        // Por defecto: cualquier otra ruta exige autenticacion
                        .anyRequest().authenticated())
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    /** Codifica y verifica contrasenas con BCrypt. */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /** Usado por AuthController para verificar credenciales en el login. */
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration configuration) throws Exception {
        return configuration.getAuthenticationManager();
    }

    /** CORS preparado para un futuro frontend web o movil. */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuracion = new CorsConfiguration();
        configuracion.setAllowedOrigins(origenesPermitidos);
        configuracion.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        configuracion.setAllowedHeaders(List.of("Authorization", "Content-Type", "Accept"));
        configuracion.setAllowCredentials(true);
        configuracion.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource fuente = new UrlBasedCorsConfigurationSource();
        fuente.registerCorsConfiguration("/api/**", configuracion);
        return fuente;
    }
}

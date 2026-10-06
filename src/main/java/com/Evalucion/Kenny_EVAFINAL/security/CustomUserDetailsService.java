package com.Evalucion.Kenny_EVAFINAL.security;

import com.Evalucion.Kenny_EVAFINAL.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

/**
 * Carga el usuario a partir de su email (username = email).
 * El unico rol se expone como autoridad {@code ROLE_<ROL>} para que
 * {@code hasRole(...)} funcione.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CustomUserDetailsService implements UserDetailsService {

    private final UsuarioRepository usuarioRepository;

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        var usuario = usuarioRepository.findByEmail(email.toLowerCase().trim())
                .orElseThrow(() -> {
                    log.warn("Intento de autenticacion con email inexistente: {}", email);
                    return new UsernameNotFoundException("Usuario no encontrado: " + email);
                });

        return User.withUsername(usuario.getEmail())
                .password(usuario.getPassword())
                .authorities(usuario.getRol().getAuthority())
                .build();
    }
}

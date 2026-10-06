package com.Evalucion.Kenny_EVAFINAL.service.impl;

import com.Evalucion.Kenny_EVAFINAL.entity.Usuario;
import com.Evalucion.Kenny_EVAFINAL.enums.PrestamoEstado;
import com.Evalucion.Kenny_EVAFINAL.enums.Rol;
import com.Evalucion.Kenny_EVAFINAL.enums.UsuarioEstado;
import com.Evalucion.Kenny_EVAFINAL.exception.BusinessRuleException;
import com.Evalucion.Kenny_EVAFINAL.repository.PrestamoRepository;
import com.Evalucion.Kenny_EVAFINAL.service.SancionService;
import com.Evalucion.Kenny_EVAFINAL.service.UsuarioService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/**
 * Implementacion unica de la politica de sanciones.
 *
 * <p>No existe reactivacion automatica: la especificacion no la define. Si en el
 * futuro se requiere, se agregara aqui (por ejemplo un metodo
 * {@code levantarSancionSiCorresponde}).</p>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class SancionServiceImpl implements SancionService {

    private final PrestamoRepository prestamoRepository;
    private final UsuarioService usuarioService;

    @Override
    public void validarPoliticaPreviaAPrestamo(Usuario usuario) {
        // REGLA 5: un usuario ya sancionado no puede recibir prestamos.
        if (usuario.getEstado() == UsuarioEstado.SANCIONADO) {
            log.warn("Prestamo rechazado: usuario #{} ({}) esta sancionado", usuario.getId(), usuario.getEmail());
            throw new BusinessRuleException("El usuario esta sancionado y no puede solicitar prestamos");
        }

        // REGLA 4: solo los LECTORES se sancionan automaticamente por vencimiento.
        if (usuario.getRol() != Rol.LECTOR) {
            return;
        }

        boolean tieneVencido = prestamoRepository
                .existsByUsuarioIdAndEstadoNotAndFechaDevolucionEsperadaBefore(
                        usuario.getId(), PrestamoEstado.DEVUELTO, LocalDateTime.now());

        if (tieneVencido) {
            // Se persiste en una transaccion propia: debe quedar registrada aunque
            // el prestamo que esta pidiendo termine en rollback.
            usuarioService.sancionar(usuario.getId());
            log.warn("Prestamo rechazado: usuario #{} tenia prestamos vencidos y fue sancionado", usuario.getId());
            throw new BusinessRuleException(
                    "El usuario tiene prestamos vencidos y ha sido sancionado. No puede solicitar nuevos prestamos");
        }
    }
}

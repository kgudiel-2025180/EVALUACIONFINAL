package com.Evalucion.Kenny_EVAFINAL.service.impl;

import com.Evalucion.Kenny_EVAFINAL.dto.usuario.UsuarioResponse;
import com.Evalucion.Kenny_EVAFINAL.entity.Usuario;
import com.Evalucion.Kenny_EVAFINAL.enums.UsuarioEstado;
import com.Evalucion.Kenny_EVAFINAL.exception.ResourceNotFoundException;
import com.Evalucion.Kenny_EVAFINAL.mapper.UsuarioMapper;
import com.Evalucion.Kenny_EVAFINAL.repository.UsuarioRepository;
import com.Evalucion.Kenny_EVAFINAL.service.UsuarioService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class UsuarioServiceImpl implements UsuarioService {

    private final UsuarioRepository usuarioRepository;

    @Override
    @Transactional(readOnly = true)
    public Usuario obtenerEntidadPorId(Long id) {
        return usuarioRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario", id));
    }

    @Override
    @Transactional(readOnly = true)
    public Usuario obtenerEntidadPorEmail(String email) {
        return usuarioRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario", email));
    }

    @Override
    @Transactional(readOnly = true)
    public UsuarioResponse obtenerPorId(Long id) {
        return UsuarioMapper.aResponse(obtenerEntidadPorId(id));
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existeEmail(String email) {
        return usuarioRepository.existsByEmail(email);
    }

    /**
     * Transaccion propia: la sancion debe sobrevivir aunque la operacion de prestamo
     * que la origino haga rollback. Si se aplicara dentro de la transaccion del
     * prestamo, el rechazo del prestamo borraria tambien la sancion.
     */
    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void sancionar(Long usuarioId) {
        Usuario usuario = usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario", usuarioId));
        usuario.setEstado(UsuarioEstado.SANCIONADO);
        usuarioRepository.saveAndFlush(usuario);
        log.warn("Usuario #{} ({}) sancionado por prestamo vencido", usuario.getId(), usuario.getEmail());
    }
}

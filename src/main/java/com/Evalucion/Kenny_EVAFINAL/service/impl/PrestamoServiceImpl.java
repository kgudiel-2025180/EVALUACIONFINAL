package com.Evalucion.Kenny_EVAFINAL.service.impl;

import com.Evalucion.Kenny_EVAFINAL.dto.prestamo.DevolucionResponse;
import com.Evalucion.Kenny_EVAFINAL.dto.prestamo.PrestamoRequest;
import com.Evalucion.Kenny_EVAFINAL.dto.prestamo.PrestamoResponse;
import com.Evalucion.Kenny_EVAFINAL.entity.Libro;
import com.Evalucion.Kenny_EVAFINAL.entity.Prestamo;
import com.Evalucion.Kenny_EVAFINAL.entity.Usuario;
import com.Evalucion.Kenny_EVAFINAL.enums.PrestamoEstado;
import com.Evalucion.Kenny_EVAFINAL.enums.Rol;
import com.Evalucion.Kenny_EVAFINAL.exception.BusinessRuleException;
import com.Evalucion.Kenny_EVAFINAL.exception.ConflictException;
import com.Evalucion.Kenny_EVAFINAL.exception.ResourceNotFoundException;
import com.Evalucion.Kenny_EVAFINAL.mapper.PrestamoMapper;
import com.Evalucion.Kenny_EVAFINAL.repository.LibroRepository;
import com.Evalucion.Kenny_EVAFINAL.repository.PrestamoRepository;
import com.Evalucion.Kenny_EVAFINAL.service.PrestamoService;
import com.Evalucion.Kenny_EVAFINAL.service.SancionService;
import com.Evalucion.Kenny_EVAFINAL.service.UsuarioService;
import com.Evalucion.Kenny_EVAFINAL.util.Constantes;
import com.Evalucion.Kenny_EVAFINAL.util.PageUtils;
import com.Evalucion.Kenny_EVAFINAL.util.StockValidator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Slf4j
public class PrestamoServiceImpl implements PrestamoService {

    /** Prestamos que siguen abiertos: cuentan para el limite de 3. */
    private static final Set<PrestamoEstado> ESTADOS_ABIERTOS =
            Set.of(PrestamoEstado.ACTIVO, PrestamoEstado.ATRASADO);

    private static final Sort ORDEN_ATRASADOS = Sort.by(Sort.Direction.ASC, "fechaDevolucionEsperada");
    /** Historial: lo mas reciente primero. */
    private static final Sort ORDEN_HISTORIAL = Sort.by(Sort.Direction.DESC, "fechaPrestamo");

    private final PrestamoRepository prestamoRepository;
    private final LibroRepository libroRepository;
    private final UsuarioService usuarioService;
    private final SancionService sancionService;

    /**
     * REGLAS 1, 2, 3, 4, 5, 6, 8, 9, 10 y 13 en una sola transaccion atomica.
     *
     * <p><b>Orden deliberado respecto al bloqueo:</b> primero se ejecutan las
     * comprobaciones que no necesitan escribir (existencia del usuario, politica de
     * sanciones, conteo de prestamos) y <i>el bloqueo pesimista del libro se adquiere
     * el ultimo posible</i>, justo antes de leer y descontar stock. Asi se minimiza el
     * tiempo en que una fila del libro esta bloqueada, que es el cuello de botella
     * cuando muchas peticiones concurrentes compiten por el mismo ejemplar.</p>
     */
    @Override
    @Transactional
    public PrestamoResponse registrar(PrestamoRequest request) {
        log.debug("Solicitud de prestamo: usuarioId={}, libroId={}", request.usuarioId(), request.libroId());

        // REGLA 10: el usuario debe existir
        Usuario usuario = usuarioService.obtenerEntidadPorId(request.usuarioId());

        // REGLA 4 y 5: politica de sanciones
        sancionService.validarPoliticaPreviaAPrestamo(usuario);

        // REGLA 2: maximo 3 prestamos abiertos por LECTOR
        if (usuario.getRol() == Rol.LECTOR) {
            long abiertos = prestamoRepository.countByUsuarioIdAndEstadoIn(usuario.getId(), ESTADOS_ABIERTOS);
            if (abiertos >= Constantes.MAX_PRESTAMOS_ACTIVOS_LECTOR) {
                log.warn("Prestamo rechazado: lector #{} ya tiene {} prestamos abiertos",
                        usuario.getId(), abiertos);
                throw new BusinessRuleException(
                        "El lector no puede tener mas de " + Constantes.MAX_PRESTAMOS_ACTIVOS_LECTOR
                                + " prestamos activos al mismo tiempo");
            }
        }

        // REGLA 9: el libro debe existir y estar en catalogo. Bloqueo pesado del stock.
        Libro libro = libroRepository.findActivoByIdWithLock(request.libroId())
                .orElseThrow(() -> new ResourceNotFoundException("Libro", request.libroId()));

        // REGLA 1: sin ejemplares no hay prestamo
        if (libro.getStockDisponible() <= 0) {
            log.warn("Prestamo rechazado: libro #{} sin stock disponible", libro.getId());
            throw new BusinessRuleException("El libro no tiene ejemplares disponibles");
        }

        // REGLA 3: el plazo lo calcula el backend, nunca el cliente
        LocalDateTime fechaPrestamo = LocalDateTime.now();
        LocalDateTime fechaDevolucionEsperada = fechaPrestamo.plusDays(Constantes.PRESTAMO_DIAS_PLAZO);

        Prestamo prestamo = Prestamo.builder()
                .usuario(usuario)
                .libro(libro)
                .fechaPrestamo(fechaPrestamo)
                .fechaDevolucionEsperada(fechaDevolucionEsperada)
                .fechaDevolucionReal(null)
                .estado(PrestamoEstado.ACTIVO)
                .build();

        // REGLA 6 y REGLA 8
        libro.setStockDisponible(libro.getStockDisponible() - 1);
        StockValidator.validar(libro);

        Prestamo guardado = prestamoRepository.save(prestamo);
        libroRepository.save(libro);

        log.info("Prestamo #{} registrado. Libro #{} stock -> {}/{}",
                guardado.getId(), libro.getId(), libro.getStockDisponible(), libro.getStockTotal());
        return PrestamoMapper.aResponse(guardado);
    }

    /**
     * REGLAS 7, 8, 11 y 12 en una sola transaccion atomica.
     */
    @Override
    @Transactional
    public DevolucionResponse devolver(Long prestamoId) {
        // REGLA 11
        Prestamo prestamo = prestamoRepository.findById(prestamoId)
                .orElseThrow(() -> new ResourceNotFoundException("Prestamo", prestamoId));

        // REGLA 12: no se devuelve dos veces
        if (prestamo.getEstado() == PrestamoEstado.DEVUELTO) {
            throw new ConflictException("El prestamo #" + prestamoId + " ya fue devuelto");
        }

        // Se bloquea el libro sin filtrar la baja logica: el ejemplar salio antes
        // de la baja y debe poder regresar.
        Libro libro = libroRepository.findByIdWithLock(prestamo.getLibro().getId())
                .orElseThrow(() -> new ResourceNotFoundException("Libro", prestamo.getLibro().getId()));

        prestamo.setFechaDevolucionReal(LocalDateTime.now());
        prestamo.setEstado(PrestamoEstado.DEVUELTO);

        // REGLA 7 y REGLA 8
        libro.setStockDisponible(libro.getStockDisponible() + 1);
        StockValidator.validar(libro);

        prestamoRepository.save(prestamo);
        libroRepository.save(libro);

        log.info("Devolucion registrada: prestamo #{} libro #{} stock -> {}/{}",
                prestamo.getId(), libro.getId(), libro.getStockDisponible(), libro.getStockTotal());

        return new DevolucionResponse(prestamo.getId(), libro.getId(), libro.getTitulo(),
                prestamo.getFechaDevolucionReal(), prestamo.getEstado(), libro.getStockDisponible());
    }

    @Override
    @Transactional(readOnly = true)
    public Page<PrestamoResponse> obtenerMisPrestamos(Long usuarioId, Pageable pageable) {
        Pageable normalizado = PageUtils.normalizar(pageable, ORDEN_HISTORIAL);
        return prestamoRepository.findHistorialByUsuario(usuarioId, normalizado)
                .map(PrestamoMapper::aResponse);
    }

    /**
     * Detecta los atrasos en la capa de negocio/repositorio (nunca en el controller)
     * y deja el estado persistido como ATRASADO para que el resto del sistema
     * consulte un estado consistente.
     */
    @Override
    @Transactional
    public Page<PrestamoResponse> obtenerAtrasados(Pageable pageable) {
        Pageable normalizado = PageUtils.normalizar(pageable, ORDEN_ATRASADOS);
        LocalDateTime ahora = LocalDateTime.now();

        Page<Prestamo> vencidos = prestamoRepository
                .findVencidos(ahora, PrestamoEstado.DEVUELTO, normalizado);

        List<Prestamo> porMarcar = vencidos.getContent().stream()
                .filter(p -> p.getEstado() == PrestamoEstado.ACTIVO)
                .toList();
        if (!porMarcar.isEmpty()) {
            porMarcar.forEach(p -> p.setEstado(PrestamoEstado.ATRASADO));
            prestamoRepository.saveAll(porMarcar);
            log.info("Marcados {} prestamos como ATRASADOS", porMarcar.size());
        }

        return vencidos.map(PrestamoMapper::aResponse);
    }
}

package com.Evalucion.Kenny_EVAFINAL.repository;

import com.Evalucion.Kenny_EVAFINAL.entity.Prestamo;
import com.Evalucion.Kenny_EVAFINAL.enums.PrestamoEstado;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

@Repository
public interface PrestamoRepository extends JpaRepository<Prestamo, Long> {

    /**
     * REGLA 2: cuenta los prestamos que siguen abiertos (ACTIVO o ATRASADO).
     * Con {@code count} no se cargan registros completos a memoria.
     */
    long countByUsuarioIdAndEstadoIn(Long usuarioId, Collection<PrestamoEstado> estados);

    /**
     * REGLA 4: préstamos no devueltos cuya fecha esperada ya paso.
     * La deteccion de vencimiento vive en la capa de datos/negocio, nunca en el controller.
     */
    @Query("""
            select p from Prestamo p
            where p.usuario.id = :usuarioId
              and p.estado <> :devuelto
              and p.fechaDevolucionEsperada < :ahora
            """)
    List<Prestamo> findVencidosByUsuario(@Param("usuarioId") Long usuarioId,
                                         @Param("ahora") LocalDateTime ahora,
                                         @Param("devuelto") PrestamoEstado devuelto);

    /** Existencia de al menos un prestamo vencido (consulta ligera). */
    boolean existsByUsuarioIdAndEstadoNotAndFechaDevolucionEsperadaBefore(Long usuarioId,
                                                                         PrestamoEstado estado,
                                                                         LocalDateTime fecha);

    /**
     * Historial completo de un usuario.
     *
     * <p>JOIN FETCH sobre las dos relaciones {@code ManyToOne}: sin el, el mapeo a
     * DTO dispararia una consulta por fila (N+1), que bajo carga concurrente
     * multiplica el uso de conexiones del pool. Al ser relaciones {@code ToOne},
     * el fetch join no duplica filas y la paginacion se resuelve en SQL.</p>
     *
     * <p>El orden se aplica desde el {@code Pageable}: Hibernate y Spring Data
     * generan entonces la consulta de conteo sin clausula ORDER BY.</p>
     */
    @Query("""
            select p from Prestamo p
            join fetch p.usuario u
            join fetch p.libro l
            where u.id = :usuarioId
            """)
    Page<Prestamo> findHistorialByUsuario(@Param("usuarioId") Long usuarioId, Pageable pageable);

    /** Prestamos abiertos de un usuario. */
    List<Prestamo> findByUsuarioIdAndEstadoInOrderByFechaPrestamoDesc(Long usuarioId,
                                                                      Collection<PrestamoEstado> estados);

    /**
     * Prestamos atrasados: no devueltos y con fecha de devolucion esperada vencida.
     * El orden se controla desde el {@code Pageable} que envia el servicio.
     */
    @Query("""
            select p from Prestamo p
            join fetch p.usuario u
            join fetch p.libro l
            where p.estado <> :devuelto
              and p.fechaDevolucionEsperada < :ahora
            """)
    Page<Prestamo> findVencidos(@Param("ahora") LocalDateTime ahora,
                                @Param("devuelto") PrestamoEstado devuelto,
                                Pageable pageable);
}

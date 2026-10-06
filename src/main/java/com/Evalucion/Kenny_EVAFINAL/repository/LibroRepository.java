package com.Evalucion.Kenny_EVAFINAL.repository;

import com.Evalucion.Kenny_EVAFINAL.entity.Libro;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface LibroRepository extends JpaRepository<Libro, Long>, JpaSpecificationExecutor<Libro> {

    /**
     * Libro visible en catalogo (no eliminado logicamente).
     * El catalogo y el alta/edicion siempre deben usar este metodo.
     */
    Optional<Libro> findByIdAndEliminadoFalse(Long id);

    boolean existsByIsbn(String isbn);

    boolean existsByIsbnAndIdNot(String isbn, Long id);

    /**
     * Bloqueo pesado (SELECT ... FOR UPDATE) sobre un libro no eliminado.
     *
     * <p>Se usa en el registro de prestamos y devoluciones para serializar el acceso
     * al stock frente a solicitudes simultaneas: dos transacciones no pueden leer y
     * decrementar/incrementar el mismo libro a la vez, lo que impide stock negativo
     * o el prestamo doble del ultimo ejemplar. Se complementa con {@code @Version}
     * en la entidad como defensa en profundidad.</p>
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select l from Libro l where l.id = :id and l.eliminado = false")
    Optional<Libro> findActivoByIdWithLock(@Param("id") Long id);

    /**
     * Lectura sin bloqueo para el historial: un libro puede estar eliminado
     * logicamente y aun asi ser referenciado por prestamos previos.
     */
    Optional<Libro> findById(Long id);
}

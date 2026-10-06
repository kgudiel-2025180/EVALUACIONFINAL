package com.Evalucion.Kenny_EVAFINAL.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.persistence.Version;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Libro del catalogo.
 *
 * <p>Decisiones tecnicas:</p>
 * <ul>
 *   <li><b>Eliminacion logica</b> ({@code eliminado}): el historial de prestamos debe
 *       conservarse, por lo que nunca se borran filas de esta tabla. El catalogo solo
 *       muestra los libros no eliminados (ver {@code LibroRepository}).</li>
 *   <li><b>@Version</b>: proteccion adicional frente a escrituras concurrentes sobre el
 *       stock. Se combina con bloqueo pesado (PESSIMISTIC_WRITE) en el flujo de prestamo.</li>
 * </ul>
 */
@Entity
@Table(
        name = "libros",
        uniqueConstraints = @UniqueConstraint(name = "uq_libros_isbn", columnNames = "isbn")
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Libro {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    @Size(max = 20)
    @Column(name = "isbn", nullable = false, length = 20, unique = true)
    private String isbn;

    @NotBlank
    @Size(max = 200)
    @Column(name = "titulo", nullable = false, length = 200)
    private String titulo;

    @NotBlank
    @Size(max = 120)
    @Column(name = "autor", nullable = false, length = 120)
    private String autor;

    @NotBlank
    @Size(max = 80)
    @Column(name = "categoria", nullable = false, length = 80)
    private String categoria;

    @NotNull
    @Min(value = 0, message = "stockTotal no puede ser negativo")
    @Column(name = "stock_total", nullable = false)
    private Integer stockTotal;

    @NotNull
    @Min(value = 0, message = "stockDisponible no puede ser negativo")
    @Column(name = "stock_disponible", nullable = false)
    private Integer stockDisponible;

    /** Eliminacion logica: conserva el historial de prestamos. */
    @NotNull
    @Column(name = "eliminado", nullable = false)
    @Builder.Default
    private Boolean eliminado = false;

    /** Bloqueo optimista (defensa en profundidad frente a actualizaciones concurrentes). */
    @Version
    @Column(name = "version", nullable = false)
    private Long version;

    @CreationTimestamp
    @Column(name = "fecha_creacion", nullable = false, updatable = false)
    private LocalDateTime fechaCreacion;

    /** Historial de prestamos del libro (carga diferida). */
    @OneToMany(mappedBy = "libro", fetch = FetchType.LAZY)
    @Builder.Default
    private List<Prestamo> prestamos = new ArrayList<>();
}

package com.Evalucion.Kenny_EVAFINAL.entity;

import com.Evalucion.Kenny_EVAFINAL.enums.PrestamoEstado;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * Prestamo de un libro a un usuario.
 *
 * <p>La fecha de devolucion esperada se calcula en el servicio de negocio
 * ({@code fechaPrestamo + 14 dias}); el cliente nunca puede manipularla.</p>
 *
 * <p>Las relaciones se declaran {@code FetchType.LAZY} y los controladores devuelven
 * DTOs, de modo que no existe riesgo de serializacion circular.</p>
 */
@Entity
@Table(name = "prestamos")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Prestamo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "libro_id", nullable = false)
    private Libro libro;

    @NotNull
    @Column(name = "fecha_prestamo", nullable = false)
    private LocalDateTime fechaPrestamo;

    /** Calculado por el negocio: fechaPrestamo + 14 dias. */
    @NotNull
    @Column(name = "fecha_devolucion_esperada", nullable = false)
    private LocalDateTime fechaDevolucionEsperada;

    /** Null mientras el prestamo no ha sido devuelto. */
    @Column(name = "fecha_devolucion_real")
    private LocalDateTime fechaDevolucionReal;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "estado", nullable = false, length = 20)
    private PrestamoEstado estado;

    @CreationTimestamp
    @Column(name = "fecha_creacion", nullable = false, updatable = false)
    private LocalDateTime fechaCreacion;

    /**
     * Un prestamo esta vencido si no fue devuelto y la fecha actual supera
     * la fecha de devolucion esperada.
     */
    public boolean estaVencido(LocalDateTime ahora) {
        return estado != PrestamoEstado.DEVUELTO && ahora.isAfter(fechaDevolucionEsperada);
    }
}

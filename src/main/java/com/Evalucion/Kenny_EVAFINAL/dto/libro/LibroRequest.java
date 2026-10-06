package com.Evalucion.Kenny_EVAFINAL.dto.libro;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Alta y edicion de libros. Las reglas de coherencia de stock
 * (0 &lt;= stockDisponible &lt;= stockTotal) se validan en el servicio.
 */
public record LibroRequest(
        @NotBlank(message = "el ISBN es obligatorio")
        @Size(max = 20, message = "el ISBN no puede superar 20 caracteres")
        @Pattern(regexp = "^[0-9Xx\\-]+$", message = "el ISBN solo puede contener digitos, 'X' y guiones")
        String isbn,

        @NotBlank(message = "el titulo es obligatorio")
        @Size(max = 200, message = "el titulo no puede superar 200 caracteres")
        String titulo,

        @NotBlank(message = "el autor es obligatorio")
        @Size(max = 120, message = "el autor no puede superar 120 caracteres")
        String autor,

        @NotBlank(message = "la categoria es obligatoria")
        @Size(max = 80, message = "la categoria no puede superar 80 caracteres")
        String categoria,

        @NotNull(message = "stockTotal es obligatorio")
        @Min(value = 0, message = "stockTotal no puede ser negativo")
        Integer stockTotal,

        @NotNull(message = "stockDisponible es obligatorio")
        @Min(value = 0, message = "stockDisponible no puede ser negativo")
        Integer stockDisponible) {
}

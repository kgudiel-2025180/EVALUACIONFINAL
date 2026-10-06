package com.Evalucion.Kenny_EVAFINAL.util;

import com.Evalucion.Kenny_EVAFINAL.entity.Libro;
import com.Evalucion.Kenny_EVAFINAL.exception.BusinessRuleException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * REGLA 8: invariante 0 &lt;= stockDisponible &lt;= stockTotal.
 * Prueba unitaria pura: sin contexto Spring.
 */
class StockValidatorTest {

    @Test
    @DisplayName("Valores coherentes no lanzan excepcion")
    void stockCoherente_esValido() {
        assertDoesNotThrow(() -> StockValidator.validar(5, 5));
        assertDoesNotThrow(() -> StockValidator.validar(5, 0));
        assertDoesNotThrow(() -> StockValidator.validar(0, 0));
        assertDoesNotThrow(() -> StockValidator.validar(10, 3));
    }

    @Test
    @DisplayName("stockDisponible mayor que stockTotal se rechaza")
    void disponibleMayorQueTotal_rechaza() {
        BusinessRuleException ex = assertThrows(BusinessRuleException.class,
                () -> StockValidator.validar(3, 4));
        assertEquals("El stock disponible no puede superar el stock total", ex.getMessage());
    }

    @Test
    @DisplayName("stockTotal negativo se rechaza")
    void stockTotalNegativo_rechaza() {
        BusinessRuleException ex = assertThrows(BusinessRuleException.class,
                () -> StockValidator.validar(-1, 0));
        assertEquals("El stock total no puede ser negativo", ex.getMessage());
    }

    @Test
    @DisplayName("stockDisponible negativo se rechaza")
    void stockDisponibleNegativo_rechaza() {
        BusinessRuleException ex = assertThrows(BusinessRuleException.class,
                () -> StockValidator.validar(5, -1));
        assertEquals("El stock disponible no puede ser negativo", ex.getMessage());
    }

    @Test
    @DisplayName("La sobrecarga que recibe la entidad valida el mismo invariante")
    void entidadLibro_validaElMismoInvariante() {
        Libro libro = Libro.builder()
                .isbn("978-000-1")
                .titulo("t")
                .autor("a")
                .categoria("c")
                .stockTotal(2)
                .stockDisponible(3)
                .eliminado(false)
                .build();

        assertThrows(BusinessRuleException.class, () -> StockValidator.validar(libro));
    }
}

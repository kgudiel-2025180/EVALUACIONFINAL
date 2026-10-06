package com.Evalucion.Kenny_EVAFINAL.util;

import com.Evalucion.Kenny_EVAFINAL.exception.BusinessRuleException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Proteccion de la paginacion frente a parametros manipulados por el cliente.
 */
class PageUtilsTest {

    private static final Sort ORDEN = Sort.by(Sort.Direction.ASC, "titulo");

    @Test
    @DisplayName("Pageable nulo o sin paginar usa el tamano por defecto")
    void pageableNulo_usaPorDefecto() {
        var normalizado = PageUtils.normalizar(null, ORDEN);
        assertEquals(0, normalizado.getPageNumber());
        assertEquals(Constantes.TAMANO_PAGINA_POR_DEFECTO, normalizado.getPageSize());
        assertTrue(normalizado.getSort().isSorted());

        assertEquals(Constantes.TAMANO_PAGINA_POR_DEFECTO,
                PageUtils.normalizar(org.springframework.data.domain.Pageable.unpaged(), ORDEN)
                        .getPageSize());
    }

    @Test
    @DisplayName("Pagina negativa se rechaza")
    void paginaNegativa_rechaza() {
        var pageable = mock(Pageable.class);
        when(pageable.isUnpaged()).thenReturn(false);
        when(pageable.getPageNumber()).thenReturn(-1);
        when(pageable.getPageSize()).thenReturn(10);
        when(pageable.getSort()).thenReturn(ORDEN);

        assertThrows(BusinessRuleException.class, () -> PageUtils.normalizar(pageable, ORDEN));
    }

    @Test
    @DisplayName("Tamano de pagina cero se rechaza")
    void tamanoCero_rechaza() {
        var pageable = mock(Pageable.class);
        when(pageable.isUnpaged()).thenReturn(false);
        when(pageable.getPageNumber()).thenReturn(0);
        when(pageable.getPageSize()).thenReturn(0);
        when(pageable.getSort()).thenReturn(ORDEN);

        assertThrows(BusinessRuleException.class, () -> PageUtils.normalizar(pageable, ORDEN));
    }

    @Test
    @DisplayName("Tamano por encima del maximo permitido se rechaza")
    void tamanoExcesivo_rechaza() {
        var pageable = PageRequest.of(0, Constantes.TAMANO_MAXIMO_PAGINA + 1, ORDEN);
        BusinessRuleException ex = assertThrows(BusinessRuleException.class,
                () -> PageUtils.normalizar(pageable, ORDEN));
        assertTrue(ex.getMessage().contains(String.valueOf(Constantes.TAMANO_MAXIMO_PAGINA)));
    }

    @Test
    @DisplayName("Sin orden del cliente se aplica el orden por defecto")
    void sinOrden_delOrdenPorDefecto() {
        var normalizado = PageUtils.normalizar(PageRequest.of(2, 20), ORDEN);
        assertEquals(2, normalizado.getPageNumber());
        assertEquals(20, normalizado.getPageSize());
        assertEquals("titulo", normalizado.getSort().iterator().next().getProperty());
    }

    @Test
    @DisplayName("El limite superior exacto de tamano esta permitido")
    void tamanoMaximoExacto_esPermitido() {
        var pageable = PageRequest.of(0, Constantes.TAMANO_MAXIMO_PAGINA, ORDEN);
        assertEquals(Constantes.TAMANO_MAXIMO_PAGINA,
                PageUtils.normalizar(pageable, ORDEN).getPageSize());
    }
}

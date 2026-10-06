package com.Evalucion.Kenny_EVAFINAL.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.nio.charset.StandardCharsets;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Catalogo de libros (bloque SEGURIDAD + PERSISTENCIA):
 * autorizacion por rol, validaciones, codigos HTTP y eliminacion logica.
 */
@SpringBootTest
@AutoConfigureMockMvc
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class LibroApiTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private String tokenAdmin;
    private String tokenLector;
    private String tokenBibliotecario;

    @BeforeAll
    void autenticar() throws Exception {
        tokenAdmin = login("admin@kinal.edu.gt", "Admin123!");
        tokenLector = login("lector@kinal.edu.gt", "Lector123!");
        tokenBibliotecario = login("bibliotecario@kinal.edu.gt", "Biblio123!");
    }

    private String login(String email, String password) throws Exception {
        MvcResult resultado = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                Map.of("email", email, "password", password))))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode json = objectMapper.readTree(resultado.getResponse().getContentAsString(StandardCharsets.UTF_8));
        return json.path("data").path("token").asText();
    }

    private String isbnUnico() {
        return "978-" + String.valueOf(System.nanoTime()).substring(0, 13);
    }

    private String cuerpoLibro(String isbn, int total, int disponible) throws Exception {
        return objectMapper.writeValueAsString(Map.of(
                "isbn", isbn,
                "titulo", "Titulo de prueba",
                "autor", "Autor de prueba",
                "categoria", "Pruebas",
                "stockTotal", total,
                "stockDisponible", disponible));
    }

    private long crearLibroComoAdmin(int total, int disponible) throws Exception {
        String isbn = isbnUnico();
        MvcResult resultado = mockMvc.perform(post("/api/v1/libros")
                        .header("Authorization", "Bearer " + tokenAdmin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpoLibro(isbn, total, disponible)))
                .andExpect(status().isCreated())
                .andReturn();
        JsonNode json = objectMapper.readTree(resultado.getResponse().getContentAsString(StandardCharsets.UTF_8));
        return json.path("data").path("id").asLong();
    }

    // ------------------------------------------------------------------
    // SEGURIDAD
    // ------------------------------------------------------------------

    @Test
    @DisplayName("401 sin token en cualquier endpoint del catalogo")
    void sinToken_noAutorizado() throws Exception {
        mockMvc.perform(get("/api/v1/libros")).andExpect(status().isUnauthorized());
        mockMvc.perform(post("/api/v1/libros")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpoLibro(isbnUnico(), 1, 1)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("401 con un token invalido o manipulado")
    void tokenInvalido_noAutorizado() throws Exception {
        mockMvc.perform(get("/api/v1/libros")
                        .header("Authorization", "Bearer token-falso-123456"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("INVALID_TOKEN"));
    }

    @Test
    @DisplayName("403 para LECTOR al crear, editar o eliminar libros")
    void lector_noPuedeEscribir() throws Exception {
        mockMvc.perform(post("/api/v1/libros")
                        .header("Authorization", "Bearer " + tokenLector)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpoLibro(isbnUnico(), 1, 1)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("FORBIDDEN"));

        mockMvc.perform(put("/api/v1/libros/1")
                        .header("Authorization", "Bearer " + tokenLector)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpoLibro("978-111-1", 1, 1)))
                .andExpect(status().isForbidden());

        mockMvc.perform(delete("/api/v1/libros/1")
                        .header("Authorization", "Bearer " + tokenLector))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("403 para BIBLIOTECARIO al crear libros (solo ADMIN)")
    void bibliotecario_noPuedeCrearLibros() throws Exception {
        mockMvc.perform(post("/api/v1/libros")
                        .header("Authorization", "Bearer " + tokenBibliotecario)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpoLibro(isbnUnico(), 1, 1)))
                .andExpect(status().isForbidden());
    }

    // ------------------------------------------------------------------
    // PERSISTENCIA Y REGLAS DE NEGOCIO
    // ------------------------------------------------------------------

    @Test
    @DisplayName("Catalogo paginado disponible para cualquier rol autenticado")
    void catalogo_paginado() throws Exception {
        mockMvc.perform(get("/api/v1/libros")
                        .param("page", "0")
                        .param("size", "5")
                        .header("Authorization", "Bearer " + tokenLector))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.totalElementos").isNumber())
                .andExpect(jsonPath("$.data.tamanoPagina").value(5))
                .andExpect(jsonPath("$.data.contenido").isArray())
                .andExpect(jsonPath("$.data.contenido[0].password").doesNotExist());
    }

    @Test
    @DisplayName("La carga inicial contiene los libros de ejemplo")
    void cargaInicial_librosSemilla() throws Exception {
        MvcResult resultado = mockMvc.perform(get("/api/v1/libros")
                        .param("size", "100")
                        .header("Authorization", "Bearer " + tokenAdmin))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode json = objectMapper.readTree(resultado.getResponse().getContentAsString(StandardCharsets.UTF_8));
        long total = json.path("data").path("totalElementos").asLong();
        assertTrue(total >= 10, "Deberian existir al menos 10 libros semilla, hay " + total);
    }

    @Test
    @DisplayName("Filtro por titulo y categoria")
    void catalogo_conFiltros() throws Exception {
        mockMvc.perform(get("/api/v1/libros")
                        .param("titulo", "principito")
                        .header("Authorization", "Bearer " + tokenAdmin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalElementos").value(1));

        mockMvc.perform(get("/api/v1/libros")
                        .param("categoria", "NOEXISTENTE")
                        .header("Authorization", "Bearer " + tokenAdmin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalElementos").value(0))
                .andExpect(jsonPath("$.data.contenido").isEmpty());
    }

    @Test
    @DisplayName("POST: 201 crea el libro y puede consultarse por id")
    void crear_libro() throws Exception {
        String isbn = isbnUnico();

        MvcResult creado = mockMvc.perform(post("/api/v1/libros")
                        .header("Authorization", "Bearer " + tokenAdmin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpoLibro(isbn, 4, 2)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.isbn").value(isbn))
                .andExpect(jsonPath("$.data.stockTotal").value(4))
                .andExpect(jsonPath("$.data.stockDisponible").value(2))
                .andReturn();

        JsonNode json = objectMapper.readTree(creado.getResponse().getContentAsString(StandardCharsets.UTF_8));
        long id = json.path("data").path("id").asLong();

        mockMvc.perform(get("/api/v1/libros/" + id)
                        .header("Authorization", "Bearer " + tokenLector))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(id));
    }

    @Test
    @DisplayName("REGLA 8: 400 si stockDisponible supera stockTotal")
    void crear_stockIncoherente_badRequest() throws Exception {
        mockMvc.perform(post("/api/v1/libros")
                        .header("Authorization", "Bearer " + tokenAdmin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpoLibro(isbnUnico(), 2, 5)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error").value("BUSINESS_RULE_VIOLATION"))
                .andExpect(jsonPath("$.message").value(
                        "El stock disponible no puede superar el stock total"));
    }

    @Test
    @DisplayName("409 si el ISBN ya existe")
    void crear_isbnDuplicado_conflict() throws Exception {
        String isbn = isbnUnico();
        mockMvc.perform(post("/api/v1/libros")
                        .header("Authorization", "Bearer " + tokenAdmin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpoLibro(isbn, 1, 1)))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/v1/libros")
                        .header("Authorization", "Bearer " + tokenAdmin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpoLibro(isbn, 5, 5)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("CONFLICT"));
    }

    @Test
    @DisplayName("400 si el ISBN tiene caracteres no permitidos o faltan campos")
    void crear_datosInvalidos_badRequest() throws Exception {
        mockMvc.perform(post("/api/v1/libros")
                        .header("Authorization", "Bearer " + tokenAdmin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpoLibro("isbn con espacios!", 1, 1)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"));

        mockMvc.perform(post("/api/v1/libros")
                        .header("Authorization", "Bearer " + tokenAdmin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"isbn\":\"978-999-9\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"));
    }

    @Test
    @DisplayName("PUT: 200 actualiza el libro")
    void actualizar_libro() throws Exception {
        long id = crearLibroComoAdmin(3, 3);
        String cuerpo = objectMapper.writeValueAsString(Map.of(
                "isbn", "978-222-" + String.valueOf(System.nanoTime()).substring(0, 9),
                "titulo", "Titulo actualizado",
                "autor", "Autor actualizado",
                "categoria", "Actualizado",
                "stockTotal", 7,
                "stockDisponible", 6));

        mockMvc.perform(put("/api/v1/libros/" + id)
                        .header("Authorization", "Bearer " + tokenAdmin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpo))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.titulo").value("Titulo actualizado"))
                .andExpect(jsonPath("$.data.stockTotal").value(7))
                .andExpect(jsonPath("$.data.stockDisponible").value(6));
    }

    @Test
    @DisplayName("404 al actualizar o eliminar un libro inexistente")
    void operaciones_libroInexistente_notFound() throws Exception {
        mockMvc.perform(put("/api/v1/libros/999999")
                        .header("Authorization", "Bearer " + tokenAdmin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpoLibro(isbnUnico(), 1, 1)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("RESOURCE_NOT_FOUND"));

        mockMvc.perform(delete("/api/v1/libros/999999")
                        .header("Authorization", "Bearer " + tokenAdmin))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("DELETE: 204 y desaparece del catalogo (eliminacion logica)")
    void eliminar_libro_borradoLogico() throws Exception {
        long id = crearLibroComoAdmin(2, 2);

        mockMvc.perform(delete("/api/v1/libros/" + id)
                        .header("Authorization", "Bearer " + tokenAdmin))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/v1/libros/" + id)
                        .header("Authorization", "Bearer " + tokenAdmin))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("GET /{id}: 404 si no existe")
    void obtener_libroInexistente_notFound() throws Exception {
        mockMvc.perform(get("/api/v1/libros/424242")
                        .header("Authorization", "Bearer " + tokenAdmin))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("RESOURCE_NOT_FOUND"));
    }

    @Test
    @DisplayName("Un tamano de pagina por encima del maximo responde 400")
    void paginacionTamanoExcesivo_badRequest() throws Exception {
        mockMvc.perform(get("/api/v1/libros")
                        .param("size", "9999")
                        .header("Authorization", "Bearer " + tokenAdmin))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("BUSINESS_RULE_VIOLATION"))
                .andExpect(jsonPath("$.message").value(
                        "El tamano de pagina no puede superar 100 registros"));
    }

    @Test
    @DisplayName("Una pagina negativa se normaliza a la primera pagina en lugar de romper")
    void paginacionPaginaNegativa_normalizada() throws Exception {
        MvcResult resultado = mockMvc.perform(get("/api/v1/libros")
                        .param("page", "-1")
                        .header("Authorization", "Bearer " + tokenAdmin))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode json = objectMapper.readTree(resultado.getResponse().getContentAsString(StandardCharsets.UTF_8));
        assertEquals(0, json.path("data").path("pagina").asInt());
    }

    @Test
    @DisplayName("Un campo de orden inexistente responde 400 y no 500")
    void ordenInexistente_badRequest() throws Exception {
        mockMvc.perform(get("/api/v1/libros")
                        .param("sort", "campoInexistente,asc")
                        .header("Authorization", "Bearer " + tokenAdmin))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("INVALID_SORT"));
    }

    @Test
    @DisplayName("La respuesta del catalogo nunca incluye datos internos")
    void catalogo_noExponeInterno() throws Exception {
        MvcResult resultado = mockMvc.perform(get("/api/v1/libros")
                        .param("size", "1")
                        .header("Authorization", "Bearer " + tokenAdmin))
                .andExpect(status().isOk())
                .andReturn();

        String cuerpo = resultado.getResponse().getContentAsString(StandardCharsets.UTF_8);
        assertTrue(cuerpo.contains("\"success\":true"));
        assertFalse(cuerpo.contains("stackTrace"));
        assertFalse(cuerpo.contains("password"));
        assertFalse(cuerpo.contains("eliminado"));
    }
}

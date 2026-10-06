package com.Evalucion.Kenny_EVAFINAL.api;

import com.Evalucion.Kenny_EVAFINAL.entity.Libro;
import com.Evalucion.Kenny_EVAFINAL.entity.Prestamo;
import com.Evalucion.Kenny_EVAFINAL.entity.Usuario;
import com.Evalucion.Kenny_EVAFINAL.enums.PrestamoEstado;
import com.Evalucion.Kenny_EVAFINAL.enums.UsuarioEstado;
import com.Evalucion.Kenny_EVAFINAL.repository.LibroRepository;
import com.Evalucion.Kenny_EVAFINAL.repository.PrestamoRepository;
import com.Evalucion.Kenny_EVAFINAL.repository.UsuarioRepository;
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
import java.time.LocalDateTime;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Reglas de negocio de prestamos y devoluciones (REGLAS 1-13) mas el control
 * de acceso por rol. Cada prueba crea sus propios datos, por lo que el orden
 * de ejecucion no influye en el resultado.
 */
@SpringBootTest
@AutoConfigureMockMvc
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class PrestamoApiTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private LibroRepository libroRepository;

    @Autowired
    private PrestamoRepository prestamoRepository;

    private String tokenAdmin;
    private String tokenBibliotecario;
    private String tokenLector;

    @BeforeAll
    void autenticar() throws Exception {
        tokenAdmin = login("admin@kinal.edu.gt", "Admin123!");
        tokenBibliotecario = login("bibliotecario@kinal.edu.gt", "Biblio123!");
        tokenLector = login("lector@kinal.edu.gt", "Lector123!");
    }

    // ------------------------------------------------------------------
    // Utilidades
    // ------------------------------------------------------------------

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

    private String registrarLector(String prefijo) throws Exception {
        String email = prefijo + "-" + System.nanoTime() + "@kinal.edu.gt";
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "nombre", "Lector de pruebas",
                                "email", email,
                                "password", "Secreta123!"))))
                .andExpect(status().isCreated());
        return email;
    }

    private long idDe(String email) {
        return usuarioRepository.findByEmail(email).orElseThrow().getId();
    }

    private long crearLibro(int stockTotal, int stockDisponible) throws Exception {
        String isbn = "978-" + String.valueOf(System.nanoTime()).substring(0, 13);
        MvcResult resultado = mockMvc.perform(post("/api/v1/libros")
                        .header("Authorization", "Bearer " + tokenAdmin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "isbn", isbn,
                                "titulo", "Libro de prestamos",
                                "autor", "Autor",
                                "categoria", "Pruebas",
                                "stockTotal", stockTotal,
                                "stockDisponible", stockDisponible))))
                .andExpect(status().isCreated())
                .andReturn();
        JsonNode json = objectMapper.readTree(resultado.getResponse().getContentAsString(StandardCharsets.UTF_8));
        return json.path("data").path("id").asLong();
    }

    private JsonNode prestar(String token, long usuarioId, long libroId) throws Exception {
        MvcResult resultado = mockMvc.perform(post("/api/v1/prestamos")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "usuarioId", usuarioId, "libroId", libroId))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.estado").value("ACTIVO"))
                .andReturn();
        return objectMapper.readTree(resultado.getResponse().getContentAsString(StandardCharsets.UTF_8));
    }

    private void forzarVencimiento(long prestamoId) {
        Prestamo prestamo = prestamoRepository.findById(prestamoId).orElseThrow();
        prestamo.setFechaDevolucionEsperada(LocalDateTime.now().minusDays(2));
        prestamo.setFechaPrestamo(LocalDateTime.now().minusDays(20));
        prestamoRepository.save(prestamo);
    }

    private int stockActual(long libroId) {
        Libro libro = libroRepository.findById(libroId).orElseThrow();
        return libro.getStockDisponible();
    }

    // ------------------------------------------------------------------
    // SEGURIDAD
    // ------------------------------------------------------------------

    @Test
    @DisplayName("401 sin token")
    void sinToken_noAutorizado() throws Exception {
        mockMvc.perform(post("/api/v1/prestamos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"usuarioId\":1,\"libroId\":1}"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(get("/api/v1/prestamos/mis-prestamos"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(get("/api/v1/prestamos/atrasados"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("403 para LECTOR al registrar prestamo y al consultar atrasados")
    void lector_noPuedePrestarNiVerAtrasados() throws Exception {
        mockMvc.perform(post("/api/v1/prestamos")
                        .header("Authorization", "Bearer " + tokenLector)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"usuarioId\":1,\"libroId\":1}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("FORBIDDEN"));

        mockMvc.perform(get("/api/v1/prestamos/atrasados")
                        .header("Authorization", "Bearer " + tokenLector))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("403 para LECTOR y ADMIN en /mis-prestamos (solo LECTOR)")
    void misPrestamos_soloLECTOR() throws Exception {
        mockMvc.perform(get("/api/v1/prestamos/mis-prestamos")
                        .header("Authorization", "Bearer " + tokenBibliotecario))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/v1/prestamos/mis-prestamos")
                        .header("Authorization", "Bearer " + tokenAdmin))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/v1/prestamos/mis-prestamos")
                        .header("Authorization", "Bearer " + tokenLector))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.contenido").isArray());
    }

    @Test
    @DisplayName("BIBLIOTECARIO si puede registrar prestamos y devoluciones")
    void bibliotecario_puedePrestar() throws Exception {
        String email = registrarLector("lector-bib");
        long usuarioId = idDe(email);
        long libroId = crearLibro(3, 3);

        JsonNode prestamo = prestar(tokenBibliotecario, usuarioId, libroId);
        long prestamoId = prestamo.path("data").path("id").asLong();

        mockMvc.perform(patch("/api/v1/prestamos/" + prestamoId + "/devolucion")
                        .header("Authorization", "Bearer " + tokenBibliotecario))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.estado").value("DEVUELTO"));
    }

    // ------------------------------------------------------------------
    // REGLAS 3, 6 y 8: fechas y stock
    // ------------------------------------------------------------------

    @Test
    @DisplayName("REGLA 3 y 6: plazo de 14 dias calculado por el backend y stock descontado")
    void plazoCatorceDias_yDescuentoDeStock() throws Exception {
        String email = registrarLector("lector-plazo");
        long usuarioId = idDe(email);
        long libroId = crearLibro(5, 5);
        assertEquals(5, stockActual(libroId));

        JsonNode respuesta = prestar(tokenAdmin, usuarioId, libroId);

        String fechaPrestamo = respuesta.path("data").path("fechaPrestamo").asText();
        String fechaEsperada = respuesta.path("data").path("fechaDevolucionEsperada").asText();
        assertNotNull(fechaPrestamo);
        assertNotNull(fechaEsperada);

        LocalDateTime p = LocalDateTime.parse(fechaPrestamo);
        LocalDateTime e = LocalDateTime.parse(fechaEsperada);
        assertEquals(14, java.time.Duration.between(p, e).toDays(),
                "El plazo debe ser de exactamente 14 dias");

        assertEquals(4, stockActual(libroId), "REGLA 6: stockDisponible debe disminuir en 1");
    }

    // ------------------------------------------------------------------
    // REGLA 1: sin stock no hay prestamo
    // ------------------------------------------------------------------

    @Test
    @DisplayName("REGLA 1: 400 si el libro no tiene ejemplares disponibles")
    void sinStock_rechazado() throws Exception {
        String email = registrarLector("lector-sinstock");
        long usuarioId = idDe(email);
        long libroId = crearLibro(2, 0);

        mockMvc.perform(post("/api/v1/prestamos")
                        .header("Authorization", "Bearer " + tokenAdmin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "usuarioId", usuarioId, "libroId", libroId))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("BUSINESS_RULE_VIOLATION"))
                .andExpect(jsonPath("$.message").value("El libro no tiene ejemplares disponibles"));

        assertEquals(0, stockActual(libroId));
    }

    @Test
    @DisplayName("REGLA 1: el ultimo ejemplar se puede prestar y luego no")
    void ultimoEjemplar_luegoSinStock() throws Exception {
        long libroId = crearLibro(1, 1);

        String email1 = registrarLector("lector-ultimo-a");
        prestar(tokenAdmin, idDe(email1), libroId);
        assertEquals(0, stockActual(libroId));

        String email2 = registrarLector("lector-ultimo-b");
        mockMvc.perform(post("/api/v1/prestamos")
                        .header("Authorization", "Bearer " + tokenAdmin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "usuarioId", idDe(email2), "libroId", libroId))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("El libro no tiene ejemplares disponibles"));
    }

    // ------------------------------------------------------------------
    // REGLA 2: maximo 3 prestamos activos
    // ------------------------------------------------------------------

    @Test
    @DisplayName("REGLA 2: el lector no puede tener mas de 3 prestamos activos")
    void maximoTresPrestamosActivos() throws Exception {
        String email = registrarLector("lector-limitado");
        long usuarioId = idDe(email);
        long libroId = crearLibro(10, 10);

        prestar(tokenAdmin, usuarioId, libroId);
        prestar(tokenAdmin, usuarioId, libroId);
        prestar(tokenAdmin, usuarioId, libroId);

        mockMvc.perform(post("/api/v1/prestamos")
                        .header("Authorization", "Bearer " + tokenAdmin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "usuarioId", usuarioId, "libroId", libroId))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("BUSINESS_RULE_VIOLATION"))
                .andExpect(jsonPath("$.message").value(
                        "El lector no puede tener mas de 3 prestamos activos al mismo tiempo"));

        assertEquals(7, stockActual(libroId), "Solo se descontaron 3 unidades");
    }

    @Test
    @DisplayName("REGLA 2: al devolver un prestamo se libera cupo")
    void devolver_liberaCupo() throws Exception {
        String email = registrarLector("lector-cupo");
        long usuarioId = idDe(email);
        long libroId = crearLibro(10, 10);

        long p1 = prestar(tokenAdmin, usuarioId, libroId).path("data").path("id").asLong();
        prestar(tokenAdmin, usuarioId, libroId);
        prestar(tokenAdmin, usuarioId, libroId);

        mockMvc.perform(patch("/api/v1/prestamos/" + p1 + "/devolucion")
                        .header("Authorization", "Bearer " + tokenAdmin))
                .andExpect(status().isOk());

        prestar(tokenAdmin, usuarioId, libroId);
    }

    // ------------------------------------------------------------------
    // REGLA 7: devolucion devuelve el stock
    // ------------------------------------------------------------------

    @Test
    @DisplayName("REGLA 7: la devolucion incrementa stockDisponible en 1")
    void devolver_incrementaStock() throws Exception {
        String email = registrarLector("lector-devuelve");
        long usuarioId = idDe(email);
        long libroId = crearLibro(4, 4);

        long prestamoId = prestar(tokenAdmin, usuarioId, libroId).path("data").path("id").asLong();
        assertEquals(3, stockActual(libroId));

        mockMvc.perform(patch("/api/v1/prestamos/" + prestamoId + "/devolucion")
                        .header("Authorization", "Bearer " + tokenAdmin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.prestamoId").value(prestamoId))
                .andExpect(jsonPath("$.data.libroId").value(libroId))
                .andExpect(jsonPath("$.data.estado").value("DEVUELTO"))
                .andExpect(jsonPath("$.data.stockDisponibleActualizado").value(4));

        assertEquals(4, stockActual(libroId));
    }

    @Test
    @DisplayName("REGLA 12: 409 al intentar devolver dos veces")
    void devolver_dosVeces_conflict() throws Exception {
        String email = registrarLector("lector-doble");
        long usuarioId = idDe(email);
        long libroId = crearLibro(3, 3);

        long prestamoId = prestar(tokenAdmin, usuarioId, libroId).path("data").path("id").asLong();

        mockMvc.perform(patch("/api/v1/prestamos/" + prestamoId + "/devolucion")
                        .header("Authorization", "Bearer " + tokenAdmin))
                .andExpect(status().isOk());

        mockMvc.perform(patch("/api/v1/prestamos/" + prestamoId + "/devolucion")
                        .header("Authorization", "Bearer " + tokenAdmin))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("CONFLICT"));

        assertEquals(3, stockActual(libroId), "El stock no debe incrementarse dos veces");
    }

    @Test
    @DisplayName("REGLA 11: 404 al devolver un prestamo inexistente")
    void devolver_prestamoInexistente_notFound() throws Exception {
        mockMvc.perform(patch("/api/v1/prestamos/999999/devolucion")
                        .header("Authorization", "Bearer " + tokenAdmin))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("RESOURCE_NOT_FOUND"));
    }

    // ------------------------------------------------------------------
    // REGLAS 4 y 5: sanciones
    // ------------------------------------------------------------------

    @Test
    @DisplayName("REGLA 4: el lector con prestamo vencido pasa a SANCIONADO y se rechaza")
    void lectorVencido_seSancionaAutomaticamente() throws Exception {
        String email = registrarLector("lector-vencido");
        long usuarioId = idDe(email);
        long libroId = crearLibro(5, 5);

        long prestamoId = prestar(tokenAdmin, usuarioId, libroId).path("data").path("id").asLong();
        forzarVencimiento(prestamoId);

        mockMvc.perform(post("/api/v1/prestamos")
                        .header("Authorization", "Bearer " + tokenAdmin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "usuarioId", usuarioId, "libroId", libroId))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("BUSINESS_RULE_VIOLATION"))
                .andExpect(jsonPath("$.message").value(
                        "El usuario tiene prestamos vencidos y ha sido sancionado. "
                                + "No puede solicitar nuevos prestamos"));

        Usuario sancionado = usuarioRepository.findById(usuarioId).orElseThrow();
        assertEquals(UsuarioEstado.SANCIONADO, sancionado.getEstado(),
                "REGLA 4: la sancion debe quedar persistida");
    }

    @Test
    @DisplayName("REGLA 5: un usuario ya sancionado no puede recibir prestamos")
    void usuarioSancionado_rechazado() throws Exception {
        String email = registrarLector("lector-sancionado");
        long usuarioId = idDe(email);
        long libroId = crearLibro(5, 5);

        Usuario usuario = usuarioRepository.findById(usuarioId).orElseThrow();
        usuario.setEstado(UsuarioEstado.SANCIONADO);
        usuarioRepository.save(usuario);

        mockMvc.perform(post("/api/v1/prestamos")
                        .header("Authorization", "Bearer " + tokenAdmin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "usuarioId", usuarioId, "libroId", libroId))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(
                        "El usuario esta sancionado y no puede solicitar prestamos"));

        assertEquals(5, stockActual(libroId), "El stock no debe tocarse");
    }

    // ------------------------------------------------------------------
    // REGLAS 9 y 10: existencia
    // ------------------------------------------------------------------

    @Test
    @DisplayName("REGLA 10: 404 si el usuario no existe")
    void usuarioInexistente_notFound() throws Exception {
        long libroId = crearLibro(3, 3);
        mockMvc.perform(post("/api/v1/prestamos")
                        .header("Authorization", "Bearer " + tokenAdmin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "usuarioId", 999999, "libroId", libroId))))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("RESOURCE_NOT_FOUND"));
    }

    @Test
    @DisplayName("REGLA 9: 404 si el libro no existe")
    void libroInexistente_notFound() throws Exception {
        String email = registrarLector("lector-libro-faltante");
        mockMvc.perform(post("/api/v1/prestamos")
                        .header("Authorization", "Bearer " + tokenAdmin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "usuarioId", idDe(email), "libroId", 999999))))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("400 si faltan datos de la solicitud de prestamo")
    void prestamoDatosInvalidos_badRequest() throws Exception {
        mockMvc.perform(post("/api/v1/prestamos")
                        .header("Authorization", "Bearer " + tokenAdmin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"));
    }

    // ------------------------------------------------------------------
    // AISLAMIENTO DEL HISTORIAL Y ATRASOS
    // ------------------------------------------------------------------

    @Test
    @DisplayName("/mis-prestamos solo devuelve los prestamos del usuario autenticado")
    void misPrestamos_noExponeHistorialAjeno() throws Exception {
        String emailPropio = registrarLector("lector-propio");
        String emailAjeno = registrarLector("lector-ajeno");
        long libroId = crearLibro(10, 10);

        prestar(tokenAdmin, idDe(emailAjeno), libroId);
        prestar(tokenAdmin, idDe(emailPropio), libroId);

        String tokenPropio = login(emailPropio, "Secreta123!");
        MvcResult resultado = mockMvc.perform(get("/api/v1/prestamos/mis-prestamos")
                        .header("Authorization", "Bearer " + tokenPropio))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalElementos").value(1))
                .andExpect(jsonPath("$.data.contenido[0].usuarioEmail").value(emailPropio))
                .andReturn();

        String cuerpo = resultado.getResponse().getContentAsString(StandardCharsets.UTF_8);
        assertTrue(!cuerpo.contains(emailAjeno),
                "El historial ajeno no debe aparecer: " + cuerpo);
    }

    @Test
    @DisplayName("/atrasados lista los prestamos vencidos y marca su estado")
    void atrasados_listaYMarcaEstado() throws Exception {
        String email = registrarLector("lector-atraso-api");
        long usuarioId = idDe(email);
        long libroId = crearLibro(6, 6);

        long prestamoId = prestar(tokenAdmin, usuarioId, libroId).path("data").path("id").asLong();
        forzarVencimiento(prestamoId);

        MvcResult resultado = mockMvc.perform(get("/api/v1/prestamos/atrasados")
                        .param("size", "100")
                        .header("Authorization", "Bearer " + tokenBibliotecario))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.contenido").isArray())
                .andExpect(jsonPath("$.data.totalElementos").value(
                        org.hamcrest.Matchers.greaterThanOrEqualTo(1)))
                .andReturn();

        JsonNode json = objectMapper.readTree(resultado.getResponse().getContentAsString(StandardCharsets.UTF_8));
        JsonNode contenido = json.path("data").path("contenido");
        boolean encontrado = false;
        for (JsonNode nodo : contenido) {
            if (email.equals(nodo.path("usuarioEmail").asText())) {
                encontrado = true;
                assertEquals("ATRASADO", nodo.path("estado").asText());
                assertNotNull(nodo.path("fechaDevolucionEsperada").asText());
            }
        }
        assertTrue(encontrado, "El prestamo vencido debe aparecer en /atrasados");

        Prestamo marcado = prestamoRepository.findById(prestamoId).orElseThrow();
        assertEquals(PrestamoEstado.ATRASADO, marcado.getEstado(),
                "El listado debe dejar el estado persistido como ATRASADO");
    }

    @Test
    @DisplayName("GET /atrasados con ADMIN tambien esta permitido")
    void atrasados_tambienParaAdmin() throws Exception {
        mockMvc.perform(get("/api/v1/prestamos/atrasados")
                        .header("Authorization", "Bearer " + tokenAdmin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }
}

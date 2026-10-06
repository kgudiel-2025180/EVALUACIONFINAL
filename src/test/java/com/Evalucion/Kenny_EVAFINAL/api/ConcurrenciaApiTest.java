package com.Evalucion.Kenny_EVAFINAL.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Verificacion de la <b>capacidad bajo saturacion</b> contra el servidor real
 * (Tomcat + H2), no contra MockMvc.
 *
 * <ol>
 *   <li>100 peticiones GET simultaneas: todas deben responder 200.</li>
 *   <li>20 prestamos simultaneos sobre el mismo libro: todos deben concederse
 *       y el stock final debe ser exactamente 0 (sin actualizaciones perdidas
 *       ni ejemplar prestado dos veces).</li>
 * </ol>
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ConcurrenciaApiTest {

    private static final int PETICIONES_SIMULTANEAS = 100;
    private static final int EJEMPLARES = 20;
    private static final Duration ESPERA_TOTAL = Duration.ofSeconds(90);

    @Value("${local.server.port}")
    private int puerto;

    @Autowired
    private ObjectMapper objectMapper;

    private HttpClient cliente;
    private String base;
    private String tokenAdmin;

    @BeforeAll
    void preparar() {
        cliente = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();
        base = "http://localhost:" + puerto + "/api/v1";
        tokenAdmin = login("admin@kinal.edu.gt", "Admin123!");
        assertNotNull(tokenAdmin);
    }

    // ------------------------------------------------------------------
    // Utilidades HTTP sobre el servidor real
    // ------------------------------------------------------------------

    private HttpRequest peticion(String metodo, String ruta, String token, String cuerpo) {
        HttpRequest.Builder builder = HttpRequest.newBuilder()
                .uri(URI.create(base + ruta))
                .timeout(Duration.ofSeconds(30))
                .header("Accept", "application/json");
        if (token != null) {
            builder.header("Authorization", "Bearer " + token);
        }
        return switch (metodo) {
            case "POST" -> builder.header("Content-Type", "application/json")
                    .POST(body(cuerpo)).build();
            case "PATCH" -> builder.header("Content-Type", "application/json")
                    .method("PATCH", body(cuerpo)).build();
            default -> builder.GET().build();
        };
    }

    private HttpRequest.BodyPublisher body(String contenido) {
        return HttpRequest.BodyPublishers.ofString(contenido == null ? "" : contenido,
                StandardCharsets.UTF_8);
    }

    private HttpResponse<String> ejecutar(String metodo, String ruta, String token, String cuerpo)
            throws Exception {
        return cliente.send(peticion(metodo, ruta, token, cuerpo),
                HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
    }

    private JsonNode json(HttpResponse<String> respuesta) throws Exception {
        return objectMapper.readTree(respuesta.body());
    }

    private String login(String email, String password) {
        try {
            HttpResponse<String> respuesta = ejecutar("POST", "/auth/login", null,
                    objectMapper.writeValueAsString(Map.of("email", email, "password", password)));
            assertEquals(200, respuesta.statusCode(), "Login fallido: " + respuesta.body());
            return json(respuesta).path("data").path("token").asText();
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    // ------------------------------------------------------------------
    // SATURACION
    // ------------------------------------------------------------------

    @Test
    @DisplayName("100 peticiones GET simultaneas responden todas 200")
    void cienPeticionesSimultaneas_todasOk() throws Exception {
        List<CompletableFuture<HttpResponse<String>>> tareas = new ArrayList<>();
        for (int i = 0; i < PETICIONES_SIMULTANEAS; i++) {
            tareas.add(cliente.sendAsync(peticion("GET", "/libros?size=10", tokenAdmin, null),
                    HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8)));
        }

        CompletableFuture.allOf(tareas.toArray(new CompletableFuture[0]))
                .get(ESPERA_TOTAL.toSeconds(), TimeUnit.SECONDS);

        long correctas = tareas.stream().filter(t -> t.join().statusCode() == 200).count();
        long fallidas = tareas.stream()
                .filter(t -> t.join().statusCode() != 200)
                .map(t -> t.join().statusCode() + ": " + t.join().body())
                .count();

        assertEquals(PETICIONES_SIMULTANEAS, correctas,
                "Todas las peticiones concurrentes deben responder 200. Fallidas=" + fallidas);
    }

    @Test
    @DisplayName("20 prestamos simultaneos sobre el mismo libro no pierden actualizaciones")
    void prestamosSimultaneos_sinCarreraDeStock() throws Exception {
        String isbn = "978-" + String.valueOf(System.nanoTime()).substring(0, 13);
        HttpResponse<String> libroCreado = ejecutar("POST", "/libros", tokenAdmin,
                objectMapper.writeValueAsString(Map.of(
                        "isbn", isbn,
                        "titulo", "Libro bajo concurrencia",
                        "autor", "Autor",
                        "categoria", "Pruebas",
                        "stockTotal", EJEMPLARES,
                        "stockDisponible", EJEMPLARES)));
        assertEquals(201, libroCreado.statusCode(), libroCreado.body());
        long libroId = json(libroCreado).path("data").path("id").asLong();

        // Un lector distinto por prestamo: la REGLA 2 limita a 3 por lector.
        List<Long> usuarios = new ArrayList<>();
        for (int i = 0; i < EJEMPLARES; i++) {
            HttpResponse<String> registro = ejecutar("POST", "/auth/register", null,
                    objectMapper.writeValueAsString(Map.of(
                            "nombre", "Concurrente " + i,
                            "email", "conc-" + System.nanoTime() + "-" + i + "@kinal.edu.gt",
                            "password", "Secreta123!")));
            assertEquals(201, registro.statusCode(), registro.body());
            usuarios.add(json(registro).path("data").path("id").asLong());
        }

        List<CompletableFuture<HttpResponse<String>>> tareas = new ArrayList<>();
        for (Long usuarioId : usuarios) {
            tareas.add(cliente.sendAsync(
                    peticion("POST", "/prestamos", tokenAdmin,
                            objectMapper.writeValueAsString(Map.of(
                                    "usuarioId", usuarioId, "libroId", libroId))),
                    HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8)));
        }
        CompletableFuture.allOf(tareas.toArray(new CompletableFuture[0]))
                .get(ESPERA_TOTAL.toSeconds(), TimeUnit.SECONDS);

        long concedidos = tareas.stream().filter(t -> t.join().statusCode() == 201).count();
        String detalle = tareas.stream()
                .filter(t -> t.join().statusCode() != 201)
                .map(t -> t.join().statusCode() + ": " + t.join().body())
                .reduce("", (a, b) -> a + " | " + b);

        assertEquals(EJEMPLARES, concedidos,
                "Todos los prestamos simultaneos deben concederse. Rechazados=" + detalle);

        // REGLA 8: el stock final debe ser exactamente 0, sin perdidas ni excesos.
        HttpResponse<String> consultado = ejecutar("GET", "/libros/" + libroId, tokenAdmin, null);
        assertEquals(200, consultado.statusCode());
        assertEquals(0, json(consultado).path("data").path("stockDisponible").asInt(),
                "El stock debe quedar en 0 tras prestar todos los ejemplares");

        // REGLA 1: el siguiente intento debe rechazarse.
        HttpResponse<String> extra = ejecutar("POST", "/prestamos", tokenAdmin,
                objectMapper.writeValueAsString(Map.of(
                        "usuarioId", usuarios.get(0), "libroId", libroId)));
        assertEquals(400, extra.statusCode());
        assertTrue(json(extra).path("message").asText().contains("ejemplares disponibles"));
    }
}

package com.Evalucion.Kenny_EVAFINAL.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
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
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Autenticacion (bloque SEGURIDAD): registro publico, login y codigos HTTP.
 * Cada prueba usa emails propios para no depender del orden de ejecucion.
 */
@SpringBootTest
@AutoConfigureMockMvc
class AuthApiTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private static long contador = 0;

    private String emailUnico() {
        return "auth-" + System.nanoTime() + "-" + (contador++) + "@kinal.edu.gt";
    }

    private String registro(String nombre, String email, String password) throws Exception {
        return objectMapper.writeValueAsString(Map.of(
                "nombre", nombre, "email", email, "password", password));
    }

    @Test
    @DisplayName("Registro: 201 y el rol se fuerza a LECTOR aunque el cliente envie otro")
    void registro_forzaRolLector() throws Exception {
        String email = emailUnico();
        String cuerpo = objectMapper.writeValueAsString(Map.of(
                "nombre", "Estudiante Nuevo",
                "email", email,
                "password", "Secreta123!",
                "rol", "ADMIN",
                "estado", "SANCIONADO"));

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpo))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.email").value(email))
                .andExpect(jsonPath("$.data.rol").value("LECTOR"))
                .andExpect(jsonPath("$.data.estado").value("ACTIVO"))
                .andExpect(jsonPath("$.data.password").doesNotExist());
    }

    @Test
    @DisplayName("Registro: 409 si el email ya existe")
    void registro_emailDuplicado_conflict() throws Exception {
        String email = emailUnico();
        String cuerpo = registro("Primero", email, "Secreta123!");

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpo))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpo))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error").value("CONFLICT"));
    }

    @Test
    @DisplayName("Registro: 400 si la contrasena es demasiado corta o el email invalido")
    void registroDatosInvalidos_badRequest() throws Exception {
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registro("Corto", emailUnico(), "123")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"));

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registro("Sin Arroba", "no-es-email", "Secreta123!")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"));
    }

    @Test
    @DisplayName("Registro: 400 si falta el cuerpo")
    void registroSinCuerpo_badRequest() throws Exception {
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(""))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("MALFORMED_REQUEST"));
    }

    @Test
    @DisplayName("Login: 200 con token, tipo y usuario sin contrasena")
    void login_exitoso_devuelveToken() throws Exception {
        String email = emailUnico();
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registro("Con Token", email, "Secreta123!")))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                Map.of("email", email, "password", "Secreta123!"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.token").isNotEmpty())
                .andExpect(jsonPath("$.data.tipoToken").value("Bearer"))
                .andExpect(jsonPath("$.data.usuario.email").value(email))
                .andExpect(jsonPath("$.data.usuario.rol").value("LECTOR"))
                .andExpect(jsonPath("$.data.usuario.password").doesNotExist());
    }

    @Test
    @DisplayName("Login: 401 con contrasena incorrecta (nunca 500)")
    void login_passwordIncorrecta_noAutorizado() throws Exception {
        String email = emailUnico();
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registro("Credencial", email, "Secreta123!")))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                Map.of("email", email, "password", "OtraClave123!"))))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error").value("INVALID_CREDENTIALS"))
                .andExpect(jsonPath("$.message").value("Credenciales invalidas"));
    }

    @Test
    @DisplayName("Login: 401 si el usuario no existe")
    void login_usuarioInexistente_noAutorizado() throws Exception {
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                Map.of("email", "nadie-" + System.nanoTime() + "@kinal.edu.gt",
                                        "password", "Secreta123!"))))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("INVALID_CREDENTIALS"));
    }

    @Test
    @DisplayName("Login: 400 si falta la contrasena")
    void loginSinPassword_badRequest() throws Exception {
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                Map.of("email", emailUnico()))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"));
    }

    @Test
    @DisplayName("El token emitido permite acceder a rutas protegidas")
    void tokenSirveParaAccederARutaProtegida() throws Exception {
        String email = emailUnico();
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registro("Con Acceso", email, "Secreta123!")))
                .andExpect(status().isCreated());

        MvcResult login = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                Map.of("email", email, "password", "Secreta123!"))))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode json = objectMapper.readTree(login.getResponse().getContentAsString(StandardCharsets.UTF_8));
        String token = json.path("data").path("token").asText();
        assertNotNull(token);

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .get("/api/v1/libros")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        assertFalse(token.isBlank());
    }
}

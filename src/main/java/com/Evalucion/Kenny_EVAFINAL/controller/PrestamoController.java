package com.Evalucion.Kenny_EVAFINAL.controller;

import com.Evalucion.Kenny_EVAFINAL.dto.common.ApiResponse;
import com.Evalucion.Kenny_EVAFINAL.dto.common.PageResponse;
import com.Evalucion.Kenny_EVAFINAL.dto.prestamo.DevolucionResponse;
import com.Evalucion.Kenny_EVAFINAL.dto.prestamo.PrestamoRequest;
import com.Evalucion.Kenny_EVAFINAL.dto.prestamo.PrestamoResponse;
import com.Evalucion.Kenny_EVAFINAL.entity.Usuario;
import com.Evalucion.Kenny_EVAFINAL.service.PrestamoService;
import com.Evalucion.Kenny_EVAFINAL.service.UsuarioService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Prestamos y devoluciones.
 *
 * <p><b>Seguridad:</b> en {@code GET /mis-prestamos} la identidad se toma
 * exclusivamente del {@code SecurityContext} (email del JWT). No se acepta ningun
 * parametro de usuario, de modo que es imposible consultar el historial ajeno
 * aunque el cliente manipule la peticion.</p>
 */
@RestController
@RequestMapping("/api/v1/prestamos")
@RequiredArgsConstructor
public class PrestamoController {

    private final PrestamoService prestamoService;
    private final UsuarioService usuarioService;

    @PostMapping
    public ResponseEntity<ApiResponse<PrestamoResponse>> registrar(@Valid @RequestBody PrestamoRequest request) {
        PrestamoResponse creado = prestamoService.registrar(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Prestamo registrado correctamente", creado));
    }

    @PatchMapping("/{id}/devolucion")
    public ResponseEntity<ApiResponse<DevolucionResponse>> devolver(@PathVariable Long id) {
        DevolucionResponse respuesta = prestamoService.devolver(id);
        return ResponseEntity.ok(ApiResponse.ok("Devolucion registrada correctamente", respuesta));
    }

    @GetMapping("/mis-prestamos")
    public ResponseEntity<ApiResponse<PageResponse<PrestamoResponse>>> misPrestamos(
            @AuthenticationPrincipal UserDetails userDetails,
            @PageableDefault(size = 10, sort = "fechaPrestamo",
                    direction = Sort.Direction.DESC) Pageable pageable) {

        Usuario usuario = usuarioService.obtenerEntidadPorEmail(userDetails.getUsername());
        PageResponse<PrestamoResponse> pagina =
                PageResponse.from(prestamoService.obtenerMisPrestamos(usuario.getId(), pageable));
        return ResponseEntity.ok(ApiResponse.ok("Historial del usuario autenticado", pagina));
    }

    @GetMapping("/atrasados")
    public ResponseEntity<ApiResponse<PageResponse<PrestamoResponse>>> atrasados(
            @PageableDefault(size = 10, sort = "fechaDevolucionEsperada",
                    direction = Sort.Direction.ASC) Pageable pageable) {

        PageResponse<PrestamoResponse> pagina =
                PageResponse.from(prestamoService.obtenerAtrasados(pageable));
        return ResponseEntity.ok(ApiResponse.ok("Prestamos atrasados", pagina));
    }
}
